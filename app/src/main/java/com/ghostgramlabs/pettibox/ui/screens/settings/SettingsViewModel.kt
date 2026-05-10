package com.ghostgramlabs.pettibox.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ghostgramlabs.pettibox.data.ocr.OcrWorker
import com.ghostgramlabs.pettibox.data.ocr.PdfTextWorker
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SaveRepository,
    private val ocrPreferences: OcrPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val autoScanOcr: Flow<Boolean> = ocrPreferences.autoScan

    suspend fun exportBackupJson(): String = repo.exportBackupJson()

    suspend fun setAutoScanOcr(enabled: Boolean) {
        ocrPreferences.setAutoScan(enabled)
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
