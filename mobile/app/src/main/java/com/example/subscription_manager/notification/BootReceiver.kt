package com.example.subscription_manager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.subscription_manager.data.worker.RescheduleRemindersWorker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val request = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>()
            .setInputData(workDataOf())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NotificationScheduler.UNIQUE_RESCHEDULE_ALL,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
