package com.example.doitalready.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NeoColorScheme = lightColorScheme(
    primary = NeoTeal,
    onPrimary = NeoBlack,
    primaryContainer = NeoTeal,
    onPrimaryContainer = NeoBlack,
    secondary = NeoYellow,
    onSecondary = NeoBlack,
    secondaryContainer = NeoYellow,
    onSecondaryContainer = NeoBlack,
    tertiary = NeoMagenta,
    onTertiary = NeoBlack,
    tertiaryContainer = NeoMint,
    onTertiaryContainer = NeoBlack,
    background = NeoBeige,
    onBackground = NeoBlack,
    surface = NeoWhite,
    onSurface = NeoBlack,
    surfaceVariant = NeoWhite,
    onSurfaceVariant = NeoGrayText,
    outline = NeoBlack,
    outlineVariant = NeoBlack,
    error = NeoError,
    onError = NeoWhite
)

private val NeoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

// Tema aplikasi. Sengaja hanya mode terang dan tanpa dynamic color, supaya palet Neo-Brutalist selalu konsisten
@Composable
fun DoItAlreadyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeoColorScheme,
        typography = AppTypography,
        shapes = NeoShapes,
        content = content
    )
}