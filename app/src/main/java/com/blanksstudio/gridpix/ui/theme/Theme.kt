package com.blanksstudio.gridpix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.blanksstudio.gridpix.data.settings.ThemeMode

/*
 * GridPix brand palette. Always used (no Material You wallpaper colours) so the game looks the same,
 * bright and recognisable, on every phone.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4DF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFFF4F8B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = Color(0xFF12A594),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC6F4EB),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFF6F3FF),
    onBackground = Color(0xFF1C1A27),
    surface = Color(0xFFF6F3FF),
    onSurface = Color(0xFF1C1A27),
    surfaceVariant = Color(0xFFE9E4F5),
    onSurfaceVariant = Color(0xFF5B566B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF9FF),
    surfaceContainer = Color(0xFFF1EDFB),
    surfaceContainerHigh = Color(0xFFECE7F8),
    surfaceContainerHighest = Color(0xFFE6E0F4),
    outline = Color(0xFF8A849A),
    outlineVariant = Color(0xFFD6D0E4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBBA8FF),
    onPrimary = Color(0xFF2B0C7A),
    primaryContainer = Color(0xFF4A2FC4),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFFF8FB3),
    onSecondary = Color(0xFF5E1131),
    secondaryContainer = Color(0xFF8A1F4A),
    onSecondaryContainer = Color(0xFFFFD9E3),
    tertiary = Color(0xFF5DE0CB),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF00504A),
    onTertiaryContainer = Color(0xFFC6F4EB),
    background = Color(0xFF13111C),
    onBackground = Color(0xFFECE8F6),
    surface = Color(0xFF13111C),
    onSurface = Color(0xFFECE8F6),
    surfaceVariant = Color(0xFF2A2638),
    onSurfaceVariant = Color(0xFFC9C3D9),
    surfaceContainerLowest = Color(0xFF0E0C15),
    surfaceContainerLow = Color(0xFF1A1725),
    surfaceContainer = Color(0xFF1F1C2C),
    surfaceContainerHigh = Color(0xFF292537),
    surfaceContainerHighest = Color(0xFF332E43),
    outline = Color(0xFF948DA7),
    outlineVariant = Color(0xFF443F55),
)

private val BaseTypography = Typography()

/** Heavier headings give the playful, game-like feel without bundling a font file. */
private val GridPixTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
)

/** Colours that must read well in both themes. */
object BoardColors {
    val mistake = Color(0xFFE5484D)
    val hint = Color(0xFF12A594)
}

/** One accent per pack / mode. Used for the board while solving, cards, covers and the collection. */
object Accents {
    val starter = Color(0xFF6C4DF6)
    val anime = Color(0xFFFF4F9A)
    val animals = Color(0xFFF08A24)
    val vehicles = Color(0xFF2F7FED)
    val food = Color(0xFFE5484D)
    val nature = Color(0xFF27A35A)
    val daily = Color(0xFFF5A524)
    val tutorial = Color(0xFF12A594)

    fun forPack(packId: String): Color = when (packId) {
        "starter" -> starter
        "anime" -> anime
        "animals" -> animals
        "vehicles" -> vehicles
        "food" -> food
        "nature" -> nature
        else -> starter
    }

    fun forEndless(size: Int): Color = when (size) {
        5 -> tutorial
        10 -> starter
        15 -> vehicles
        else -> food
    }

    /** A lighter/darker partner colour for gradients. */
    fun partner(accent: Color): Color = Color(
        red = (accent.red + (1f - accent.red) * 0.35f),
        green = (accent.green + (1f - accent.green) * 0.15f),
        blue = (accent.blue + (1f - accent.blue) * 0.45f),
    )

    fun gradient(accent: Color): Brush = Brush.linearGradient(listOf(accent, partner(accent)))

    val heroGradient: Brush = Brush.linearGradient(listOf(Color(0xFF6C4DF6), Color(0xFFB04DF0), Color(0xFFFF4F8B)))
}

@Composable
fun GridPixTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme(mode)) DarkColors else LightColors,
        typography = GridPixTypography,
        content = content,
    )
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
