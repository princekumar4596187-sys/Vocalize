package com.vocalize.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Color Palette ────────────────────────────────────────────
val VocalizeRed = Color(0xFFEF4444)
val VocalizeRedDark = Color(0xFFDC2626)
val VocalizePurple = Color(0xFF8B5CF6)
val VocalizePurpleDark = Color(0xFF7C3AED)
val VocalizeGray900 = Color(0xFF0F0F10)
val VocalizeGray800 = Color(0xFF1A1A2E)
val VocalizeGray700 = Color(0xFF16213E)
val VocalizeGray600 = Color(0xFF0F3460)
val VocalizeGray400 = Color(0xFF6B7280)
val VocalizeGray200 = Color(0xFFE5E7EB)
val VocalizeGray100 = Color(0xFFF9FAFB)
val VocalizeSurface = Color(0xFF1C1C2E)
val VocalizeCardDark = Color(0xFF252540)
val VocalizeAccentBlue = Color(0xFF3B82F6)
val VocalizeGreen = Color(0xFF10B981)
val VocalizeOrange = Color(0xFFF59E0B)
val VocalizeWhite = Color(0xFFFFFFFF)

// ─── Dark Color Scheme ────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = VocalizeRed,
    onPrimary = VocalizeWhite,
    primaryContainer = Color(0xFF7F1D1D),
    onPrimaryContainer = Color(0xFFFEE2E2),
    secondary = VocalizePurple,
    onSecondary = VocalizeWhite,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = VocalizeAccentBlue,
    onTertiary = VocalizeWhite,
    background = VocalizeGray900,
    onBackground = Color(0xFFF1F5F9),
    surface = VocalizeSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = VocalizeCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),
    error = Color(0xFFF87171),
    onError = VocalizeWhite
)

// ─── Light Color Scheme ────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = VocalizeRed,
    onPrimary = VocalizeWhite,
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = VocalizePurpleDark,
    onSecondary = VocalizeWhite,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = VocalizeAccentBlue,
    onTertiary = VocalizeWhite,
    background = VocalizeGray100,
    onBackground = Color(0xFF111827),
    surface = VocalizeWhite,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF374151),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = VocalizeWhite
)

@Composable
fun VocalizeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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
        typography = VocalizeTypography,
        content = content
    )
}
