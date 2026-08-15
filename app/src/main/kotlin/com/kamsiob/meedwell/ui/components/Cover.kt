package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.meedwell.ui.theme.InstrumentSerif
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius

/**
 * Album art, shown whole and never written on.
 *
 * `DESIGN.md` section 5 is the law here and it is absolute: **artwork and words
 * never share pixels**. Artwork is never cropped, never faded, never scrimmed,
 * never written on. Text lives only on theme surface, past a hard edge. This
 * component therefore draws the art and nothing else; a caller that wants a
 * caption puts it outside, past the edge.
 *
 * Covers live in sharp-cornered squares full of their own life. The rounded
 * square frame is one of the three privileges reserved for the mark.
 */
@Composable
fun Cover(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.cover,
    contentDescription: String? = null,
) {
    val colors = MeedwellTheme.colors
    var failed by remember(url) { mutableStateOf(false) }

    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            // Shadow before clip, always. Clipping first casts the shadow away
            // and produces nothing, silently, which is how the app shipped with
            // no shadows at all.
            .clip(shape)
            .border(0.5.dp, colors.hairline, shape)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            },
    ) {
        if (url.isNullOrBlank() || failed) {
            MissingCover(title = title, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(!MeedwellTheme.reducedMotion)
                    .build(),
                contentDescription = null,
                // Fit rather than Crop, deliberately. Crop would mean cutting
                // artwork, and the law says never cropped. Bandcamp art is
                // square so this is almost always identical anyway; the
                // difference only shows on a cover that is not, and there the
                // whole image is what a person bought.
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * What a missing cover looks like.
 *
 * The legibility law makes this load bearing rather than cosmetic: if words may
 * never sit on art, absent art must not leave a hole where words are forbidden.
 *
 * Drawn as **surface, never as a fake image**: a surface panel with a hairline,
 * carrying the title's own first letters in Instrument Serif italic at
 * secondary ink.
 *
 * Never a gray box. Never a generic music-note icon. **Never the Meedwell
 * mark**, because borrowing the mark for album art blurs the one thing it must
 * never be confused with.
 */
@Composable
fun MissingCover(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    BoxWithConstraints(
        modifier = modifier.background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        // The letters scale with the tile. A fixed size nearly filled a 48dp
        // thumbnail and was lost in the middle of a 218dp grid cover.
        val letterSize = (maxWidth.value * 0.30f).coerceIn(13f, 46f).sp
        Text(
            text = coverInitials(title),
            fontFamily = InstrumentSerif,
            fontStyle = FontStyle.Italic,
            fontSize = letterSize,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The letters a missing cover carries.
 *
 * The title's own first letters, so "Switchyard" reads "Sw" and "untitled 2"
 * reads "u2". Two characters, because one is anonymous and three starts to look
 * like an abbreviation of something.
 */
fun coverInitials(title: String): String {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return "·"
    val words = trimmed.split(" ").filter { it.isNotBlank() }
    return when {
        // Two or more words: first letter of each of the first two, which reads
        // better for "Field Recordings" than "Fi" does.
        words.size >= 2 -> (words[0].take(1) + words[1].take(1))
        else -> trimmed.take(2)
    }
}

/** A small square cover for list rows. 48dp art inside a 56dp minimum row. */
@Composable
fun CoverThumb(
    url: String?,
    title: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    Cover(
        url = url,
        title = title,
        cornerRadius = Radius.cover,
        modifier = modifier.size(size),
    )
}

/** A square cover for the grid. */
@Composable
fun CoverSquare(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Cover(
        url = url,
        title = title,
        cornerRadius = Radius.cover,
        modifier = modifier.aspectRatio(1f),
    )
}
