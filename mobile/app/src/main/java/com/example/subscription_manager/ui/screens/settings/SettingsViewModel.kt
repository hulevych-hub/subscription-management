package com.example.subscription_manager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription_manager.domain.model.ReminderTime
import com.example.subscription_manager.domain.model.ThemeMode
import com.example.subscription_manager.domain.usecases.GetReminderTimeUseCase
import com.example.subscription_manager.domain.usecases.GetThemeModeUseCase
import com.example.subscription_manager.domain.usecases.UpdateReminderTimeUseCase
import com.example.subscription_manager.domain.usecases.UpdateThemeModeUseCase
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reminderTime: ReminderTime = ReminderTime.Default,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getReminderTimeUseCase: GetReminderTimeUseCase,
    getThemeModeUseCase: GetThemeModeUseCase,
    private val updateReminderTimeUseCase: UpdateReminderTimeUseCase,
    private val updateThemeModeUseCase: UpdateThemeModeUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val repository: SubscriptionRepository
) : ViewModel() {

    val reminderTime = getReminderTimeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReminderTime.Default
        )

    val themeMode = getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    fun updateReminderTime(reminderTime: ReminderTime) {
        viewModelScope.launch {
            updateReminderTimeUseCase(reminderTime)
            notificationScheduler.rescheduleAllReminders()
        }
    }

    fun onNotificationPermissionGranted() {
        viewModelScope.launch {
            repository.clearAllReminderSent()
            notificationScheduler.rescheduleAllReminders()
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            updateThemeModeUseCase(themeMode)
        }
    }
}
