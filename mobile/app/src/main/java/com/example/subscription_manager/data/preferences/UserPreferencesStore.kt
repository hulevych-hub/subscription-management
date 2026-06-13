package com.example.subscription_manager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.subscription_manager.domain.model.ReminderTime
import com.example.subscription_manager.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val reminderTimeFlow: Flow<ReminderTime> = context.settingsDataStore.data.map { preferences ->
        ReminderTime.fromMinutesOfDay(
            preferences[Keys.REMINDER_TIME_MINUTES] ?: ReminderTime.Default.minutesOfDay
        )
    }

    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        runCatching {
            ThemeMode.valueOf(
                preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            )
        }.getOrElse { ThemeMode.SYSTEM }
    }

    suspend fun updateReminderTime(reminderTime: ReminderTime) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.REMINDER_TIME_MINUTES] = reminderTime.minutesOfDay
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    private object Keys {
        val REMINDER_TIME_MINUTES = intPreferencesKey("reminder_time_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
