package com.vocalize.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.vocalize.app.MainActivity
import com.vocalize.app.R
import com.vocalize.app.util.Constants

class VocalizeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, VocalizeWidget::class.java)
                )
                ids.forEach { id ->
                    manager.notifyAppWidgetViewDataChanged(id, R.id.widget_memo_list)
                    updateAppWidget(context, manager, id)
                }
            }
            ACTION_PLAY_MEMO -> {
                val memoId = intent.getStringExtra(Constants.EXTRA_MEMO_ID) ?: return
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_PLAY_MEMO_ID, memoId)
                }
                context.startActivity(openIntent)
            }
            ACTION_OPEN_RECORDER -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_RECORDER, true)
                }
                context.startActivity(openIntent)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.vocalize.app.widget.ACTION_REFRESH"
        const val ACTION_PLAY_MEMO = "com.vocalize.app.widget.ACTION_PLAY_MEMO"
        const val ACTION_OPEN_RECORDER = "com.vocalize.app.widget.ACTION_OPEN_RECORDER"
        const val EXTRA_PLAY_MEMO_ID = "play_memo_id"
        const val EXTRA_OPEN_RECORDER = "open_recorder"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_vocalize)

            // ── Record button ──────────────────────────────────────────────────
            val recordIntent = Intent(context, VocalizeWidget::class.java).apply {
                action = ACTION_OPEN_RECORDER
            }
            val recordPending = PendingIntent.getBroadcast(
                context, 0, recordIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_record_button, recordPending)

            // ── Open app on root tap ───────────────────────────────────────────
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // ── ListView with RemoteViewsService ──────────────────────────────
            val serviceIntent = Intent(context, WidgetListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_memo_list, serviceIntent)
            views.setEmptyView(R.id.widget_memo_list, R.id.widget_empty_text)

            // ── Item click template (broadcasts play action) ──────────────────
            val itemClickIntent = Intent(context, VocalizeWidget::class.java).apply {
                action = ACTION_PLAY_MEMO
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val itemClickPending = PendingIntent.getBroadcast(
                context, appWidgetId, itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_memo_list, itemClickPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_memo_list)
        }

        fun requestWidgetRefresh(context: Context) {
            val intent = Intent(context, VocalizeWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
