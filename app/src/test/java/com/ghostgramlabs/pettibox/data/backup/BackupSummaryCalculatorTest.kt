package com.ghostgramlabs.pettibox.data.backup

import com.ghostgramlabs.pettibox.data.local.AttachmentEntity
import com.ghostgramlabs.pettibox.data.local.ItemTagCrossRef
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.local.TagEntity
import com.ghostgramlabs.pettibox.domain.model.ContentType
import com.ghostgramlabs.pettibox.domain.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSummaryCalculatorTest {
    @Test
    fun summarizeCountsBackupConfidenceFields() {
        val saves = listOf(
            save(id = 1, favorite = true, archived = true, remindAt = 1000),
            save(id = 2, pinned = true)
        )
        val attachments = listOf(AttachmentEntity(itemId = 1, uri = "file://one.jpg", kind = "image"))
        val tags = listOf(TagEntity(id = 10, name = "recipe"))
        val links = listOf(ItemTagCrossRef(itemId = 1, tagId = 10))

        val summary = BackupSummaryCalculator.summarize(saves, attachments, tags, links)

        assertEquals(2, summary.getInt("saves"))
        assertEquals(1, summary.getInt("attachments"))
        assertEquals(1, summary.getInt("favorites"))
        assertEquals(1, summary.getInt("pinned"))
        assertEquals(1, summary.getInt("archived"))
        assertEquals(1, summary.getInt("reminders"))
        assertEquals(1, summary.getInt("tags"))
        assertEquals(1, summary.getInt("tagLinks"))
    }

    private fun save(
        id: Long,
        favorite: Boolean = false,
        pinned: Boolean = false,
        archived: Boolean = false,
        remindAt: Long? = null
    ) = SaveItemEntity(
        id = id,
        title = "Save $id",
        contentType = ContentType.NOTE.name,
        sourceApp = SourceApp.UNKNOWN.name,
        isFavorite = favorite,
        isPinned = pinned,
        isArchived = archived,
        remindAt = remindAt
    )
}
