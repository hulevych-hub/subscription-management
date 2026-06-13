package com.example.subscription_manager.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription_manager.domain.model.HomeSubscriptionItem
import com.example.subscription_manager.domain.usecases.GetSubscriptionsUseCase
import com.example.subscription_manager.domain.usecases.MarkPaidUseCase
import com.example.subscription_manager.domain.usecases.ToggleRenewalUseCase
import com.example.subscription_manager.domain.utils.DateCalculator
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val items: List<HomeSubscriptionItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalSpend: Double = 0.0 // Added for the Dashboard Header
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val markPaidUseCase: MarkPaidUseCase,
    private val toggleRenewalUseCase: ToggleRenewalUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    val state = getSubscriptionsUseCase()
        .map { subscriptions ->
            // 1. Map each subscription to HomeSubscriptionItem

            val homeItems = subscriptions.map { sub ->
                val item = DateCalculator.toHomeItem(sub)
                // If the item needs a formattedAmount, we ensure it's set here
                // or updated if DateCalculator doesn't already provide it
                item.copy(
                    formattedAmount = sub.amount.toString()
                )
            }.sortedWith(
                compareBy<HomeSubscriptionItem> { it.sortBucket.ordinal }
                    .thenBy { it.subscription.nextPaymentDate }
                    .thenBy { it.subscription.name.lowercase() }
            )

            // 2. Calculate total spend based on the formatted amounts
            val total = homeItems.sumOf { item ->
                // This handles it regardless of whether amount is a String or a Number
                val amount = item.subscription.amount
                when (amount) {
                    is Number -> amount.toDouble()
                    is String -> amount.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }

            HomeUiState(
                items = homeItems,
                isLoading = false,
                totalSpend = total
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun markPaid(id: Long) {
        viewModelScope.launch {
            markPaidUseCase(id)
            notificationScheduler.scheduleSubscription(id)
        }
    }

    fun toggleRenewal(id: Long) {
        viewModelScope.launch {
            toggleRenewalUseCase(id)
            notificationScheduler.scheduleSubscription(id)
        }
    }
}