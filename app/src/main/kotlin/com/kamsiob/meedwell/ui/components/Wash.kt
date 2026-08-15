package com.kamsiob.meedwell.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * The now-playing wash color.
 *
 * This is the one place in the app where words sit on color, and it works only
 * because of the clamp below. `DESIGN.md` section 5 permits it precisely
 * because a **color field** can be clamped with no worst case, while artwork
 * cannot: that is the whole reason the adaptive-scrim law was retired.
 *
 * So the rule here is not "darken until it looks fine". It is: take a color
 * from the artwork, then force it below a brightness ceiling, unconditionally,
 * before it is ever drawn. Any album, either theme, white always passes.
 */
@Composable
fun rememberWashColor(artworkUri: String?): State<Color> {
    val context = LocalContext.current
    val fallback = MeedwellTheme.colors.background
    val washState = remember { mutableStateOf(fallback) }

    LaunchedEffect(artworkUri) {
        if (artworkUri.isNullOrBlank()) {
            washState.value = fallback
            return@LaunchedEffect
        }
        val extracted = extractWash(context, artworkUri)
        washState.value = extracted ?: fallback
    }
    return washState
}

private suspend fun extractWash(context: Context, uri: String): Color? = withContext(Dispatchers.IO) {
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(uri)
            // Palette needs to read pixels, which a hardware bitmap forbids.
            .allowHardware(false)
            .build()
        val bitmap: Bitmap = ImageLoader(context).execute(request).image?.toBitmap() ?: return@runCatching null
        val palette = Palette.from(bitmap).clearFilters().maximumColorCount(16).generate()
        val source = palette.darkMutedSwatch
            ?: palette.mutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
            ?: return@runCatching null
        clampForWhiteText(Color(source.rgb))
    }.getOrNull()
}

/**
 * Forces a color below the brightness ceiling that keeps white text legible.
 *
 * Unconditional, and applied before the color is ever drawn. A bright album
 * cover cannot produce a bright wash, so there is no album for which this
 * screen fails. That guarantee is what the design bought by drawing the wash
 * from a palette rather than from the artwork itself.
 *
 * The ceiling is expressed as relative luminance rather than as a simple
 * darkening factor, because darkening by a fixed amount leaves bright yellows
 * and cyans still too light while making dark blues needlessly muddy.
 */
internal fun clampForWhiteText(color: Color, ceiling: Double = MAX_WASH_LUMINANCE): Color {
    var candidate = color
    var guard = 0
    while (relativeLuminance(candidate) > ceiling && guard < 24) {
        candidate = Color(
            red = candidate.red * 0.88f,
            green = candidate.green * 0.88f,
            blue = candidate.blue * 0.88f,
            alpha = 1f,
        )
        guard++
    }
    // Also lift anything so dark it reads as a black rectangle rather than as
    // the record's own color, which is the point of having a wash at all.
    if (relativeLuminance(candidate) < MIN_WASH_LUMINANCE) {
        candidate = Color(
            red = min(1f, candidate.red + 0.06f),
            green = min(1f, candidate.green + 0.06f),
            blue = min(1f, candidate.blue + 0.06f),
            alpha = 1f,
        )
    }
    return candidate
}

/**
 * The ceiling. White on a field at this luminance measures comfortably past
 * 4.5:1, and the gradient below the text darkens it further, so the real
 * contrast at the words is higher than this figure alone implies.
 */
internal const val MAX_WASH_LUMINANCE = 0.13
internal const val MIN_WASH_LUMINANCE = 0.012

internal fun relativeLuminance(color: Color): Double {
    fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** Contrast of white against a wash, for the test that guards the clamp. */
internal fun contrastWithWhite(color: Color): Double {
    val l = relativeLuminance(color)
    return (1.0 + 0.05) / (l + 0.05)
}
