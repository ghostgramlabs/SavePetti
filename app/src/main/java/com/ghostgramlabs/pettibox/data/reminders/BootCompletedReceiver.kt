package com.ghostgramlabs.pettibox.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms every pending reminder on device boot.
 *
 * AlarmManager alarms do not survive a reboot — without this receiver
 * any reminder the user set before turning their phone off would simply
 * never fire. PettiBoxApp.onCreate also reconciles on cold start, but
 * that runs only when the user opens the app; this receiver covers the
 * window between boot and the first launch.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: SaveRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        // Some OEM packagers send a quickboot-style action instead of
        // (or in addition to) BOOT_COMPLETED. Cover both rather than
        // discovering a missed reminder on a specific phone in the field.
        val isBoot = action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.TIME_SET" ||
            action == "android.intent.action.TIMEZONE_CHANGED"
        if (!isBoot) return

        val appContext = context.applicationContext
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                ReminderScheduler.rescheduleAll(appContext, repository)
            } finally {
                pending.finish()
            }
        }
    }
}
