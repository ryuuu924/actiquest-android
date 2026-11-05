package com.actiquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import android.content.Context

object ThemeManager {
    var isDarkMode by mutableStateOf(true)
        private set

    private var preferences: android.content.SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences("actiquest_theme", Context.MODE_PRIVATE)
        isDarkMode = preferences?.getBoolean("dark_mode", true) ?: true
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        isDarkMode = enabled
        preferences?.edit()?.putBoolean("dark_mode", enabled)?.apply()
    }

    fun toggleTheme() {
        setDarkModeEnabled(!isDarkMode)
    }
}

private val DarkAppColorScheme = darkColorScheme(
    primary = CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF292265),
    onPrimaryContainer = TextPrimary,
    secondary = MintGreenLight,
    onSecondary = Color(0xFF062B25),
    secondaryContainer = Color(0xFF103A35),
    onSecondaryContainer = Color(0xFF9AF0DC),
    tertiary = VioletAccent,
    onTertiary = Color(0xFF21163D),
    tertiaryContainer = IndigoPurple,
    onTertiaryContainer = Color(0xFFD8CCFF),
    background = DarkNavyBackground,
    onBackground = TextPrimary,
    surface = SurfaceCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceCharcoal,
    surfaceContainerHigh = SurfaceElevated,
    error = StatusRejected,
    onError = TextPrimary,
    errorContainer = Color(0xFF541D2A),
    onErrorContainer = Color(0xFFFFDAD9),
    outline = Color(0xFF8173AA),
    outlineVariant = Color(0xFF362762)
)

private val LightAppColorScheme = lightColorScheme(
    primary = Color(0xFF4939D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF2C1C67),
    secondary = MintEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F4ED),
    onSecondaryContainer = Color(0xFF064B3D),
    tertiary = Color(0xFF6448C5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF311A52),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceElevatedHigh,
    error = Color(0xFFB42333),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF41000A),
    outline = Color(0xFF756B80),
    outlineVariant = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkAppColorScheme else LightAppColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
