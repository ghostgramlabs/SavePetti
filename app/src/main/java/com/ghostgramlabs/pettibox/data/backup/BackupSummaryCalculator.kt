package com.ghostgramlabs.pettibox.data.backup

import com.ghostgramlabs.pettibox.data.local.AttachmentEntity
import com.ghostgramlabs.pettibox.data.local.ItemTagCrossRef
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.local.TagEntity
import org.json.JSONObject

internal object BackupSummaryCalculator {
    fun summarize(
        saves: List<SaveItemEntity>,
        attachments: List<AttachmentEntity>,
        tags: List<TagEntity>,
        itemTags: List<ItemTagCrossRef>
    ): JSONObject = JSONObject()
        .put("saves", saves.size)
        .put("attachments", attachments.size)
        .put("favorites", saves.count { it.isFavorite })
        .put("pinned", saves.count { it.isPinned })
        .put("archived", saves.count { it.isArchived })
        .put("reminders", saves.count { it.remindAt != null })
        .put("tags", tags.size)
        .put("tagLinks", itemTags.size)
}
