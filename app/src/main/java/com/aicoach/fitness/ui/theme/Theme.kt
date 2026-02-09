package com.aicoach.fitness.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue500,
    onPrimary = Color.White,
    primaryContainer = ElectricBlue600,
    onPrimaryContainer = ElectricBlue100,
    secondary = Cyan400,
    onSecondary = Color.White,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate200,
    tertiary = Cyan500,
    onTertiary = Color.White,
    tertiaryContainer = Slate800,
    onTertiaryContainer = Slate200,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate500,
    outlineVariant = Slate600,
    scrim = Color.Black.copy(alpha = 0.5f),
    inverseSurface = Slate200,
    inverseOnSurface = Slate900,
    inversePrimary = ElectricBlue400,
    surfaceDim = Slate900,
    surfaceBright = Slate700,
    surfaceContainerLowest = Slate900,
    surfaceContainerLow = Slate800,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerHighest = Slate700,
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue600,
    onPrimary = Color.White,
    primaryContainer = ElectricBlue100,
    onPrimaryContainer = ElectricBlue900,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,
    tertiary = Cyan600,
    onTertiary = Color.White,
    tertiaryContainer = Slate200,
    onTertiaryContainer = Slate800,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFECACA),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate400,
    outlineVariant = Slate300,
    scrim = Color.Black.copy(alpha = 0.5f),
    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    inversePrimary = ElectricBlue400,
    surfaceDim = Slate200,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate200,
    surfaceContainerHighest = Slate300,
)

@Composable
fun AIFitnessCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}