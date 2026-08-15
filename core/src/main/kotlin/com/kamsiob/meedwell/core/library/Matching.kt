package com.kamsiob.meedwell.core.library

/**
 * Matching files on the phone to records in the collection.
 *
 * Tier C made this the centre of the app rather than a convenience. Bandcamp's
 * API will not release purchased files, so a user downloads them from Bandcamp
 * themselves and Meedwell recognises what arrives. If this is wrong, the
 * ownership story does not work.
 *
 * The rule from `MASTER_SPEC.md` section 7: match on artist plus album plus
 * track, never duplicate, prefer the local file for playback, mark the album as
 * owned. It has to work in both directions, and the reverse case is likelier
 * than it sounds: somebody who downloaded from Bandcamp's website before
 * installing Meedwell should find those files recognised and merged, not
 * duplicated alongside the streamed copies.
 */
object Matching {

    /**
     * Normalises a name for comparison.
     *
     * Deliberately aggressive, because the two sides of a match come from
     * different places: one from Bandcamp's API, the other from a file name or
     * an ID3 tag written by whatever encoded it. "Copper Lines (Remastered)"
     * and "Copper Lines [remastered]" are the same track, and a listener would
     * be baffled to see both.
     *
     * What it does **not** do is drop words. Removing "remastered" entirely
     * would collapse genuinely different releases, which is worse than showing
     * two rows.
     */
    fun normalise(raw: String): String {
        val lowered = raw.lowercase().trim()
        val builder = StringBuilder(lowered.length)
        var lastWasSpace = false
        for (ch in lowered) {
            when {
                ch.isLetterOrDigit() -> {
                    builder.append(ch)
                    lastWasSpace = false
                }
                // Every separator collapses to a single space, so brackets,
                // dashes, underscores and multiple spaces all compare equal.
                ch.isWhitespace() || ch in "-_()[]{}.,'\"!?&/\\:;" -> {
                    if (!lastWasSpace && builder.isNotEmpty()) {
                        builder.append(' ')
                        lastWasSpace = true
                    }
                }
                else -> {
                    builder.append(ch)
                    lastWasSpace = false
                }
            }
        }
        return builder.toString().trim()
    }

    /**
     * Strips a leading track number from a file-derived title.
     *
     * Files are routinely named "03 Copper Lines.flac" or "03 - Copper
     * Lines.mp3", and the API's title carries no number. Without this, nothing
     * downloaded from Bandcamp's website would ever match, which would break
     * the entire Tier C story.
     *
     * Only leading digits followed by a separator are removed, so a track
     * genuinely called "1979" survives intact.
     */
    fun stripLeadingTrackNumber(title: String): String {
        val match = Regex("""^\s*\d{1,3}\s*[-._)]\s*(.+)$""").find(title)
        if (match != null) return match.groupValues[1].trim()
        // "03 Copper Lines", with only whitespace after the number. Requires at
        // least one following word so a title that is only a number survives.
        val spaced = Regex("""^\s*\d{1,3}\s+(\S.*)$""").find(title)
        return spaced?.groupValues?.get(1)?.trim() ?: title.trim()
    }

    /** A key that both a collection track and a file on disk can produce. */
    data class TrackKey(val artist: String, val album: String, val title: String)

    fun keyOf(artist: String, album: String, title: String): TrackKey = TrackKey(
        artist = normalise(artist),
        album = normalise(album),
        title = normalise(stripLeadingTrackNumber(title)),
    )

    /**
     * A weaker key for the case where the file's artist tag disagrees with the
     * album artist, which happens constantly on compilations: the API reports
     * one album artist while the file carries the individual performer.
     *
     * Used only after an exact match fails, so it can never override a
     * confident match with a looser one.
     */
    data class AlbumTrackKey(val album: String, val title: String)

    fun looseKeyOf(album: String, title: String): AlbumTrackKey = AlbumTrackKey(
        album = normalise(album),
        title = normalise(stripLeadingTrackNumber(title)),
    )

    /**
     * Matches a set of local files against a set of collection tracks.
     *
     * Exact artist-album-title matches are taken first, then album-title
     * matches for anything left over. A file already claimed by an exact match
     * is never reconsidered, and a collection track is never matched twice, so
     * the result cannot contain a duplicate in either direction.
     *
     * Anything unmatched is returned so the caller can decide what it is:
     * usually a local-only album, which is a first-class thing rather than a
     * failure.
     */
    fun <F> match(
        collection: List<CollectionTrack>,
        files: List<LocalFile<F>>,
    ): MatchResult<F> {
        val exactByKey = HashMap<TrackKey, MutableList<CollectionTrack>>()
        val looseByKey = HashMap<AlbumTrackKey, MutableList<CollectionTrack>>()
        collection.forEach { track ->
            exactByKey.getOrPut(keyOf(track.artist, track.album, track.title)) { mutableListOf() } += track
            looseByKey.getOrPut(looseKeyOf(track.album, track.title)) { mutableListOf() } += track
        }

        val matches = mutableListOf<Match<F>>()
        val claimed = HashSet<String>()
        val unmatched = mutableListOf<LocalFile<F>>()

        // Pass one: exact.
        val leftOver = mutableListOf<LocalFile<F>>()
        for (file in files) {
            val candidates = exactByKey[keyOf(file.artist, file.album, file.title)]
            val target = candidates?.firstOrNull { it.id !in claimed }
            if (target != null) {
                claimed += target.id
                matches += Match(target, file)
            } else {
                leftOver += file
            }
        }

        // Pass two: album plus title, for compilations where the artist tag
        // names the performer rather than the album artist.
        for (file in leftOver) {
            val candidates = looseByKey[looseKeyOf(file.album, file.title)]
            val target = candidates?.firstOrNull { it.id !in claimed }
            if (target != null) {
                claimed += target.id
                matches += Match(target, file)
            } else {
                unmatched += file
            }
        }

        return MatchResult(matches = matches, unmatchedFiles = unmatched)
    }

    data class CollectionTrack(
        val id: String,
        val albumId: String,
        val artist: String,
        val album: String,
        val title: String,
    )

    data class LocalFile<F>(
        val artist: String,
        val album: String,
        val title: String,
        val payload: F,
    )

    data class Match<F>(val track: CollectionTrack, val file: LocalFile<F>)

    data class MatchResult<F>(
        val matches: List<Match<F>>,
        val unmatchedFiles: List<LocalFile<F>>,
    )
}
