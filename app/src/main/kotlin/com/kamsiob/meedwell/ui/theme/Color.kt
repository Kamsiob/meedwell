package com.kamsiob.meedwell.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The color tokens from `DESIGN.md` section 2, which is a binding document.
 * Where this file and that document disagree, that document wins.
 *
 * Two rules are load bearing rather than stylistic:
 *
 *  - **The light mode ink floor.** Nothing lighter than slate `#56525E` on
 *    paper, ever. It was corrected twice in design and is not open to
 *    reinterpretation. Measured on `#F5F3ED`: `#17151D` 16.3:1, `#33303B`
 *    11.6:1, `#44414C` 9.0:1, `#56525E` 6.9:1.
 *  - **The gold rule.** Gold means money reaching makers and nothing else. It
 *    has exactly two uses in the entire app: the Bandcamp Friday dot, and the
 *    "Support this work" button. Light gold is `#8A6215` at 4.93:1, corrected
 *    from `#9A6F1E`, which measured 4.06:1 and failed AA for a 14sp label.
 *
 * Any new color pair introduced anywhere gets measured before it ships. There
 * is a contrast test in the unit suite that fails the build if a token pair
 * drops below its floor.
 */

// Dark, the default.
val DarkBackground = Color(0xFF0B0B0E)      // warm near-black
val DarkPrimaryText = Color(0xFFF4F3F6)
val DarkSecondaryText = Color(0xFFA7A5B1)
val DarkTertiaryText = Color(0xFF8B8993)
val DarkHairline = Color(0x14FFFFFF)        // rgba(255,255,255,.08)
val DarkSurfacePanel = Color(0x0DFFFFFF)    // rgba(255,255,255,.05)
val DarkGold = Color(0xFFE7C171)            // 11.5:1 on #0B0B0E

// Light, warm paper.
val LightBackground = Color(0xFFF5F3ED)
val LightPrimaryText = Color(0xFF17151D)
val LightSecondaryText = Color(0xFF33303B)
val LightSecondaryTextSoft = Color(0xFF44414C)
val LightTertiaryText = Color(0xFF56525E)   // the floor. Never lighter than this.
val LightHairline = Color(0x1C1E1C26)       // rgba(30,28,38,.11)
val LightSurfacePanel = Color(0x0A1E1C26)   // rgba(30,28,38,.04)
val LightGold = Color(0xFF8A6215)           // 4.93:1 on #F5F3ED

/** The mark, in both themes. It never changes color with the theme. */
val Copper = Color(0xFFAE6738)
val MarkField = Color(0xFF16121C)

/**
 * The ambient glow washes, `DESIGN.md` section 2. Soft radial fields that drift
 * over roughly 16 seconds, at half opacity in light theme, and absent entirely
 * when the system asks for reduced motion.
 */
val GlowViolet = Color(0x42705496)          // rgba(112,84,150,.26)
val GlowTeal = Color(0x3D3A7A74)            // rgba(58,122,116,.24)
val GlowRose = Color(0x38964A66)            // rgba(150,74,102,.22)
val GlowEmber = Color(0x38A86842)           // rgba(168,104,66,.22)

/**
 * The app icon finishes, `DESIGN.md` section 7. Switched through Android's
 * activity alias technique, which is instant, offline and free. The coin stays
 * at rest in its cradle in every finish.
 */
enum class IconFinish(val mark: Color, val field: Color) {
    RusticCopper(Color(0xFFAE6738), Color(0xFF16121C)),
    Dusk(Color(0xFF8B84AE), Color(0xFF12121B)),
    Moss(Color(0xFF7C8F5E), Color(0xFF11150D)),
    Ink(Color(0xFFF4F3F6), Color(0xFF0B0B0E)),
    Paper(Color(0xFF17151D), Color(0xFFF5F3ED)),
}

/**
 * Meedwell's own palette, carried beside Material 3's rather than bent into it.
 *
 * Material's color roles do not have a place for "the tertiary ink floor" or
 * "the one color that means money reaching makers", and forcing them into
 * `surfaceVariant` and `tertiary` would lose the meaning that makes them rules.
 */
@Immutable
data class MeedwellColors(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val hairline: Color,
    val surfacePanel: Color,
    val gold: Color,
    val copper: Color,
    val isDark: Boolean,
)

val DarkColors = MeedwellColors(
    background = DarkBackground,
    primaryText = DarkPrimaryText,
    secondaryText = DarkSecondaryText,
    tertiaryText = DarkTertiaryText,
    hairline = DarkHairline,
    surfacePanel = DarkSurfacePanel,
    gold = DarkGold,
    copper = Copper,
    isDark = true,
)

val LightColors = MeedwellColors(
    background = LightBackground,
    primaryText = LightPrimaryText,
    secondaryText = LightSecondaryText,
    tertiaryText = LightTertiaryText,
    hairline = LightHairline,
    surfacePanel = LightSurfacePanel,
    gold = LightGold,
    copper = Copper,
    isDark = false,
)
