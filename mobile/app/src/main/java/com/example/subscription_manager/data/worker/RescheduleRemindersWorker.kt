package com.example.subscription_manager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: NotificationScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        scheduler.rescheduleAllReminders()
        return Result.success()
    }
}
