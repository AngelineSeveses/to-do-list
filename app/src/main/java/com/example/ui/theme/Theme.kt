package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkMintPrimary,
    onPrimary = DarkForestOnPrimary,
    primaryContainer = DarkForestContainer,
    onPrimaryContainer = LightMintOnContainer,
    secondary = DarkTerracotta,
    onSecondary = DarkTerracottaOn,
    secondaryContainer = DarkTerracottaContainer,
    onSecondaryContainer = LightPeachOnContainer,
    tertiary = DarkSage,
    onTertiary = DarkSageOn,
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = LightSageOnContainer,
    background = DarkCharcoalBg,
    onBackground = OffWhiteText,
    surface = DarkOliveSurface,
    onSurface = OffWhiteText,
    surfaceVariant = DarkOliveVariant,
    onSurfaceVariant = MutedStoneText,
    outline = DarkOliveOutline,
    outlineVariant = DarkHairline,
    error = DarkCoralError,
    errorContainer = DarkCoralContainer,
    onErrorContainer = DarkOnCoralContainer
)

private val LightColorScheme = lightColorScheme(
    primary = ForestInk,
    onPrimary = CreamOnPrimary,
    primaryContainer = SoftMintContainer,
    onPrimaryContainer = DeepForestText,
    secondary = Terracotta,
    onSecondary = WarmWhite,
    secondaryContainer = PeachMistContainer,
    onSecondaryContainer = DeepTerracotta,
    tertiary = SageAccent,
    onTertiary = WarmWhite,
    tertiaryContainer = SageTintContainer,
    onTertiaryContainer = DeepSage,
    background = WarmLinenBackground,
    onBackground = CharcoalInk,
    surface = PaperSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSandVariant,
    onSurfaceVariant = SlateSecondaryText,
    outline = SubtleSandOutline,
    outlineVariant = SoftHairline,
    error = CoralError,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralOnErrorContainer
)

val DaylistShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DaylistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DaylistTypography,
        shapes = DaylistShapes,
        content = content
    )
}
