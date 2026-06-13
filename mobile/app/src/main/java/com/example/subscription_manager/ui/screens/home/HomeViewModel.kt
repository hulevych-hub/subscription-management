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
    val isLoading: Boolean = true
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
            HomeUiState(
                items = subscriptions
                    .map { DateCalculator.toHomeItem(it) }
                    .sortedWith(
                        compareBy<HomeSubscriptionItem> { it.sortBucket.ordinal }
                            .thenBy { it.subscription.nextPaymentDate }
                            .thenBy { it.subscription.name.lowercase() }
                    ),
                isLoading = false
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
