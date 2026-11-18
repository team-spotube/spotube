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
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.append
import io.ktor.http.contentLength
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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

/** [httpClient] must have JSON content negotiation installed (see `HttpClientFactory.json`). */
class ThemeGitHubRepository(private val httpClient: HttpClient) {

    /** Non-fork repositories tagged `spotube` and `spotube-theme`, most starred first. */
    suspend fun searchThemes(page: Int = 1, perPage: Int = 30): GitHubRepoSearchResponse =
        httpClient.get("https://api.github.com/search/repositories") {
            githubHeaders()
            parameter("q", THEME_DISCOVERY_QUERY)
            parameter("sort", "stars")
            parameter("order", "desc")
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()

    suspend fun releases(owner: String, repo: String, perPage: Int = 30): List<GitHubRelease> =
        httpClient.get("https://api.github.com/repos/$owner/$repo/releases") {
            githubHeaders()
            parameter("per_page", perPage)
        }.body()

    /** Downloads a release asset, refusing anything that is not a GitHub URL or is larger than a theme package may be. */
    suspend fun download(url: String): ByteArray = withContext(Dispatchers.IO) {
        require(isGitHubAssetUrl(url)) { "not a GitHub release asset" }
        val response = httpClient.get(url)
        val limit = ThemeLimits.MAX_ARCHIVE_BYTES.toLong()
        if ((response.contentLength() ?: 0L) > limit) error("theme package is too large")
        val bytes = response.bodyAsChannel().readRemaining(limit + 1).readByteArray()
        if (bytes.size > limit) error("theme package is too large")
        bytes
    }

    private fun io.ktor.client.request.HttpRequestBuilder.githubHeaders() = headers {
        append("Accept", "application/vnd.github+json")
        append("X-GitHub-Api-Version", "2022-11-28")
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
