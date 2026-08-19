package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import com.kamsiob.meedwell.core.library.Programme
import com.kamsiob.meedwell.ui.theme.Motion
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.QueueItem
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.sheetShadow

/**
 * What is playing after this.
 *
 * The queue button on the now-playing screen did nothing for the whole of the
 * first build, which is a bad one to leave dead: "what's next" is the single
 * most asked question of a music player after "what is this".
 *
 * Two decisions worth naming:
 *
 *  - **It opens on the current track, not at the top.** The interesting part of
 *    a queue is the boundary between played and coming, so that is where it
 *    lands rather than making somebody scroll to find themselves.
 *  - **Played rows stay.** Some players drop them. Keeping them means the
 *    position of the current row carries information, and going back to
 *    something from ten minutes ago is a tap rather than a search.
 */
@Composable
fun QueueSheet(
    items: List<QueueItem>,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val listState = rememberLazyListState()
    val currentIndex = items.indexOfFirst { it.isCurrent }

    LaunchedEffect(Unit) {
        // One row of lead-in above the current track, so it does not sit
        // flush against the header looking like the first thing in the queue.
        if (currentIndex > 0) listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = "Close" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Tall, but never the whole screen. A sliver of what is behind
                // it keeps the sheet reading as a sheet.
                .fillMaxHeight(0.82f)
                .sheetShadow()
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(colors.background)
                .clickable(enabled = false) {}
                .navigationBarsPadding(),
        ) {
            SheetHandle(onDismiss = onDismiss, modifier = Modifier.padding(top = 12.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Up next", style = type.h2, color = colors.primaryText)
                Box(Modifier.weight(1f))
                Text(
                    remainingLine(items, currentIndex),
                    style = type.meta,
                    color = colors.tertiaryText,
                )
            }
            Hairline()

            // Reorder state, held at the sheet so only one row can ever be in
            // hand, with the commit on release: the same shape the playlist
            // uses, for the same reason, that reordering mid drag rewrites the
            // row keys and kills the gesture driving it.
            var dragging by remember { mutableStateOf<Int?>(null) }
            var dragBy by remember { mutableIntStateOf(0) }
            var rowHeightPx by remember { mutableIntStateOf(1) }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 6.dp),
            ) {
                val numeralWidth =
                    numeralColumnWidth(items.maxOfOrNull { it.trackNumber } ?: 0)
                itemsIndexed(items, key = { _, it -> it.index }) { position, item ->
                    QueueRow(
                        item = item,
                        numeralWidth = numeralWidth,
                        lifted = dragging == position,
                        liftBy = if (dragging == position) dragBy else 0,
                        onPlay = { onPlay(item.index) },
                        onRemove = { onRemove(item.index) },
                        onHeight = { if (it > 0) rowHeightPx = it },
                        onDragStart = { dragging = position; dragBy = 0 },
                        onDragBy = { dragBy += it },
                        onDragEnd = {
                            val from = dragging
                            if (from != null) {
                                val to = (from + Math.round(dragBy.toFloat() / rowHeightPx))
                                    .coerceIn(0, items.lastIndex)
                                if (to != from) onMove(items[from].index, items[to].index)
                            }
                            dragging = null
                            dragBy = 0
                        },
                    )
                }
                item(key = "close") {
                    // The bill closes the way a printed one does.
                    Box(
                        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ClosingSprig()
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueItem,
    numeralWidth: Dp,
    lifted: Boolean,
    liftBy: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onHeight: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragBy: (Int) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // Swipe to remove: the row rides with the finger and lets go past the
    // threshold. The named remove stays beside it, so the swipe is the
    // shortcut rather than the only route, which is what squares the spec's
    // gesture with the recorded rule that destruction gets a name.
    var swipeBy by remember { mutableFloatStateOf(0f) }
    val swipeRide by animateFloatAsState(
        targetValue = swipeBy,
        animationSpec = if (swipeBy == 0f) Motion.standard else snap(),
        label = "queue swipe",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height) }
            .graphicsLayer {
                translationX = swipeRide
                if (lifted) {
                    translationY = liftBy.toFloat()
                    shadowElevation = 8f
                }
                alpha = 1f - (kotlin.math.abs(swipeRide) / (size.width.coerceAtLeast(1f))) * 0.7f
            }
            .background(if (lifted) colors.recess else colors.background)
            .defaultMinSize(minHeight = 60.dp)
            .clickable(role = Role.Button, onClick = onPlay)
            .pointerInput(item.index) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(swipeBy) > 96.dp.toPx() && !item.isCurrent) onRemove()
                        swipeBy = 0f
                    },
                    onDragCancel = { swipeBy = 0f },
                ) { change, delta ->
                    change.consume()
                    swipeBy += delta
                }
            }
            .padding(horizontal = 26.dp, vertical = 10.dp)
            .semantics {
                contentDescription = if (item.isCurrent) "${item.title}, playing now" else item.title
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // **A bill of movements, not a gallery.** The grid draws the queue with
        // numerals and no artwork: the covers were the same record repeated
        // down the sheet. Roman when the queue is one whole record, Arabic when
        // it is a mixed evening, which is the same honesty rule the player's
        // programme line follows.
        Text(
            if (item.wholeRecordCount > 0 && item.trackNumber > 0) {
                Programme.roman(item.trackNumber)
            } else {
                "${item.index + 1}"
            },
            style = type.movementNumeral,
            color = if (item.isCurrent) colors.mossInk else colors.tertiaryText,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(numeralWidth).padding(end = 10.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = type.rowTitle,
                color = if (item.isCurrent) colors.primaryText else colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    item.artist.takeIf { it.isNotBlank() },
                    Programme.tempoIn(item.title),
                ).joinToString(" · "),
                style = type.meta,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (item.durationSeconds > 0) {
            Text(
                "%d:%02d".format(item.durationSeconds / 60, item.durationSeconds % 60),
                style = type.meta,
                color = colors.tertiaryText,
                textAlign = TextAlign.End,
                modifier = Modifier.width(46.dp),
            )
        }
        if (item.isCurrent) {
            Waveform(
                progress = 1f,
                animate = true,
                barCount = 5,
                modifier = Modifier.padding(start = 8.dp).size(width = 22.dp, height = 16.dp),
            )
        } else {
            IconButton(
                icon = MeedwellIcons.Close,
                contentDescription = "Take ${item.title} out of the queue",
                onClick = onRemove,
                tint = colors.tertiaryText,
                size = 18.dp,
            )
            // The handle, a real 48dp target, same rules as the playlist's.
            Box(
                Modifier
                    .size(44.dp)
                    .pointerInput(item.index) {
                        detectVerticalDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        ) { change, delta ->
                            change.consume()
                            onDragBy(delta.toInt())
                        }
                    }
                    .semantics { contentDescription = "Reorder ${item.title}. Drag up or down." },
                contentAlignment = Alignment.Center,
            ) {
                MeedwellIcon(MeedwellIcons.Handle, size = 16.dp, tint = colors.tertiaryText)
            }
        }
    }
}

/**
 * How many are left, which is the number somebody actually wants.
 *
 * "12 tracks" in a queue you are eight into is not the answer to any question.
 */
private fun remainingLine(items: List<QueueItem>, currentIndex: Int): String {
    val ahead = if (currentIndex >= 0) items.drop(currentIndex + 1) else items
    if (ahead.isEmpty()) return "last one"
    val minutes = (ahead.sumOf { it.durationSeconds } / 60).toInt()
    val pieces = if (ahead.size == 1) "1 piece left" else "${ahead.size} pieces left"
    return if (minutes > 0) "$pieces · $minutes min" else pieces
}

private fun queueCount(total: Int, currentIndex: Int): String {
    val remaining = if (currentIndex >= 0) total - currentIndex - 1 else total
    return when (remaining) {
        0 -> "last one"
        1 -> "1 after this"
        else -> "$remaining after this"
    }
}
