package com.blanksstudio.gridpix.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.blanksstudio.gridpix.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E5AA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF00696E),
    secondaryContainer = Color(0xFF6FF6FD),
    tertiary = Color(0xFF7B5800),
    tertiaryContainer = Color(0xFFFFDEA6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF4CD9E0),
    secondaryContainer = Color(0xFF004F53),
    tertiary = Color(0xFFF7BD48),
    tertiaryContainer = Color(0xFF5D4200),
)

/** Board colours that must read well in both themes. */
object BoardColors {
    val filledLight = Color(0xFF1F2933)
    val filledDark = Color(0xFFE8EDF2)
    val mistake = Color(0xFFD32F2F)
    val hint = Color(0xFF2E7D32)
}

/**
 * App theme. [mode] comes from Settings (SPEC S7). Dynamic colour is used on Android 12+
 * for the system choice; an explicit light/dark choice uses the GridPix palette so the
 * board colours stay predictable.
 */
@Composable
fun GridPixTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        mode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
