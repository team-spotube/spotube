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

package dev.krtirtho.spotube.modules.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.krtirtho.spotube.AppBuildInfo
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.swiftzer.semver.SemVer

data class AvailableUpdate(
    val tag: String,
    val releaseNotesMarkdown: String,
)

class UpdateCheckerViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val logger = Logger.withTag("UpdateChecker")
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _availableUpdate = MutableStateFlow<AvailableUpdate?>(null)
    val availableUpdate: StateFlow<AvailableUpdate?> = _availableUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            checkForUpdate()
        }
    }

    private suspend fun checkForUpdate() {
        try {
            // Read directly from DataStore so the check honors persisted preferences,
            // rather than the repository StateFlow's default value during startup.
            val settings = settingsRepository.getCurrentSettings()
            if (!settings.autoCheckForUpdates) return

            val release = httpClient.get(AppBuildInfo.LATEST_RELEASE_API_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "Spotube/${AppBuildInfo.VERSION}")
            }.body<GitHubRelease>()

            val latestVersion = parseVersion(release.tagName.removePrefix("v"))
                ?: return logger.w { "Ignoring release with invalid SemVer tag: ${release.tagName}" }
            val currentVersion = parseVersion(AppBuildInfo.VERSION)
                ?: return logger.w { "Ignoring update check for invalid app version: ${AppBuildInfo.VERSION}" }

            if (latestVersion > currentVersion && release.tagName != settings.ignoredUpdateVersion) {
                _availableUpdate.value = AvailableUpdate(
                    tag = release.tagName,
                    releaseNotesMarkdown = release.body.orEmpty(),
                )
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to check for app updates" }
        }
    }

    fun ignoreUpdate() {
        val update = _availableUpdate.value ?: return
        _availableUpdate.value = null
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(settings.copy(ignoredUpdateVersion = update.tag))
            }.onFailure { e ->
                logger.e(e) { "Failed to persist ignored app version" }
            }
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    private fun parseVersion(version: String): SemVer? = runCatching {
        SemVer.parse(version)
    }.getOrNull()

    override fun onCleared() {
        httpClient.close()
    }
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
)
