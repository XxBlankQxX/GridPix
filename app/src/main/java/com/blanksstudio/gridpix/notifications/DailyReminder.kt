package com.blanksstudio.gridpix.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.blanksstudio.gridpix.MainActivity
import com.blanksstudio.gridpix.R
import com.blanksstudio.gridpix.data.PlayerProgressRepository
import com.blanksstudio.gridpix.data.settings.SettingsRepository
import com.blanksstudio.gridpix.util.LocalDates
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Opt-in daily puzzle reminder (decision D27). WorkManager runs [DailyReminderWorker] once a day at
 * the chosen hour; it only notifies when today's daily puzzle is not solved yet. Nothing leaves the phone.
 */
object DailyReminder {
    private const val WORK_NAME = "daily_reminder"
    const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1001

    /** Schedules (or cancels) the reminder to match the current settings. Safe to call often. */
    fun sync(context: Context, enabled: Boolean, hour: Int) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(hour, ZonedDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // Re-enqueue so the first run lines up with the chosen hour again (24h periods drift slightly).
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    /** Time from [now] until the next [hour]:00 local time (today if still ahead, otherwise tomorrow). */
    fun delayUntil(hour: Int, now: ZonedDateTime): Duration {
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    internal fun show(context: Context, streak: Int) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.reminder_channel_desc)
            },
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (streak > 0) context.getString(R.string.reminder_text_streak, streak) else context.getString(R.string.reminder_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call: nothing to do.
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyReminderEntryPoint {
    fun settings(): SettingsRepository
    fun playerProgress(): PlayerProgressRepository
}

class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, DailyReminderEntryPoint::class.java)
        val settings = deps.settings().settings.first()
        if (!settings.reminderEnabled) return Result.success()
        val progress = deps.playerProgress().current()
        if (LocalDate.parse(LocalDates.today()) in progress.dailyDays) return Result.success() // already played today
        DailyReminder.show(applicationContext, progress.currentStreak)
        return Result.success()
    }
}
