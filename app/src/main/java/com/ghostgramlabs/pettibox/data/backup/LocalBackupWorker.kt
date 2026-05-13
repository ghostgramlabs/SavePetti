package com.ghostgramlabs.pettibox.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.data.util.LocalBackupStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalBackupWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val repository: SaveRepository,
    private val backupStore: LocalBackupStore,
    private val backupPreferences: BackupPreferences
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result =
        runCatching {
            val file = backupStore.createBackupFile()
            repository.exportBackupZip(file)
            val status = backupPreferences.status.first()
            backupStore.copyToPickedFolder(file, status.folderUri)
            backupStore.pruneOldBackups()
            backupPreferences.recordLocalBackup(file.name, file.lastModified())
            Result.success()
        }.getOrElse {
            Result.retry()
        }

    companion object {
        private const val WORK_NAME = "pettibox_local_auto_backup"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<LocalBackupWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextTwoAm(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        }

        private fun delayUntilNextTwoAm(): Long {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis - now.timeInMillis
        }
    }
}
