package com.ghostgramlabs.pettibox.data.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ghostgramlabs.pettibox.MainActivity
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * One worker per active reminder. Scheduled with the exact delay between
 * "now" and the user's requested wall-clock time. When it fires it:
 *   1. Verifies the item still has the same remindAt (so a user-cancel
 *      via the UI takes priority over a stale worker that's already in
 *      flight).
 *   2. Posts a notification on the reminders channel.
 *   3. Clears remind_at on the row so the bell glyph disappears from the
 *      card and re-snoozing always starts from a clean state.
 *
 * Keyed by a deterministic unique work name so scheduling, cancelling,
 * and rescheduling for the same item are all idempotent through WorkManager.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val repository: SaveRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId <= 0L) return Result.success()
        val expectedAt = inputData.getLong(KEY_REMIND_AT, 0L)

        val item = repository.getById(itemId) ?: return Result.success()
        // Item was edited (re-snoozed, cancelled, archived) since we were
        // scheduled. Defer to the latest state — a fresh worker will have
        // been enqueued for the new time, or none if the user cancelled.
        if (item.remindAt == null || (expectedAt > 0L && item.remindAt != expectedAt)) {
            return Result.success()
        }
        if (item.isArchived) {
            repository.setRemindAt(itemId, null)
            return Result.success()
        }

        ReminderNotifications.ensureChannel(ctx)
        postNotification(itemId, item.title.ifBlank { "Saved item" })
        // Clear after posting so the card glyph disappears and the user
        // can pick a new time from scratch.
        repository.setRemindAt(itemId, null)
        return Result.success()
    }

    private fun postNotification(itemId: Long, title: String) {
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val pending = PendingIntent.getActivity(
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
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(itemId.toInt(), notif)
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "pettibox.reminder.itemId"

        private const val KEY_ITEM_ID = "itemId"
        private const val KEY_REMIND_AT = "remindAt"

        private fun workName(itemId: Long): String = "pettibox_reminder_$itemId"

        fun schedule(ctx: Context, itemId: Long, remindAtMillis: Long) {
            val delay = (remindAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        KEY_ITEM_ID to itemId,
                        KEY_REMIND_AT to remindAtMillis
                    )
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                workName(itemId),
                ExistingWorkPolicy.REPLACE,
                req
            )
        }

        fun cancel(ctx: Context, itemId: Long) {
            WorkManager.getInstance(ctx).cancelUniqueWork(workName(itemId))
        }

        @Suppress("unused")
        private val _keepDataReference = Data.EMPTY
    }
}
