package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 20 in the visual reference: Search.
 *
 * **Searches what you own, and nothing leaves the phone.** Bandcamp's API
 * exposes only the user's own collection, with no store or catalogue search,
 * and that is the single largest constraint on this app's scope. So the screen
 * says so plainly and offers the one honest way past it: a deep link that hands
 * the query to Bandcamp's own site in the user's browser.
 *
 * That handoff is the only thing about a search that ever leaves the device,
 * and it happens only when the user taps it.
 */
@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onTrackClick: (Track) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSearchBandcamp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier.fillMaxSize().padding(horizontal = 22.dp)) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .defaultMinSize(minHeight = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = type.body.fontFamily,
                    fontSize = type.body.fontSize,
                    color = colors.primaryText,
                ),
                cursorBrush = SolidColor(colors.primaryText),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics { contentDescription = "Search your shelf" },
            )
            if (state.query.isNotEmpty()) {
                Box(
                    Modifier
                        .height(48.dp)
                        .clickable(role = Role.Button) { onQueryChange("") }
                        .semantics { contentDescription = "Clear the search" }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Clear", style = type.metadata, color = colors.secondaryText) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))

        if (state.query.isBlank()) {
            Text(
                "Search covers what you own, and never leaves the phone.",
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 22.dp),
            )
            return@Column
        }

        LazyColumn(Modifier.weight(1f)) {
            if (state.albums.isNotEmpty()) {
                item(key = "albums-head") { SectionHead("Albums") }
                items(state.albums, key = { "a-" + it.id }) { album ->
                    ResultRow(
                        title = album.name,
                        subtitle = "${album.artist}${if (album.year > 0) " · ${album.year}" else ""}",
                        coverUrl = album.coverUrl,
                        round = false,
                        onClick = { onAlbumClick(album) },
                    )
                }
            }
            if (state.tracks.isNotEmpty()) {
                item(key = "tracks-head") { SectionHead("Tracks") }
                items(state.tracks, key = { "t-" + it.id }) { track ->
                    ResultRow(
                        title = track.title,
                        subtitle = track.artist,
                        coverUrl = null,
                        round = false,
                        trailing = formatDuration(track.durationSeconds),
                        onClick = { onTrackClick(track) },
                    )
                }
            }
            if (state.artists.isNotEmpty()) {
                item(key = "artists-head") { SectionHead("Artists") }
                items(state.artists, key = { "r-" + it.id }) { artist ->
                    ResultRow(
                        title = artist.name,
                        subtitle = if (artist.albumCount == 1) "1 album on your shelf" else "${artist.albumCount} albums on your shelf",
                        coverUrl = artist.imageUrl.takeIf { it.isNotBlank() },
                        round = true,
                        onClick = { onArtistClick(artist) },
                    )
                }
            }
            if (state.isEmpty) {
                item(key = "nothing") {
                    // An empty result is an invitation rather than a scolding,
                    // and it names the one thing that can still be done.
                    Text(
                        "Nothing on your shelf matches that. Bandcamp's own site can look through " +
                            "everything they carry.",
                        style = type.metadata,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 22.dp),
                    )
                }
            }
        }

        // The deliberate handoff. Nothing about the search has left the phone
        // until this is tapped.
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button) { onSearchBandcamp(state.query) }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Search all of Bandcamp for \"${state.query}\" ↗",
                style = type.provenance,
                color = colors.primaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionHead(title: String) {
    Text(
        title.uppercase(),
        style = MeedwellTheme.typography.capsEyebrow,
        color = MeedwellTheme.colors.secondaryText,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    coverUrl: String?,
    round: Boolean,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverThumb(
                url = coverUrl,
                title = title,
                size = 44.dp,
                modifier = if (round) Modifier.clip(CircleShape) else Modifier,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = type.rowTitle, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = type.metadata, color = colors.tertiaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) {
                Text(trailing, style = type.metadata, color = colors.tertiaryText)
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
    }
}

data class SearchState(
    val query: String = "",
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    val isEmpty: Boolean get() = albums.isEmpty() && tracks.isEmpty() && artists.isEmpty()
}

/**
 * The Bandcamp search deep link.
 *
 * `item_type` is `b` for artists, `a` for albums and `t` for tracks. No type is
 * passed here, so the site searches everything, which is what somebody looking
 * beyond their own shelf actually wants.
 */
fun bandcampSearchUrl(query: String): String =
    "https://bandcamp.com/search?q=" + query.trim().replace(" ", "+")
