package com.yueyin.ui.theme

import android.app.Activity
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

@Composable
fun YueYinTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true; isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(colorScheme = LightColorScheme, typography = Typography(), content = content)
}
