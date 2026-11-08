package com.jesse.updatedmorningandevening

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import java.time.LocalDateTime

class VerseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_CONFIGURATION_CHANGED,
            )
        ) {
            updateAll(context)
        }
    }

    companion object {
        fun isPinned(ctx: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, VerseWidgetProvider::class.java))
            return ids.isNotEmpty()
        }

        fun requestPin(ctx: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(ctx, VerseWidgetProvider::class.java)
            return mgr.requestPinAppWidget(provider, null, null)
        }

        fun updateAll(ctx: Context) {
            val appCtx = ctx.applicationContext
            val mgr = AppWidgetManager.getInstance(appCtx)
            val ids = mgr.getAppWidgetIds(ComponentName(appCtx, VerseWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(appCtx, mgr, ids)
            }
        }

        private fun updateWidgets(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val settings = ReaderSettings(ctx)
            val now = LocalDateTime.now()
            val page = settings.currentPage(now)
            val dayIdx = page / 2
            val evening = page % 2 == 1
            val (month, day) = Cal.monthDay(dayIdx)

            val sysDark = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            settings.systemIsDark = sysDark
            val theme = settings.rawThemeFor(evening)

            val section = runCatching {
                val raw = ctx.assets.open(settings.edition.asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
                EditionData(JSONObject(raw)).get(month, day, evening)
            }.getOrNull()

            val bgRes = when (theme) {
                ReaderTheme.Light -> R.drawable.widget_bg_light
                ReaderTheme.Sepia -> R.drawable.widget_bg_sepia
                ReaderTheme.Dark -> R.drawable.widget_bg_dark
                ReaderTheme.Black -> R.drawable.widget_bg_black
            }

            val timeLabel = if (evening) "Evening" else "Morning"
            val header = "${Cal.title(dayIdx)} · $timeLabel".uppercase()
            val cleanVerse = section?.verse?.replace(Regex("</?[ib]>"), "") ?: "Open to read today's devotion."
            val refText = if (section != null && section.ref.isNotBlank()) "— ${section.ref}" else ""

            val launchIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Reminders.EXTRA_EVENING, evening)
            }
            val pi = PendingIntent.getActivity(
                ctx,
                5001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            for (id in ids) {
                val views = RemoteViews(ctx.packageName, R.layout.verse_widget).apply {
                    setInt(R.id.widget_root, "setBackgroundResource", bgRes)
                    setTextViewText(R.id.widget_header, header)
                    setTextColor(R.id.widget_header, theme.accent.toArgb())
                    setTextViewText(R.id.widget_edition, settings.edition.label)
                    setTextColor(R.id.widget_edition, theme.muted.toArgb())
                    setTextViewText(R.id.widget_verse, cleanVerse)
                    setTextColor(R.id.widget_verse, theme.fg.toArgb())
                    setTextViewText(R.id.widget_ref, refText)
                    setTextColor(R.id.widget_ref, theme.muted.toArgb())
                    setOnClickPendingIntent(R.id.widget_root, pi)
                }
                mgr.updateAppWidget(id, views)
            }
        }
    }
}
