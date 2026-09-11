package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VisionColorScheme = lightColorScheme(
    primary = VisionDeepPlum,
    onPrimary = Color.White,
    primaryContainer = VisionLilacLight,
    onPrimaryContainer = VisionDeepPlum,
    secondary = VisionPrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = VisionLilacPill,
    onSecondaryContainer = VisionDeepPlum,
    tertiary = VisionEmerald,
    onTertiary = Color.White,
    background = VisionBackground,
    onBackground = VisionTextPrimary,
    surface = VisionSurface,
    onSurface = VisionTextPrimary,
    surfaceVariant = VisionLilacPill,
    onSurfaceVariant = VisionTextSecondary,
    outline = VisionCardBorder
)

@Composable
fun VisionTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VisionBackground.toArgb()
            window.navigationBarColor = VisionSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = VisionColorScheme,
        typography = Typography,
        content = content
    )
}

