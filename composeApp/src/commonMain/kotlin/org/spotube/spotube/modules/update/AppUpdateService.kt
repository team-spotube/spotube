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

package org.spotube.spotube.modules.update

import org.spotube.spotube.core.network.HttpClientFactory
import co.touchlab.kermit.Logger
import org.spotube.spotube.AppBuildInfo
import org.spotube.spotube.modules.settings.SettingsRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.swiftzer.semver.SemVer
import org.koin.core.component.KoinComponent

data class AvailableAppUpdate(
    val tag: String,
    val releaseNotesMarkdown: String)

/**
 * Checks GitHub for a newer app release once on startup and exposes it for the UI to
 * surface via [org.spotube.spotube.modules.update.AppUpdateDialog]. This is an
 * app-scoped singleton (not a ViewModel) since the check runs once for the whole
 * process, independent of any screen's lifecycle.
 */
class AppUpdateService(
    private val settingsRepository: SettingsRepository,
    httpClientFactory: HttpClientFactory) : KoinComponent {
    private val logger = Logger.withTag("AppUpdateService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val httpClient = httpClientFactory.json

    private val _availableUpdate = MutableStateFlow<AvailableAppUpdate?>(null)
    val availableUpdate: StateFlow<AvailableAppUpdate?> = _availableUpdate.asStateFlow()

    init {
        scope.launch {
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

            if (latestVersion > currentVersion && release.tagName != settings.ignoredAppUpdateVersion) {
                _availableUpdate.value = AvailableAppUpdate(
                    tag = release.tagName,
                    releaseNotesMarkdown = release.body.orEmpty())
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to check for app updates" }
        }
    }

    fun ignoreUpdate() {
        val update = _availableUpdate.value ?: return
        _availableUpdate.value = null
        scope.launch {
            runCatching {
                val settings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(settings.copy(ignoredAppUpdateVersion = update.tag))
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
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null)
