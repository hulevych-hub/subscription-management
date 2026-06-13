package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.data.preferences.UserPreferencesStore
import com.example.subscription_manager.domain.model.ReminderTime
import javax.inject.Inject

class UpdateReminderTimeUseCase @Inject constructor(
    private val preferencesStore: UserPreferencesStore
) {
    suspend operator fun invoke(reminderTime: ReminderTime) {
        preferencesStore.updateReminderTime(reminderTime)
    }
}
