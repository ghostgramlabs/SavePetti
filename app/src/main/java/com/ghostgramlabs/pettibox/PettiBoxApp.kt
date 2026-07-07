package com.ghostgramlabs.pettibox

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ghostgramlabs.pettibox.data.backup.LocalBackupWorker
import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import com.ghostgramlabs.pettibox.data.preferences.OnboardingPreferences
import com.ghostgramlabs.pettibox.data.reminders.ReminderNotifications
import com.ghostgramlabs.pettibox.data.reminders.ReminderScheduler
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
    @Inject lateinit var onboardingPreferences: OnboardingPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Register the notification channel up-front. Idempotent — the OS
        // ignores duplicate registrations after the first one took effect.
        ReminderNotifications.ensureChannel(this)
        // Seed starter collections once per install. Users can rename and
        // delete starters now, so seeding must never re-run — the flag,
        // not the table contents, decides.
        appScope.launch {
            if (!onboardingPreferences.categoriesSeeded.first()) {
                repository.seedDefaultCategories()
                onboardingPreferences.markCategoriesSeeded()
            }
        }
        // Sweep rows that were mid-flight in the "Delete with Undo"
        // staging when the process died (force-stop, OS kill, user
        // closed PettiBox during the Undo snackbar). They're invisible
        // anyway because every listing query filters
        // is_pending_delete = 0; without this sweep they'd accumulate
        // in the DB forever with their attachment files orphaned.
        appScope.launch { repository.sweepOrphanedPendingDeletes() }
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
        // Re-arm any reminder alarms that should still fire. AlarmManager
        // alarms do not survive a reboot or a data wipe; the BOOT_COMPLETED
        // receiver handles boot, this handles cold-start after a data
        // clear or fresh install. Past-due reminders fire immediately so
        // the user doesn't lose them entirely.
        appScope.launch {
            ReminderScheduler.rescheduleAll(this@PettiBoxApp, repository)
        }
    }
}
