package com.ghostgramlabs.pettibox.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ghostgramlabs.pettibox.data.backup.LocalBackupWorker
import com.ghostgramlabs.pettibox.data.ocr.OcrWorker
import com.ghostgramlabs.pettibox.data.ocr.PdfTextWorker
import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import com.ghostgramlabs.pettibox.data.preferences.LocalBackupStatus
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.data.util.LocalBackupStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
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
        localBackupStore.pruneOldBackups()
        backupPreferences.recordLocalBackup(file.name, file.lastModified())
        return file to result
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
