package com.vocalize.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.vocalize.app.R
import com.vocalize.app.data.local.AppDatabase
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.util.Constants
import com.vocalize.app.util.Utils
import kotlinx.coroutines.runBlocking

class WidgetListService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetMemoListFactory(applicationContext)
    }
}

class WidgetMemoListFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var memos: List<MemoEntity> = emptyList()
    private var categoryColors: Map<String, String> = emptyMap()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        runBlocking {
            val db = AppDatabase.getDatabase(context)
            memos = db.memoDao().getRecentMemosSync(10)
            val categories = db.categoryDao().getAllCategoriesSync()
            categoryColors = categories.associate { it.id to it.colorHex }
        }
    }

    override fun onDestroy() {
        memos = emptyList()
    }

    override fun getCount(): Int = memos.size

    override fun getViewAt(position: Int): RemoteViews {
        val memo = memos.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.widget_memo_item)

        return RemoteViews(context.packageName, R.layout.widget_memo_item).apply {
            setTextViewText(R.id.widget_item_title, memo.title.ifBlank { Utils.autoTitle(memo.duration) })
            setTextViewText(
                R.id.widget_item_subtitle,
                "${Utils.formatDuration(memo.duration)} · ${Utils.formatTimestamp(memo.dateCreated)}"
            )

            val colorHex = memo.categoryId?.let { categoryColors[it] }
            if (colorHex != null) {
                try {
                    setInt(R.id.widget_item_color_dot, "setBackgroundColor", Color.parseColor(colorHex))
                } catch (_: Exception) {}
            } else {
                setInt(R.id.widget_item_color_dot, "setBackgroundColor", Color.parseColor("#6B7280"))
            }

            val fillInIntent = Intent().apply {
                putExtra(Constants.EXTRA_MEMO_ID, memo.id)
            }
            setOnClickFillInIntent(R.id.widget_item_play, fillInIntent)
            setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_memo_item).apply {
            setTextViewText(R.id.widget_item_title, "Loading…")
            setTextViewText(R.id.widget_item_subtitle, "")
        }
    }

    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = memos.getOrNull(position)?.dateCreated ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
