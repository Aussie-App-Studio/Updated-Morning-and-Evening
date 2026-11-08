package com.jesse.updatedmorningandevening

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL_ID = "daily_readings"
    const val ACTION_REMIND = "com.jesse.updatedmorningandevening.ACTION_REMIND"
    const val EXTRA_EVENING = "evening"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Devotional Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning and evening reading reminders"
        }
        nm.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun scheduleAll(ctx: Context, s: ReaderSettings) {
        ensureChannel(ctx)
        scheduleOne(ctx, evening = false, enabled = s.morningReminder, hour = s.morningHour, minute = s.morningMinute)
        scheduleOne(ctx, evening = true, enabled = s.eveningReminder, hour = s.eveningHour, minute = s.eveningMinute)
    }

    private fun scheduleOne(ctx: Context, evening: Boolean, enabled: Boolean, hour: Int, minute: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(ctx, evening)
        am.cancel(pi)
        if (!enabled) return

        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAtMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Use a 10-minute window alarm so no restricted exact-alarm permission is needed,
        // and reschedule the next day's alarm each time it fires.
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 10 * 60_000L, pi)
    }

    private fun pendingIntent(ctx: Context, evening: Boolean): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_EVENING, evening)
        }
        val reqCode = if (evening) 2002 else 1001
        return PendingIntent.getBroadcast(
            ctx,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showNotification(ctx: Context, evening: Boolean) {
        if (!hasNotificationPermission(ctx)) return
        ensureChannel(ctx)
        val settings = ReaderSettings(ctx)
        val dayIndex = settings.currentPage() / 2
        val (month, day) = Cal.monthDay(dayIndex)
        val section = runCatching {
            val raw = ctx.assets.open(settings.edition.asset).bufferedReader(Charsets.UTF_8).use { it.readText() }
            EditionData(JSONObject(raw)).get(month, day, evening)
        }.getOrNull()

        val timeLabel = if (evening) "Evening" else "Morning"
        val title = "$timeLabel Reading · ${Cal.title(dayIndex)}"
        val cleanVerse = section?.verse?.replace(Regex("</?[ib]>"), "") ?: "Your daily reading is ready."
        val body = if (section != null && section.ref.isNotBlank()) {
            "$cleanVerse — ${section.ref}"
        } else {
            cleanVerse
        }

        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENING, evening)
        }
        val contentPi = PendingIntent.getActivity(
            ctx,
            if (evening) 3002 else 3001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(if (evening) 4002 else 4001, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = ReaderSettings(context)
        if (intent?.action == Reminders.ACTION_REMIND) {
            val evening = intent.getBooleanExtra(Reminders.EXTRA_EVENING, false)
            Reminders.showNotification(context, evening)
        }
        // Always reschedule upcoming daily alarms (handles boot, time changes, and next-day rollover).
        Reminders.scheduleAll(context, settings)
        VerseWidgetProvider.updateAll(context)
    }
}
