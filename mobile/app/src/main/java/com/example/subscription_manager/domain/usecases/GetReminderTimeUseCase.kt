package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.data.preferences.UserPreferencesStore
import com.example.subscription_manager.domain.model.ReminderTime
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReminderTimeUseCase @Inject constructor(
    private val preferencesStore: UserPreferencesStore
) {
    operator fun invoke(): Flow<ReminderTime> = preferencesStore.reminderTimeFlow
}
