package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.kamsiob.meedwell.ui.components.GhostButton
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * One list.
 *
 * **Built rather than browsed.** Everything the shelf shows is somebody else's
 * ordering, whether Bandcamp's or the alphabet's. A list is the one place in the
 * app where the order is the listener's own, so this screen is mostly about
 * changing it: a handle on every row, remove beside it, rename and delete at the
 * top.
 *
 * A list that came from Bandcamp is shown and played and never edited, because
 * their API implements no way to change one. The screen says so in a line rather
 * than by greying out controls and leaving somebody to guess.
 */
@Composable
fun PlaylistScreen(
    state: PlaylistState,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val density = LocalDensity.current

    // Which row is under a finger, and how far it has travelled. Held here
    // rather than in each row so only one can ever be dragging.
    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragBy by remember { mutableIntStateOf(0) }

    /**
     * How tall a row actually is, measured rather than assumed.
     *
     * The step was a constant, and it was wrong: rows come out around 69dp once
     * two lines of text and their padding are in, not the 60dp the constant
     * guessed, so a drag moved the row less far than the finger went and the
     * whole gesture felt like it was slipping.
     */
    var rowHeightPx by remember { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        DetailHeader(state.name, onBack)

        Text(
            state.subtitle,
            style = type.voice,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (state.tracks.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton("Play", onClick = { onPlay(0) }, modifier = Modifier.weight(1f))
                GhostButton(
                    label = "Shuffle",
                    onClick = onShuffle,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.editable) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onRename)
                        .semantics { contentDescription = "Rename this list" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Rename", style = type.meta, color = colors.secondaryText)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onDelete)
                        .semantics { contentDescription = "Delete this list" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Delete", style = type.meta, color = colors.alarm)
                }
            }
        } else {
            Text(
                "This list came from Bandcamp. It plays here, and it is changed there: " +
                    "their service offers no way for an app to edit one.",
                style = type.meta,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        SectionHead(
            if (state.editable) "Drag to reorder" else "Tracks",
            Modifier.padding(top = 18.dp, bottom = 2.dp),
        )

        if (state.tracks.isEmpty()) {
            Text(
                "Nothing in here yet. Add a track from its own row anywhere in the app, " +
                    "or from the player while it is going.",
                style = type.body,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = state.bottomInset)) {
            itemsIndexed(state.tracks, key = { index, track -> "$index:${track.id}" }) { index, track ->
                val lifted = dragging == index
                Row(
                    Modifier
                        .fillMaxWidth()
                        // The row being dragged rides with the finger and sits
                        // above its neighbours, so it reads as picked up rather
                        // than as the list flickering underneath it.
                        .graphicsLayer {
                            if (lifted) {
                                translationY = dragBy.toFloat()
                                shadowElevation = 8f
                            }
                        }
                        .background(if (lifted) colors.recess else colors.background)
                        .defaultMinSize(minHeight = ROW_HEIGHT)
                        .onSizeChanged { if (it.height > 0) rowHeightPx = it.height }
                        .clickable(role = Role.Button) { onPlay(index) }
                        .padding(vertical = 10.dp)
                        .semantics {
                            contentDescription =
                                "${index + 1}. ${track.title}, ${track.artist}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = type.meta,
                        color = colors.tertiaryText,
                        textAlign = TextAlign.End,
                        // A minimum, not a box. The fixed 20dp height clipped
                        // the digit at large font scale.
                        modifier = Modifier.widthIn(min = 26.dp).padding(end = 6.dp),
                    )
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            track.title,
                            style = type.rowTitle,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = type.rowSub,
                            color = colors.tertiaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    // The per-piece time: somebody building a set for an
                    // evening is doing arithmetic, and the list was
                    // withholding the numbers.
                    if (track.durationSeconds > 0) {
                        Text(
                            "%d:%02d".format(track.durationSeconds / 60, track.durationSeconds % 60),
                            style = type.meta,
                            color = colors.tertiaryText,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(46.dp),
                        )
                    }
                    if (state.editable) {
                        IconButton(
                            icon = MeedwellIcons.Close,
                            contentDescription = "Take ${track.title} out of this list",
                            onClick = { onRemove(index) },
                            size = 18.dp,
                            tint = colors.tertiaryText,
                        )

                        // The handle.
                        //
                        // A full 48dp target around two short rules, because a
                        // drag handle you have to aim at is worse than no drag
                        // handle at all. The whole row is not draggable on
                        // purpose: a list you scroll would reorder itself every
                        // time a thumb moved.
                        Box(
                            Modifier
                                .size(48.dp)
                                .pointerInput(index, state.tracks.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = index
                                            dragBy = 0
                                        },
                                        onDragEnd = {
                                            // Committed here, once, rather than
                                            // step by step during the drag.
                                            //
                                            // Reordering mid-gesture rewrote
                                            // every row's key, which rebuilt this
                                            // very `pointerInput` and cancelled
                                            // the drag that was driving it: a
                                            // three row flick landed one along,
                                            // every time, and no amount of
                                            // arithmetic in here could fix it
                                            // because the gesture was already
                                            // dead. Nothing is reordered until
                                            // the finger lifts.
                                            val from = dragging
                                            val step = if (rowHeightPx > 0) rowHeightPx else 1
                                            if (from != null) {
                                                val to = (from + Math.round(dragBy.toFloat() / step))
                                                    .coerceIn(0, state.tracks.lastIndex)
                                                if (to != from) onMove(from, to)
                                            }
                                            dragging = null
                                            dragBy = 0
                                        },
                                        onDragCancel = {
                                            dragging = null
                                            dragBy = 0
                                        },
                                    ) { change, amount ->
                                        change.consume()
                                        dragBy += amount.y.toInt()
                                    }
                                }
                                .semantics {
                                    contentDescription =
                                        "Reorder ${track.title}. Drag up or down."
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            MeedwellIcon(
                                MeedwellIcons.Handle,
                                size = 18.dp,
                                tint = colors.tertiaryText,
                            )
                        }
                    }
                }
                Hairline()
            }
        }
    }
}

/** Tall enough to drag against, and the step one drag moves a row by. */
private val ROW_HEIGHT = 60.dp

data class PlaylistState(
    val id: String = "",
    val name: String = "",
    val tracks: List<Track> = emptyList(),
    val editable: Boolean = true,
    val bottomInset: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val subtitle: String
        get() = when (tracks.size) {
            0 -> "Nothing in here yet"
            1 -> "One track, in your order"
            else -> "${tracks.size} tracks, in your order"
        }
}
