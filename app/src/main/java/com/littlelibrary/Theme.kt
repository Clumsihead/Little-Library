package com.littlelibrary

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Warm paper, walnut shelves, and the muted colours of old cloth-bound books. */
object Cozy {
    val Paper = Color(0xFFF4EBDD)
    val PaperDeep = Color(0xFFE8D9C0)
    val Card = Color(0xFFFBF5EA)
    val Walnut = Color(0xFF4A3426)
    val WalnutLight = Color(0xFF7A5A43)
    val Ink = Color(0xFF3B2A20)
    val InkSoft = Color(0xFF8A7360)
    val Gold = Color(0xFFE2BE6E)

    val Burgundy = Color(0xFF8C3B3B)
    val Forest = Color(0xFF3F5B45)
    val Mustard = Color(0xFFB98A33)
    val Navy = Color(0xFF3E4C6B)
    val Plum = Color(0xFF6B4560)
    val Teal = Color(0xFF3F6B6B)

    private val spines = listOf(Burgundy, Forest, Mustard, Navy, Plum, Teal)

    /** Every tag always gets the same book colour. */
    fun spineColor(name: String): Color = spines[(name.hashCode() and 0x7fffffff) % spines.size]
}

@Composable
fun LibraryTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val serif = FontFamily.Serif
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Cozy.Burgundy,
            onPrimary = Cozy.Card,
            secondary = Cozy.Forest,
            background = Cozy.Paper,
            onBackground = Cozy.Ink,
            surface = Cozy.Card,
            onSurface = Cozy.Ink,
        ),
        typography = Typography(
            headlineSmall = base.headlineSmall.copy(fontFamily = serif),
            titleLarge = base.titleLarge.copy(fontFamily = serif),
            titleMedium = base.titleMedium.copy(fontFamily = serif),
            bodyLarge = base.bodyLarge.copy(fontFamily = serif),
            bodyMedium = base.bodyMedium.copy(fontFamily = serif),
            labelLarge = base.labelLarge.copy(fontFamily = serif),
        ),
        content = content,
    )
}
