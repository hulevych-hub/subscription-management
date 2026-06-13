package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(id: Long): Flow<Subscription?> = repository.subscription(id)
}
