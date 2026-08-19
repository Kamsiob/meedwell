package com.kamsiob.meedwell.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.R

/**
 * The type scale, read straight out of
 * `reference/meedwell-screen-grid-CURRENT.html`.
 *
 * **The grid is the measurement authority.** Every size, weight, letter spacing
 * and line height below is the value in that file's CSS, and the name of each
 * token is the CSS class it comes from, so the two can be checked against each
 * other in a minute rather than argued about.
 *
 * **Grid pixels multiply by 1.25 to reach sp. Lengths do not.**
 *
 * The mock is 330px wide standing for roughly a 412dp screen, so its 286px
 * content column maps to this device's 367dp one: a factor of about 1.28. Type
 * set at 1:1 therefore rendered the whole app at about 80 percent of the size
 * `DESIGN.md` section 9 specifies, with `.plate`, `.tabLabel`, `.section` and
 * `.meta` all under the 12sp floor that document sets.
 *
 * That was the mechanical reason the app read as flat. When a heading is 13.5sp
 * and its body is 13.5sp, no amount of spacing makes one lead the other. The
 * owner arbitrated it on 15 August 2026 in favor of `DESIGN.md`, which the
 * project's own rules say wins on anything visual, and `DECISIONS.md` records
 * it so the multiplier is not quietly removed a third time.
 *
 * **Lengths are still 1:1.** The screen gutter is 22px in the mock and 22dp on
 * the device, because that number was given directly rather than derived. Only
 * type is scaled.
 *
 * Presence is not meant to come from bigger bold sans. The serif dynamics line
 * and the staff carry more of it than the title does.
 *
 * Both fonts are bundled rather than fetched. An app that fetched its fonts
 * would be making a network call it tells users it does not make.
 */

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_variable, FontWeight.Normal),
    Font(R.font.instrument_sans_variable, FontWeight.Medium),
    Font(R.font.instrument_sans_variable, FontWeight.SemiBold),
    Font(R.font.instrument_sans_variable, FontWeight.Bold),
)

/**
 * The serif, and it is **italic for voice lines only**.
 *
 * One voice line per screen at most, and it carries the emotional weight. The
 * regular cut is bundled for the missing-cover placeholder letter and for the
 * Roman numerals on a programme, which are set in italic there too.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Trims the extra leading above and below a heading so it sits on its baseline. */
private val TrimBoth = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

@Immutable
data class MeedwellTypography(

    /** `.h1`: 21px, 700, letter-spacing -.1px. A screen title. */
    val h1: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.2.sp,
        lineHeight = 32.5.sp,
        letterSpacing = (-0.12).sp,
        lineHeightStyle = TrimBoth,
    ),

    /** `.h2`: 18px, 700. */
    val h2: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.5.sp,
        lineHeight = 28.8.sp,
        lineHeightStyle = TrimBoth,
    ),

    /**
     * `.voice`: Instrument Serif italic, 15px, ink-2.
     *
     * One per screen at most. This is the only place the serif appears in
     * running text, and it is what stops the app reading like a spec sheet.
     */
    val voice: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 18.8.sp,
        lineHeight = 26.2.sp,
    ),

    /** `.bd`: 13.5px, line-height 1.62, ink-2. Running body copy. */
    val body: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.9.sp,
        lineHeight = 27.4.sp,
    ),

    /** `.sec`: 10.5px, 600, letter-spacing 2px, uppercase, ink-3. Section head. */
    val section: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.1.sp,
        lineHeight = 17.5.sp,
        letterSpacing = 2.5.sp,
    ),

    /** `.meta`: 11.5px, ink-3, tabular figures. */
    val meta: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.4.sp,
        lineHeight = 18.8.sp,
        fontFeatureSettings = "tnum",
    ),

    /**
     * `.plate`: 9.5px, letter-spacing 1.4px, uppercase, ink-3.
     *
     * The label-and-year line under an album, set the way a printed plate line
     * is set. The smallest type in the app, and deliberately so: it is a
     * credit, read once.
     */
    val plate: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.2.sp,
        letterSpacing = 1.75.sp,
    ),

    /** `.row .t`: 13.5px, 500. */
    val rowTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.9.sp,
        lineHeight = 22.5.sp,
    ),

    /** `.row .s`: 11.5px, ink-3, line-height 1.4. */
    val rowSub: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.4.sp,
        lineHeight = 20.1.sp,
    ),

    /** `.btn`: 14px, 600, letter-spacing .1px. */
    val button: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.5.sp,
        lineHeight = 22.5.sp,
        letterSpacing = 0.12.sp,
    ),

    /** `.tabs .tb`: 10px, 600. */
    val tabLabel: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.2.sp,
        letterSpacing = 0.12.sp,
    ),

    /** `.chip`, `.spill span`: 12 to 12.5px, 600. */
    val chip: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.6.sp,
        lineHeight = 20.sp,
    ),

    /** The album grid caption, 12.5px 600, line-height 1.3. */
    val gridTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.6.sp,
        lineHeight = 20.4.sp,
    ),

    /** `.mini .mt`: 12.5px, 600. */
    val miniTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.6.sp,
        lineHeight = 20.sp,
    ),

    /** `.mini .ma`: 11px, ink-3. */
    val miniSub: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.8.sp,
        lineHeight = 17.5.sp,
    ),

    /**
     * `.mv .n`: the Roman numeral on a programme, Instrument Serif italic
     * 12.5px.
     */
    val movementNumeral: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 15.6.sp,
        lineHeight = 21.2.sp,
    ),

    /** `.mv .nm`: the movement's name, 13px. */
    val movementName: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.2.sp,
        lineHeight = 21.2.sp,
    ),

    /**
     * `.mv .tp`: the tempo marking, Instrument Serif italic 11px.
     *
     * "andante", "adagio". A tempo marking is a word in Italian set in italic,
     * which is how it has been printed for three hundred years.
     */
    val tempo: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 13.8.sp,
        lineHeight = 18.8.sp,
    ),

    /**
     * The player's dynamics line, Instrument Serif italic 13px.
     *
     * "andante · IV of IX · mp". Dynamics replace numbers wherever a number
     * would otherwise do.
     */
    val dynamics: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 16.2.sp,
        lineHeight = 22.5.sp,
    ),

    /** The player's title, 19px 700. */
    val playerTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 23.8.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.12).sp,
        lineHeightStyle = TrimBoth,
    ),

    /** The status bar and any figure that must not jump as it counts. */
    val numeric: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.8.sp,
        lineHeight = 18.8.sp,
        fontFeatureSettings = "tnum",
    ),

    /**
     * The centred programme title on an album, 22px 700.
     *
     * An album is set as a programme: centred titling, a plate line under it,
     * movements below. This is the titling.
     */
    val programme: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 27.5.sp,
        lineHeight = 33.8.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = TrimBoth,
    ),

    /**
     * The large serif opening line, used on the declaration and About.
     *
     * `font-size:29px` in screen 01. The one place the serif is allowed to be
     * the largest thing on a screen.
     */
    val serifOpening: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 36.2.sp,
        lineHeight = 43.5.sp,
    ),
)
