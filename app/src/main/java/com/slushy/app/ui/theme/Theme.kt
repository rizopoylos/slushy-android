package com.slushy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SlushyPrimary,
    onPrimary = SlushyOnPrimary,
    background = SlushyBackground,
    surface = SlushySurface,
    error = SlushyError,
)

@Composable
fun SlushyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
