package com.yueyin.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary, onPrimary = Color.White, primaryContainer = PrimaryLight,
    background = Background, surface = Surface, surfaceVariant = SurfaceVariant,
    onBackground = OnBackground, onSurface = OnSurface, onSurfaceVariant = OnSurfaceVariant,
    outline = Border, error = Primary
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary, onPrimary = Color.White, primaryContainer = PrimaryLight,
    background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground, onSurface = DarkOnSurface, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder, error = Primary,
    surfaceTint = DarkSurface,
    tertiaryContainer = DarkElevated
)

@Composable
fun YueYinTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
