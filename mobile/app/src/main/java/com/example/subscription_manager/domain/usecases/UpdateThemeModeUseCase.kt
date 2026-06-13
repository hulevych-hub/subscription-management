package com.example.subscription_manager.domain.usecases

import com.example.subscription_manager.data.preferences.UserPreferencesStore
import com.example.subscription_manager.domain.model.ThemeMode
import javax.inject.Inject

class UpdateThemeModeUseCase @Inject constructor(
    private val preferencesStore: UserPreferencesStore
) {
    suspend operator fun invoke(themeMode: ThemeMode) {
        preferencesStore.updateThemeMode(themeMode)
    }
}
