package com.example.subscription_manager.domain.model

enum class PaymentStatus {
    PAID,
    OVERDUE,
    DUE_SOON,
    UPCOMING
}

enum class SortBucket {
    UNPAID_DUE_THIS_MONTH,
    UPCOMING,
    PAID
}

data class HomeSubscriptionItem(
    val subscription: Subscription,
    val status: PaymentStatus,
    val sortBucket: SortBucket,
    val formattedAmount: String
)