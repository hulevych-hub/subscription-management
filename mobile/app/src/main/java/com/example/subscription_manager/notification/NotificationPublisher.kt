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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.subscription_manager.MainActivity
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.domain.utils.DateCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SubscriptionRepository
) {
    suspend fun showReminder(subscription: Subscription, cycleKey: String): Boolean {
        if (subscription.isPaid) {
            Log.d(TAG, "Skipping paid subscription ${subscription.id}")
            return false
        }

        if (subscription.lastReminderCycleKey == cycleKey) {
            Log.d(TAG, "Skipping already reminded subscription ${subscription.id} for cycle $cycleKey")
            return false
        }

        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission missing for subscription ${subscription.id}")
            repository.clearReminderSent(subscription.id, cycleKey)
            return false
        }

        createNotificationChannel()

        val notificationId = notificationId(subscription.id)
        val daysUntil = DateCalculator.daysUntil(subscription.nextPaymentDate)
        val isUrgent = daysUntil <= 1
        val smallIconId = smallIconId(isUrgent)
        val reminderText = reminderText(subscription, daysUntil)
        val paymentDateText = subscription.nextPaymentDate.format(DateFormatter)
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconId)
            .setColor(accentColor(isUrgent))
            .setContentTitle(reminderText)
            .setContentText(paymentDateText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$reminderText Payment date: $paymentDateText."))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    smallIconId,
                    "Mark Paid",
                    markPaidPendingIntent
                ).build()
            )
            .setAutoCancel(true)
            .build()

        Log.d(TAG, "Posting notification $notificationId for subscription ${subscription.id}")
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }.onFailure { exception ->
            Log.e(TAG, "Failed to post notification $notificationId for subscription ${subscription.id}", exception)
            return false
        }

        repository.markReminderSent(subscription.id, cycleKey)
        Log.d(TAG, "Posted notification $notificationId for subscription ${subscription.id}")
        return true
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payment reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you before subscription payments are due."
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun smallIconId(isUrgent: Boolean): Int {
        val preferredIcon = if (isUrgent) "ic_notification_urgent_red" else "ic_notification_warning_orange"
        val preferredId = context.resources.getIdentifier(preferredIcon, "drawable", context.packageName)
        return preferredId.takeIf { it != 0 }
            ?: context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
    }

    private fun accentColor(isUrgent: Boolean): Int {
        return if (isUrgent) {
            android.graphics.Color.rgb(220, 38, 38)
        } else {
            android.graphics.Color.rgb(245, 158, 11)
        }
    }

    private fun reminderText(subscription: Subscription, daysUntil: Long): String {
        val amount = formatAmount(subscription.amount)
        return when {
            daysUntil > 1 -> "${subscription.name} is due: $amount in $daysUntil days."
            daysUntil == 1L -> "${subscription.name} is due: $amount tomorrow."
            daysUntil == 0L -> "${subscription.name} is due: $amount today."
            else -> "${subscription.name} is overdue: $amount."
        }
    }

    private fun formatAmount(amount: Double): String {
        return "CHF ${String.format(Locale.US, "%.2f", amount)}"
    }

    private fun notificationId(subscriptionId: Long): Int {
        return (subscriptionId % Int.MAX_VALUE).toInt()
    }

    companion object {
        private const val TAG = "NotificationPublisher"
        const val CHANNEL_ID = "payment_reminders"
        private val DateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
}
