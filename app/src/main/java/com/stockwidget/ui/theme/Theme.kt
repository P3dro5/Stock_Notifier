package com.stockwidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────

val GainGreen   = Color(0xFF00C853)
val LossRed     = Color(0xFFFF1744)

val DarkBackground = Color(0xFF0D1117)
val DarkSurface    = Color(0xFF161B22)
val DarkCard       = Color(0xFF21262D)

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF58A6FF),
    secondary        = Color(0xFF79C0FF),
    background       = DarkBackground,
    surface          = DarkSurface,
    onPrimary        = Color.White,
    onBackground     = Color(0xFFE6EDF3),
    onSurface        = Color(0xFFE6EDF3),
    surfaceVariant   = DarkCard,
    outline          = Color(0xFF30363D)
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF0969DA),
    secondary        = Color(0xFF1F6FEB),
    background       = Color(0xFFF6F8FA),
    surface          = Color.White,
    onPrimary        = Color.White,
    onBackground     = Color(0xFF1F2328),
    onSurface        = Color(0xFF1F2328),
    surfaceVariant   = Color(0xFFF0F3F6),
    outline          = Color(0xFFD1D9E0)
)

@Composable
fun StockWidgetTheme(
    darkTheme: Boolean = true,  // default dark for finance app
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}
