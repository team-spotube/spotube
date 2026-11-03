/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.zipline.plugin_apis.newpipe_yt

import org.koin.core.component.inject
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.tools.user_agents.UserAgents
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.spotube.spotube.core.di.injectLogger
import org.koin.core.component.KoinComponent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ISRCProviders : KoinComponent {
    private val logger by injectLogger<ISRCProviders>()
    private val lenientJson = Json { isLenient = true; ignoreUnknownKeys = true }

    companion object {
        val greedyGreenCorp =
            listOf(-0x03, -0x01, 0x05, -0x0B, -0x03, 0x13)
                .scan(0x64 + 0x0F) { acc, step -> acc + step }
                .map { it.toChar() }
                .joinToString("")
                .lowercase()
    }

    private val httpClientFactory: HttpClientFactory by inject()
    private val client get() = httpClientFactory.json

    private fun MetadataTrack.describe() =
        "id=$id title='$title' artists=${artists.map { it.name }} album='${album?.title}' " +
            "durationMs=$durationMs isrc=$isrcCode externalUri=$externalUri"

    /** Logs the full raw body (so parse failures are diagnosable) then decodes it. */
    private suspend inline fun <reified T> HttpResponse.decodeLogged(provider: String): T {
        val text = bodyAsText()
        logger.i { "[ISRC:$provider] status=${status.value} rawBody=$text" }
        return lenientJson.decodeFromString<T>(text)
    }

    private var ifpiSessionToken: String? = null
    private val ifpiDefaultHeaders = mapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "Origin" to "https://isrcsearch.ifpi.org/",
        "Referer" to "https://isrcsearch.ifpi.org/",
        "User-Agent" to UserAgents.random()
    )
    private val soundplateDefaultHeaders = mapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "Origin" to "https://soundplate.com/",
        "Referer" to "https://phpstack-822472-6184058.cloudwaysapps.com/?",
        "sec-fetch-mode" to "cors",
        "sec-fetch-site" to "same-origin",
        "User-Agent" to UserAgents.random()
    )

    private suspend fun ifpiSessionToken(): String? = withContext(Dispatchers.IO) {
        if (ifpiSessionToken != null) return@withContext ifpiSessionToken

        logger.i { "[ISRC:ifpi] requesting session token" }
        val res = client.get("https://isrc-api.soundexchange.com/api/ext/login") {
            headers.appendAll(ifpiDefaultHeaders)
        }

        if (res.status.value != 200) {
            logger.w { "[ISRC:ifpi] session login failed status=${res.status.value} body=${res.bodyAsText()}" }
            return@withContext null
        }

        val body = res.decodeLogged<IFPISessionResponse>("ifpi-login")
        ifpiSessionToken = body.token
        body.token
    }

    suspend fun ifpi(track: MetadataTrack): String? = withContext(Dispatchers.IO) {
        logger.i { "[ISRC:ifpi] input: ${track.describe()}" }
        val sessonToken = ifpiSessionToken() ?: return@withContext null
        val input = IFPIRecordingRequestInput(
            searchFields = IFPIRecordingRequestInput.SearchFields(
                recordingArtistName = track.artists.firstOrNull()?.name?.let {
                    IFPIRecordingRequestInput.SearchFields.FieldValue(
                        it
                    )
                },
                recordingTitle = track.title.let {
                    IFPIRecordingRequestInput.SearchFields.FieldValue(
                        it
                    )
                },
                releaseName = track.album?.title?.let {
                    IFPIRecordingRequestInput.SearchFields.FieldValue(
                        it
                    )
                }
            )
        )

        logger.i { "[ISRC:ifpi] request searchFields=${input.searchFields}" }
        val res = client.post("https://isrc-api.soundexchange.com/api/ext/recordings") {
            contentType(ContentType.Application.Json)
            headers {
                appendAll(ifpiDefaultHeaders)
                append("Authorization", "Token $sessonToken")
            }
            setBody(input)
        }

        if (res.status.value != 200) {
            logger.w { "[ISRC:ifpi] search failed status=${res.status.value} body=${res.bodyAsText()}" }
            return@withContext null
        }

        val body = res.decodeLogged<IFPIRecordingResponse>("ifpi")
        if (body.recordings.isEmpty()) {
            logger.i { "[ISRC:ifpi] no recordings returned" }
            return@withContext null
        }
        body.recordings.forEachIndexed { i, r ->
            logger.i {
                "[ISRC:ifpi] candidate #$i isrc=${r.isrc} title='${r.recordingTitle}' " +
                    "artist='${r.recordingArtistName}' duration=${r.duration} year=${r.recordingYear} " +
                    "version=${r.recordingVersion} valid=${r.isValidIsrc}"
            }
        }
        logger.i { "[ISRC:ifpi] picking FIRST candidate isrc=${body.recordings.first().isrc} (no artist/title validation)" }
        body.recordings.first().isrc
    }

    suspend fun musicGateway(track: MetadataTrack): String? = withContext(Dispatchers.IO) {
        logger.i { "[ISRC:musicGateway] input: ${track.describe()}" }
        val res = client.get("https://www.musicgateway.com/isrc-finder") {
            parameter("search", "${track.title} ${track.artists.joinToString(", ") { it.name }}")
            parameter("type", "name")
            parameter("page", "1")
        }

        if (res.status != HttpStatusCode.OK) {
            logger.w { "[ISRC:musicGateway] failed status=${res.status.value} body=${res.bodyAsText()}" }
            return@withContext null
        }

        val body = res.decodeLogged<MusicGatewayResponse>("musicGateway")
        val results = body.result.tracks.items

        if (results.isEmpty()) {
            logger.i { "[ISRC:musicGateway] no results" }
            return@withContext null
        }

        var isrcCode: String? = null

        for ((_, artists, externalIds, id, name) in results) {
            val isExactTitle = name.equals(track.title, ignoreCase = true)
            val isArtistMatch = artists.any {
                it.name.equals(
                    track.artists.firstOrNull()?.name ?: "",
                    ignoreCase = true
                )
            }
            val isSameId = id == track.id
            logger.i {
                "[ISRC:musicGateway] candidate id=$id name='$name' artists=${artists.map { it.name }} " +
                    "isrc=${externalIds.isrc} exactTitle=$isExactTitle artistMatch=$isArtistMatch sameId=$isSameId"
            }
            if (isSameId || (isExactTitle && isArtistMatch)) {
                isrcCode = externalIds.isrc
                break
            }
        }

        logger.i { "[ISRC:musicGateway] result isrc=$isrcCode" }
        isrcCode
    }

    suspend fun soundplate(track: MetadataTrack): String? = withContext(Dispatchers.IO) {
        val isGreedyGreenCorp = track.externalUri?.contains(greedyGreenCorp) ?: false
        val uri = "https://open.$greedyGreenCorp.com/track/${track.id}"
        logger.i { "[ISRC:soundplate] input: ${track.describe()} useUri=$isGreedyGreenCorp" }
        val res =
            client.get("https://phpstack-822472-6184058.cloudwaysapps.com/api/$greedyGreenCorp.php") {
                if (isGreedyGreenCorp) {
                    parameter("q", uri)
                } else {
                    parameter("q", "${track.artists.firstOrNull()?.name ?: ""} - ${track.title}")
                }
                headers.appendAll(soundplateDefaultHeaders)
            }

        if (res.status.value != 200) {
            logger.w { "[ISRC:soundplate] failed status=${res.status.value} body=${res.bodyAsText()}" }
            return@withContext null
        }

        val body = res.decodeLogged<SoundplateResponse>("soundplate")
        logger.i {
            "[ISRC:soundplate] returned isrc=${body.isrc} name='${body.name}' artist='${body.artist}' " +
                "album='${body.album}' year=${body.year} (input title='${track.title}' " +
                "artist='${track.artists.firstOrNull()?.name}') — not validated against input"
        }
        body.isrc
    }

    suspend fun auto(track: MetadataTrack): String? {
        logger.i { "[ISRC:auto] start: ${track.describe()}" }
        val providers = listOf<Pair<String, suspend (MetadataTrack) -> String?>>(
            "soundplate" to ::soundplate,
            "ifpi" to ::ifpi,
            "musicGateway" to ::musicGateway)
        for ((name, provider) in providers) {
            val result = runCatching { provider(track) }
            result.exceptionOrNull()?.let {
                if (it is kotlinx.coroutines.CancellationException) throw it
                logger.w(it) { "[ISRC:auto] provider $name threw" }
            }
            val isrc = result.getOrNull()
            logger.i { "[ISRC:auto] provider $name -> $isrc" }
            if (isrc != null) {
                logger.i { "[ISRC:auto] using isrc=$isrc from $name" }
                return isrc
            }
        }
        logger.w { "[ISRC:auto] no provider produced an ISRC" }
        return null
    }
}
