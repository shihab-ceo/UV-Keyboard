package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UVKeyboardColorScheme = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF241047),
    primaryContainer = Color(0xFF493080),
    onPrimaryContainer = Color(0xFFEDE5FF),
    secondary = Color(0xFF67E8F1),
    onSecondary = Color(0xFF00363B),
    secondaryContainer = Color(0xFF164D56),
    onSecondaryContainer = Color(0xFFB4F5F7),
    tertiary = Color(0xFFFFA8CF),
    background = Color(0xFF080B18),
    surface = Color(0xFF111729),
    surfaceVariant = Color(0xFF222B43),
    onBackground = Color(0xFFF4F2FA),
    onSurface = Color(0xFFF4F2FA),
    onSurfaceVariant = Color(0xFFA4B0C8),
    outline = Color(0xFF3A465F),
    error = Color(0xFFFF7B8A)
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UVKeyboardColorScheme,
        typography = Typography,
        content = content
    )
}
