package com.ghostgramlabs.pettibox.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgramlabs.pettibox.data.backup.LocalBackupWorker
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.ocr.OcrWorker
import com.ghostgramlabs.pettibox.data.ocr.PdfTextWorker
import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import com.ghostgramlabs.pettibox.data.preferences.LocalBackupStatus
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.data.util.LocalBackupStore
import com.ghostgramlabs.pettibox.ui.components.NewCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SaveRepository,
    private val ocrPreferences: OcrPreferences,
    private val backupPreferences: BackupPreferences,
    private val localBackupStore: LocalBackupStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val autoScanOcr: Flow<Boolean> = ocrPreferences.autoScan
    val pdfPageLimit: Flow<Int> = ocrPreferences.pdfPageLimit
    val localBackupStatus: Flow<LocalBackupStatus> = backupPreferences.status
    /** Live category list — drives both the count label and conflict checks. */
    val collections: Flow<List<CategoryEntity>> = repo.observeCategories()

    /**
     * Create a collection from Settings. Mirrors [SaveSheetViewModel]'s
     * createCollection but without the auto-select-and-save side-effect —
     * the user is in Settings, not mid-save.
     *
     * The callback returns the new id so the UI can flash a confirmation
     * containing the collection's name and emoji.
     */
    fun createCollection(nc: NewCollection, onCreated: ((CategoryEntity) -> Unit)? = null) =
        viewModelScope.launch {
            val existing = repo.observeCategories().first()
            val id = "user_" + UUID.randomUUID().toString().take(8)
            val maxOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val entity = CategoryEntity(
                id = id,
                name = nc.name,
                emoji = nc.emoji,
                colorHex = nc.colorHex,
                sortOrder = maxOrder,
                userCreated = true
            )
            repo.upsertCategory(entity)
            onCreated?.invoke(entity)
        }

    /**
     * Update an existing user-created collection. Built-in collections
     * are intentionally locked here so the Settings flow can't bypass
     * the same guard the Browse flow already enforces in
     * [CategoriesViewModel.updateSelectedCategory]. Silently no-ops on
     * built-ins rather than throwing — the calling UI should already
     * prevent the user from reaching this for non-user-created rows.
     */
    fun updateCollection(
        category: CategoryEntity,
        name: String,
        emoji: String,
        colorHex: Long,
        onDone: ((CategoryEntity) -> Unit)? = null
    ) = viewModelScope.launch {
        if (!category.userCreated || name.isBlank()) return@launch
        val updated = category.copy(
            name = name.trim().take(28),
            emoji = emoji,
            colorHex = colorHex
        )
        repo.upsertCategory(updated)
        onDone?.invoke(updated)
    }

    /**
     * Delete a user-created collection. Saves inside it stay in the
     * library (their category_id becomes null) — same behaviour as the
     * Browse-side delete dialog.
     */
    fun deleteCollection(category: CategoryEntity, onDone: (() -> Unit)? = null) =
        viewModelScope.launch {
            if (!category.userCreated) return@launch
            repo.deleteCategory(category.id)
            onDone?.invoke()
        }

    suspend fun exportBackupJson(): String = repo.exportBackupJson()

    suspend fun exportBackupZipFile(): Pair<File, SaveRepository.BackupExportResult> {
        val file = File(appContext.cacheDir, "pettibox-backup-${System.currentTimeMillis()}.zip")
        val result = repo.exportBackupZip(file)
        return file to result
    }

    suspend fun importBackupJson(json: String): SaveRepository.BackupImportResult =
        repo.importBackupJson(json)

    suspend fun createLocalBackupNow(): Pair<File, SaveRepository.BackupExportResult> {
        val file = localBackupStore.createBackupFile()
        val result = repo.exportBackupZip(file)
        val status = backupPreferences.status.first()
        localBackupStore.copyToPickedFolder(file, status.folderUri)
        localBackupStore.pruneOldBackups()
        backupPreferences.recordLocalBackup(file.name, file.lastModified())
        return file to result
    }

    suspend fun setLocalBackupFolder(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        backupPreferences.setBackupFolder(uri.toString())
    }

    suspend fun setAutoLocalBackup(enabled: Boolean) {
        backupPreferences.setAutoLocalBackup(enabled)
        if (enabled) {
            LocalBackupWorker.schedule(appContext)
        } else {
            LocalBackupWorker.cancel(appContext)
        }
    }

    fun localBackupLocationLabel(): String = localBackupStore.backupLocationLabel()

    fun localBackupPath(): String = localBackupStore.backupPath()

    suspend fun importBackupUri(uri: Uri): SaveRepository.BackupImportResult {
        val name = appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }.orEmpty()
        val mimeType = appContext.contentResolver.getType(uri).orEmpty()
        val isZip = name.endsWith(".zip", ignoreCase = true) ||
            mimeType.equals("application/zip", ignoreCase = true) ||
            mimeType.equals("application/x-zip-compressed", ignoreCase = true)
        return appContext.contentResolver.openInputStream(uri)?.use { input ->
            if (isZip) {
                repo.importBackupZip(input)
            } else {
                repo.importBackupJson(input.bufferedReader().use { it.readText() })
            }
        } ?: error("Could not open backup")
    }

    suspend fun setAutoScanOcr(enabled: Boolean) {
        ocrPreferences.setAutoScan(enabled)
    }

    suspend fun setPdfPageLimit(limit: Int) {
        ocrPreferences.setPdfPageLimit(limit)
    }

    suspend fun scanExistingSaves(): Int {
        var count = 0
        repo.imageItemsNeedingOcr().forEach { item ->
            val uri = item.localUri ?: return@forEach
            OcrWorker.enqueueForItem(appContext, item.id, uri)
            count++
        }
        repo.imageAttachmentsNeedingOcr().forEach { attachment ->
            OcrWorker.enqueueForAttachment(appContext, attachment.itemId, attachment.id, attachment.uri)
            count++
        }
        repo.pdfItemsNeedingOcr().forEach { item ->
            val uri = item.localUri ?: return@forEach
            PdfTextWorker.enqueue(appContext, item.id, uri)
            count++
        }
        repo.pdfAttachmentsNeedingOcr().forEach { attachment ->
            PdfTextWorker.enqueue(appContext, attachment.itemId, attachment.uri)
            count++
        }
        return count
    }
}
