package com.example.subscription_manager.domain.utils

import com.example.subscription_manager.domain.model.HomeSubscriptionItem
import com.example.subscription_manager.domain.model.PaymentStatus
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SortBucket
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object DateCalculator {
    const val ReminderLeadDays = 3

    fun clampPaymentDay(year: Int, month: Int, paymentDay: Int): Int {
        val safeMonth = month.coerceIn(1, 12)
        val maxDay = YearMonth.of(year, safeMonth).lengthOfMonth()
        return paymentDay.coerceIn(1, maxDay)
    }

    fun nextPaymentDate(
        paymentDay: Int,
        paymentMonth: Int,
        recurrence: Recurrence,
        endDate: LocalDate?,
        asOf: LocalDate = LocalDate.now()
    ): LocalDate {
        val safePaymentDay = paymentDay.coerceIn(1, 31)
        val safePaymentMonth = paymentMonth.coerceIn(1, 12)
        val scheduledDate = when (recurrence) {
            Recurrence.MONTHLY -> {
                var date = LocalDate.of(
                    asOf.year,
                    asOf.monthValue,
                    clampPaymentDay(asOf.year, asOf.monthValue, safePaymentDay)
                )
                while (date < asOf) {
                    date = date.plusMonths(1)
                }
                date
            }

            Recurrence.ANNUAL -> {
                var date = LocalDate.of(
                    asOf.year,
                    safePaymentMonth,
                    clampPaymentDay(asOf.year, safePaymentMonth, safePaymentDay)
                )
                if (date < asOf) {
                    date = date.plusYears(1)
                }
                date
            }
        }

        return endDate?.takeIf { scheduledDate > it } ?: scheduledDate
    }

    fun advancePaymentDate(paymentDate: LocalDate, recurrence: Recurrence, times: Int = 1): LocalDate {
        require(times >= 0) { "Times must be non-negative." }
        return when (recurrence) {
            Recurrence.MONTHLY -> paymentDate.plusMonths(times.toLong())
            Recurrence.ANNUAL -> paymentDate.plusYears(times.toLong())
        }
    }

    fun cycleKeyForDate(date: LocalDate, recurrence: Recurrence): String {
        return when (recurrence) {
            Recurrence.MONTHLY -> "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
            Recurrence.ANNUAL -> date.year.toString()
        }
    }

    fun isPaidForCycle(
        paidCycleKey: String?,
        nextPaymentDate: LocalDate,
        recurrence: Recurrence
    ): Boolean {
        return paidCycleKey == cycleKeyForDate(nextPaymentDate, recurrence)
    }

    fun paymentStatus(
        isPaid: Boolean,
        nextPaymentDate: LocalDate,
        now: LocalDate = LocalDate.now()
    ): PaymentStatus {
        if (isPaid) return PaymentStatus.PAID
        return when {
            nextPaymentDate < now -> PaymentStatus.OVERDUE
            ChronoUnit.DAYS.between(now, nextPaymentDate) <= ReminderLeadDays.toLong() -> PaymentStatus.DUE_SOON
            else -> PaymentStatus.UPCOMING
        }
    }

    fun sortBucket(
        isPaid: Boolean,
        nextPaymentDate: LocalDate,
        now: LocalDate = LocalDate.now()
    ): SortBucket {
        if (isPaid) return SortBucket.PAID
        return if (nextPaymentDate <= now || YearMonth.from(nextPaymentDate) == YearMonth.from(now)) {
            SortBucket.UNPAID_DUE_THIS_MONTH
        } else {
            SortBucket.UPCOMING
        }
    }

    fun toHomeItem(subscription: com.example.subscription_manager.domain.model.Subscription): HomeSubscriptionItem {
        return HomeSubscriptionItem(
            subscription = subscription,
            status = paymentStatus(subscription.isPaid, subscription.nextPaymentDate),
            sortBucket = sortBucket(subscription.isPaid, subscription.nextPaymentDate),
            // Add this line to satisfy the new requirement:
            formattedAmount = String.format("%.2f", subscription.amount)
        )
    }

    fun daysUntil(date: LocalDate, now: LocalDate = LocalDate.now()): Long {
        return ChronoUnit.DAYS.between(now, date)
    }

    fun samePaymentSchedule(
        leftPaymentDay: Int,
        leftPaymentMonth: Int,
        leftRecurrence: Recurrence,
        rightPaymentDay: Int,
        rightPaymentMonth: Int,
        rightRecurrence: Recurrence
    ): Boolean {
        return leftPaymentDay == rightPaymentDay &&
            leftPaymentMonth == rightPaymentMonth &&
            leftRecurrence == rightRecurrence
    }
}
