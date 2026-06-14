package com.example.subscription_manager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import com.example.subscription_manager.data.preferences.UserPreferencesStore
import com.example.subscription_manager.domain.model.ReminderTime
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesStore: UserPreferencesStore,
    private val repository: SubscriptionRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleSubscription(subscription: Subscription) {
        rescheduleAllReminders()
    }

    suspend fun scheduleSubscription(id: Long) {
        val subscription = repository.subscription(id).first() ?: return
        scheduleSubscription(subscription)
    }

    suspend fun rescheduleAllReminders() {
        cancelLegacyWorkManagerReminders()

        val reminderTime = preferencesStore.reminderTimeFlow.first()
        Log.d(TAG, "Rescheduling reminder alarm for reminderTime=$reminderTime")
        scheduleReminderAlarm(reminderTime)
    }

    fun scheduleReminderAlarm(reminderTime: ReminderTime) {
        cancelReminderAlarm()

        val triggerAtMillis = nextTriggerAtMillis(reminderTime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission is not granted; alarm may be treated as inexact")
        }

        alarmManager.setExactAlarm(
            type = AlarmManager.RTC_WAKEUP,
            triggerAtMillis = triggerAtMillis,
            operation = reminderPendingIntent()
        )
        Log.d(TAG, "Scheduled reminder alarm for triggerAtMillis=$triggerAtMillis")
    }

    fun cancelReminderAlarm() {
        alarmManager.cancel(reminderPendingIntent())
    }

    fun cancelSubscriptionReminders(id: Long) {
        // Reminder alarms are shared and driven by the daily reminderTime.
    }

    private fun cancelLegacyWorkManagerReminders() {
        workManager.cancelAllWorkByTag(TAG_REMINDER)
        workManager.cancelUniqueWork(UNIQUE_NOTIFICATION_CHECK)
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAtMillis(reminderTime: ReminderTime): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var trigger = LocalDate.now(zone).atTime(reminderTime.hour, reminderTime.minute)

        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }

        return trigger.atZone(zone).toInstant().toEpochMilli()
    }

    private fun AlarmManager.setExactAlarm(
        type: Int,
        triggerAtMillis: Long,
        operation: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
        } else {
            setExact(type, triggerAtMillis, operation)
        }
    }

    companion object {
        private const val TAG = "NotificationScheduler"
        const val ACTION_REMINDER_ALARM = "com.example.subscription_manager.action.REMINDER_ALARM"
        const val REQUEST_CODE_REMINDER_ALARM = 1001

        const val TAG_REMINDER = "tag_reminder"
        const val TAG_NOTIFICATION_CHECK = "tag_notification_check"
        const val UNIQUE_NOTIFICATION_CHECK = "unique_notification_check"
        const val UNIQUE_RESCHEDULE_ALL = "unique_reschedule_all"
        const val KEY_SUBSCRIPTION_ID = "key_subscription_id"
        const val KEY_CYCLE_KEY = "key_cycle_key"
    }
}
