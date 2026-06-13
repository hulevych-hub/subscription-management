package com.example.subscription_manager.data.database

import androidx.room.TypeConverter
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SubscriptionType
import java.time.LocalDate

object SubscriptionTypeConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromSubscriptionType(type: SubscriptionType): String = type.name

    @TypeConverter
    fun toSubscriptionType(value: String): SubscriptionType = SubscriptionType.valueOf(value)

    @TypeConverter
    fun fromRecurrence(recurrence: Recurrence): String = recurrence.name

    @TypeConverter
    fun toRecurrence(value: String): Recurrence = Recurrence.valueOf(value)
}
