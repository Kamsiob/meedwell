package com.kamsiob.meedwell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Theme choice. **Dark is the default**, per `DESIGN.md` section 2. */
enum class ThemeChoice { Dark, Light, System }

val LocalMeedwellColors = staticCompositionLocalOf { DarkColors }
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
 * Dynamic color is deliberately not used. The whole design rests on artwork
 * being the only color that matters and on gold meaning exactly one thing;
 * letting the wallpaper repaint the interface would break both.
 */
@Composable
fun MeedwellTheme(
    themeChoice: ThemeChoice = ThemeChoice.Dark,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeChoice) {
        ThemeChoice.Dark -> true
        ThemeChoice.Light -> false
        ThemeChoice.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors

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
            primary = colors.primaryText,
            onPrimary = colors.background,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.primaryText,
            onSurface = colors.primaryText,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.hairline,
            primary = colors.primaryText,
            onPrimary = colors.background,
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
