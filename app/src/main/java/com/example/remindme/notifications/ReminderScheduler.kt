package com.example.remindme.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.remindme.data.ReminderEntity
import java.util.Date

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (reminder.dueDateTimeMillis <= System.currentTimeMillis()) {
            Toast.makeText(appContext, "Reminder time is in the past", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Not scheduling reminder ${reminder.id}; time is in the past: ${Date(reminder.dueDateTimeMillis)}")
            return
        }

        val pendingIntent = createPendingIntent(reminder)

        try {
            val alarmInfo = AlarmManager.AlarmClockInfo(
                reminder.dueDateTimeMillis,
                pendingIntent
            )

            alarmManager.setAlarmClock(alarmInfo, pendingIntent)

            Toast.makeText(
                appContext,
                "Reminder scheduled for ${Date(reminder.dueDateTimeMillis)}",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "Scheduled reminder ${reminder.id} for ${Date(reminder.dueDateTimeMillis)}")
        } catch (securityException: SecurityException) {
            Log.e(TAG, "setAlarmClock failed; falling back to non-exact alarm", securityException)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
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

            Toast.makeText(
                appContext,
                "Reminder scheduled with fallback alarm",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun cancel(reminder: ReminderEntity) {
        alarmManager.cancel(createPendingIntent(reminder))
        Log.d(TAG, "Cancelled reminder ${reminder.id}")
    }

    private fun createPendingIntent(reminder: ReminderEntity): PendingIntent {
        val intent = Intent(appContext, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderNotificationReceiver.EXTRA_REMINDER_TITLE, reminder.title)
        }

        return PendingIntent.getBroadcast(
            appContext,
            reminder.id,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val ACTION_SHOW_REMINDER = "com.example.remindme.SHOW_REMINDER"
    }
}