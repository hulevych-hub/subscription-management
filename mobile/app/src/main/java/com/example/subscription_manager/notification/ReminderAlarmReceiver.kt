package com.example.subscription_manager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.domain.utils.DateCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SubscriptionRepository

    @Inject
    lateinit var publisher: NotificationPublisher

    @Inject
    lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationScheduler.ACTION_REMINDER_ALARM) return

        Log.d(TAG, "Reminder alarm received")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                publishDueReminders()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to publish reminder notifications", exception)
            } finally {
                runCatching { scheduler.rescheduleAllReminders() }
                    .onFailure { Log.e(TAG, "Failed to reschedule reminder alarm", it) }
                pendingResult.finish()
            }
        }
    }

    private suspend fun publishDueReminders() {
        val now = LocalDate.now()
        val subscriptions = repository.subscriptions.first()
        val dueSubscriptions = subscriptions.filter { subscription ->
            !subscription.isPaid &&
                DateCalculator.daysUntil(subscription.nextPaymentDate, now) <= DateCalculator.ReminderLeadDays.toLong()
        }

        Log.d(TAG, "Reminder alarm checked ${subscriptions.size} subscriptions, ${dueSubscriptions.size} due")

        dueSubscriptions
            .forEach { subscription ->
                val cycleKey = DateCalculator.cycleKeyForDate(
                    date = subscription.nextPaymentDate,
                    recurrence = subscription.recurrence
                )

                publisher.showReminder(
                    subscription = subscription,
                    cycleKey = cycleKey
                )
            }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
    }
}
