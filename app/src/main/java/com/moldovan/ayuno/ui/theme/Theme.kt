package com.moldovan.ayuno.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary         = Color(0xFF6B8F71),   // soft green — matches "accent"
    onPrimary       = Color.White,
    background      = Color(0xFFFAF9F7),
    surface         = Color.White,
    surfaceVariant  = Color(0xFFE8E4DE),
    onSurfaceVariant= Color(0xFF6B6560)
)

@Composable
fun AyunoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography(),
        content     = content
    )
}