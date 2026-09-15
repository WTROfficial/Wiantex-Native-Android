package com.wiantex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WiantexDark = darkColorScheme(
    primary = Color(0xFF9D72FF),
    onPrimary = Color.White,
    secondary = Color(0xFF6EA8FF),
    background = Color(0xFF09070D),
    surface = Color(0xFF111018),
    surfaceVariant = Color(0xFF1B1824),
    onBackground = Color(0xFFF4F0FA),
    onSurface = Color(0xFFF4F0FA),
    outline = Color(0xFF373142),
)

private val WiantexLight = lightColorScheme(
    primary = Color(0xFF6D3BE5),
    secondary = Color(0xFF356CCC),
    background = Color(0xFFF8F6FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0ECF8),
    onBackground = Color(0xFF17131C),
    onSurface = Color(0xFF17131C),
    outline = Color(0xFFD7D0E0),
)

@Composable
fun WiantexTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) WiantexDark else WiantexLight,
        content = content,
    )
}
