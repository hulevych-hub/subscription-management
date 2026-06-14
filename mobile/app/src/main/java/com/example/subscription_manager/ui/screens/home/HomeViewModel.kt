package com.example.subscription_manager.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription_manager.domain.model.HomeSubscriptionItem
import com.example.subscription_manager.domain.model.PaymentStatus
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.usecases.GetSubscriptionsUseCase
import com.example.subscription_manager.domain.usecases.MarkPaidUseCase
import com.example.subscription_manager.domain.usecases.MarkUnpaidUseCase
import com.example.subscription_manager.domain.usecases.ToggleRenewalUseCase
import com.example.subscription_manager.domain.utils.DateCalculator
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

enum class HomeFilter {
    ALL,
    THIS_MONTH,
    DUE_SOON,
    PAID
}

data class HomeUiState(
    val items: List<HomeSubscriptionItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalSpend: Double = 0.0,
    val totalCount: Int = 0,
    val thisMonthCount: Int = 0,
    val dueSoonCount: Int = 0,
    val paidCount: Int = 0,
    val activeFilter: HomeFilter = HomeFilter.ALL
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val markPaidUseCase: MarkPaidUseCase,
    private val markUnpaidUseCase: MarkUnpaidUseCase,
    private val toggleRenewalUseCase: ToggleRenewalUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val filterState = MutableStateFlow(HomeFilter.ALL)

    val state = getSubscriptionsUseCase()
        .combine(filterState) { subscriptions, activeFilter ->
            val homeItems = subscriptions
                .map { sub ->
                    DateCalculator.toHomeItem(sub).copy(
                        formattedAmount = sub.amount.toString()
                    )
                }
                .sortedWith(
                    compareBy<HomeSubscriptionItem> { it.sortBucket.ordinal }
                        .thenBy { it.subscription.nextPaymentDate }
                        .thenBy { it.subscription.name.lowercase() }
                )

            HomeUiState(
                items = homeItems.filter { it.matchesFilter(activeFilter) },
                isLoading = false,
                totalSpend = homeItems.sumOf { it.monthlyAmount() },
                totalCount = homeItems.size,
                thisMonthCount = homeItems.count { it.isUnpaidThisMonth() },
                dueSoonCount = homeItems.count { it.status == PaymentStatus.DUE_SOON || it.status == PaymentStatus.OVERDUE },
                paidCount = homeItems.count { it.status == PaymentStatus.PAID },
                activeFilter = activeFilter
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun toggleFilter(filter: HomeFilter) {
        filterState.value = if (filterState.value == filter) HomeFilter.ALL else filter
    }

    fun markPaid(id: Long) {
        viewModelScope.launch {
            markPaidUseCase(id)
            notificationScheduler.scheduleSubscription(id)
        }
    }

    fun markUnpaid(id: Long) {
        viewModelScope.launch {
            markUnpaidUseCase(id)
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

private fun HomeSubscriptionItem.matchesFilter(filter: HomeFilter): Boolean {
    return when (filter) {
        HomeFilter.ALL -> true
        HomeFilter.THIS_MONTH -> isUnpaidThisMonth()
        HomeFilter.DUE_SOON -> status == PaymentStatus.DUE_SOON || status == PaymentStatus.OVERDUE
        HomeFilter.PAID -> status == PaymentStatus.PAID
    }
}

private fun HomeSubscriptionItem.isUnpaidThisMonth(now: YearMonth = YearMonth.now()): Boolean {
    return !subscription.isPaid && YearMonth.from(subscription.nextPaymentDate) == now
}

private fun HomeSubscriptionItem.monthlyAmount(): Double {
    return when (subscription.recurrence) {
        Recurrence.MONTHLY -> subscription.amount
        Recurrence.ANNUAL -> subscription.amount / 12.0
    }
}