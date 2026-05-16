package com.ghostgramlabs.pettibox.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.ghostgramlabs.pettibox.data.repository.SaveRepository

/**
 * Schedules wall-clock reminders via [AlarmManager]. The prior
 * WorkManager-based implementation was subject to Doze, App Standby, and
 * the OS battery optimizer — a "remind me tonight at 9 PM" could fire
 * the next morning. AlarmManager with `setExactAndAllowWhileIdle` (or
 * `setAndAllowWhileIdle` when the user hasn't granted exact-alarm
 * permission on Android 12+) is the right mechanism for user-visible,
 * time-critical notifications.
 *
 * Each item gets one pending intent keyed off its row id, so scheduling
 * twice for the same item replaces the prior alarm rather than
 * duplicating it.
 */
object ReminderScheduler {
    const val ACTION_FIRE = "com.ghostgramlabs.pettibox.action.REMINDER_FIRE"
    const val EXTRA_ITEM_ID = "pettibox.reminder.itemId"
    const val EXTRA_EXPECTED_AT = "pettibox.reminder.expectedAt"

    private fun pendingIntent(ctx: Context, itemId: Long, expectedAt: Long, mutableFlag: Int = 0): PendingIntent {
        val intent = Intent(ctx, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_EXPECTED_AT, expectedAt)
        }
        return PendingIntent.getBroadcast(
            ctx,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or mutableFlag
        )
    }

    fun schedule(ctx: Context, itemId: Long, remindAtMillis: Long) {
        val am = ContextCompat.getSystemService(ctx, AlarmManager::class.java) ?: return
        // Cancel any prior pending intent for this item so re-scheduling
        // (user changes their mind, re-snooze, reboot reconciliation) is
        // idempotent rather than leaving stale alarms around.
        cancel(ctx, itemId)
        val pi = pendingIntent(ctx, itemId, remindAtMillis)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        runCatching {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pi)
            } else {
                // User hasn't granted SCHEDULE_EXACT_ALARM. setAndAllowWhileIdle
                // still beats WorkManager — it fires during Doze, just with a
                // small window of latency. We don't auto-route to the system
                // settings page; that's intrusive and the user can opt in
                // when they hit a missed-by-a-minute reminder.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pi)
            }
        }
    }

    fun cancel(ctx: Context, itemId: Long) {
        val am = ContextCompat.getSystemService(ctx, AlarmManager::class.java) ?: return
        val intent = Intent(ctx, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /**
     * Re-arms every pending reminder. Called on cold start (the existing
     * PettiBoxApp reconciliation) and on BOOT_COMPLETED. Past-due
     * reminders are scheduled at `now` so we fire them immediately —
     * losing them entirely because the phone was off when they were due
     * would be a worse experience than firing them late.
     */
    suspend fun rescheduleAll(ctx: Context, repository: SaveRepository) {
        val pending = repository.dueOrPendingReminders()
        if (pending.isEmpty()) return
        val now = System.currentTimeMillis()
        pending.forEach { item ->
            val at = item.remindAt ?: return@forEach
            schedule(ctx, item.id, if (at < now) now + 1_500L else at)
        }
    }
}
