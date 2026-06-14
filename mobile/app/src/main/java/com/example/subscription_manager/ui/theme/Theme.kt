package com.example.subscription_manager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.subscription_manager.domain.model.ThemeMode

internal val LocalUseDarkTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF3B0764),
    onTertiaryContainer = Color(0xFFE9D5FF),
    error = DarkDeepRed,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate500,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate950,
    inversePrimary = Blue40,
    surfaceTint = Blue80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = SoftGreen,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = DeepPurple,
    onTertiary = Color.White,
    tertiaryContainer = SoftPurple,
    onTertiaryContainer = Color(0xFF3B0764),
    error = Red,
    onError = Color.White,
    errorContainer = SoftRed,
    onErrorContainer = DeepRed,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate600,
    outline = Slate400,
    outlineVariant = Slate300,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = Blue80,
    surfaceTint = Blue40
)

@Composable
fun SubscriptionmanagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = darkTheme ?: when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    CompositionLocalProvider(LocalUseDarkTheme provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
