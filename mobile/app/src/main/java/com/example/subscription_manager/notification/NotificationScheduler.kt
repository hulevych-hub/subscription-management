package com.example.subscription_manager.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.subscription_manager.data.preferences.UserPreferencesStore
import com.example.subscription_manager.data.worker.NotificationCheckWorker
import com.example.subscription_manager.data.worker.ReminderNotificationWorker
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.domain.utils.DateCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesStore: UserPreferencesStore,
    private val repository: SubscriptionRepository
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleSubscription(subscription: Subscription) {
        val reminderTime = preferencesStore.reminderTimeFlow.first()
        val zone = ZoneId.systemDefault()
        val nextPaymentDate = subscription.nextPaymentDate

        (0..DateCalculator.ReminderLeadDays).forEach { daysBefore ->
            val targetDate = nextPaymentDate.minusDays(daysBefore.toLong())
            val targetDateTime = targetDate.atTime(reminderTime.hour, reminderTime.minute)
            val delayMillis = Duration.between(LocalDateTime.now(zone), targetDateTime.atZone(zone)).toMillis()

            if (delayMillis >= 0L) {
                val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag(tagsForSubscription(subscription.id))
                    .addTag(TAG_REMINDER)
                    .setInputData(
                        workDataOf(
                            KEY_SUBSCRIPTION_ID to subscription.id,
                            KEY_CYCLE_KEY to DateCalculator.cycleKeyForDate(nextPaymentDate, subscription.recurrence)
                        )
                    )
                    .build()

                workManager.enqueueUniqueWork(
                    uniqueWorkName(subscription.id, daysBefore),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }

    suspend fun scheduleSubscription(id: Long) {
        val subscription = repository.subscription(id).first() ?: return
        scheduleSubscription(subscription)
    }

    suspend fun rescheduleAllReminders() {
        workManager.cancelAllWorkByTag(TAG_REMINDER)
        repository.subscriptions.first().forEach { scheduleSubscription(it) }
        enqueueNotificationCheck()
    }

    fun cancelSubscriptionReminders(id: Long) {
        workManager.cancelAllWorkByTag(tagsForSubscription(id))
    }

    fun enqueueNotificationCheck() {
        val request: PeriodicWorkRequest = PeriodicWorkRequestBuilder<NotificationCheckWorker>(
            repeatInterval = 1L,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(15L, TimeUnit.MINUTES)
            .addTag(TAG_NOTIFICATION_CHECK)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NOTIFICATION_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun uniqueWorkName(subscriptionId: Long, daysBefore: Int): String {
        return "subscription_reminder_${subscriptionId}_$daysBefore"
    }

    private fun tagsForSubscription(subscriptionId: Long): String {
        return "subscription_$subscriptionId"
    }

    companion object {
        const val TAG_REMINDER = "tag_reminder"
        const val TAG_NOTIFICATION_CHECK = "tag_notification_check"
        const val UNIQUE_NOTIFICATION_CHECK = "unique_notification_check"
        const val UNIQUE_RESCHEDULE_ALL = "unique_reschedule_all"
        const val KEY_SUBSCRIPTION_ID = "key_subscription_id"
        const val KEY_CYCLE_KEY = "key_cycle_key"
    }
}
