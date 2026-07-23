package com.p2pchat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = TextOnPurple,
    primaryContainer = Purple20,
    onPrimaryContainer = Purple80,
    secondary = Cyan60,
    onSecondary = TextOnPurple,
    secondaryContainer = Cyan20,
    onSecondaryContainer = Cyan80,
    tertiary = Cyan40,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary.copy(alpha = 0.3f),
    error = Disconnected
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = TextOnPurple,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple10,
    secondary = Cyan40,
    onSecondary = TextOnPurple,
    secondaryContainer = Cyan80,
    onSecondaryContainer = Cyan20,
    tertiary = Cyan60,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = TextSecondaryLight.copy(alpha = 0.3f),
    error = Disconnected
)

@Composable
fun P2PChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = P2PChatTypography,
        content = content
    )
}
