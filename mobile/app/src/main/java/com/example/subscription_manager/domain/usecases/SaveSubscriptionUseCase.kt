package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import javax.inject.Inject

class SaveSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(form: SubscriptionForm): Long {
        return repository.saveSubscription(form)
    }
}
