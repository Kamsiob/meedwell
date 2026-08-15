package com.kamsiob.meedwell.core.library

/**
 * Works out which artist an album belongs to from the artists of its tracks.
 *
 * This exists because Bandcamp's `getAlbumList2` returns an album's artist
 * *name* but no `artistId`. Without an ID, `observeByArtistId` matches nothing,
 * so every artist page showed an artist with no records and the action sheet
 * never offered "Go to artist". The tracks do carry the ID, so the album's is
 * recovered from them.
 *
 * The rule is a plurality with no ties broken:
 *
 *  - **Most tracks wins.** Taking the first track's artist would give a
 *    compilation the artist of whoever happens to open it, which is a wrong
 *    answer presented with total confidence.
 *  - **A tie returns nothing.** Two artists with equal claim on a record means
 *    there is no single album artist, and leaving the field empty is honest.
 *    The album still appears on the shelf under its artist *name*; only the
 *    link to an artist page is withheld.
 *  - **Blanks do not vote.** A track with no artist ID is missing data, not a
 *    vote for nobody.
 */
fun resolveAlbumArtistId(trackArtistIds: List<String>): String? {
    val counts = trackArtistIds
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
    if (counts.isEmpty()) return null

    val ranked = counts.entries.sortedByDescending { it.value }
    val decided = ranked.size == 1 || ranked[0].value > ranked[1].value
    return if (decided) ranked[0].key else null
}
