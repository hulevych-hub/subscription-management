package com.example.subscription_manager.domain.model

import java.time.LocalDate

data class Subscription(
    val id: Long,
    val name: String,
    val type: SubscriptionType,
    val notes: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val paymentDay: Int,
    val paymentMonth: Int,
    val recurrence: Recurrence,
    val renewalEnabled: Boolean,
    val isPaid: Boolean,
    val paidCycleKey: String?,
    val lastPaidAt: Long?,
    val lastReminderCycleKey: String?,
    val nextPaymentDate: LocalDate,
    val willRenew: Boolean
)
