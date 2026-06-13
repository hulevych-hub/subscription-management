package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(): Flow<List<Subscription>> = repository.subscriptions
}
