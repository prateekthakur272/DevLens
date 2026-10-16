package dev.prateekthakur.devlens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Seed: Android green (#3DDC84), expanded into a Material 3 tonal palette by hand
// (no dynamic-color source is available on desktop, so the roles below are picked to satisfy
// M3 contrast guidance in both themes rather than derived at runtime).

private val md_light_primary = Color(0xFF3DDC84)
private val md_light_onPrimary = Color(0xFF00391B)
private val md_light_primaryContainer = Color(0xFFC4F5D6)
private val md_light_onPrimaryContainer = Color(0xFF00210E)
private val md_light_secondary = Color(0xFF2E7D5B)
private val md_light_onSecondary = Color(0xFFFFFFFF)
private val md_light_secondaryContainer = Color(0xFFD5EFE0)
private val md_light_onSecondaryContainer = Color(0xFF0B2016)
private val md_light_tertiary = Color(0xFF00677F)
private val md_light_onTertiary = Color(0xFFFFFFFF)
private val md_light_tertiaryContainer = Color(0xFFBDEAFF)
private val md_light_onTertiaryContainer = Color(0xFF001F29)
private val md_light_error = Color(0xFFBA1A1A)
private val md_light_onError = Color(0xFFFFFFFF)
private val md_light_errorContainer = Color(0xFFFFDAD6)
private val md_light_onErrorContainer = Color(0xFF410002)
private val md_light_background = Color(0xFFF5F6F8)
private val md_light_onBackground = Color(0xFF1A1B20)
private val md_light_surface = Color(0xFFFFFFFF)
private val md_light_onSurface = Color(0xFF1A1B20)
private val md_light_surfaceVariant = Color(0xFFDDE5DD)
private val md_light_onSurfaceVariant = Color(0xFF414942)
private val md_light_outline = Color(0xFF717972)
private val md_light_outlineVariant = Color(0xFFC1C9C1)
private val md_light_surfaceContainerLowest = Color(0xFFFFFFFF)
private val md_light_surfaceContainerLow = Color(0xFFFFFFFF)
private val md_light_surfaceContainer = Color(0xFFF5F5F8)
private val md_light_surfaceContainerHigh = Color(0xFFE6EBE6)
private val md_light_surfaceContainerHighest = Color(0xFFE0E5E0)

private val md_dark_primary = Color(0xFF3DDC84)
private val md_dark_onPrimary = Color(0xFF00391B)
private val md_dark_primaryContainer = Color(0xFF005229)
private val md_dark_onPrimaryContainer = Color(0xFFC4F5D6)
private val md_dark_secondary = Color(0xFF86D7AC)
private val md_dark_onSecondary = Color(0xFF00391E)
private val md_dark_secondaryContainer = Color(0xFF1F4D36)
private val md_dark_onSecondaryContainer = Color(0xFFD5EFE0)
private val md_dark_tertiary = Color(0xFF5DD5FC)
private val md_dark_onTertiary = Color(0xFF003544)
private val md_dark_tertiaryContainer = Color(0xFF004D62)
private val md_dark_onTertiaryContainer = Color(0xFFBDEAFF)
private val md_dark_error = Color(0xFFFFB4AB)
private val md_dark_onError = Color(0xFF690005)
private val md_dark_errorContainer = Color(0xFF93000A)
private val md_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_dark_background = Color(0xFF121318)
private val md_dark_onBackground = Color(0xFFE4E2E9)
private val md_dark_surface = Color(0xFF121318)
private val md_dark_onSurface = Color(0xFFE4E2E9)
private val md_dark_surfaceVariant = Color(0xFF414942)
private val md_dark_onSurfaceVariant = Color(0xFFC1C9C1)
private val md_dark_outline = Color(0xFF8B938B)
private val md_dark_outlineVariant = Color(0xFF414942)
private val md_dark_surfaceContainerLowest = Color(0xFF0C0D12)
private val md_dark_surfaceContainerLow = Color(0xFF1A1B20)
private val md_dark_surfaceContainer = Color(0xFF1E1F25)
private val md_dark_surfaceContainerHigh = Color(0xFF29292F)
private val md_dark_surfaceContainerHighest = Color(0xFF34343A)

val DevLensLightColors = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary,
    onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    error = md_light_error,
    onError = md_light_onError,
    errorContainer = md_light_errorContainer,
    onErrorContainer = md_light_onErrorContainer,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    surfaceContainerLowest = md_light_surfaceContainerLowest,
    surfaceContainerLow = md_light_surfaceContainerLow,
    surfaceContainer = md_light_surfaceContainer,
    surfaceContainerHigh = md_light_surfaceContainerHigh,
    surfaceContainerHighest = md_light_surfaceContainerHighest,
)

val DevLensDarkColors = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant,
    surfaceContainerLowest = md_dark_surfaceContainerLowest,
    surfaceContainerLow = md_dark_surfaceContainerLow,
    surfaceContainer = md_dark_surfaceContainer,
    surfaceContainerHigh = md_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_dark_surfaceContainerHighest,
)

/** Semantic severity/risk tones layered on top of the M3 scheme (success has no M3 role, so we add one). */
object DevLensExtraColors {
    val successLight = Color(0xFF2E7D32)
    val successContainerLight = Color(0xFFC8F5C6)
    val onSuccessContainerLight = Color(0xFF002204)
    val successDark = Color(0xFF8FDB8C)
    val successContainerDark = Color(0xFF0F5132)
    val onSuccessContainerDark = Color(0xFFC8F5C6)

    val warningLight = Color(0xFFEF6C00)
    val warningContainerLight = Color(0xFFFFDDB3)
    val onWarningContainerLight = Color(0xFF2A1700)
    val warningDark = Color(0xFFFFB877)
    val warningContainerDark = Color(0xFF7A4100)
    val onWarningContainerDark = Color(0xFFFFDDB3)
}

@Composable
fun DevLensTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DevLensDarkColors else DevLensLightColors
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            shapes = DevLensShapes,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}
