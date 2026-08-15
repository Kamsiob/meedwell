package com.kamsiob.meedwell.core.subsonic

import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Genre
import com.kamsiob.meedwell.core.model.Origin
import com.kamsiob.meedwell.core.model.Playlist
import com.kamsiob.meedwell.core.model.Track

/**
 * Turns wire shapes into the domain model.
 *
 * The parser's job was to survive whatever arrived. This layer's job is to
 * decide what it means, and that is where Bandcamp's specific oddities get
 * cleaned up exactly once rather than in every screen that touches the data.
 */

/**
 * Bandcamp sends dates as `07 Aug 2026 16:24:01 GMT`, which is RFC 1123 with a
 * one or two digit day. Parsed here without a date library, because `:core` has
 * no dependencies and this is the only date format the API uses.
 *
 * Returns null rather than a wrong date on anything unexpected. "On your shelf
 * since" simply does not appear when the date is unreadable, which is better
 * than showing a confidently wrong month.
 */
fun parseSubsonicDate(raw: String): Long? {
    if (raw.isBlank()) return null
    val parts = raw.trim().split(" ")
    if (parts.size < 4) return null

    val day = parts[0].toIntOrNull() ?: return null
    val month = MONTHS.indexOf(parts[1]).takeIf { it >= 0 } ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    val time = parts.getOrNull(3)?.split(":") ?: emptyList()
    val hour = time.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = time.getOrNull(1)?.toIntOrNull() ?: 0
    val second = time.getOrNull(2)?.toIntOrNull() ?: 0

    if (day !in 1..31 || year !in 1900..3000) return null

    // Days from the epoch to the start of the given year, then to the month,
    // then the day. Written out rather than pulled from a library so `:core`
    // keeps no dependencies at all.
    var days = 0L
    for (y in 1970 until year) days += if (isLeap(y)) 366 else 365
    for (m in 0 until month) days += DAYS_IN_MONTH[m] + if (m == 1 && isLeap(year)) 1 else 0
    days += (day - 1)

    return ((days * 24 + hour) * 60 + minute) * 60 + second
}

private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

/**
 * Bandcamp sends both a `genre` string and a `genres` array, and the array
 * contains duplicates. Verified: one album carried "soundtrack" twice.
 *
 * Deduplicated case-insensitively while keeping the first spelling seen, so
 * "Drone" and "drone" collapse to whichever the album led with rather than to
 * an arbitrary one.
 */
internal fun mergeGenres(single: String, list: List<SubsonicGenreName>): List<String> {
    val seen = LinkedHashMap<String, String>()
    (listOf(single) + list.map { it.name })
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { seen.putIfAbsent(it.lowercase(), it) }
    return seen.values.toList()
}

fun SubsonicAlbum.toDomain(origin: Origin = Origin.Bandcamp): Album = Album(
    id = id,
    // `name` is the album title on every Bandcamp response, but `search3`
    // returns `title` as well and other Subsonic servers prefer it. Take
    // whichever is present so this keeps working elsewhere.
    name = name.ifBlank { title },
    artist = artist,
    artistId = artistId,
    coverArtId = coverArt,
    year = year,
    trackCount = songCount,
    durationSeconds = duration,
    addedAt = parseSubsonicDate(created),
    genres = mergeGenres(genre, genres),
    origin = origin,
    isStarred = starred.isNotBlank(),
)

fun SubsonicSong.toDomain(): Track = Track(
    id = id,
    // `albumId` is the reliable one; `parent` carries the same value and is
    // the fallback for servers that only send the older field.
    albumId = albumId.ifBlank { parent },
    title = title,
    artist = artist,
    artistId = artistId,
    trackNumber = track,
    // Bandcamp sends no disc number at all. Local files do, so the field stays
    // in the model and is simply always 1 for anything streamed.
    discNumber = if (discNumber > 0) discNumber else 1,
    durationSeconds = duration,
    year = year,
    suffix = suffix,
    bitRate = bitRate,
    sizeBytes = size,
    coverArtId = coverArt,
    isStarred = starred.isNotBlank(),
)

fun SubsonicArtist.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArtId = coverArt,
    imageUrl = artistImageUrl,
)

fun SubsonicGenre.toDomain(): Genre = Genre(
    name = value,
    albumCount = albumCount,
    songCount = songCount,
)

fun SubsonicPlaylist.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    trackIds = entry.map { it.id },
    durationSeconds = duration,
    // Anything arriving from the API came from the account, and the API offers
    // no way to change it, so it is shown and not editable.
    fromBandcamp = true,
)

/** Flattens the indexed artists response, which groups by first letter. */
fun SubsonicArtists.toDomain(): List<Artist> = index.flatMap { it.artist }.map { it.toDomain() }
