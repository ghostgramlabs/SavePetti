package com.ghostgramlabs.pettibox

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ghostgramlabs.pettibox.data.backup.LocalBackupWorker
import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PettiBoxApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repository: SaveRepository
    @Inject lateinit var backupPreferences: BackupPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch { repository.seedCategoriesIfEmpty() }
        // Reconcile the saved backup preference with WorkManager state on
        // every cold start. If the user enabled auto-backup but the
        // WorkManager db got wiped (data clear, fresh install over old
        // install, OS update edge cases), the periodic worker would never
        // run again until they toggled the setting. ExistingPeriodicWorkPolicy
        // .UPDATE makes this idempotent — re-running on every start is
        // cheap and safe.
        appScope.launch {
            val status = backupPreferences.status.first()
            if (status.enabled) {
                LocalBackupWorker.schedule(this@PettiBoxApp)
            } else {
                LocalBackupWorker.cancel(this@PettiBoxApp)
            }
        }
    }
}
