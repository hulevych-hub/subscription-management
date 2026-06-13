package com.example.subscription_manager.domain.model

data class ReminderTime(
    val hour: Int = 9,
    val minute: Int = 0
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23." }
        require(minute in 0..59) { "Minute must be between 0 and 59." }
    }

    val minutesOfDay: Int
        get() = hour * 60 + minute

    companion object {
        val Default = ReminderTime(hour = 9, minute = 0)

        fun fromMinutesOfDay(minutes: Int): ReminderTime {
            val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
            return ReminderTime(
                hour = safeMinutes / 60,
                minute = safeMinutes % 60
            )
        }
    }
}
