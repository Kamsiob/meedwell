package com.kamsiob.meedwell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Theme choice. **Daylight is the default.**
 *
 * The grid is unambiguous: the app opens on warm paper. Dark is the alternate
 * and it is called Lamplight, because it is a deep pine evening ground rather
 * than a neutral night mode.
 *
 * The names are the ones the design uses. `Dark` and `Light` were generic
 * words for a generic result, and an enum that says `Daylight` is harder to
 * quietly default the wrong way round.
 */
enum class ThemeChoice { Daylight, Lamplight, System }

val LocalMeedwellColors = staticCompositionLocalOf { DaylightColors }
val LocalMeedwellTypography = staticCompositionLocalOf { MeedwellTypography() }

/**
 * Whether the system has asked for reduced motion.
 *
 * Provided as a composition local so that every animated surface reads the same
 * value from one place. `DESIGN.md` section 10 requires it to be respected
 * everywhere: no drift on the ambient washes, no shimmer on waiting tiles, and
 * a static waveform envelope rather than a moving one.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

object MeedwellTheme {
    val colors: MeedwellColors
        @Composable @ReadOnlyComposable get() = LocalMeedwellColors.current

    val typography: MeedwellTypography
        @Composable @ReadOnlyComposable get() = LocalMeedwellTypography.current

    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

/**
 * A fully custom theme over Material 3, rather than Material 3 with a tint.
 *
 * Dynamic color is deliberately not used, and neither is any Material surface
 * elevation. The design has exactly one working accent and carries all of its
 * structure in hairlines and whitespace, so a wallpaper-derived palette or a
 * tonal elevation overlay would both put color and fills where the design has
 * none.
 *
 * Material's `surface` is therefore set equal to the ground rather than to a
 * lighter tone, and `surfaceVariant` too: if a Material component is ever
 * pulled in, it renders flat rather than as a card.
 */
@Composable
fun MeedwellTheme(
    themeChoice: ThemeChoice = ThemeChoice.Daylight,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeChoice) {
        ThemeChoice.Lamplight -> true
        ThemeChoice.Daylight -> false
        ThemeChoice.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) LamplightColors else DaylightColors

    // Material's scheme is filled in from Meedwell's own tokens so that any
    // Material component pulled in later lands inside the design rather than
    // beside it. Never pure black or pure white, per DESIGN.md section 2.
    val materialScheme = if (dark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.hairline,
            primary = colors.moss,
            onPrimary = colors.background,
            // Equal to the ground, so nothing Material draws can become a card.
            surfaceVariant = colors.background,
            surfaceContainer = colors.background,
            surfaceContainerHigh = colors.background,
            surfaceContainerHighest = colors.background,
            surfaceContainerLow = colors.background,
            surfaceContainerLowest = colors.background,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.hairline,
            primary = colors.moss,
            onPrimary = colors.background,
            surfaceVariant = colors.background,
            surfaceContainer = colors.background,
            surfaceContainerHigh = colors.background,
            surfaceContainerHighest = colors.background,
            surfaceContainerLow = colors.background,
            surfaceContainerLowest = colors.background,
        )
    }

    CompositionLocalProvider(
        LocalMeedwellColors provides colors,
        LocalMeedwellTypography provides MeedwellTypography(),
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
