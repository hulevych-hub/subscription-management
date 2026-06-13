package com.example.subscription_manager.domain.model

import java.time.LocalDate

data class SubscriptionForm(
    val id: Long = 0L,
    val name: String = "",
    val type: SubscriptionType = SubscriptionType.SUBSCRIPTION,
    val notes: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val paymentDay: Int = 1,
    val paymentMonth: Int = LocalDate.now().monthValue,
    val recurrence: Recurrence = Recurrence.MONTHLY,
    val renewalEnabled: Boolean = true
)
