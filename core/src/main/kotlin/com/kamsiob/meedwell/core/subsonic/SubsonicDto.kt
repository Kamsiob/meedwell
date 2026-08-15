package com.kamsiob.meedwell.core.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shapes, exactly as Bandcamp's Subsonic beta actually returns them
 * rather than as the 1.16.1 schema describes them.
 *
 * Three rules hold everywhere in this file, and they are what keep a beta from
 * breaking the app:
 *
 *  1. Every field has a default, so absence is never fatal.
 *  2. Every scalar goes through a tolerant serializer, so a type change is
 *     never fatal.
 *  3. Unknown fields are ignored rather than rejected, configured on the Json
 *     instance in [SubsonicJson]. Bandcamp adds nonstandard fields such as
 *     `artistImageUrl`, and will add more.
 *
 * Where a field is absent on Bandcamp specifically, it is still declared if the
 * protocol has it, so that another Subsonic server would populate it. That
 * costs nothing and is what makes "other Subsonic servers" a small change later
 * rather than a rewrite.
 */

@Serializable
data class SubsonicSong(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("parent") @Serializable(with = TolerantString::class) val parent: String = "",
    @SerialName("title") @Serializable(with = TolerantString::class) val title: String = "",
    @SerialName("album") @Serializable(with = TolerantString::class) val album: String = "",
    @SerialName("artist") @Serializable(with = TolerantString::class) val artist: String = "",
    @SerialName("albumId") @Serializable(with = TolerantString::class) val albumId: String = "",
    @SerialName("artistId") @Serializable(with = TolerantString::class) val artistId: String = "",
    @SerialName("coverArt") @Serializable(with = TolerantString::class) val coverArt: String = "",
    @SerialName("contentType") @Serializable(with = TolerantString::class) val contentType: String = "",
    @SerialName("suffix") @Serializable(with = TolerantString::class) val suffix: String = "",
    @SerialName("genre") @Serializable(with = TolerantString::class) val genre: String = "",
    @SerialName("type") @Serializable(with = TolerantString::class) val type: String = "",
    @SerialName("created") @Serializable(with = TolerantString::class) val created: String = "",
    @SerialName("track") @Serializable(with = TolerantInt::class) val track: Int = 0,
    @SerialName("year") @Serializable(with = TolerantInt::class) val year: Int = 0,
    @SerialName("bitRate") @Serializable(with = TolerantInt::class) val bitRate: Int = 0,
    // Absent on Bandcamp entirely. Declared because the protocol has it and
    // another server would send it. Local files carry their own disc numbers.
    @SerialName("discNumber") @Serializable(with = TolerantInt::class) val discNumber: Int = 0,
    // The float that field reports warned about. Integer on the test account,
    // read tolerantly regardless.
    @SerialName("duration") @Serializable(with = TolerantLong::class) val duration: Long = 0L,
    @SerialName("size") @Serializable(with = TolerantLong::class) val size: Long = 0L,
    @SerialName("isDir") @Serializable(with = TolerantBoolean::class) val isDir: Boolean = false,
    /**
     * Not a typo here. It is a typo in Bandcamp's `getStarred` response, which
     * returns `idDir` where every other endpoint returns `isDir`. Verified 15
     * August 2026. Read both and prefer whichever arrived.
     */
    @SerialName("idDir") @Serializable(with = TolerantBoolean::class) val idDirTypo: Boolean = false,
    @SerialName("isVideo") @Serializable(with = TolerantBoolean::class) val isVideo: Boolean = false,
    @SerialName("starred") @Serializable(with = TolerantString::class) val starred: String = "",
)

@Serializable
data class SubsonicGenreName(
    @SerialName("name") @Serializable(with = TolerantString::class) val name: String = "",
)

@Serializable
data class SubsonicAlbum(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("name") @Serializable(with = TolerantString::class) val name: String = "",
    @SerialName("title") @Serializable(with = TolerantString::class) val title: String = "",
    @SerialName("artist") @Serializable(with = TolerantString::class) val artist: String = "",
    @SerialName("artistId") @Serializable(with = TolerantString::class) val artistId: String = "",
    @SerialName("coverArt") @Serializable(with = TolerantString::class) val coverArt: String = "",
    @SerialName("genre") @Serializable(with = TolerantString::class) val genre: String = "",
    /** Purchase date on Bandcamp, which is what "On your shelf since" reads. */
    @SerialName("created") @Serializable(with = TolerantString::class) val created: String = "",
    @SerialName("starred") @Serializable(with = TolerantString::class) val starred: String = "",
    @SerialName("songCount") @Serializable(with = TolerantInt::class) val songCount: Int = 0,
    @SerialName("playCount") @Serializable(with = TolerantInt::class) val playCount: Int = 0,
    @SerialName("year") @Serializable(with = TolerantInt::class) val year: Int = 0,
    @SerialName("duration") @Serializable(with = TolerantLong::class) val duration: Long = 0L,
    /** Bandcamp sends duplicates in here. Deduplicated when mapped, not when read. */
    @SerialName("genres") @Serializable(with = GenreNameListSerializer::class) val genres: List<SubsonicGenreName> = emptyList(),
    @SerialName("song") @Serializable(with = SongListSerializer::class) val song: List<SubsonicSong> = emptyList(),
)

@Serializable
data class SubsonicArtist(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("name") @Serializable(with = TolerantString::class) val name: String = "",
    @SerialName("coverArt") @Serializable(with = TolerantString::class) val coverArt: String = "",
    /** Nonstandard, and the only URL Bandcamp returns anywhere. */
    @SerialName("artistImageUrl") @Serializable(with = TolerantString::class) val artistImageUrl: String = "",
    @SerialName("starred") @Serializable(with = TolerantString::class) val starred: String = "",
    @SerialName("albumCount") @Serializable(with = TolerantInt::class) val albumCount: Int = 0,
    @SerialName("album") @Serializable(with = AlbumListSerializer::class) val album: List<SubsonicAlbum> = emptyList(),
)

@Serializable
data class SubsonicIndex(
    @SerialName("name") @Serializable(with = TolerantString::class) val name: String = "",
    @SerialName("artist") @Serializable(with = ArtistListSerializer::class) val artist: List<SubsonicArtist> = emptyList(),
)

@Serializable
data class SubsonicArtists(
    @SerialName("ignoredArticles") @Serializable(with = TolerantString::class) val ignoredArticles: String = "",
    @SerialName("index") @Serializable(with = IndexListSerializer::class) val index: List<SubsonicIndex> = emptyList(),
)

@Serializable
data class SubsonicGenre(
    @SerialName("value") @Serializable(with = TolerantString::class) val value: String = "",
    @SerialName("songCount") @Serializable(with = TolerantInt::class) val songCount: Int = 0,
    @SerialName("albumCount") @Serializable(with = TolerantInt::class) val albumCount: Int = 0,
)

@Serializable
data class SubsonicGenres(
    @SerialName("genre") @Serializable(with = GenreListSerializer::class) val genre: List<SubsonicGenre> = emptyList(),
)

@Serializable
data class SubsonicAlbumList2(
    @SerialName("album") @Serializable(with = AlbumListSerializer::class) val album: List<SubsonicAlbum> = emptyList(),
)

@Serializable
data class SubsonicSearchResult3(
    @SerialName("artist") @Serializable(with = ArtistListSerializer::class) val artist: List<SubsonicArtist> = emptyList(),
    @SerialName("album") @Serializable(with = AlbumListSerializer::class) val album: List<SubsonicAlbum> = emptyList(),
    @SerialName("song") @Serializable(with = SongListSerializer::class) val song: List<SubsonicSong> = emptyList(),
)

@Serializable
data class SubsonicStarred(
    @SerialName("artist") @Serializable(with = ArtistListSerializer::class) val artist: List<SubsonicArtist> = emptyList(),
    @SerialName("album") @Serializable(with = AlbumListSerializer::class) val album: List<SubsonicAlbum> = emptyList(),
    @SerialName("song") @Serializable(with = SongListSerializer::class) val song: List<SubsonicSong> = emptyList(),
)

@Serializable
data class SubsonicPlaylist(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("name") @Serializable(with = TolerantString::class) val name: String = "",
    @SerialName("comment") @Serializable(with = TolerantString::class) val comment: String = "",
    @SerialName("songCount") @Serializable(with = TolerantInt::class) val songCount: Int = 0,
    @SerialName("duration") @Serializable(with = TolerantLong::class) val duration: Long = 0L,
    @SerialName("entry") @Serializable(with = SongListSerializer::class) val entry: List<SubsonicSong> = emptyList(),
)

@Serializable
data class SubsonicPlaylists(
    @SerialName("playlist") @Serializable(with = PlaylistListSerializer::class) val playlist: List<SubsonicPlaylist> = emptyList(),
)

@Serializable
data class SubsonicError(
    @SerialName("code") @Serializable(with = TolerantInt::class) val code: Int = 0,
    @SerialName("message") @Serializable(with = TolerantString::class) val message: String = "",
)

/**
 * The envelope. Every field is optional because any given call fills in one of
 * them, and a failed call fills in none.
 */
@Serializable
data class SubsonicResponseBody(
    @SerialName("status") @Serializable(with = TolerantString::class) val status: String = "",
    @SerialName("version") @Serializable(with = TolerantString::class) val version: String = "",
    @SerialName("type") @Serializable(with = TolerantString::class) val type: String = "",
    @SerialName("serverVersion") @Serializable(with = TolerantString::class) val serverVersion: String = "",
    @SerialName("openSubsonic") @Serializable(with = TolerantBoolean::class) val openSubsonic: Boolean = false,
    @SerialName("error") val error: SubsonicError? = null,
    @SerialName("albumList2") val albumList2: SubsonicAlbumList2? = null,
    @SerialName("album") val album: SubsonicAlbum? = null,
    @SerialName("artist") val artist: SubsonicArtist? = null,
    @SerialName("artists") val artists: SubsonicArtists? = null,
    @SerialName("genres") val genres: SubsonicGenres? = null,
    @SerialName("searchResult3") val searchResult3: SubsonicSearchResult3? = null,
    @SerialName("starred") val starred: SubsonicStarred? = null,
    @SerialName("playlists") val playlists: SubsonicPlaylists? = null,
    @SerialName("openSubsonicExtensions") val openSubsonicExtensions: List<kotlinx.serialization.json.JsonElement> = emptyList(),
)

@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicResponseBody = SubsonicResponseBody(),
)

// The list serializers, one per type, each absorbing "a single object where an
// array was promised" and "null where an array was promised".
internal object SongListSerializer : kotlinx.serialization.KSerializer<List<SubsonicSong>> by TolerantList(SubsonicSong.serializer())
internal object AlbumListSerializer : kotlinx.serialization.KSerializer<List<SubsonicAlbum>> by TolerantList(SubsonicAlbum.serializer())
internal object ArtistListSerializer : kotlinx.serialization.KSerializer<List<SubsonicArtist>> by TolerantList(SubsonicArtist.serializer())
internal object IndexListSerializer : kotlinx.serialization.KSerializer<List<SubsonicIndex>> by TolerantList(SubsonicIndex.serializer())
internal object GenreListSerializer : kotlinx.serialization.KSerializer<List<SubsonicGenre>> by TolerantList(SubsonicGenre.serializer())
internal object GenreNameListSerializer : kotlinx.serialization.KSerializer<List<SubsonicGenreName>> by TolerantList(SubsonicGenreName.serializer())
internal object PlaylistListSerializer : kotlinx.serialization.KSerializer<List<SubsonicPlaylist>> by TolerantList(SubsonicPlaylist.serializer())
