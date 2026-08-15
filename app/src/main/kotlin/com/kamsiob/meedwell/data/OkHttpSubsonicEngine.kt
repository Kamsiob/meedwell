package com.kamsiob.meedwell.data

import com.kamsiob.meedwell.core.subsonic.SubsonicHttpEngine
import com.kamsiob.meedwell.core.subsonic.SubsonicHttpResponse
import com.kamsiob.meedwell.core.subsonic.SubsonicRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The socket, which lives here rather than in `:core`.
 *
 * `:core` describes what it wants and this fetches it. That seam is what keeps
 * `:core` free of both Android and HTTP, and it is the same seam a future Linux
 * desktop or web build would implement with whatever its platform offers.
 *
 * One shared client, because OkHttp's connection pool and thread pool are meant
 * to be shared and creating a client per request is the classic way to leak
 * both.
 */
class OkHttpSubsonicEngine(
    private val client: OkHttpClient = defaultClient(),
) : SubsonicHttpEngine {

    override suspend fun get(request: SubsonicRequest): SubsonicHttpResponse =
        withContext(Dispatchers.IO) {
            val call = client.newCall(
                Request.Builder()
                    .url(request.url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
            )
            call.execute().use { response ->
                // The body is read even on a failure, because Bandcamp puts
                // meaning in it: an unknown route returns a non-envelope JSON
                // body with a 200, and a rejected login returns nothing at all
                // with a 500. Only the parser can tell those apart, so both
                // reach it intact.
                SubsonicHttpResponse(
                    status = response.code,
                    body = response.body?.string().orEmpty(),
                )
            }
        }

    companion object {
        /**
         * Identifies the client honestly. Being an unremarkable, identifiable
         * client of a service in open beta is both correct and in the app's
         * interest.
         */
        const val USER_AGENT = "Meedwell/1.0 (Android; +https://github.com/Kamsiob/meedwell)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            // Redirects are followed because both `stream` and `getCoverArt`
            // answer with a 302 to a bcbits.com asset rather than serving bytes
            // directly.
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
