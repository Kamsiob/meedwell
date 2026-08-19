package com.kamsiob.meedwell.playback

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.data.ShelfScope
import com.kamsiob.meedwell.data.ShelfSort
import com.kamsiob.meedwell.ui.screens.CoverUrls
import kotlinx.coroutines.flow.first

/**
 * The shelf, as a car can read it.
 *
 * Android Auto never draws this app's own interface. It asks the media session
 * what is at the root, what is inside each node, and hands back a media id when
 * somebody taps something. Everything the design does with staves, contours and
 * serif italics is absent by construction: the car draws Google's templates
 * from the titles and the artwork this file supplies, and nothing else.
 *
 * So the whole job is choosing what the tree contains, and the constraints are
 * not visual. **A person doing this is driving.** Auto enforces some of that,
 * capping how many items a screen shows and refusing custom views, but the part
 * that matters is not enforced: how many taps it takes to start music, and
 * whether the thing you wanted is on the first screen.
 *
 * ## Why these five nodes
 *
 * **Recent is first because it is what a car is for.** Somebody getting into a
 * car overwhelmingly wants the record they were already playing, or the one
 * from yesterday. Making that the first tab turns the common case into one tap,
 * and it is read from the play log the shelf already keeps.
 *
 * **Albums, Composers and Lists mirror the shelf**, so a person who knows the
 * app knows the car. Genres are deliberately not here: they are the library's
 * idea of order rather than the listener's, and a fourth browse tab earns its
 * place only if somebody reaches for it while driving.
 *
 * **Surroundings is deliberately absent.** The bed is a second sound layered
 * under the first, at a level you set by dragging, which is a two control
 * interaction and the car is the one place that is a genuinely bad idea. If it
 * ever appears here it should be as plain playable items with no layering, and
 * that is a product decision rather than an oversight.
 *
 * ## What is served from where
 *
 * Every node is answered from the local database. Nothing here waits on
 * Bandcamp, because a browse tree that needs the network is a browse tree that
 * is empty in a parking garage, which is exactly where somebody is when they
 * connect. Streaming a track still needs the network, and an item whose file is
 * on the phone plays from the file, which is the same rule the app already
 * follows everywhere else.
 */
@UnstableApi
class MediaTree(private val container: AppContainer) {

    /**
     * How many items one browse screen may hold.
     *
     * Auto has its own cap and enforces it. This one exists so that a shelf of
     * a thousand records does not build a thousand `MediaItem`s to have all but
     * the first few thrown away.
     */
    private val screenLimit = 200

    // The media id scheme. A prefix and an id, so that parsing a node is
    // reading a string rather than consulting a table held somewhere else.
    private companion object {
        const val ROOT = "meedwell:root"
        const val RECENT = "meedwell:recent"
        const val ALBUMS = "meedwell:albums"
        const val COMPOSERS = "meedwell:composers"
        const val LISTS = "meedwell:lists"

        const val ALBUM = "meedwell:album:"
        const val ARTIST = "meedwell:artist:"
        const val LIST = "meedwell:list:"
        const val TRACK = "meedwell:track:"
    }

    /**
     * The root, carrying the style hints for everything below it.
     *
     * Browsable things are drawn as a grid, because they are records and a
     * record is its cover. Playable things are drawn as a list, because they
     * are movements and a movement is its name. Auto reads these once, from the
     * root, and applies them throughout unless a node says otherwise.
     */
    fun rootExtras(): Bundle = Bundle().apply {
        putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )
        putInt(
            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
    }

    fun root(): MediaItem = browsable(ROOT, "Meedwell", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    /** One node, asked for by id. Auto does this when restoring where it was. */
    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT -> root()
        mediaId == RECENT -> browsable(RECENT, "Recent", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
        mediaId == ALBUMS -> browsable(ALBUMS, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
        mediaId == COMPOSERS ->
            browsable(COMPOSERS, "Composers", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
        mediaId == LISTS -> browsable(LISTS, "Lists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
        mediaId.startsWith(ALBUM) ->
            container.library.observeAlbum(mediaId.removePrefix(ALBUM)).first()?.let(::albumItem)
        mediaId.startsWith(ARTIST) ->
            container.library.observeArtist(mediaId.removePrefix(ARTIST)).first()?.let(::artistItem)
        mediaId.startsWith(TRACK) ->
            container.library.track(mediaId.removePrefix(TRACK))?.let(::trackItem)
        else -> null
    }

    /** What is inside a node, one screen at a time. */
    suspend fun children(parentId: String, page: Int, pageSize: Int): List<MediaItem> {
        val all: List<MediaItem> = when {
            parentId == ROOT -> listOf(
                browsable(RECENT, "Recent", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsable(ALBUMS, "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                browsable(COMPOSERS, "Composers", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                browsable(LISTS, "Lists", MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            )

            // **Recently played records, most recent first, each appearing
            // once.** The play log is per track, so the same record arrives
            // many times over; what somebody wants here is the record, not the
            // forty times they played it.
            parentId == RECENT -> container.library.observeHistory().first()
                .distinctBy { it.albumId }
                .take(screenLimit)
                .mapNotNull { row -> container.library.observeAlbum(row.albumId).first() }
                .map(::albumItem)

            parentId == ALBUMS ->
                container.library.observeAlbums(ShelfSort.Artist, ShelfScope.Everything).first()
                    .take(screenLimit)
                    .map(::albumItem)

            parentId == COMPOSERS ->
                container.library.observeArtists().first().take(screenLimit).map(::artistItem)

            parentId == LISTS -> container.playlists.observeAll().first()
                .take(screenLimit)
                .map { list ->
                    browsable(
                        LIST + list.id,
                        list.name,
                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    )
                }

            parentId.startsWith(ALBUM) ->
                container.library.tracksForAlbum(parentId.removePrefix(ALBUM)).map(::trackItem)

            parentId.startsWith(ARTIST) ->
                container.library.observeAlbumsByArtist(parentId.removePrefix(ARTIST)).first()
                    .map(::albumItem)

            parentId.startsWith(LIST) ->
                container.playlists.observeTracks(parentId.removePrefix(LIST)).first()
                    .map(::trackItem)

            else -> emptyList()
        }

        val from = page * pageSize
        if (from >= all.size) return emptyList()
        return all.subList(from, minOf(from + pageSize, all.size))
    }

    /**
     * Voice search, which in a car is most of how anybody finds anything.
     *
     * Tracks come first deliberately. "Play Gaelic Earth" names a piece far
     * more often than a record, and Auto plays the first result, so the first
     * result had better be the thing that was said.
     */
    suspend fun search(query: String, page: Int, pageSize: Int): List<MediaItem> {
        val results = container.library.search(query)
        val all = results.tracks.map(::trackItem) +
            results.albums.map(::albumItem) +
            results.artists.map(::artistItem)
        val from = page * pageSize
        if (from >= all.size) return emptyList()
        return all.subList(from, minOf(from + pageSize, all.size))
    }

    /**
     * A media id, turned into something the player can actually play.
     *
     * **This is what makes a tap in the car produce sound.** Auto hands back an
     * item carrying only the id it was given, with no uri on it, and the
     * session has to fill in the rest. A browsable id resolves to everything
     * inside it, so tapping a record plays the record.
     */
    suspend fun resolve(mediaId: String): List<MediaItem> = when {
        mediaId.startsWith(TRACK) ->
            listOfNotNull(container.library.track(mediaId.removePrefix(TRACK))).map(::playable)

        mediaId.startsWith(ALBUM) ->
            container.library.tracksForAlbum(mediaId.removePrefix(ALBUM)).map(::playable)

        mediaId.startsWith(LIST) ->
            container.playlists.observeTracks(mediaId.removePrefix(LIST)).first().map(::playable)

        mediaId.startsWith(ARTIST) ->
            container.library.observeAlbumsByArtist(mediaId.removePrefix(ARTIST)).first()
                .flatMap { container.library.tracksForAlbum(it.id) }
                .map(::playable)

        else -> emptyList()
    }

    /**
     * The same conversion the app's own queue uses, client and all.
     *
     * Reusing it is the point: the local file wins over the stream here exactly
     * as it does everywhere else, and a rule with one implementation cannot
     * drift into two.
     */
    private fun playable(track: Track): MediaItem = track.toMediaItem(container.client())

    private fun browsable(id: String, title: String, mediaType: Int, artwork: Uri? = null) =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(mediaType)
                    .setArtworkUri(artwork)
                    .build()
            )
            .build()

    /**
     * A record: browsable so it can be opened, playable so it can be started.
     *
     * Both, because in a car either is a reasonable thing to want and guessing
     * wrong costs a tap at exactly the wrong moment.
     */
    private fun albumItem(album: Album) = MediaItem.Builder()
        .setMediaId(ALBUM + album.id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(album.name)
                .setArtist(album.artist)
                .setAlbumTitle(album.name)
                .setIsBrowsable(true)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                .setArtworkUri(CoverUrls.of(album.coverArtId)?.toUri())
                .build()
        )
        .build()

    private fun artistItem(artist: Artist) = MediaItem.Builder()
        .setMediaId(ARTIST + artist.id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(artist.name)
                .setIsBrowsable(true)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                .setArtworkUri(CoverUrls.of(artist.coverArtId)?.toUri())
                .build()
        )
        .build()

    /**
     * A movement, as the car lists it.
     *
     * No uri: this is a browse entry, and Auto asks for the playable version
     * separately through [resolve]. Building the stream url here would mint one
     * for every row on screen, most of which are never played.
     */
    private fun trackItem(track: Track) = MediaItem.Builder()
        .setMediaId(TRACK + track.id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.albumName)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setDurationMs((track.durationSeconds * 1000).takeIf { it > 0 })
                .setArtworkUri(CoverUrls.of(track.coverArtId)?.toUri())
                .build()
        )
        .build()
}
