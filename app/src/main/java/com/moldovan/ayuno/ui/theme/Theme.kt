package com.moldovan.ayuno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.moldovan.ayuno.data.ThemeMode

private val LightColors = lightColorScheme(
    primary              = GreenMedium,
    onPrimary            = SurfaceLight,
    primaryContainer     = GreenMedium.copy(alpha = 0.15f),
    onPrimaryContainer   = GreenDark,
    secondary            = Gold,
    onSecondary          = GreenDark,
    background           = Cream,
    onBackground         = GreenDark,
    surface              = SurfaceLight,
    onSurface            = GreenDark,
    surfaceVariant       = Cream.copy(alpha = 0.6f),
    onSurfaceVariant     = GreenDark.copy(alpha = 0.7f),
    outline              = GreenMedium.copy(alpha = 0.4f),
    outlineVariant       = GreenMedium.copy(alpha = 0.2f),
    error                = ErrorRed,
    onError              = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary              = GreenLight,
    onPrimary            = GreenDark,
    primaryContainer     = GreenDark,
    onPrimaryContainer   = GreenLight,
    secondary            = GoldSoft,
    onSecondary          = GreenDark,
    background           = SurfaceDark2,
    onBackground         = Cream,
    surface              = SurfaceDark,
    onSurface            = CreamDark,
    surfaceVariant       = GreenDark.copy(alpha = 0.8f),
    onSurfaceVariant     = CreamDark.copy(alpha = 0.7f),
    outline              = GreenLight.copy(alpha = 0.4f),
    outlineVariant       = GreenLight.copy(alpha = 0.2f),
    error                = ErrorRedDark,
    onError              = GreenDark
)

@Composable
fun AyunoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography,
        content     = content
    )
}