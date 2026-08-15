package com.kamsiob.meedwell.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The palette, read out of `reference/meedwell-screen-grid-CURRENT.html`.
 *
 * **The grid is the measurement authority.** Every value below is copied from
 * the `:root` block of that file rather than described from `DESIGN.md`. Where
 * the two disagree, the grid wins and the prose gets corrected. Building from a
 * translated prose description is how the first version of this file ended up
 * with a violet-tinted near-black and five glow washes that appear nowhere in
 * the design.
 *
 * ## Daylight is the default
 *
 * `--paper` #F6F4EC is the ground the app opens on. Warm paper, not white, not
 * grey, not dark. Lamplight is the alternate, and it is deep pine #12160F
 * rather than a neutral black: an evening ground for a green-inked app.
 *
 * ## One accent
 *
 * Moss #5C7358 is the only color that does working color. Everything else is
 * reserved and appears in exactly one place:
 *
 *  - **Copper** #AE6738: the app mark, and the sun on the day line. Nowhere
 *    else, ever.
 *  - **Gold ink** #8A6215: the two "Support this work" blocks. Nowhere else.
 *  - **Alarm** #8C4A2F: destructive rows. Nowhere else.
 *
 * There is no purple, no gradient, no glassmorphism, no tinted container, and
 * no Material dynamic color.
 */

// ---------- Daylight, the default ground ----------

/** `--paper`. The ground. */
val Paper = Color(0xFFF6F4EC)

/** `--paper-2`. A recess in the paper, never a raised card. */
val Paper2 = Color(0xFFEFEBDF)

/** `--ink`. Primary. */
val Ink = Color(0xFF1C2420)

/** `--ink-2`. Secondary. */
val Ink2 = Color(0xFF3D473F)

/** `--ink-3`. Tertiary, and the floor. Nothing on paper is lighter than this. */
val Ink3 = Color(0xFF57605A)

/** `--hair`, rgba(28,36,32,.12). The hairline that carries structure. */
val Hair = Color(0x1F1C2420)

/** `--hair-2`, rgba(28,36,32,.22). The heavier rule and the middle staff line. */
val Hair2 = Color(0x381C2420)

// ---------- Lamplight, the alternate ground ----------

/** `--lamp`. Deep pine, not black. */
val Lamp = Color(0xFF12160F)
val LampInk = Color(0xFFEFEEE6)
val Lamp2 = Color(0xFFB4B8AC)
val Lamp3 = Color(0xFF8A8F84)

/** `--lamp-hair`, rgba(239,238,230,.12). */
val LampHair = Color(0x1FEFEEE6)

/** `--lamp-hair-2`, rgba(239,238,230,.2). */
val LampHair2 = Color(0x33EFEEE6)

// ---------- The reserved colors ----------

/** `--moss`. The single working accent. */
val Moss = Color(0xFF5C7358)

/** `--moss-deep`. The playing row's ink on paper. */
val MossDeep = Color(0xFF43563F)

/** `--lamp-moss`. Moss, lifted so it reads on pine. */
val LampMoss = Color(0xFF7E9478)

/**
 * `--copper`. The app mark and the day line's sun.
 *
 * Nothing else in the entire app is copper. It is not an accent; it is the
 * mark's own color, on loan to the one other thing that is literally a sun.
 */
val Copper = Color(0xFFAE6738)

/** `--gold-ink`. The two support blocks and nothing else. */
val GoldInk = Color(0xFF8A6215)

/** `--alarm`. Destructive rows and nothing else. */
val Alarm = Color(0xFF8C4A2F)

/** The field behind the app mark, which is its own thing rather than a theme color. */
val MarkField = Color(0xFF16121C)

/**
 * The app icon finishes, `DESIGN.md` section 7. Switched through Android's
 * activity alias technique, which is instant, offline and free. The coin stays
 * at rest in its cradle in every finish.
 */
enum class IconFinish(val mark: Color, val field: Color) {
    RusticCopper(Copper, MarkField),
    Dusk(Color(0xFF8B84AE), Color(0xFF12121B)),
    Moss(Color(0xFF7C8F5E), Color(0xFF11150D)),
    Ink(Color(0xFFEFEEE6), Color(0xFF12160F)),
    Paper(Color(0xFF1C2420), Color(0xFFF6F4EC)),
}

/**
 * Meedwell's own palette.
 *
 * There is **no `surfacePanel` any more.** It was the token that let cards in.
 * Structure in this design is carried by hairlines and whitespace only, so a
 * filled container has nowhere to get its fill from, which is the point.
 */
@Immutable
data class MeedwellColors(
    /** The ground. Paper by day, Lamplight by night. */
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    /** The tertiary floor. Nothing is fainter than this. */
    val tertiaryText: Color,
    /** The hairline that carries structure, and the outer staff lines. */
    val hairline: Color,
    /** The heavier rule, and the middle staff line. */
    val hairline2: Color,
    /**
     * A recess, used only where the grid uses `--paper-2`: never as a card, and
     * never with a shadow under it.
     */
    val recess: Color,
    /** The one working accent. */
    val moss: Color,
    /** Moss as ink, for a playing row's text. */
    val mossInk: Color,
    /** The mark and the sun. Nothing else. */
    val copper: Color,
    /** The support blocks. Nothing else. */
    val gold: Color,
    /** Destructive rows. Nothing else. */
    val alarm: Color,
    val isDark: Boolean,
)

/** Lamplight. The alternate. */
val LamplightColors = MeedwellColors(
    background = Lamp,
    primaryText = LampInk,
    secondaryText = Lamp2,
    tertiaryText = Lamp3,
    hairline = LampHair,
    hairline2 = LampHair2,
    // Lamplight has no recessed surface in the grid. Kept equal to the ground
    // so any accidental use is invisible rather than a card appearing at night.
    recess = Lamp,
    moss = LampMoss,
    mossInk = LampMoss,
    copper = Copper,
    gold = Color(0xFFC9A34E),
    alarm = Color(0xFFC57A57),
    isDark = true,
)

/** Daylight. The default. */
val DaylightColors = MeedwellColors(
    background = Paper,
    primaryText = Ink,
    secondaryText = Ink2,
    tertiaryText = Ink3,
    hairline = Hair,
    hairline2 = Hair2,
    recess = Paper2,
    moss = Moss,
    mossInk = MossDeep,
    copper = Copper,
    gold = GoldInk,
    alarm = Alarm,
    isDark = false,
)
