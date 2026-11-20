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

package org.spotube.spotube.modules.settings

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import net.swiftzer.semver.SemVer
import org.spotube.spotube.core.ui.theming.engine.ThemeLimits
import org.spotube.spotube.core.ui.theming.engine.ThemeUpdateSource
import org.spotube.spotube.modules.plugin.GitHubRelease
import org.spotube.spotube.modules.plugin.GitHubRepoSearchResponse

/** The GitHub topics theme authors tag their repositories with so people can find them. */
const val THEME_DISCOVERY_TOPIC = "spotube-theme"
const val THEME_DISCOVERY_QUERY = "topic:spotube topic:$THEME_DISCOVERY_TOPIC fork:false"

/** Release assets are only downloaded from github.com (which redirects to GitHub's own asset hosts). */
internal fun isGitHubAssetUrl(url: String): Boolean =
    url.startsWith("https://github.com/") && url.none { it.isWhitespace() || it.code < 0x20 }

/** GitHub says to wait: the unauthenticated limits are 10 searches and 60 other requests per hour/minute window. */
class GitHubRateLimitedException(val retryAfterSeconds: Long) : Exception("GitHub rate limit, retry in ${retryAfterSeconds}s")

/**
 * Talks to GitHub for theme discovery, release lists and downloads. One instance is shared app-wide (Koin single) so
 * the Appearance page, the update service and every visit to the page share one cache:
 *
 * - Responses are cached in memory ([SEARCH_TTL] for searches, [RELEASES_TTL] for release lists), so reopening the page
 *   or re-checking a theme costs no request at all.
 * - Once a response is stale it is revalidated with `If-None-Match`; a `304 Not Modified` does not count against
 *   GitHub's rate limit.
 * - A rate-limit answer (403/429 with an exhausted limit or `Retry-After`) pauses every API request until the limit
 *   resets. Meanwhile cached data (even stale) is served; with nothing cached a [GitHubRateLimitedException] is thrown.
 * - Concurrent identical requests are serialised, so the second one hits the cache.
 *
 * [httpClient] must have JSON content negotiation installed (see `HttpClientFactory.json`).
 */
@OptIn(ExperimentalTime::class)
class ThemeGitHubRepository(private val httpClient: HttpClient) {
    private class Entry(val body: String, val etag: String?, var checkedAt: TimeMark)

    private val cache = HashMap<String, Entry>()
    private val lock = Mutex()
    private var blockedUntil: TimeMark? = null
    private val json = Json { ignoreUnknownKeys = true }

    /** Non-fork repositories tagged `spotube` and `spotube-theme`, most starred first. [force] revalidates a cached page. */
    suspend fun searchThemes(page: Int = 1, perPage: Int = 30, force: Boolean = false): GitHubRepoSearchResponse =
        json.decodeFromString(
            cached(
                "https://api.github.com/search/repositories", SEARCH_TTL, force,
                "q" to THEME_DISCOVERY_QUERY, "sort" to "stars", "order" to "desc", "page" to page.toString(), "per_page" to perPage.toString(),
            ),
        )

    suspend fun releases(owner: String, repo: String, perPage: Int = 30, force: Boolean = false): List<GitHubRelease> =
        json.decodeFromString(cached("https://api.github.com/repos/$owner/$repo/releases", RELEASES_TTL, force, "per_page" to perPage.toString()))

    /** Cached GET: see the class comment for the rules. */
    private suspend fun cached(url: String, ttl: Duration, force: Boolean, vararg params: Pair<String, String>): String = lock.withLock {
        val key = url + params.joinToString("&", "?") { "${it.first}=${it.second}" }
        val entry = cache[key]
        if (entry != null && !force && entry.checkedAt.elapsedNow() < ttl) return entry.body
        val blocked = blockedUntil
        if (blocked != null && blocked.hasPassedNow().not()) {
            return entry?.body ?: throw GitHubRateLimitedException((-blocked.elapsedNow()).inWholeSeconds.coerceAtLeast(1))
        }
        val response = try {
            httpClient.get(url) {
                githubHeaders()
                params.forEach { (k, v) -> parameter(k, v) }
                entry?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return entry?.body ?: throw e
        }
        when {
            response.status == HttpStatusCode.NotModified && entry != null -> {
                entry.checkedAt = TimeSource.Monotonic.markNow()
                entry.body
            }
            response.status.isSuccess() -> {
                val body = response.bodyAsText()
                cache[key] = Entry(body, response.headers[HttpHeaders.ETag], TimeSource.Monotonic.markNow())
                body
            }
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.TooManyRequests -> {
                val wait = rateLimitWaitSeconds(response)
                if (wait != null) {
                    blockedUntil = TimeSource.Monotonic.markNow() + wait.seconds
                    entry?.body ?: throw GitHubRateLimitedException(wait)
                } else entry?.body ?: error("GitHub answered ${response.status}")
            }
            else -> entry?.body ?: error("GitHub answered ${response.status}")
        }
    }

    /** Seconds to wait when [response] is a rate-limit refusal, null when it is some other 403. */
    private fun rateLimitWaitSeconds(response: HttpResponse): Long? {
        response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.let { return it.coerceIn(1, MAX_BLOCK_SECONDS) }
        if (response.headers["X-RateLimit-Remaining"] != "0") {
            return if (response.status == HttpStatusCode.TooManyRequests) DEFAULT_BLOCK_SECONDS else null
        }
        val reset = response.headers["X-RateLimit-Reset"]?.toLongOrNull() ?: return DEFAULT_BLOCK_SECONDS
        return (reset - Clock.System.now().epochSeconds).coerceIn(1, MAX_BLOCK_SECONDS)
    }

    /** Downloads a release asset, refusing anything that is not a GitHub URL or is larger than a theme package may be. */
    suspend fun download(url: String): ByteArray {
        require(isGitHubAssetUrl(url)) { "not a GitHub release asset" }
        val response = httpClient.get(url)
        val limit = ThemeLimits.MAX_ARCHIVE_BYTES.toLong()
        if ((response.contentLength() ?: 0L) > limit) error("theme package is too large")
        val bytes = response.bodyAsChannel().readRemaining(limit + 1).readByteArray()
        if (bytes.size > limit) error("theme package is too large")
        return bytes
    }

    private fun io.ktor.client.request.HttpRequestBuilder.githubHeaders() = headers {
        append("Accept", "application/vnd.github+json")
        append("X-GitHub-Api-Version", "2022-11-28")
    }

    private companion object {
        val SEARCH_TTL = 30.minutes
        val RELEASES_TTL = 10.minutes
        const val DEFAULT_BLOCK_SECONDS = 60L
        const val MAX_BLOCK_SECONDS = 3600L
    }
}

/** `v1.2.3` and `1.2.3` are both versions; anything else is not. */
internal fun parseReleaseVersion(tag: String): SemVer? = try {
    SemVer.parse(tag.trim().removePrefix("v").removePrefix("V"))
} catch (_: Exception) {
    null
}

/** A newer release of an installed theme that carries the declared archive. */
data class ThemeUpdate(val version: String, val tag: String, val assetUrl: String, val notes: String = "")

/**
 * The newest release that is a published (not draft, not pre-release) semantic version with the [source]'s archive,
 * if it is newer than [installedVersion]. Releases whose tag isn't semver, or without the archive, are ignored.
 */
fun latestThemeUpdate(installedVersion: String?, releases: List<GitHubRelease>, source: ThemeUpdateSource): ThemeUpdate? {
    val installed = installedVersion?.let(::parseReleaseVersion)
    val best = releases.asSequence()
        .filter { !it.draft && !it.prerelease }
        .mapNotNull { release ->
            val version = parseReleaseVersion(release.tagName) ?: return@mapNotNull null
            val asset = release.assets.firstOrNull { it.name == source.asset && isGitHubAssetUrl(it.browserDownloadUrl) }
                ?: return@mapNotNull null
            Triple(version, release, asset)
        }
        .maxByOrNull { it.first } ?: return null
    if (installed != null && best.first <= installed) return null
    return ThemeUpdate(best.first.toString(), best.second.tagName, best.third.browserDownloadUrl, best.second.body.orEmpty())
}
