package com.ghostgramlabs.pettibox.data.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ghostgramlabs.pettibox.MainActivity
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by [AlarmManager] when a reminder's wall-clock time arrives.
 *
 * The receiver:
 *   1. Looks up the item and verifies the alarm is still wanted (the
 *      user may have cancelled it, archived the row, or re-snoozed since
 *      we were scheduled).
 *   2. Posts a notification on the reminders channel.
 *   3. Clears remind_at on the row so the bell glyph disappears from
 *      cards and the user can re-snooze from scratch.
 *
 * Uses `goAsync()` + a coroutine because Room calls suspend, but Android
 * lets us live for ~10s after onReceive returns when goAsync is held.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: SaveRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_FIRE) return
        val itemId = intent.getLongExtra(ReminderScheduler.EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return
        val expectedAt = intent.getLongExtra(ReminderScheduler.EXTRA_EXPECTED_AT, 0L)
        val appContext = context.applicationContext
        val pending = goAsync()
        // SupervisorJob so a coroutine failure doesn't tear down a shared
        // scope; receivers must call finish() on the PendingResult
        // exactly once regardless of how the work completed.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                fire(appContext, itemId, expectedAt)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(ctx: Context, itemId: Long, expectedAt: Long) {
        val item = repository.getById(itemId) ?: return
        if (item.remindAt == null) return
        // Stale alarm: the item's remind_at was changed (re-snoozed) since
        // we were scheduled. A newer alarm is already in flight for the
        // current time; do nothing.
        if (expectedAt > 0L && item.remindAt != expectedAt) return
        if (item.isArchived) {
            repository.setRemindAt(itemId, null)
            return
        }
        ReminderNotifications.ensureChannel(ctx)
        postNotification(ctx, itemId, item.title.ifBlank { "Saved item" })
        repository.setRemindAt(itemId, null)
    }

    private fun postNotification(ctx: Context, itemId: Long, title: String) {
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_ITEM_ID, itemId)
        }
        val openPi = PendingIntent.getActivity(
            ctx,
            itemId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("From your shelf")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(itemId.toInt(), notif)
        }
    }

    companion object {
        const val EXTRA_OPEN_ITEM_ID = "pettibox.reminder.itemId"
    }
}
