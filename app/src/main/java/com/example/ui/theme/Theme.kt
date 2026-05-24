package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Define standard helper color for error states (destructive color)
private val ColorTypeErrors = androidx.compose.ui.graphics.Color(0xFFBA1A1A)

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    secondary = SleekSecondary,
    background = SleekBg,
    surface = SleekSurface,
    onPrimary = SleekSurface,
    onBackground = SleekTextMain,
    onSurface = SleekTextMain,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekTextAlt,
    primaryContainer = SleekFolderBg,
    onPrimaryContainer = SleekFolderText,
    secondaryContainer = SleekCodeBg,
    onSecondaryContainer = SleekCodeText,
    error = ColorTypeErrors
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Sleek Interface is light-themed by default
    dynamicColor: Boolean = false, // Force the hand-crafted Sleek palette
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SleekColorScheme,
        typography = Typography,
        content = content
    )
}
