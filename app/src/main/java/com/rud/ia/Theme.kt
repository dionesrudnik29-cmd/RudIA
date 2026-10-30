package com.rud.ia

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0A0612)
val CardSurface = Color(0xFF1A1229)
val CardBorder = Color(0x33FFFFFF)
val Violet = Color(0xFF9B5CFF)
val VioletDeep = Color(0xFF6B3FD4)
val Lavender = Color(0xFFD6BCFF)
val TextWhite = Color(0xFFF7F1FF)
val Muted = Color(0xFFB9AACD)
val UserBubble = Color(0xFF3A2357)
val StatusReady = Color(0xFF66BB6A)
val StatusWaiting = Color(0xFFFFD54F)
val StatusError = Color(0xFFEF5350)

private val RudColors = darkColorScheme(
    primary = Violet,
    secondary = Lavender,
    background = Background,
    surface = CardSurface,
    onPrimary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun RudTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RudColors, content = content)
}
