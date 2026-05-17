package com.example.remindme.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.remindme.data.ReminderEntity

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (reminder.dueDateTimeMillis <= System.currentTimeMillis()) {
            return
        }

        val pendingIntent = createPendingIntent(reminder)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueDateTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueDateTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueDateTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDateTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancel(reminder: ReminderEntity) {
        alarmManager.cancel(createPendingIntent(reminder))
    }

    private fun createPendingIntent(reminder: ReminderEntity): PendingIntent {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_TITLE, reminder.title)
        }

        return PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}