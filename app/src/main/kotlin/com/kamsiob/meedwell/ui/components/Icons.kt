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

    /**
     * The tone control, as a bank of three faders.
     *
     * The player used to name the voicing in words, "Tone: Orchestral &
     * Scores", set in the serif across the middle of the subrow. It was the
     * only text in a row of marks and it read as an announcement rather than a
     * control, which is disruptive on the one screen meant to be quiet. The
     * mark says the same thing and the screen it opens says the rest.
     */
    /**
     * Where the sound is going: this phone, headphones, a speaker, a TV.
     *
     * Android owns the actual picker. This only opens it, because routing is a
     * system matter and an app that drew its own list would be guessing at what
     * is connected.
     */
    Output(
        listOf(
            "M4 9.5h3.5L12 5.5v13L7.5 14.5H4z",
            "M16 9a4.5 4.5 0 0 1 0 6",
            "M19 6.5a8.5 8.5 0 0 1 0 11",
        )
    ),
    Tone(
        listOf(
            "M6 4v16", "M12 4v16", "M18 4v16",
            "M3.5 9h5", "M9.5 14.5h5", "M15.5 7.5h5",
        )
    ),
    Search(listOf("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14", "M20 20l-3.5-3.5")),

    /** The four tab icons. */
    /**
     * A shelf of spines, from the grid: a rectangle with three uprights.
     *
     * It was a four-square grid, which is the universal icon for "app drawer"
     * and says nothing about records standing on a shelf.
     */
    TabShelf(
        listOf(
            "M3 6.5h18v10.5H3z",
            "M8.5 6.5v8.5M12 6.5v8.5M15.5 6.5v8.5",
        )
    ),
    TabSearch(listOf("M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14", "M20 20l-3.5-3.5")),
    /**
     * Surroundings: the sun on the horizon, which is the app's own sign for it.
     *
     * A circle above a line. It is the same shape as the day line's sun and as
     * the right-hand page mark on the player spread, so the three places
     * Surroundings appears all carry one mark. Deliberately **not** a leaf, a
     * moon, or a speaker.
     */
    TabSurroundings(
        listOf(
            "M12 8a4.4 4.4 0 1 0 0 8.8a4.4 4.4 0 1 0 0-8.8",
            "M2.5 17.5H21.5",
        )
    ),
    /**
     * More: a circle with a short stem, from the grid.
     *
     * It was a sun with eight rays, which is now the mark that means
     * Surroundings. Two suns in one tab bar would have been the clearest
     * possible way to make both meaningless.
     */
    TabMore(
        listOf(
            "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16",
            "M12 8.5v5",
        )
    ),

    /**
     * Albums, from the grid: a music stand seen from the front.
     *
     * A triangle of legs with the desk's ribs inside it. Not a stack of discs
     * and not a grid of squares, both of which mean "media" in general.
     */
    AlbumsView(
        listOf(
            "M3.5 19L12 3.5L20.5 19Z",
            "M9 19V8M12 19V5.8M15 19V8",
        )
    ),

    /**
     * The composer glyph from the grid: a treble-clef-like curve with a stem.
     */
    Composers(
        listOf(
            "M8.5 21C4.5 17.5 4.5 8.5 9.5 5C14.5 1.5 20.5 5 19 10C17.5 15 11.5 15 10.5 11",
            "M15.5 17L21 22",
        )
    ),

    /**
     * The fermata: a hold, then a rest. The sleep timer's mark everywhere.
     *
     * **There is no moon in this app.** A moon means night; a fermata means
     * hold this, then stop, which is what a sleep timer actually does and what
     * the copy on that screen is written around.
     */
    Fermata(
        listOf(
            "M3.5 16.5a8.5 8.5 0 0 1 17 0",
            "M12 14.2a1.9 1.9 0 1 0 0 3.8a1.9 1.9 0 1 0 0-3.8",
        )
    ),

    Back(listOf("M15 5l-7 7 7 7")),
    ChevronDown(listOf("M6 9l6 6 6-6")),
    ChevronRight(listOf("M9 5l7 7-7 7")),
    Close(listOf("M6 6l12 12M18 6L6 18")),

    /**
     * The mark against a chosen thing, as on Tone's voicings.
     *
     * Deliberately narrow and slightly extended past the corner, so it reads as
     * a pen stroke rather than a checkbox tick. Every list it appears in is a
     * pick-one list, and the row also carries `selected` for a screen reader,
     * so the glyph is never the only thing saying which one is in use.
     */
    Check(listOf("M4.5 12.5l5 5L20 6.5")),

    /**
     * The grip on a row that can be dragged.
     *
     * Two rules rather than the usual six dots: this design draws structure with
     * hairlines, and a dot grid would be the one stippled thing in the app.
     */
    Handle(listOf("M5 9.5h14M5 14.5h14")),

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

    /** An "i" in a circle. Opens who recorded a sound and under what terms. */
    Info(listOf("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18", "M12 10.5v6", "M12 7.4h.01")),

    /** The quiet end of the volume control: a speaker with no waves. */
    VolumeLow(listOf("M4 9.5h3.5L12 5.5v13L7.5 14.5H4z")),

    /** The loud end: the same speaker with two arcs coming off it. */
    VolumeHigh(listOf(
        "M4 9.5h3.5L12 5.5v13L7.5 14.5H4z",
        "M15.5 9.4a3.6 3.6 0 0 1 0 5.2M18.2 6.8a7.4 7.4 0 0 1 0 10.4",
    )),

    /**
     * Surroundings: three arcs opening outward, like sound in a room.
     *
     * Not a speaker, not a leaf, not a moon. This is a place rather than a
     * device or a mood, and the shape says so by being open on both sides.
     */
    Surroundings(listOf(
        "M7.5 8.2a6 6 0 0 0 0 7.6M4.4 5.6a10 10 0 0 0 0 12.8",
        "M16.5 8.2a6 6 0 0 1 0 7.6M19.6 5.6a10 10 0 0 1 0 12.8",
        "M12 10.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
    )),
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

