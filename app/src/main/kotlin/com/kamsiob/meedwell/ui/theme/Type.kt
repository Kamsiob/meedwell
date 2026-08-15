package com.kamsiob.meedwell.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.R

/**
 * The type scale from `DESIGN.md` section 4.
 *
 * The visual reference is a 330px mock of roughly a 412dp screen, so mock
 * pixels multiply by about 1.25 to reach sp. Those conversions are already done
 * here; the numbers below are real sp.
 *
 * **Nothing in the app is smaller than 12sp**, and every screen has to survive
 * 200 percent font scale with display size enlarged. The album screen, Settings
 * and the action sheet are the known pressure points.
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
 * The serif is italic almost everywhere it appears, because its whole job is
 * the app's quiet editorial voice: "148 albums, 62 of them living here", "On
 * your shelf since June 2023", and the "yours" provenance markers. The regular
 * cut is bundled for the missing-cover placeholder letters.
 */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

@Immutable
data class MeedwellTypography(
    /** 12sp, weight 600, letter spacing 2.2, uppercase. The floor of the scale. */
    val capsEyebrow: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.2.sp,
    ),
    /** Secondary and metadata. */
    val metadata: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    /** Body copy. */
    val body: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    /** List row titles. */
    val rowTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
    ),
    /** Section heading, 32sp with tight tracking. */
    /**
     * Headings trim their own leading.
     *
     * `LineHeightStyle` with `Trim.Both` makes a heading measure to its cap
     * height, so a 12dp gap in a layout is a 12dp gap on screen. Without it
     * Instrument Sans's default leading adds roughly 6sp above and below every
     * heading and the spacing constants throughout the app quietly stop
     * meaning what they say.
     */
    val sectionHeading: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-1.2).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    ),
    /** Large heading, up to 50sp, tracking to -2. */
    val largeHeading: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-2).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    ),
    /** The shelf's own heading, `.h.xl` in the reference: 40px, so 50sp. */
    val extraLargeHeading: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 50.sp,
        lineHeight = 52.sp,
        letterSpacing = (-2).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    ),
    /**
     * The voice line. Instrument Serif italic, the app's editorial register.
     * Used sparingly and never for anything the user has to act on.
     */
    val voice: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    val voiceSmall: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    /** The "yours" provenance marker, and other inline serif italic. */
    val provenance: TextStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    /** A grid card's title: heavier than its artist line, per the reference. */
    val cardTitle: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.5.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    /** The Albums / Artists / Genres switcher. */
    val filterTab: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    /** The sort control, deliberately quieter than the switcher beside it. */
    val sortLabel: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    /** The tab bar label. Its own token rather than a borrowed eyebrow. */
    val tabLabel: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
    ),
    /**
     * Times, durations, sizes and counts.
     *
     * Tabular figures are not a refinement here: a right-aligned column of
     * durations visibly jitters between rows without them, and `DESIGN.md`
     * section 4 asks for them on every time, duration, size and count.
     */
    val numeric: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    ),

    /** Button labels. An action keeps the same name through its whole flow. */
    val button: TextStyle = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
    ),
)

/**
 * Tabular numerals on every time, duration, size and count, per `DESIGN.md`
 * section 4. Applied through a `FontFeatureSetting` at the call site rather
 * than baked into a style, since the same style carries both words and numbers
 * in several places.
 */
const val TabularNumerals = "tnum"
