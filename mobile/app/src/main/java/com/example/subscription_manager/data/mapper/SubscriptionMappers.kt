package com.example.subscription_manager.data.mapper

import com.example.subscription_manager.data.entity.SubscriptionEntity
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.utils.DateCalculator

fun SubscriptionEntity.toDomain(): Subscription {
    val nextPaymentDate = DateCalculator.nextPaymentDate(
        paymentDay = paymentDay,
        paymentMonth = paymentMonth,
        recurrence = recurrence,
        endDate = endDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
    )
    val computedIsPaid = DateCalculator.isPaidForCycle(
        paidCycleKey = paidCycleKey,
        nextPaymentDate = nextPaymentDate,
        recurrence = recurrence
    )
    val willRenew = renewalEnabled && (endDateEpochDay == null || nextPaymentDate <= java.time.LocalDate.ofEpochDay(endDateEpochDay))

    return Subscription(
        id = id,
        name = name,
        type = type,
        notes = notes,
        startDate = startDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) },
        endDate = endDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) },
        paymentDay = paymentDay,
        paymentMonth = paymentMonth,
        recurrence = recurrence,
        renewalEnabled = renewalEnabled,
        isPaid = computedIsPaid,
        paidCycleKey = paidCycleKey,
        lastPaidAt = lastPaidAtEpochMillis,
        lastReminderCycleKey = lastReminderCycleKey,
        nextPaymentDate = nextPaymentDate,
        willRenew = willRenew
    )
}

fun SubscriptionForm.toEntity(
    existing: SubscriptionEntity?,
    nowMillis: Long
): SubscriptionEntity {
    val safePaymentDay = paymentDay.coerceIn(1, 31)
    val safePaymentMonth = paymentMonth.coerceIn(1, 12)
    val paymentScheduleChanged = existing == null || !DateCalculator.samePaymentSchedule(
        leftPaymentDay = existing.paymentDay,
        leftPaymentMonth = existing.paymentMonth,
        leftRecurrence = existing.recurrence,
        rightPaymentDay = safePaymentDay,
        rightPaymentMonth = safePaymentMonth,
        rightRecurrence = recurrence
    )

    return SubscriptionEntity(
        id = id,
        name = name.trim(),
        type = type,
        notes = notes.trim().ifBlank { null },
        startDateEpochDay = startDate?.toEpochDay(),
        endDateEpochDay = endDate?.toEpochDay(),
        paymentDay = safePaymentDay,
        paymentMonth = safePaymentMonth,
        recurrence = recurrence,
        renewalEnabled = renewalEnabled,
        isPaid = if (paymentScheduleChanged) false else existing?.isPaid == true,
        paidCycleKey = if (paymentScheduleChanged) null else existing?.paidCycleKey,
        lastPaidAtEpochMillis = if (paymentScheduleChanged) null else existing?.lastPaidAtEpochMillis,
        lastReminderCycleKey = null,
        createdAtEpochMillis = existing?.createdAtEpochMillis ?: nowMillis,
        updatedAtEpochMillis = nowMillis
    )
}
