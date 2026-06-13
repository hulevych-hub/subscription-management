package com.example.subscription_manager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.domain.utils.DateCalculator
import com.example.subscription_manager.notification.NotificationPublisher
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class NotificationCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SubscriptionRepository,
    private val publisher: NotificationPublisher,
    private val scheduler: NotificationScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = LocalDate.now()
        repository.subscriptions.first().forEach { subscription ->
            val daysUntilPayment = DateCalculator.daysUntil(subscription.nextPaymentDate, now)
            if (!subscription.isPaid && daysUntilPayment <= DateCalculator.ReminderLeadDays.toLong()) {
                val cycleKey = DateCalculator.cycleKeyForDate(subscription.nextPaymentDate, subscription.recurrence)
                publisher.showReminder(subscription, cycleKey)
            }
            scheduler.scheduleSubscription(subscription.id)
        }
        return Result.success()
    }
}
