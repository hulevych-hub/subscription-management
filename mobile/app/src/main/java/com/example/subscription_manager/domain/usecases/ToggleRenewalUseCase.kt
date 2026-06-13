package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.domain.repository.SubscriptionRepository
import javax.inject.Inject

class ToggleRenewalUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.toggleRenewal(id)
    }
}
