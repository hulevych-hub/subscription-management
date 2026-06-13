package com.example.subscription_manager.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.subscription_manager.MainActivity
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SubscriptionRepository
) {
    suspend fun showReminder(subscription: Subscription, cycleKey: String): Boolean {
        if (subscription.isPaid || subscription.lastReminderCycleKey == cycleKey) {
            return false
        }

        createNotificationChannel()

        val notificationId = notificationId(subscription.id)
        val smallIconId = smallIconId()
        val markPaidIntent = Intent(context, MarkPaidBroadcastReceiver::class.java).apply {
            action = MarkPaidBroadcastReceiver.ACTION_MARK_PAID
            putExtra(MarkPaidBroadcastReceiver.EXTRA_SUBSCRIPTION_ID, subscription.id)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            subscription.id.hashCode(),
            markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            subscription.id.hashCode() + 1_000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val paymentDateText = subscription.nextPaymentDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconId)
            .setContentTitle("Subscription payment due")
            .setContentText("$subscription.name • $paymentDateText")
            .setStyle(Notification.BigTextStyle().bigText("$subscription.name payment is due on $paymentDateText."))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                Notification.Action.Builder(
                    smallIconId,
                    "Mark Paid",
                    markPaidPendingIntent
                ).build()
            )
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        repository.markReminderSent(subscription.id, cycleKey)
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payment reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you before subscription payments are due."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun smallIconId(): Int {
        return context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
    }

    private fun notificationId(subscriptionId: Long): Int {
        return (subscriptionId % Int.MAX_VALUE).toInt()
    }

    companion object {
        const val CHANNEL_ID = "payment_reminders"
    }
}
