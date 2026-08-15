package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The icon set, taken from the visual reference rather than from a library.
 *
 * Every path here is copied from `reference/meedwell-screen-grid-final.html`,
 * where the icons are hand drawn SVGs on a 24 by 24 grid with a 1.7 stroke,
 * round caps and round joins. Using Material's icon set instead would have been
 * quicker and would have made the app look like every other app: the reference
 * icons are lighter, rounder and quieter than Material's, and that difference
 * is most of why the interface reads as considered rather than assembled.
 *
 * Drawn as stroked paths on a Canvas rather than shipped as vector drawables,
 * so a single stroke width and color flows from the theme and every icon in
 * the app changes together.
 */
enum class MeedwellIcons(
    /** SVG path data on a 24x24 viewport, from the reference. */
    val paths: List<String>,
    /** Paths that are filled rather than stroked. */
    val filled: List<String> = emptyList(),
) {
    Grid(
        listOf(
            // rx=1.5 in the reference. StrokeJoin.Round only rounds by half
            // the stroke, so the corners have to be drawn.
            "M5.5 4h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
            "M14.5 4h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
            "M5.5 13h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
            "M14.5 13h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
        )
    ),
    ListView(listOf("M9 6h11M9 12h11M9 18h11", "M4 6h.5M4 12h.5M4 18h.5")),
    Search(listOf("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14", "M20 20l-3.5-3.5")),

    /** The four tab icons. */
    TabShelf(
        listOf(
            "M5.5 3.5h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1-2 2h-3.5a2 2 0 0 1-2-2v-3.5a2 2 0 0 1 2-2z",
            "M15 3.5h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1-2 2h-3.5a2 2 0 0 1-2-2v-3.5a2 2 0 0 1 2-2z",
            "M5.5 13h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1-2 2h-3.5a2 2 0 0 1-2-2v-3.5a2 2 0 0 1 2-2z",
            "M15 13h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1-2 2h-3.5a2 2 0 0 1-2-2v-3.5a2 2 0 0 1 2-2z",
        )
    ),
    TabSearch(listOf("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14", "M20 20l-3.5-3.5")),
    TabLists(
        listOf(
            "M4 6h13M4 12h13M4 18h8",
            "M19.5 14.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5",
            "M19.5 14.5V7l2.5 1",
        )
    ),
    TabMore(
        listOf(
            "M12 8.8a3.2 3.2 0 1 0 0 6.4a3.2 3.2 0 1 0 0-6.4",
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3",
            "M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1",
        )
    ),

    Back(listOf("M15 5l-7 7 7 7")),
    ChevronDown(listOf("M6 9l6 6 6-6")),
    ChevronRight(listOf("M9 5l7 7-7 7")),
    Close(listOf("M6 6l12 12M18 6L6 18")),

    /** The overflow menu: three filled dots, as in the reference. */
    Dots(
        emptyList(),
        filled = listOf(
            "M5 10.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
            "M12 10.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
            "M19 10.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
        )
    ),

    Play(emptyList(), filled = listOf("M6 4.5v15l12-7.5L6 4.5z")),
    Pause(emptyList(), filled = listOf("M7 5h4v14H7zM13 5h4v14h-4z")),
    Previous(emptyList(), filled = listOf("M19 5v14l-9.5-7L19 5zM7 5H5v14h2V5z")),
    Next(emptyList(), filled = listOf("M5 5v14l9.5-7L5 5zM17 5h2v14h-2V5z")),

    Shuffle(listOf("M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5")),
    Repeat(listOf("M17 2l4 4-4 4M3 11v-1a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v1a4 4 0 0 1-4 4H3")),

    /**
     * Repeat, with a 1 in the middle of the loop.
     *
     * A separate glyph rather than a badge stuck on the plain one, so the two
     * repeat states are told apart by shape and not only by a small mark that
     * disappears at a glance or under a large font scale.
     */
    RepeatOne(listOf(
        "M17 2l4 4-4 4M3 11v-1a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v1a4 4 0 0 1-4 4H3",
        "M11 10.5l1.6-1v6",
    )),
    SleepTimer(listOf("M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z")),

    PlayNext(listOf("M17 5v14"), filled = listOf("M5 5v14l9-7L5 5z")),
    Queue(listOf("M4 6h13M4 12h13M4 18h8M18 15v6M15 18h6")),

    /**
     * Opening the queue, as distinct from adding to it.
     *
     * `Queue` above carries a plus and means "put this in the queue". Using the
     * same glyph for "show me the queue" would make one shape mean two things a
     * tap apart, which is exactly the kind of small lie a person only notices
     * as a feeling that the app is confusing.
     */
    QueueOpen(listOf("M4 6h16M4 12h16M4 18h7", "M14 15.5l6 3.2-6 3.3z")),
    Lists(
        listOf(
            "M4 6h13M4 12h13M4 18h8",
            "M19.5 14.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5",
            "M19.5 14.5V7l2.5 1",
        )
    ),
    Heart(listOf("M12 20l-1.2-1.1C5.6 14.2 3 11.8 3 8.9 3 6.6 4.9 4.8 7.2 4.8c1.8 0 3.2 1 3.8 2.1.6-1.1 2-2.1 3.8-2.1 2.3 0 4.2 1.8 4.2 4.1 0 2.9-2.6 5.3-7.8 10.1z")),
    HeartFilled(
        emptyList(),
        filled = listOf("M12 20l-1.2-1.1C5.6 14.2 3 11.8 3 8.9 3 6.6 4.9 4.8 7.2 4.8c1.8 0 3.2 1 3.8 2.1.6-1.1 2-2.1 3.8-2.1 2.3 0 4.2 1.8 4.2 4.1 0 2.9-2.6 5.3-7.8 10.1z")
    ),
    Artwork(
        listOf(
            "M6.5 4h11a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1-2.5 2.5h-11a2.5 2.5 0 0 1-2.5-2.5v-11a2.5 2.5 0 0 1 2.5-2.5z",
            "M4 15l4.5-4.5L13 15M11 13l3.5-3.5L20 15",
            "M9.5 7.1a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
        )
    ),
    Artist(
        listOf(
            "M12 4.6a3.4 3.4 0 1 0 0 6.8a3.4 3.4 0 1 0 0-6.8",
            "M5.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5",
        )
    ),
    Share(
        listOf(
            "M18 2.4a2.6 2.6 0 1 0 0 5.2a2.6 2.6 0 1 0 0-5.2",
            "M6 9.4a2.6 2.6 0 1 0 0 5.2a2.6 2.6 0 1 0 0-5.2",
            "M18 16.4a2.6 2.6 0 1 0 0 5.2a2.6 2.6 0 1 0 0-5.2",
            "M8.3 10.8l7.4-4.6M8.3 13.2l7.4 4.6",
        )
    ),
    Download(listOf("M12 4v11M7 10.5l5 5 5-5M5 20h14")),
    Offline(
        listOf(
            "M2 9c6-5.5 14-5.5 20 0M5.5 12.8c4-3.6 9-3.6 13 0M9 16.5c2-1.8 4-1.8 6 0",
            "M3 21L21 3",
        )
    ),
    Warning(listOf("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18", "M12 8v4.5M12 16h.01")),
    Eye(listOf("M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12z", "M12 9.2a2.8 2.8 0 1 0 0 5.6a2.8 2.8 0 1 0 0-5.6")),
    Folder(listOf("M3 7a2 2 0 0 1 2-2h4l2 2.5h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")),
    Clock(listOf("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18", "M12 7v5.5l3.5 2")),
    ;
}

/**
 * Draws an icon.
 *
 * The path data is on a 24 unit grid, so the canvas is scaled to whatever size
 * is asked for and the stroke is expressed in grid units. That way a 17dp icon
 * and a 22dp icon carry visually equal weight, rather than the smaller one
 * looking heavier, which is what happens when a fixed dp stroke is reused
 * across sizes.
 */
@Composable
fun MeedwellIcon(
    icon: MeedwellIcons,
    modifier: Modifier = Modifier,
    size: Dp = 25.dp,
    tint: Color = MeedwellTheme.colors.primaryText,
    contentDescription: String? = null,
) {
    val stroked = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    val filledPaths = remember(icon) { icon.filled.map { PathParser().parsePathString(it).toPath() } }

    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            )
    ) {
        val factor = this.size.minDimension / VIEWPORT
        scale(scale = factor, pivot = Offset.Zero) {
            stroked.forEach { path ->
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(
                        width = STROKE_ON_GRID,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            filledPaths.forEach { path -> drawPath(path = path, color = tint) }
        }
    }
}

private const val VIEWPORT = 24f

/** 1.7 on a 24 unit grid, exactly as the reference draws them. */
private const val STROKE_ON_GRID = 1.7f

/**
 * An icon-only control.
 *
 * The target is always 48dp whatever the glyph size, and a spoken label is
 * required rather than optional: `DESIGN.md` section 12 asks for complete
 * screen reader labels on every icon-only control, and there are a lot of them.
 */
@Composable
fun IconButton(
    icon: MeedwellIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 25.dp,
    tint: Color = MeedwellTheme.colors.secondaryText,
    /**
     * Pulls the 48dp target back so the **glyph's** edge meets the screen
     * gutter, rather than the invisible box around it.
     *
     * Without this a back chevron in a 48dp box sits 36dp from the screen edge
     * while the heading beneath it starts at 26dp, and nothing on the screen
     * lines up with anything. It is the kind of misalignment nobody names and
     * everybody feels.
     */
    edge: IconEdge = IconEdge.None,
) {
    val inset = (48.dp - size) / 2
    Box(
        modifier = modifier
            .offset(
                x = when (edge) {
                    IconEdge.Start -> -inset
                    IconEdge.End -> inset
                    IconEdge.None -> 0.dp
                }
            )
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MeedwellIcon(icon = icon, size = size, tint = tint, contentDescription = contentDescription)
    }
}

/** Which edge, if any, an icon button should optically align to. */
enum class IconEdge { Start, End, None }

