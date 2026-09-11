package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VisionColorScheme = darkColorScheme(
    primary = VisionDeepPlum,
    onPrimary = Color.Black,
    primaryContainer = VisionLilacLight,
    onPrimaryContainer = VisionDeepPlum,
    secondary = VisionPrimaryPurple,
    onSecondary = Color.Black,
    secondaryContainer = VisionLilacPill,
    onSecondaryContainer = VisionPrimaryPurple,
    tertiary = VisionEmerald,
    onTertiary = Color.Black,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = VisionColorScheme,
        typography = Typography,
        content = content
    )
}
