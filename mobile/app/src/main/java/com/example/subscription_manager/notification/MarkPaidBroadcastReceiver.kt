package com.example.subscription_manager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.subscription_manager.domain.usecases.MarkPaidUseCase
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarkPaidBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var markPaidUseCase: MarkPaidUseCase

    @Inject
    lateinit var scheduler: NotificationScheduler

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_PAID) return

        val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1L)
        if (subscriptionId < 0L) return

        scope.launch {
            markPaidUseCase(subscriptionId)
            scheduler.cancelSubscriptionReminders(subscriptionId)
            scheduler.scheduleSubscription(subscriptionId)
            NotificationManagerCompat.from(context).cancel(notificationId(subscriptionId))
        }
    }

    private fun notificationId(subscriptionId: Long): Int {
        return (subscriptionId % Int.MAX_VALUE).toInt()
    }

    companion object {
        const val ACTION_MARK_PAID = "com.example.subscription_manager.action.MARK_PAID"
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
    }
}
