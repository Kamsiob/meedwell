package com.kamsiob.meedwell.core.subsonic

import kotlinx.serialization.json.Json

/**
 * Bandcamp's Subsonic beta fails in five distinguishable ways, and telling them
 * apart is the difference between an honest error screen and a shrug.
 *
 * Verified 15 August 2026 against the live account. Each shape below was
 * produced deliberately and its raw response saved.
 */
sealed interface SubsonicOutcome<out T> {

    data class Success<T>(val value: T) : SubsonicOutcome<T>

    /**
     * HTTP 500 with an empty body. This is what a rejected login looks like.
     * There is no Subsonic error code, no message, and no body at all.
     *
     * This is almost certainly the "unexplained 401" in the field reports:
     * clients meet a bare server error on the auth path and each renders it
     * differently. The Connection trouble screen says what actually happened
     * rather than inventing a code 40 that never arrives.
     */
    data object AuthRejected : SubsonicOutcome<Nothing>

    /**
     * The body `{"error":true,"error_message":"bad version"}`, which is not a
     * `subsonic-response` envelope at all.
     *
     * It means the route does not exist. It does **not** mean the protocol
     * version is wrong: every version from 1.8.0 to 1.16.1 produces it on a bad
     * path, and an invented endpoint name produces it too. Treat it as
     * "Bandcamp has not implemented this", which is true of `download`,
     * `createPlaylist`, `getAlbumInfo2` and several more.
     *
     * This is never shown to a user. A capability that does not exist is
     * absent from the interface, not an error in it.
     */
    data class EndpointAbsent(val endpoint: String) : SubsonicOutcome<Nothing>

    /**
     * A real Subsonic error inside a proper envelope, for example code 70,
     * "not found", for an id that does not exist. These honor `f=json`.
     */
    data class ServerError(val code: Int, val message: String) : SubsonicOutcome<Nothing>

    /**
     * The server answered in XML despite `f=json`. `unstar` does this on every
     * call, returning `status="failed" code="0" message="unknown error"`.
     *
     * A JSON-only parser crashes here, which is why this case is named rather
     * than lumped into a generic failure: the one endpoint that does it is the
     * one a user will hit twice in a row trying to remove a heart.
     */
    data class XmlFailure(val code: Int, val message: String) : SubsonicOutcome<Nothing>

    /** The transport itself failed: no network, timeout, DNS, a dropped socket. */
    data class Unreachable(val reason: String) : SubsonicOutcome<Nothing>

    /** The body arrived but could not be made sense of at all. */
    data class Unreadable(val reason: String) : SubsonicOutcome<Nothing>
}

/**
 * Unknown fields are ignored rather than fatal, which is rule three of the
 * tolerant contract. Bandcamp already sends nonstandard fields and will send
 * more; a strict parser would treat every future addition as a breaking change.
 */
val SubsonicJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** Exactly the body Bandcamp returns for a route it does not implement. */
private const val UNKNOWN_ROUTE_MARKER = "\"error_message\""

/**
 * Turns a raw HTTP result into one of the five shapes above.
 *
 * Deliberately takes the status code and the body as plain values rather than
 * any HTTP type, because `:core` has no HTTP dependency and must not gain one.
 * `:app` owns the socket; this owns the meaning.
 */
fun parseSubsonicBody(
    endpoint: String,
    httpStatus: Int,
    body: String,
): SubsonicOutcome<SubsonicResponseBody> {
    val trimmed = body.trim()

    // A rejected login: 5xx with nothing in it. Checked before anything else
    // because there is no body to inspect.
    if (trimmed.isEmpty()) {
        return if (httpStatus >= 500) SubsonicOutcome.AuthRejected
        else SubsonicOutcome.Unreadable("empty body, HTTP $httpStatus")
    }

    // XML, which arrives despite f=json on at least the unstar path.
    if (trimmed.startsWith("<")) {
        val code = Regex("""code="(\d+)"""").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val message = Regex("""message="([^"]*)"""").find(trimmed)?.groupValues?.get(1).orEmpty()
        return SubsonicOutcome.XmlFailure(code, message.ifEmpty { "unknown error" })
    }

    // The unknown-route body, which is not an envelope.
    if (trimmed.contains(UNKNOWN_ROUTE_MARKER) && !trimmed.contains("subsonic-response")) {
        return SubsonicOutcome.EndpointAbsent(endpoint)
    }

    val envelope = runCatching { SubsonicJson.decodeFromString<SubsonicEnvelope>(trimmed) }
        .getOrElse { return SubsonicOutcome.Unreadable(it.message ?: "could not parse the response") }

    val response = envelope.response
    response.error?.let { return SubsonicOutcome.ServerError(it.code, it.message) }
    if (response.status.equals("failed", ignoreCase = true)) {
        return SubsonicOutcome.ServerError(0, "the server reported a failure without saying why")
    }

    return SubsonicOutcome.Success(response)
}
