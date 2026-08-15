package com.kamsiob.meedwell.core.subsonic

import java.security.MessageDigest
import kotlin.random.Random

/**
 * Credentials for one Bandcamp Subsonic account.
 *
 * These never reach the database, an export, a log, or a crash report. On
 * Android they live in EncryptedSharedPreferences and nowhere else. `toString`
 * is overridden so a careless log line cannot leak the password: this has to be
 * impossible rather than merely discouraged.
 */
data class SubsonicCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    override fun toString(): String = "SubsonicCredentials(serverUrl=$serverUrl, username=***, password=***)"

    /**
     * The address a user pastes is `https://bandcamp.com/api/subsonic`, and
     * every call goes one level below it at `/rest/`.
     *
     * This cost real time during verification: calling the endpoint directly
     * under the server address returns `bad version`, which reads like a
     * protocol mismatch and is actually a wrong path. Appending `/rest/` is
     * standard Subsonic client behaviour, so the address the user sees stays
     * exactly what Bandcamp gave them.
     */
    val restBase: String
        get() = serverUrl.trimEnd('/').removeSuffix("/rest") + "/rest"
}

/** What a request needs, with no HTTP type anywhere near `:core`. */
data class SubsonicRequest(
    val url: String,
    val endpoint: String,
)

/** A response, reduced to the two things meaning depends on. */
data class SubsonicHttpResponse(
    val status: Int,
    val body: String,
)

/**
 * The socket lives in `:app`. This is the seam.
 *
 * `:core` has no Android and no HTTP dependency, so it describes what it wants
 * and lets the platform fetch it. That is the same seam a future Linux desktop
 * or web build implements with whatever its platform offers.
 */
interface SubsonicHttpEngine {
    suspend fun get(request: SubsonicRequest): SubsonicHttpResponse
}

/**
 * Token and salt authentication, which is the only scheme Bandcamp offers.
 * `getOpenSubsonicExtensions` returns an empty array, so there is no apiKey
 * path.
 *
 * Plaintext `p=` is never sent. Bandcamp rejects it anyway, but Meedwell would
 * refuse regardless of whether the server accepted it.
 */
object SubsonicAuth {

    private const val SALT_BYTES = 12

    fun newSalt(random: Random = Random.Default): String {
        val bytes = ByteArray(SALT_BYTES) { random.nextInt(0, 256).toByte() }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * `t = MD5(password + salt)`.
     *
     * MD5 here is a Subsonic protocol requirement, not a security choice.
     * Nothing in this app relies on it being strong; the transport is HTTPS.
     *
     * `MessageDigest` is the single JVM-only call in `:core`. If this module is
     * ever converted to Kotlin Multiplatform, this one function is what needs a
     * platform implementation, and nothing else in the module does.
     */
    fun token(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("MD5").digest((password + salt).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Builds every Subsonic call and gives back a meaning rather than a body.
 *
 * The client deliberately knows which endpoints Bandcamp does not implement.
 * That is not a hardcoded denylist standing in for a capability check: the
 * calls are simply not made, because a call that always returns "route absent"
 * is a wasted round trip and a chance to show an error for something that is
 * not an error.
 */
class SubsonicClient(
    private val engine: SubsonicHttpEngine,
    private val credentials: SubsonicCredentials,
    private val clientName: String = "Meedwell",
    private val random: Random = Random.Default,
) {

    private fun url(endpoint: String, params: Map<String, String> = emptyMap()): String {
        val salt = SubsonicAuth.newSalt(random)
        val token = SubsonicAuth.token(credentials.password, salt)
        val query = buildMap {
            put("u", credentials.username)
            put("t", token)
            put("s", salt)
            put("v", PROTOCOL_VERSION)
            put("c", clientName)
            put("f", "json")
            putAll(params)
        }.entries.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
        return "${credentials.restBase}/$endpoint?$query"
    }

    private suspend fun call(endpoint: String, params: Map<String, String> = emptyMap()): SubsonicOutcome<SubsonicResponseBody> =
        try {
            val response = engine.get(SubsonicRequest(url(endpoint, params), endpoint))
            parseSubsonicBody(endpoint, response.status, response.body)
        } catch (t: Throwable) {
            SubsonicOutcome.Unreachable(t.message ?: "the network did not answer")
        }

    /**
     * Confirms the credentials actually work.
     *
     * **Not `ping`.** `ping` returns `status: ok` for a wrong password, verified
     * on 15 August 2026, so validating with it would accept anything a user
     * typed. `getArtists` is the cheapest call that genuinely requires auth.
     *
     * A future session will look at this and see an obvious simplification.
     * It is not one.
     */
    suspend fun validateCredentials(): SubsonicOutcome<SubsonicResponseBody> = call("getArtists")

    /** Server identity only. Says nothing about whether the credentials are good. */
    suspend fun ping(): SubsonicOutcome<SubsonicResponseBody> = call("ping")

    suspend fun getArtists(): SubsonicOutcome<SubsonicResponseBody> = call("getArtists")

    suspend fun getArtist(id: String): SubsonicOutcome<SubsonicResponseBody> = call("getArtist", mapOf("id" to id))

    suspend fun getAlbum(id: String): SubsonicOutcome<SubsonicResponseBody> = call("getAlbum", mapOf("id" to id))

    suspend fun getGenres(): SubsonicOutcome<SubsonicResponseBody> = call("getGenres")

    /**
     * `type` is restricted to the four Bandcamp actually populates. `frequent`,
     * `recent` and `highest` return empty lists, so offering them as sort
     * options would be offering a sort that silently does nothing.
     */
    suspend fun getAlbumList2(
        type: String = "newest",
        size: Int = 500,
        offset: Int = 0,
    ): SubsonicOutcome<SubsonicResponseBody> = call(
        "getAlbumList2",
        mapOf("type" to type, "size" to size.toString(), "offset" to offset.toString()),
    )

    suspend fun search3(
        query: String,
        artistCount: Int = 20,
        albumCount: Int = 20,
        songCount: Int = 50,
    ): SubsonicOutcome<SubsonicResponseBody> = call(
        "search3",
        mapOf(
            "query" to query,
            "artistCount" to artistCount.toString(),
            "albumCount" to albumCount.toString(),
            "songCount" to songCount.toString(),
        ),
    )

    /** `getStarred2` does not exist on Bandcamp. `getStarred` does. */
    suspend fun getStarred(): SubsonicOutcome<SubsonicResponseBody> = call("getStarred")

    suspend fun star(songId: String? = null, albumId: String? = null, artistId: String? = null): SubsonicOutcome<SubsonicResponseBody> =
        call("star", buildMap {
            songId?.let { put("id", it) }
            albumId?.let { put("albumId", it) }
            artistId?.let { put("artistId", it) }
        })

    /**
     * Broken on Bandcamp's side and kept anyway, because the app has to be able
     * to tell a user why a heart will not come off. Every form returns XML
     * with `code="0" message="unknown error"`, which arrives here as
     * [SubsonicOutcome.XmlFailure] and is what the interface explains.
     *
     * If Bandcamp fixes it, this starts succeeding and the honest-limit line in
     * the interface stops being needed. Nothing else has to change.
     */
    suspend fun unstar(songId: String? = null, albumId: String? = null, artistId: String? = null): SubsonicOutcome<SubsonicResponseBody> =
        call("unstar", buildMap {
            songId?.let { put("id", it) }
            albumId?.let { put("albumId", it) }
            artistId?.let { put("artistId", it) }
        })

    /** Read only. Bandcamp implements no way to create, edit or delete one. */
    suspend fun getPlaylists(): SubsonicOutcome<SubsonicResponseBody> = call("getPlaylists")

    /** A direct URL for the player and the image loader to fetch. */
    fun streamUrl(songId: String): String = url("stream", mapOf("id" to songId))

    fun coverArtUrl(coverArtId: String): String = url("getCoverArt", mapOf("id" to coverArtId))

    companion object {
        const val PROTOCOL_VERSION = "1.16.1"

        /**
         * Endpoints verified absent on 15 August 2026. Listed so that a future
         * session reads why a capability is missing instead of assuming it was
         * forgotten, and so re-verification has something to check against.
         */
        val VERIFIED_ABSENT = setOf(
            "download",
            "getAlbumInfo2",
            "getArtistInfo2",
            "getStarred2",
            "getScanStatus",
            "getNowPlaying",
            "getRandomSongs",
            "createPlaylist",
            "updatePlaylist",
            "deletePlaylist",
            "getPlaylist",
        )
    }
}

private fun encode(value: String): String = buildString {
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && c.code < 128 || c in "-_.~") append(c) else append("%%%02X".format(byte))
    }
}
