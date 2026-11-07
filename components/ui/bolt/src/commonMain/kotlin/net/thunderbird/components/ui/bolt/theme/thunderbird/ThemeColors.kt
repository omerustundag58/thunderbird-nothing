package net.thunderbird.components.ui.bolt.theme.thunderbird

import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.ThemeColorScheme

/**
 * Nothing OS palette: true black surfaces, monochrome text and a single red accent.
 */
internal val lightThemeColorScheme = ThemeColorScheme(
    primary = Color(0xFFD71921),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDADB),
    onPrimaryContainer = Color(0xFF410006),

    secondary = Color(0xFF0B0B0C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7E7E4),
    onSecondaryContainer = Color(0xFF0B0B0C),

    tertiary = Color(0xFF5C5C60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE7E7E4),
    onTertiaryContainer = Color(0xFF0B0B0C),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF410006),

    surfaceDim = Color(0xFFEDEDE9),
    surface = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B0B0C),
    onSurfaceVariant = Color(0xFF5C5C60),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAF8),
    surfaceContainer = Color(0xFFF4F4F2),
    surfaceContainerHigh = Color(0xFFECECE9),
    surfaceContainerHighest = Color(0xFFE2E2DE),

    inverseSurface = Color(0xFF0B0B0C),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFB00010),

    outline = Color(0xFFC4C4C0),
    outlineVariant = Color(0xFFDDDDD9),

    scrim = Color.Black,

    info = Color(0xFF0B0B0C),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFE7E7E4),
    onInfoContainer = Color(0xFF0B0B0C),

    success = Color(0xFF1E5B31),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE6F2E9),
    onSuccessContainer = Color(0xFF123A1F),

    warning = Color(0xFF7A4E00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFF4DA),
    onWarningContainer = Color(0xFF4A3000),
)

internal val darkThemeColorScheme = ThemeColorScheme(
    primary = Color(0xFFD71921),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3A0A0E),
    onPrimaryContainer = Color(0xFFFFB4B8),

    secondary = Color(0xFFF2F2F2),
    onSecondary = Color(0xFF0A0A0B),
    secondaryContainer = Color(0xFF1B1B1D),
    onSecondaryContainer = Color(0xFFF2F2F2),

    tertiary = Color(0xFFA3A3A7),
    onTertiary = Color(0xFF0A0A0B),
    tertiaryContainer = Color(0xFF1B1B1D),
    onTertiaryContainer = Color(0xFFF2F2F2),

    error = Color(0xFFFF5252),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFF3A0A0E),
    onErrorContainer = Color(0xFFFFB4B8),

    surfaceDim = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceBright = Color(0xFF1B1B1D),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFFA3A3A7),

    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF070708),
    surfaceContainer = Color(0xFF0C0C0E),
    surfaceContainerHigh = Color(0xFF141416),
    surfaceContainerHighest = Color(0xFF1B1B1D),

    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF0A0A0B),
    inversePrimary = Color(0xFFB00010),

    outline = Color(0xFF2E2E31),
    outlineVariant = Color(0xFF1F1F22),

    scrim = Color.Black,

    info = Color(0xFFF2F2F2),
    onInfo = Color(0xFF0A0A0B),
    infoContainer = Color(0xFF1B1B1D),
    onInfoContainer = Color(0xFFF2F2F2),

    success = Color(0xFF8EE7AA),
    onSuccess = Color(0xFF082B16),
    successContainer = Color(0xFF0F2A19),
    onSuccessContainer = Color(0xFF8EE7AA),

    warning = Color(0xFFF5D06F),
    onWarning = Color(0xFF2A1F00),
    warningContainer = Color(0xFF2A2208),
    onWarningContainer = Color(0xFFF5D06F),
)
