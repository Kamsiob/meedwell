package com.kamsiob.meedwell.core.model

/**
 * The domain model: what the app thinks in, as opposed to what the wire carries.
 *
 * This is deliberately not the Subsonic shape. Two reasons, and the second one
 * is the load bearing one:
 *
 *  1. A record on the shelf can come from Bandcamp, from a file on the phone,
 *     or from both at once, and the interface should not care which. Only
 *     [Album.origin] knows.
 *  2. Tier C means the merge between streamed records and files the user
 *     downloaded themselves is the app's central mechanic rather than a corner
 *     case. Modelling both as the same thing from the start is what keeps that
 *     from becoming a special case bolted on later.
 */

/** Where a record on the shelf came from. Never shown as jargon; see [Album.provenance]. */
enum class Origin {
    /** In the Bandcamp collection, streamable, with no local file found. */
    Bandcamp,

    /** A file on this phone with no matching Bandcamp purchase. */
    Local,

    /**
     * Both: purchased on Bandcamp and present as a file here. The good case,
     * and the one the whole "Your files" surface exists to produce.
     */
    Owned,
}

/**
 * A track.
 *
 * `durationSeconds` is a Long because it arrives from a beta that has been
 * reported to send floats, and rounding at the edge is better than carrying a
 * float through the whole app for a value that is only ever displayed as
 * minutes and seconds.
 */
data class Track(
    val id: String,
    val albumId: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val trackNumber: Int,
    val discNumber: Int,
    val durationSeconds: Long,
    val year: Int,
    val suffix: String,
    val bitRate: Int,
    val sizeBytes: Long,
    val coverArtId: String,
    /** Set when a file for this track exists on the phone. Playback prefers it. */
    val localPath: String? = null,
    val isStarred: Boolean = false,
) {
    val isPresentLocally: Boolean get() = localPath != null

    /**
     * A piece long enough that losing your place in it is a real loss. Drives
     * the "Resume from 22:40" markers and the Settings toggle.
     */
    val isLongForm: Boolean get() = durationSeconds >= LONG_FORM_SECONDS

    companion object {
        const val LONG_FORM_SECONDS = 20 * 60L
    }
}

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArtId: String,
    val year: Int,
    val trackCount: Int,
    val durationSeconds: Long,
    /** Purchase date on Bandcamp, which is what "On your shelf since" reads. */
    val addedAt: Long?,
    val genres: List<String>,
    val origin: Origin,
    val isStarred: Boolean = false,
    /** How many of this album's tracks were actually found on the phone. */
    val localTrackCount: Int = 0,
) {
    /**
     * The "yours" marker in the interface. An album counts as yours when its
     * files are here, whole. A partial match is honest about being partial
     * rather than rounding up to owned, which is the difference between this
     * app and a marketing claim.
     */
    val isFullyPresent: Boolean
        get() = trackCount > 0 && localTrackCount >= trackCount

    val isPartiallyPresent: Boolean
        get() = localTrackCount in 1 until trackCount

    /**
     * The provenance line under an album, in the app's own words rather than
     * enum names. Kept here rather than in the interface so that every screen
     * says the same thing about the same state.
     */
    val provenance: Provenance
        get() = when {
            isFullyPresent -> Provenance.Yours
            isPartiallyPresent -> Provenance.PartlyHere(localTrackCount, trackCount)
            origin == Origin.Local -> Provenance.OnThisPhone
            else -> Provenance.Streaming
        }
}

sealed interface Provenance {
    /** Bought and here in full. The serif italic "yours" marker. */
    data object Yours : Provenance

    /** Bought, and some of the files arrived. Never rounded up to "yours". */
    data class PartlyHere(val found: Int, val total: Int) : Provenance

    /** A file on the phone that was never matched to a purchase. */
    data object OnThisPhone : Provenance

    /** In the collection, streamable, no file here. */
    data object Streaming : Provenance
}

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String,
    val imageUrl: String,
    /** How many of their albums are present as files. Feeds "2 albums, both yours". */
    val ownedAlbumCount: Int = 0,
)

data class Genre(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val durationSeconds: Long = 0L,
)

/**
 * A list. Local by necessity rather than by preference: verification found that
 * Bandcamp's API implements no way to create, edit or delete a playlist.
 *
 * [fromBandcamp] marks the ones the account already had, which are shown and
 * not editable, because editing them would be a promise the API cannot keep.
 */
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val durationSeconds: Long = 0L,
    val fromBandcamp: Boolean = false,
) {
    val isEditable: Boolean get() = !fromBandcamp
}

/**
 * One row of the append-only play log.
 *
 * Append-only is the point: it powers History, the Forgotten Shelf and the
 * most-played sort, all computed on device, and it is the reason none of those
 * need a network call or an algorithm. Never updated in place, so a play is a
 * fact rather than a counter someone can disagree with.
 */
data class PlayEvent(
    val trackId: String,
    val albumId: String,
    val playedAt: Long,
    /** How far in the listener actually got, so a skip is not counted as a play. */
    val playedSeconds: Long,
    val completed: Boolean,
)
