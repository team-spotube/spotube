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

import co.touchlab.kermit.Logger
import org.spotube.plugin_interfaces.plugin_apis.core.PluginUpdateInfo
import org.spotube.spotube.core.zipline.PluginService
import org.spotube.spotube.modules.plugin.PluginEntry
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.plugin.PluginManagerStates
import org.spotube.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.koin.core.component.KoinComponent

data class AvailablePluginUpdate(
    val pluginId: String,
    val pluginName: String,
    val currentVersion: String,
    val info: PluginUpdateInfo)

/**
 * Checks the currently active plugins for updates through their own [PluginService.use]
 * -> `coreAPI.checkPluginUpdates` API whenever the selected plugin set changes, and
 * exposes at most one pending update at a time for the UI to surface via
 * [org.spotube.spotube.modules.update.PluginUpdateDialog]. This is an app-scoped
 * singleton (not a ViewModel) since plugin runtimes are owned by [PluginManager]
 * independent of any screen's lifecycle.
 */
class PluginUpdateService(
    private val pluginManager: PluginManager,
    private val settingsRepository: SettingsRepository) : KoinComponent {
    private val logger = Logger.withTag("PluginUpdateService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _availableUpdate = MutableStateFlow<AvailablePluginUpdate?>(null)
    val availableUpdate: StateFlow<AvailablePluginUpdate?> = _availableUpdate.asStateFlow()

    init {
        scope.launch {
            pluginManager.ziplineServices.filterNotNull().collect { services ->
                checkForUpdates(services.values.filterNotNull())
            }
        }
    }

    private suspend fun checkForUpdates(services: List<PluginService>) {
        // Only surface one plugin update prompt at a time; re-checks happen the
        // next time the selected plugin set changes.
        if (_availableUpdate.value != null) return

        val settings = settingsRepository.getCurrentSettings()
        if (!settings.autoCheckForUpdates) return

        for (service in services.distinctBy { it.pluginId }) {
            val pluginEntry = findPluginEntry(service.pluginId) ?: continue
            val currentVersion = parseVersion(pluginEntry.version) ?: continue

            try {
                val updateInfo = service.use { coreAPI.checkPluginUpdates(currentVersion) } ?: continue
                val latestVersion = parseVersion(updateInfo.latestVersion)
                if (latestVersion == null) {
                    logger.w { "Ignoring plugin update with invalid SemVer: ${updateInfo.latestVersion}" }
                    continue
                }

                val ignoredVersion = settings.ignoredPluginUpdateVersions[pluginEntry.id]
                if (latestVersion > currentVersion && updateInfo.latestVersion != ignoredVersion) {
                    _availableUpdate.value = AvailablePluginUpdate(
                        pluginId = pluginEntry.id,
                        pluginName = pluginEntry.name,
                        currentVersion = pluginEntry.version,
                        info = updateInfo)
                    return
                }
            } catch (e: Exception) {
                logger.w(e) { "Failed to check updates for plugin ${pluginEntry.name}" }
            }
        }
    }

    private fun findPluginEntry(pluginId: String): PluginEntry? {
        val state = pluginManager.state.value as? PluginManagerStates.Data ?: return null
        return state.plugins.find { it.id == pluginId }
    }

    fun ignoreUpdate() {
        val update = _availableUpdate.value ?: return
        _availableUpdate.value = null
        scope.launch {
            runCatching {
                val settings = settingsRepository.getCurrentSettings()
                settingsRepository.updateSettings(
                    settings.copy(
                        ignoredPluginUpdateVersions = settings.ignoredPluginUpdateVersions +
                            (update.pluginId to update.info.latestVersion)
                    )
                )
            }.onFailure { e ->
                logger.e(e) { "Failed to persist ignored plugin update version" }
            }
        }
    }

    fun installUpdate() {
        val update = _availableUpdate.value ?: return
        _availableUpdate.value = null
        scope.launch {
            runCatching {
                pluginManager.updatePluginFromURL(update.info.directDownloadUrl)
            }.onFailure { e ->
                logger.e(e) { "Failed to install update for plugin ${update.pluginName}" }
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
