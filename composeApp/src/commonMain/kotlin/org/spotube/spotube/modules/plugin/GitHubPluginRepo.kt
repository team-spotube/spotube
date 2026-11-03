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

package org.spotube.spotube.modules.plugin

import org.spotube.spotube.core.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.append
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepoSearchResponse(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("incomplete_results") val incompleteResults: Boolean,
    val items: List<GitHubRepo>)

@Serializable
data class GitHubRepo(
    val id: Long,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("html_url") val htmlUrl: String,
    val owner: GitHubOwner)

@Serializable
data class GitHubOwner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset>,
    @SerialName("html_url") val htmlUrl: String,
    val prerelease: Boolean = false,
    val draft: Boolean = false)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String)

/** [httpClient] must have JSON content negotiation installed (see [HttpClientFactory.json]). */
class GitHubPluginRepository(private val httpClient: HttpClient) {

    suspend fun searchSpotubePlugins(page: Int = 1, perPage: Int = 30): GitHubRepoSearchResponse {
        return httpClient.get("https://api.github.com/search/repositories") {
            headers {
                append("Accept", "application/vnd.github+json")
                append("X-GitHub-Api-Version", "2022-11-28")
            }
            parameter("q", "topic:spotube-zipline-plugin")
            parameter("sort", "stars")
            parameter("order", "desc")
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()
    }

    suspend fun getLatestReleaseSmplugUrl(owner: String, repo: String): String? {
        return try {
            val release: GitHubRelease =
                httpClient.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
                    headers {
                        append("Accept", "application/vnd.github+json")
                        append("X-GitHub-Api-Version", "2022-11-28")
                    }
                }.body()
            release.assets.firstOrNull { it.name.endsWith(".smplug") }?.browserDownloadUrl
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getReleases(owner: String, repo: String, perPage: Int = 30): List<GitHubRelease> {
        return try {
            httpClient.get("https://api.github.com/repos/$owner/$repo/releases") {
                headers {
                    append("Accept", "application/vnd.github+json")
                    append("X-GitHub-Api-Version", "2022-11-28")
                }
                parameter("per_page", perPage)
            }.body()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
