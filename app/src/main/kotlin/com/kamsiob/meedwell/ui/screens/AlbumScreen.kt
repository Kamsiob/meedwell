package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kamsiob.meedwell.ui.components.numeralColumnWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Provenance
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.ui.components.Cover
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.IconButton
import androidx.compose.ui.text.font.FontWeight
import com.kamsiob.meedwell.core.library.Programme
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import androidx.compose.foundation.layout.width
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.StaffRule
import com.kamsiob.meedwell.ui.components.GhostButton
import com.kamsiob.meedwell.ui.components.ClosingSprig
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.combinedClickableCompat
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screens 12 and 14 in the visual reference: the album, and the album scrolled.
 *
 * The legibility law in its hardest case. The full square cover shows
 * **complete**, edge to edge, never cropped, never faded, never written on, and
 * text begins only past its bottom edge on theme surface. On scroll the cover
 * collapses into a hairline toolbar carrying a 30dp thumb, the title, Play and
 * the action menu, and **no text passes over the art at any point in the
 * transition**.
 *
 * A missing cover means the art region is **omitted entirely** rather than held
 * open as a void, so the screen opens on the title. If words can never sit on
 * art, absent art cannot leave a hole where words are forbidden.
 *
 * There is no liner notes section. `getAlbumInfo2` is absent from Bandcamp's
 * API, so there are no notes to show, and the section was always conditional on
 * data actually arriving.
 */
@Composable
fun AlbumScreen(
    album: Album,
    tracks: List<Track>,
    playingTrackId: String?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onAlbumMenu: () -> Unit,
    onOpenArtwork: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val listState = rememberLazyListState()

    // The toolbar takes over once the cover has scrolled away. Derived rather
    // than remembered so it cannot drift out of step with the actual scroll.
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val hasCover = album.coverUrl != null

    Box(modifier = modifier.fillMaxSize()) {
        val numeralWidth = numeralColumnWidth(tracks.maxOfOrNull { it.trackNumber } ?: 0)

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Column {
                    Row(
                        // Same gutter as the body below it, so the back icon and the title line up.
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackButton(onBack)
                        Box(Modifier.weight(1f))
                        MenuButton(onAlbumMenu)
                    }

                    // The cover, complete and edge to edge. Omitted entirely
                    // when there is none, so the screen opens on the title.
                    if (hasCover) {
                        Cover(
                            url = album.coverUrl,
                            title = album.name,
                            cornerRadius = 0.dp,
                            contentDescription = "Cover of ${album.name}. Open the artwork viewer.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable(role = Role.Button, onClick = onOpenArtwork),
                        )
                    }

                    // Everything below here is on theme surface, past the art's
                    // hard edge. Nothing above it carries a word.
                    // A record is **set as a programme**: a plate line, a
                    // staff, centred titling, then the movements. Not a header
                    // with a track list under it.
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        // The plate line: label, year in Roman numerals, and
                        // when it landed here. Small caps, letterspaced,
                        // centred, exactly as a printed plate line is set.
                        Text(
                            text = album.plateLine().uppercase(),
                            style = type.plate,
                            color = colors.tertiaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                        )

                        // A staff with no label, closing the plate and opening
                        // the titling. One of its three sanctioned appearances.
                        StaffRule(Modifier.padding(top = 12.dp))

                        Column(
                            Modifier.fillMaxWidth().padding(top = 13.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = album.name,
                                style = type.programme,
                                color = colors.primaryText,
                                textAlign = TextAlign.Center,
                            )
                            // The performer in the serif, the way a programme
                            // sets "Ilse Marren, piano".
                            Text(
                                text = album.artist,
                                style = type.voice,
                                color = colors.secondaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                text = album.detailLine(tracks),
                                style = type.meta,
                                color = colors.tertiaryText,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            PillButton(label = "Play", onClick = onPlay)
                            // The equal second choice, as a ghost pill rather
                            // than bare small text beside a filled button.
                            // Shuffling a record is a peer of playing it, and
                            // the components file says the ghost exists for
                            // exactly this case.
                            GhostButton(
                                label = "Shuffle",
                                onClick = onShuffle,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        SectionHead("Programme", Modifier.padding(top = 18.dp, bottom = 3.dp))
                    }
                }
            }

            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = track.id == playingTrackId,
                    numeralWidth = numeralWidth,
                    onClick = { onTrackClick(index) },
                    onLongClick = { onTrackLongClick(track) },
                )
            }

            item(key = "close") {
                // "The engraved sprig closes the sheet the way a printed score
                // closes a page." Grid 07's own caption; the programme used to
                // end at the last movement and run into blank paper.
                Box(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ClosingSprig()
                }
            }

            if (tracks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "The track list has not arrived yet. It fills in on the next sync.",
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    )
                }
            }
        }

        // The collapsed toolbar. It appears over surface, never over art,
        // because by the time it shows the art has scrolled away.
        if (collapsed) {
            Column(Modifier.fillMaxWidth().background(colors.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    BackButton(onBack)
                    CoverThumb(url = album.coverUrl, title = album.name, size = 30.dp)
                    Text(
                        text = album.name,
                        style = type.meta,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                            .clickable(role = Role.Button, onClick = onPlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Play", style = type.meta, color = colors.secondaryText)
                    }
                    MenuButton(onAlbumMenu)
                }
                Hairline()
            }
        }
    }
}

/**
 * One movement on the programme.
 *
 * ```
 * .mv{display:flex;align-items:baseline;gap:10px;padding:9px 0;
 *     border-bottom:1px solid var(--hair);}
 * .mv .n{width:25px;...italic...}   .mv .nm{flex:1;font-size:13px;}
 * .mv .tp{...italic;font-size:11px;}   .mv .ln{font-size:11.5px;}
 * ```
 *
 * The number is a **Roman numeral in the serif italic**, the tempo marking sits
 * beside the name in the same italic, and the running time closes the line. It
 * is a bill of movements, not a numbered track list.
 *
 * The tempo is read out of the title rather than invented, so most popular
 * records show none at all and the row is simply shorter. See `Programme`.
 */
@Composable
private fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    numeralWidth: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val tempo = Programme.tempoIn(track.title)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .combinedClickableCompat(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 22.dp, vertical = 9.dp)
                .semantics { contentDescription = track.accessibilityLabel(isPlaying) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = track.trackNumber.takeIf { it > 0 }?.let { Programme.roman(it) }.orEmpty(),
                style = type.movementNumeral,
                color = if (isPlaying) colors.mossInk else colors.tertiaryText,
                // Flush right against the title, the way numerals sit on a
                // printed programme. One shared width per programme, sized
                // from the highest numeral, so XVIII never wraps.
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(numeralWidth).padding(end = 10.dp),
            )
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = track.title,
                    style = type.movementName,
                    // The playing movement is moss ink and heavier, which is
                    // the grid's `.mv.on`. Not a colored bar, not a highlight.
                    color = if (isPlaying) colors.mossInk else colors.primaryText,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.resumeLabel()?.let { label ->
                    Text(
                        text = label,
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (tempo != null) {
                Text(
                    text = tempo,
                    style = type.tempo,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Text(
                text = formatDuration(track.durationSeconds),
                style = type.meta,
                color = colors.tertiaryText,
                // A fixed right-aligned box, so the times land in one column
                // whether or not the row beside them carries a tempo. Tabular
                // figures do the rest.
                textAlign = TextAlign.End,
                modifier = Modifier.width(46.dp),
            )
        }
        Hairline(Modifier.padding(horizontal = 22.dp))
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(
        icon = MeedwellIcons.Back,
        contentDescription = "Back",
        onClick = onBack,
        size = 19.dp,
        tint = MeedwellTheme.colors.primaryText,
    )
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    IconButton(
        icon = MeedwellIcons.Dots,
        contentDescription = "More actions for this album",
        onClick = onClick,
        size = 19.dp,
        tint = MeedwellTheme.colors.primaryText,
    )
}

/** Tabular, and always minutes and seconds rather than a bare count. */
fun formatDuration(seconds: Long): String {
    // Zero is a real position, and "0:00" is what a listener expects at the
    // start of a track. Only an unknown duration is dashes.
    if (seconds < 0) return "--:--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}

/**
 * "Resume from 22:40", shown on the row itself.
 *
 * Only on long pieces, and only when there is a real position worth returning
 * to. A resume point three seconds in is noise, and a resume point on a three
 * minute song is not something anybody wanted remembered.
 */
private fun Track.resumeLabel(): String? {
    if (!isLongForm) return null
    val position = resumePositionSeconds ?: return null
    if (position < RESUME_WORTH_SHOWING_SECONDS) return null
    // Nearly finished is not worth resuming either; it just means play it again.
    if (durationSeconds > 0 && position > durationSeconds - RESUME_WORTH_SHOWING_SECONDS) return null
    return "Resume from ${formatDuration(position)}"
}

private const val RESUME_WORTH_SHOWING_SECONDS = 30L

private fun Track.accessibilityLabel(isPlaying: Boolean): String = buildString {
    if (trackNumber > 0) append("Track $trackNumber, ")
    append(title)
    append(", ")
    append(formatDuration(durationSeconds))
    if (isPresentLocally) append(", here as a file")
    if (isPlaying) append(", playing")
}

/**
 * The plate line: the year in Roman numerals, and when it landed here.
 *
 * The grid sets "Lowlight Editions · MMXXIV · shelved March 2025". Meedwell has
 * no label field, because Bandcamp's API does not return one, so it sets what
 * it genuinely knows: the year and the shelving date. Inventing a label would
 * be putting words on somebody's record that nobody wrote.
 */
private fun Album.plateLine(): String = listOfNotNull(
    year.takeIf { it > 0 }?.let { Programme.roman(it) },
    shelfSince()?.let { it.replace("On your shelf since", "shelved").trim() },
).joinToString(" · ").ifBlank { "on your shelf" }

/** "On your shelf since June 2023", the collector provenance line. */
private fun Album.shelfSince(): String? {
    val at = addedAt ?: return null
    val days = at / 86_400
    var year = 1970
    var remaining = days
    while (true) {
        val length = if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 366 else 365
        if (remaining < length) break
        remaining -= length
        year++
    }
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    val lengths = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val names = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    var month = 0
    while (month < 12 && remaining >= lengths[month]) {
        remaining -= lengths[month]
        month++
    }
    return "On your shelf since ${names[month.coerceIn(0, 11)]} $year"
}

private fun Album.detailLine(tracks: List<Track>): String {
    val total = tracks.sumOf { it.durationSeconds }.takeIf { it > 0 } ?: durationSeconds
    return buildList {
        if (year > 0) add(year.toString())
        val count = tracks.size.takeIf { it > 0 } ?: trackCount
        if (count > 0) add("$count ${if (count == 1) "track" else "tracks"}")
        if (total > 0) add("${total / 60} min")
        when (val p = provenance) {
            Provenance.Yours -> add("all here as files")
            is Provenance.PartlyHere -> add("${p.found} of ${p.total} here")
            Provenance.OnThisPhone -> add("on this phone")
            // Verified: Bandcamp streams MP3 V0. No copy anywhere implies more.
            Provenance.Streaming -> add("streaming")
        }
    }.joinToString(" · ")
}
