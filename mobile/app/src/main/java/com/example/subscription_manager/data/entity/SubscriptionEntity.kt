package com.example.subscription_manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SubscriptionType

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "type")
    val type: SubscriptionType,
    val notes: String? = null,
    @ColumnInfo(name = "start_date_epoch_day")
    val startDateEpochDay: Long? = null,
    @ColumnInfo(name = "end_date_epoch_day")
    val endDateEpochDay: Long? = null,
    @ColumnInfo(name = "payment_day")
    val paymentDay: Int,
    @ColumnInfo(name = "payment_month")
    val paymentMonth: Int,
    val recurrence: Recurrence,
    @ColumnInfo(name = "renewal_enabled")
    val renewalEnabled: Boolean = true,
    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,
    @ColumnInfo(name = "paid_cycle_key")
    val paidCycleKey: String? = null,
    @ColumnInfo(name = "last_paid_at_epoch_millis")
    val lastPaidAtEpochMillis: Long? = null,
    @ColumnInfo(name = "last_reminder_cycle_key")
    val lastReminderCycleKey: String? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "amount")
    val amount: Double = 0.0,
)
