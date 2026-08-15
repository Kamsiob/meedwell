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
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.QueueItem
import com.kamsiob.meedwell.ui.theme.Elevation
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.meedwellShadow

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
            .background(Color.Black.copy(alpha = 0.5f))
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
                .meedwellShadow(Elevation.sheet, RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(colors.background)
                .clickable(enabled = false) {}
                .navigationBarsPadding(),
        ) {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .width(36.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.secondaryText.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Up next", style = type.sectionHeading, color = colors.primaryText)
                Box(Modifier.weight(1f))
                Text(
                    queueCount(items.size, currentIndex),
                    style = type.metadata,
                    color = colors.tertiaryText,
                )
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
            ) {
                items(items, key = { it.index }) { item ->
                    QueueRow(
                        item = item,
                        onPlay = { onPlay(item.index) },
                        onRemove = { onRemove(item.index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(item: QueueItem, onPlay: () -> Unit, onRemove: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(horizontal = 26.dp, vertical = 8.dp)
            .semantics {
                contentDescription = if (item.isCurrent) "${item.title}, playing now" else item.title
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(url = item.artworkUri, title = item.title, size = 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.title,
                style = type.rowTitle,
                // The row that is playing is the brightest thing in the list.
                // Everything else, played or coming, is the same weight: the
                // queue is a plan, not a scoreboard.
                color = if (item.isCurrent) colors.primaryText else colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.artist,
                style = type.metadata,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (item.isCurrent) {
            // A tiny live waveform instead of a label. It says "here" without
            // a word, and it is the same motif the mini player uses.
            Waveform(
                progress = 1f,
                animate = true,
                barCount = 5,
                modifier = Modifier.size(width = 22.dp, height = 16.dp),
            )
        } else {
            IconButton(
                icon = MeedwellIcons.Close,
                contentDescription = "Take ${item.title} out of the queue",
                onClick = onRemove,
                tint = colors.tertiaryText,
                size = 18.dp,
            )
        }
    }
}

/**
 * How many are left, which is the number somebody actually wants.
 *
 * "12 tracks" in a queue you are eight into is not the answer to any question.
 */
private fun queueCount(total: Int, currentIndex: Int): String {
    val remaining = if (currentIndex >= 0) total - currentIndex - 1 else total
    return when (remaining) {
        0 -> "last one"
        1 -> "1 after this"
        else -> "$remaining after this"
    }
}
