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

@HiltWorker
class ReminderNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SubscriptionRepository,
    private val publisher: NotificationPublisher
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val subscriptionId = inputData.getLong(NotificationScheduler.KEY_SUBSCRIPTION_ID, -1L)
        val cycleKey = inputData.getString(NotificationScheduler.KEY_CYCLE_KEY) ?: return Result.success()
        val subscription = repository.subscription(subscriptionId).first() ?: return Result.success()

        publisher.showReminder(
            subscription = subscription,
            cycleKey = cycleKey
        )

        return Result.success()
    }
}
