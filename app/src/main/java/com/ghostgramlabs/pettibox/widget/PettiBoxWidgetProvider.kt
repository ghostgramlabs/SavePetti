package com.ghostgramlabs.pettibox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.ghostgramlabs.pettibox.MainActivity
import com.ghostgramlabs.pettibox.R
import com.ghostgramlabs.pettibox.data.local.SaveItemEntity
import com.ghostgramlabs.pettibox.data.reminders.ReminderAlarmReceiver
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.domain.model.ContentType
import com.ghostgramlabs.pettibox.ui.screens.save.ShareReceiverActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home-screen widget showing the three most recent saves plus a one-tap
 * "Quick save" button.
 *
 * - Each save row deep-links into Detail using the same EXTRA_OPEN_ITEM_ID
 *   that reminder taps use.
 * - Quick save launches [ShareReceiverActivity] in its empty/manual mode, so
 *   it reuses the existing manual-note Save sheet without any nav plumbing.
 *
 * Data is read on each update broadcast (the app pokes us via [refresh] after
 * a save) rather than observed, which keeps the widget free of a long-lived
 * Flow subscription.
 */
@AndroidEntryPoint
class PettiBoxWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var repository: SaveRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val recent = runCatching { repository.recentForWidget(3) }.getOrDefault(emptyList())
                appWidgetIds.forEach { id -> renderWidget(appContext, appWidgetManager, id, recent) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun renderWidget(
        ctx: Context,
        mgr: AppWidgetManager,
        widgetId: Int,
        recent: List<SaveItemEntity>
    ) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_pettibox)

        views.setOnClickPendingIntent(R.id.widget_quick_save, quickSaveIntent(ctx))
        views.setOnClickPendingIntent(R.id.widget_header, openAppIntent(ctx))

        val rowIds = intArrayOf(R.id.widget_row_0, R.id.widget_row_1, R.id.widget_row_2)
        val emojiIds = intArrayOf(R.id.widget_emoji_0, R.id.widget_emoji_1, R.id.widget_emoji_2)
        val titleIds = intArrayOf(R.id.widget_title_0, R.id.widget_title_1, R.id.widget_title_2)

        rowIds.forEachIndexed { i, rowId ->
            val item = recent.getOrNull(i)
            if (item == null) {
                views.setViewVisibility(rowId, View.GONE)
            } else {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(emojiIds[i], emojiFor(item))
                views.setTextViewText(titleIds[i], item.title.ifBlank { "Untitled" })
                views.setOnClickPendingIntent(rowId, openItemIntent(ctx, item.id))
            }
        }
        views.setViewVisibility(R.id.widget_empty, if (recent.isEmpty()) View.VISIBLE else View.GONE)

        mgr.updateAppWidget(widgetId, views)
    }

    private fun openItemIntent(ctx: Context, itemId: Long): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ReminderAlarmReceiver.EXTRA_OPEN_ITEM_ID, itemId)
            // Unique data per item so the PendingIntents don't collapse.
            data = Uri.parse("pettibox://widget/item/$itemId")
        }
        return PendingIntent.getActivity(
            ctx, itemId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun quickSaveIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, ShareReceiverActivity::class.java).apply {
            putExtra(ShareReceiverActivity.EXTRA_QUICK_SAVE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("pettibox://widget/quicksave")
        }
        return PendingIntent.getActivity(
            ctx, REQ_QUICK_SAVE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data = Uri.parse("pettibox://widget/open")
        }
        return PendingIntent.getActivity(
            ctx, REQ_OPEN_APP, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun emojiFor(item: SaveItemEntity): String =
        when (runCatching { ContentType.valueOf(item.contentType) }.getOrDefault(ContentType.NOTE)) {
            ContentType.LINK -> "🔗"   // 🔗
            ContentType.IMAGE -> "📷"  // 📷
            ContentType.PDF -> "📄"    // 📄
            ContentType.FILE -> "📎"   // 📎
            ContentType.TEXT, ContentType.NOTE -> "📝" // 📝
        }

    companion object {
        private const val REQ_QUICK_SAVE = -100
        private const val REQ_OPEN_APP = -101

        /**
         * Re-render every placed widget. Called after a save so the recent
         * list stays current without relying on the system's (≥30 min)
         * periodic update.
         */
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            val ids = mgr.getAppWidgetIds(ComponentName(context, PettiBoxWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, PettiBoxWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
