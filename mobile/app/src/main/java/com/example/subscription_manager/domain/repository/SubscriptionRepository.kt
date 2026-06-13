package com.example.subscription_manager.domain.repository

import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.model.SubscriptionForm
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    val subscriptions: Flow<List<Subscription>>

    fun subscription(id: Long): Flow<Subscription?>

    suspend fun saveSubscription(form: SubscriptionForm): Long

    suspend fun deleteSubscription(id: Long)

    suspend fun markPaid(id: Long)

    suspend fun toggleRenewal(id: Long)

    suspend fun markReminderSent(id: Long, cycleKey: String)
}
