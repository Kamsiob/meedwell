package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The heading on a screen you can go back from: chevron, then title, on one line.
 *
 * From the grid, which uses the same three lines on Tone, Settings, Credits and
 * every other pushed screen:
 *
 * ```
 * <div style="display:flex;align-items:center;gap:12px;margin-top:11px">
 *   <span style="color:var(--ink-3);font-size:16px">‹</span><div class="h1">Tone</div>
 * </div>
 * ```
 *
 * **The chevron is beside the title, not above it.** Every screen in the app had
 * it stacked, which cost a whole line of vertical space on twelve screens and,
 * worse, left the chevron floating with nothing to belong to. Beside the title
 * it reads as "back from Tone" rather than as a lone control.
 *
 * The chevron is `--ink-3` in the grid, the same tertiary ink as a section
 * label. It is deliberately quieter than the title: the way out of a screen
 * should be findable, not the loudest thing on it.
 *
 * The 48dp touch target is kept and pulled left with [IconEdge.Start], so the
 * glyph sits on the 22dp gutter while the tappable box overhangs it.
 */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = "Back",
    /**
     * `h2` for a screen titled with somebody's name rather than with a fixed
     * word, so a long composer does not swallow the line.
     */
    style: androidx.compose.ui.text.TextStyle? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            icon = MeedwellIcons.Back,
            contentDescription = backDescription,
            onClick = onBack,
            size = 21.dp,
            tint = MeedwellTheme.colors.tertiaryText,
            edge = IconEdge.Start,
        )
        Text(
            title,
            style = style ?: MeedwellTheme.typography.h1,
            color = MeedwellTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
