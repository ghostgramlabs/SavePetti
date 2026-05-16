package com.ghostgramlabs.pettibox.data.repository

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.ghostgramlabs.pettibox.data.local.AppDatabase
import com.ghostgramlabs.pettibox.data.local.AttachmentDao
import com.ghostgramlabs.pettibox.data.local.AttachmentEntity
import com.ghostgramlabs.pettibox.data.local.CategoryCount
import com.ghostgramlabs.pettibox.data.local.CategoryDao
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.local.ItemTagCrossRef
import com.ghostgramlabs.pettibox.data.local.SaveDao
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.local.SourceCount
import com.ghostgramlabs.pettibox.data.local.TagDao
import com.ghostgramlabs.pettibox.data.local.TagEntity
import com.ghostgramlabs.pettibox.data.local.TagWithCount
import com.ghostgramlabs.pettibox.data.util.AttachmentStore
import com.ghostgramlabs.pettibox.domain.model.CategoryPalette
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveRepository @Inject constructor(
    private val database: AppDatabase,
    private val saveDao: SaveDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
    private val tagDao: TagDao,
    private val attachmentStore: AttachmentStore
) {

    data class BackupImportResult(
        val categories: Int,
        val saves: Int,
        val attachments: Int,
        val tags: Int
    )

    data class BackupExportResult(
        val saves: Int,
        val attachments: Int,
        val embeddedFiles: Int
    )

    suspend fun seedCategoriesIfEmpty() {
        val defaults = CategoryPalette.Defaults.mapIndexed { i, p ->
            CategoryEntity(
                id = p.id,
                name = p.name,
                emoji = p.emoji,
                colorHex = p.colorHex,
                sortOrder = i
            )
        }
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaults)
        } else {
            defaults.forEach { category ->
                val existing = categoryDao.getById(category.id)
                if (existing == null) {
                    categoryDao.upsert(category)
                } else if (!existing.userCreated) {
                    categoryDao.upsert(category.copy(createdAt = existing.createdAt))
                }
            }
        }
    }

    // ── Save items ────────────────────────────────────────────────────────

    suspend fun insert(item: SaveItemEntity): Long = saveDao.insert(item)
    suspend fun update(item: SaveItemEntity) = saveDao.update(item)
    suspend fun getById(id: Long) = saveDao.getById(id)
    fun observeById(id: Long) = saveDao.observeById(id)

    /**
     * Cleans up local files (attachments + the item's own localUri) before
     * letting the row's CASCADE fire. Without this, files in filesDir/
     * accumulate forever as users delete saves.
     */
    suspend fun delete(id: Long) {
        val uris = attachmentDao.urisForItem(id)
        saveDao.delete(id) // cascades attachments + item_tags
        attachmentStore.deleteByUris(uris)
    }

    fun observeRecent(limit: Int = 20) = saveDao.observeRecent(limit)
    fun observeFavorites() = saveDao.observeFavorites()
    fun observePinned() = saveDao.observePinned()
    suspend fun browseForSearch(): List<SaveItemEntity> = saveDao.browseForSearch()

    // Paged browses for large lists.
    fun pagedAll(includeArchived: Boolean = false): PagingSource<Int, SaveItemEntity> =
        saveDao.pagedAll(includeArchived)
    fun pagedByCategory(
        categoryId: String,
        includeArchived: Boolean = false
    ): PagingSource<Int, SaveItemEntity> =
        saveDao.pagedByCategory(categoryId, includeArchived)
    fun pagedBySource(sourceApp: String): PagingSource<Int, SaveItemEntity> =
        saveDao.pagedBySource(sourceApp)

    fun pagedFavorites(): PagingSource<Int, SaveItemEntity> = saveDao.pagedFavorites()

    fun pagedArchived(): PagingSource<Int, SaveItemEntity> = saveDao.pagedArchived()

    fun pagedByTag(name: String): PagingSource<Int, SaveItemEntity> = saveDao.pagedByTag(name)

    // Aggregates — never load full rows just to count.
    fun observeSourceCounts(): Flow<List<SourceCount>> = saveDao.observeSourceCounts()
    fun observeCategoryCounts(): Flow<List<CategoryCount>> = saveDao.observeCategoryCounts()
    fun observeTotal(): Flow<Int> = saveDao.observeTotal()
    fun observeArchivedTotal(): Flow<Int> = saveDao.observeArchivedTotal()
    fun observeFavoriteTotal(): Flow<Int> = saveDao.observeFavoriteTotal()

    suspend fun setFavorite(id: Long, fav: Boolean) = saveDao.setFavorite(id, fav)
    suspend fun setPinned(id: Long, pin: Boolean) = saveDao.setPinned(id, pin)
    suspend fun setArchived(id: Long, archived: Boolean) = saveDao.setArchived(id, archived)
    suspend fun setRemindAt(id: Long, at: Long?) = saveDao.setRemindAt(id, at)
    suspend fun dueReminders(): List<SaveItemEntity> = saveDao.dueReminders()
    /** Reschedule helper: every item that still has a reminder pointed at it. */
    suspend fun dueOrPendingReminders(): List<SaveItemEntity> = saveDao.pendingReminders()
    suspend fun touchOpened(id: Long) = saveDao.touchOpened(id)
    suspend fun setOcrText(id: Long, text: String) = saveDao.setOcrText(id, text)

    suspend fun imageItemsNeedingOcr(): List<SaveItemEntity> = saveDao.imageItemsNeedingOcr()
    suspend fun pdfItemsNeedingOcr(): List<SaveItemEntity> = saveDao.pdfItemsNeedingOcr()

    suspend fun search(rawQuery: String): List<SaveItemEntity> {
        val q = sanitizeFtsQuery(rawQuery)
        if (q.isBlank()) return emptyList()
        return runCatching { saveDao.search(q) }.getOrDefault(emptyList())
    }

    // ── Categories ────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun upsertCategory(c: CategoryEntity) = categoryDao.upsert(c)
    suspend fun getCategory(id: String) = categoryDao.getById(id)
    suspend fun deleteCategory(id: String) = categoryDao.delete(id)

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
            .put("schema", 1)
            .put("exportedAt", System.currentTimeMillis())
        root.put("categories", JSONArray().apply {
            categoryDao.allForExport().forEach { c ->
                put(JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("emoji", c.emoji)
                    .put("colorHex", c.colorHex)
                    .put("sortOrder", c.sortOrder)
                    .put("parentId", c.parentId)
                    .put("userCreated", c.userCreated)
                    .put("createdAt", c.createdAt))
            }
        })
        root.put("saves", JSONArray().apply {
            saveDao.allForExport().forEach { s ->
                put(JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("url", s.url)
                    .put("localUri", s.localUri)
                    .put("thumbnailUri", s.thumbnailUri)
                    .put("contentType", s.contentType)
                    .put("sourceApp", s.sourceApp)
                    .put("categoryId", s.categoryId)
                    .put("notes", s.notes)
                    .put("ocrText", s.ocrText)
                    .put("metadataJson", s.metadataJson)
                    .put("favorite", s.isFavorite)
                    .put("pinned", s.isPinned)
                    .put("archived", s.isArchived)
                    .put("remindAt", s.remindAt)
                    .put("createdAt", s.createdAt)
                    .put("updatedAt", s.updatedAt)
                    .put("openedAt", s.openedAt))
            }
        })
        root.put("attachments", JSONArray().apply {
            attachmentDao.allForExport().forEach { a ->
                put(JSONObject()
                    .put("id", a.id)
                    .put("itemId", a.itemId)
                    .put("uri", a.uri)
                    .put("kind", a.kind)
                    .put("ocrText", a.ocrText)
                    .put("sortOrder", a.sortOrder)
                    .put("createdAt", a.createdAt))
            }
        })
        root.put("tags", JSONArray().apply {
            tagDao.allForExport().forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("createdAt", t.createdAt))
            }
        })
        root.put("itemTags", JSONArray().apply {
            tagDao.linksForExport().forEach { ref ->
                put(JSONObject().put("itemId", ref.itemId).put("tagId", ref.tagId))
            }
        })
        return root.toString(2)
    }

    suspend fun exportBackupZip(targetFile: File): BackupExportResult {
        val categories = categoryDao.allForExport()
        val saves = saveDao.allForExport()
        val attachments = attachmentDao.allForExport()
        val tags = tagDao.allForExport()
        val itemTags = tagDao.linksForExport()
        var embeddedFiles = 0

        val root = JSONObject()
            .put("schema", 2)
            .put("exportedAt", System.currentTimeMillis())

        targetFile.parentFile?.mkdirs()
        ZipOutputStream(targetFile.outputStream().buffered()).use { zip ->
            root.put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("emoji", c.emoji)
                        .put("colorHex", c.colorHex)
                        .put("sortOrder", c.sortOrder)
                        .put("parentId", c.parentId)
                        .put("userCreated", c.userCreated)
                        .put("createdAt", c.createdAt))
                }
            })

            root.put("saves", JSONArray().apply {
                saves.forEach { s ->
                    val localBackupPath = s.localUri?.let { uri ->
                        val path = "files/save_${s.id}_${safeExt(uri)}"
                        if (attachmentStore.copyUriToZip(uri, zip, path)) {
                            embeddedFiles++
                            path
                        } else null
                    }
                    put(JSONObject()
                        .put("id", s.id)
                        .put("title", s.title)
                        .put("url", s.url)
                        .put("localUri", s.localUri)
                        .put("localBackupPath", localBackupPath)
                        .put("thumbnailUri", s.thumbnailUri)
                        .put("contentType", s.contentType)
                        .put("sourceApp", s.sourceApp)
                        .put("categoryId", s.categoryId)
                        .put("notes", s.notes)
                        .put("ocrText", s.ocrText)
                        .put("metadataJson", s.metadataJson)
                        .put("favorite", s.isFavorite)
                        .put("pinned", s.isPinned)
                        .put("archived", s.isArchived)
                        .put("remindAt", s.remindAt)
                        .put("createdAt", s.createdAt)
                        .put("updatedAt", s.updatedAt)
                        .put("openedAt", s.openedAt))
                }
            })

            root.put("attachments", JSONArray().apply {
                attachments.forEach { a ->
                    val backupPath = a.uri.let { uri ->
                        val path = "files/attachment_${a.id}_${safeExt(uri)}"
                        if (attachmentStore.copyUriToZip(uri, zip, path)) {
                            embeddedFiles++
                            path
                        } else null
                    }
                    put(JSONObject()
                        .put("id", a.id)
                        .put("itemId", a.itemId)
                        .put("uri", a.uri)
                        .put("backupPath", backupPath)
                        .put("kind", a.kind)
                        .put("ocrText", a.ocrText)
                        .put("sortOrder", a.sortOrder)
                        .put("createdAt", a.createdAt))
                }
            })

            root.put("tags", JSONArray().apply {
                tags.forEach { t ->
                    put(JSONObject().put("id", t.id).put("name", t.name).put("createdAt", t.createdAt))
                }
            })
            root.put("itemTags", JSONArray().apply {
                itemTags.forEach { ref ->
                    put(JSONObject().put("itemId", ref.itemId).put("tagId", ref.tagId))
                }
            })

            zip.putNextEntry(java.util.zip.ZipEntry("backup.json"))
            zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        return BackupExportResult(
            saves = saves.size,
            attachments = attachments.size,
            embeddedFiles = embeddedFiles
        )
    }

    suspend fun importBackupZip(input: InputStream): BackupImportResult {
        var backupJson: String? = null
        val fileUrisByPath = mutableMapOf<String, String>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                // Guard against zip-slip: a malicious or corrupted backup
                // with entries like "files/../../../etc/passwd" would
                // otherwise escape the attachments sandbox via the
                // originalName/extension. We only accept entries strictly
                // inside files/ with no parent-traversal segments.
                val safeName = !entry.name.contains("..") && !entry.name.startsWith("/")
                when {
                    safeName && entry.name == "backup.json" -> {
                        backupJson = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    safeName && entry.name.startsWith("files/") -> {
                        attachmentStore.ingestBackupFile(
                            input = NonClosingInputStream(zip),
                            originalName = entry.name.substringAfterLast('/')
                        )?.let { uri ->
                            fileUrisByPath[entry.name] = uri
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val json = backupJson ?: error("Missing backup.json")
        return importBackupJson(json, fileUrisByPath)
    }

    suspend fun importBackupJson(json: String): BackupImportResult =
        importBackupJson(json, emptyMap())

    private suspend fun importBackupJson(
        json: String,
        fileUrisByPath: Map<String, String>
    ): BackupImportResult = database.withTransaction {
        // Wrapping the whole import in a transaction means a mid-flight
        // failure (process death, OOM, malformed entry) rolls everything
        // back — the user retries from the same backup file and doesn't
        // end up with half their library duplicated.
        val root = JSONObject(json)
        val categories = root.optJSONArray("categories") ?: JSONArray()
        val saves = root.optJSONArray("saves") ?: JSONArray()
        val attachments = root.optJSONArray("attachments") ?: JSONArray()
        val tags = root.optJSONArray("tags") ?: JSONArray()
        val itemTags = root.optJSONArray("itemTags") ?: JSONArray()

        var categoryCount = 0
        for (i in 0 until categories.length()) {
            val c = categories.getJSONObject(i)
            val category = CategoryEntity(
                id = c.getString("id"),
                name = c.optString("name", "Imported"),
                emoji = c.optString("emoji", "\uD83D\uDCE6"),
                colorHex = c.optLong("colorHex", 0xFF8B5CF6.toLong()),
                sortOrder = c.optInt("sortOrder", i),
                parentId = c.optNullableString("parentId"),
                userCreated = c.optBoolean("userCreated", true),
                createdAt = c.optLong("createdAt", System.currentTimeMillis())
            )
            categoryDao.upsert(category)
            categoryCount++
        }

        val itemIdMap = mutableMapOf<Long, Long>()
        var saveCount = 0
        for (i in 0 until saves.length()) {
            val s = saves.getJSONObject(i)
            val oldId = s.optLong("id", -1L)
            val imported = SaveItemEntity(
                title = s.optString("title", "Imported save").ifBlank { "Imported save" },
                url = s.optNullableString("url"),
                localUri = s.optNullableString("localBackupPath")?.let { fileUrisByPath[it] }
                    ?: s.optNullableString("localUri"),
                thumbnailUri = s.optNullableString("thumbnailUri"),
                contentType = s.optString("contentType", "NOTE"),
                sourceApp = s.optString("sourceApp", "UNKNOWN"),
                categoryId = s.optNullableString("categoryId"),
                notes = s.optNullableString("notes"),
                ocrText = s.optNullableString("ocrText"),
                metadataJson = s.optNullableString("metadataJson"),
                isFavorite = s.optBoolean("favorite", false),
                isPinned = s.optBoolean("pinned", false),
                isArchived = s.optBoolean("archived", false),
                remindAt = s.optNullableLong("remindAt"),
                createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = s.optLong("updatedAt", System.currentTimeMillis()),
                openedAt = s.optNullableLong("openedAt")
            )
            val newId = saveDao.insert(imported)
            if (oldId > 0) itemIdMap[oldId] = newId
            saveCount++
        }

        var attachmentCount = 0
        val attachmentRows = buildList {
            for (i in 0 until attachments.length()) {
                val a = attachments.getJSONObject(i)
                val newItemId = itemIdMap[a.optLong("itemId", -1L)] ?: continue
                add(
                    AttachmentEntity(
                        itemId = newItemId,
                        uri = a.optNullableString("backupPath")?.let { fileUrisByPath[it] }
                            ?: a.optString("uri"),
                        kind = a.optString("kind", "FILE"),
                        ocrText = a.optNullableString("ocrText"),
                        sortOrder = a.optInt("sortOrder", i),
                        createdAt = a.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }
        if (attachmentRows.isNotEmpty()) {
            attachmentDao.insertAll(attachmentRows)
            attachmentCount = attachmentRows.size
        }

        val tagIdMap = mutableMapOf<Long, Long>()
        var tagCount = 0
        for (i in 0 until tags.length()) {
            val t = tags.getJSONObject(i)
            val name = t.optString("name").trim()
            if (name.isBlank()) continue
            val newId = tagDao.upsert(name)
            if (newId > 0) {
                val oldId = t.optLong("id", -1L)
                if (oldId > 0) tagIdMap[oldId] = newId
                tagCount++
            }
        }

        for (i in 0 until itemTags.length()) {
            val ref = itemTags.getJSONObject(i)
            val newItemId = itemIdMap[ref.optLong("itemId", -1L)] ?: continue
            val newTagId = tagIdMap[ref.optLong("tagId", -1L)] ?: continue
            tagDao.link(ItemTagCrossRef(newItemId, newTagId))
        }

        BackupImportResult(
            categories = categoryCount,
            saves = saveCount,
            attachments = attachmentCount,
            tags = tagCount
        )
    }

    // ── Attachments ───────────────────────────────────────────────────────

    suspend fun insertAttachments(items: List<AttachmentEntity>): List<Long> =
        if (items.isEmpty()) emptyList() else attachmentDao.insertAll(items)

    fun observeAttachments(itemId: Long) = attachmentDao.observeForItem(itemId)
    suspend fun attachmentsFor(itemId: Long) = attachmentDao.forItem(itemId)
    suspend fun imageAttachmentsNeedingOcr(): List<AttachmentEntity> =
        attachmentDao.imageAttachmentsNeedingOcr()
    suspend fun pdfAttachmentsNeedingOcr(): List<AttachmentEntity> =
        attachmentDao.pdfAttachmentsNeedingOcr()
    suspend fun setAttachmentOcr(id: Long, text: String) = attachmentDao.setOcrText(id, text)
    suspend fun deleteAttachment(id: Long) {
        val attachment = attachmentDao.getById(id) ?: return
        attachmentDao.delete(id)
        attachmentStore.deleteByUris(listOf(attachment.uri))
    }

    // ── Tags ──────────────────────────────────────────────────────────────

    suspend fun setTagsForItem(itemId: Long, tagNames: List<String>) {
        tagDao.unlinkAll(itemId)
        for (raw in tagNames) {
            val tagId = tagDao.upsert(raw)
            if (tagId > 0) tagDao.link(ItemTagCrossRef(itemId, tagId))
        }
    }

    suspend fun addTag(itemId: Long, name: String) {
        val tagId = tagDao.upsert(name)
        if (tagId > 0) tagDao.link(ItemTagCrossRef(itemId, tagId))
    }

    suspend fun removeTag(itemId: Long, name: String) {
        val id = tagDao.findIdByName(name) ?: return
        tagDao.unlink(itemId, id)
    }

    fun observeTagsForItem(itemId: Long): Flow<List<TagEntity>> =
        tagDao.observeTagsForItem(itemId)

    fun observeTopTags(limit: Int = 20): Flow<List<TagWithCount>> =
        tagDao.observeTopTags(limit)

    suspend fun itemIdsForTag(name: String): List<Long> = tagDao.itemIdsForTag(name)

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Sanitises raw user input for SQLite's FTS MATCH grammar. Strips control
     * chars, requires tokens of >=2 chars, appends a prefix wildcard so partial
     * words match. Returning a blank string short-circuits the search to an
     * empty result without ever touching the DB.
     */
    private fun sanitizeFtsQuery(input: String): String {
        val cleaned = input.trim().replace("\"", " ").replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }

    private fun safeExt(uri: String): String {
        val clean = uri.substringBefore('?').substringAfterLast('/', uri).substringAfterLast('.', "bin")
        return clean.takeIf { it.length in 1..8 && it.all { ch -> ch.isLetterOrDigit() } } ?: "bin"
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).ifBlank { null }

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (!has(name) || isNull(name)) null else optLong(name)

    private class NonClosingInputStream(
        private val delegate: InputStream
    ) : InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun close() = Unit
    }
}
