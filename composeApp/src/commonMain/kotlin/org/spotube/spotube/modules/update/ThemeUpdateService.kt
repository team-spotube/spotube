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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeInstallKind
import org.spotube.spotube.core.ui.theming.engine.ThemePackageRepository
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.settings.ThemeGitHubRepository
import org.spotube.spotube.modules.settings.ThemeUpdate
import org.spotube.spotube.modules.settings.latestThemeUpdate

/** A newer release of an installed theme. */
data class AvailableThemeUpdate(val themeId: String, val themeName: String, val currentVersion: String?, val update: ThemeUpdate)

/** Result of the last update check, for the Appearance page. */
data class ThemeUpdateStatus(val checking: Boolean = false, val upToDate: Boolean = false, val failed: Boolean = false)

/**
 * Finds newer releases of installed themes on GitHub (the `updates` source a theme declares).
 *
 * The **selected theme** is checked automatically, once per app start and again when the selection changes (like
 * plugin updates, only when "check for updates" is enabled in settings), and an app-level prompt offers the update.
 * [checkAll] is the manual check of every installed theme from the Appearance page.
 */
class ThemeUpdateService(
    private val settings: SettingsRepository,
    private val themes: ThemePackageRepository,
    httpClientFactory: HttpClientFactory,
) {
    private val logger = Logger.withTag("ThemeUpdateService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val github = ThemeGitHubRepository(httpClientFactory.json)

    private val _updates = MutableStateFlow<Map<String, ThemeUpdate>>(emptyMap())
    /** Every update found so far, by theme id. */
    val updates: StateFlow<Map<String, ThemeUpdate>> = _updates.asStateFlow()

    private val _prompt = MutableStateFlow<AvailableThemeUpdate?>(null)
    /** The selected theme's update, offered once in an app-level dialog. */
    val prompt: StateFlow<AvailableThemeUpdate?> = _prompt.asStateFlow()

    private val _status = MutableStateFlow(ThemeUpdateStatus())
    val status: StateFlow<ThemeUpdateStatus> = _status.asStateFlow()

    private val checkedThisRun = HashSet<String>()
    private var installing = false

    init {
        scope.launch {
            themes.ready.first { it }
            settings.userSettings.map { it?.selectedThemeId }.distinctUntilChanged().collect { id ->
                if (id != null && settings.getCurrentSettings().autoCheckForUpdates && checkedThisRun.add(id)) {
                    themes.installed.value.firstOrNull { it.id == id }?.let { theme -> check(theme, prompt = true) }
                }
            }
        }
    }

    /** Checks one theme; returns the newer release, if any. Network and parse failures count as "none". */
    private suspend fun check(theme: LoadedTheme, prompt: Boolean): Result<ThemeUpdate?> {
        val source = theme.meta.updates ?: return Result.success(null)
        return runCatching {
            val found = latestThemeUpdate(theme.meta.version, github.releases(source.owner, source.repo), source)
            if (found != null) {
                _updates.update { it + (theme.id to found) }
                val ignored = settings.getCurrentSettings().ignoredThemeUpdateVersions[theme.id]
                if (prompt && found.version != ignored && _prompt.value == null) {
                    _prompt.value = AvailableThemeUpdate(theme.id, theme.name, theme.meta.version, found)
                }
            } else {
                _updates.update { it - theme.id }
            }
            found
        }.onFailure { e ->
            if (e is CancellationException) throw e
            logger.w(e) { "Theme update check failed for ${theme.id}" }
        }
    }

    /** The manual check of every installed theme that declares an update source. */
    fun checkAll(manual: Boolean = true) {
        if (_status.value.checking) return
        val candidates = themes.installed.value.filter { it.meta.updates != null }
        if (candidates.isEmpty()) {
            if (manual) _status.value = ThemeUpdateStatus(upToDate = true)
            return
        }
        _status.value = ThemeUpdateStatus(checking = true)
        scope.launch {
            var failures = 0
            var found = 0
            for (theme in candidates) {
                check(theme, prompt = false).fold({ if (it != null) found++ }, { failures++ })
            }
            _status.value = ThemeUpdateStatus(upToDate = manual && found == 0 && failures == 0, failed = manual && failures > 0 && found == 0)
        }
    }

    fun dismissPrompt() {
        _prompt.value = null
    }

    /** Silences this version of the theme; the check moves on when a newer one is released. */
    fun ignorePrompt() {
        val prompt = _prompt.value ?: return
        _prompt.value = null
        scope.launch {
            runCatching {
                val current = settings.getCurrentSettings()
                settings.updateSettings(current.copy(ignoredThemeUpdateVersions = current.ignoredThemeUpdateVersions + (prompt.themeId to prompt.update.version)))
            }.onFailure { logger.e(it) { "Failed to persist the ignored theme update" } }
        }
    }

    /**
     * Downloads the offered update and installs it over the installed theme. The archive goes through the same strict
     * validation as any import, and only an archive with the *same id* that is newer is accepted.
     */
    fun installPrompt() {
        val prompt = _prompt.value ?: return
        if (installing) return
        installing = true
        scope.launch {
            try {
                val bytes = github.download(prompt.update.assetUrl)
                val prepared = themes.prepare(bytes)
                if (prepared.theme.id == prompt.themeId && prepared.kind == ThemeInstallKind.Update) {
                    themes.install(prepared)
                    _updates.update { it - prompt.themeId }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Failed to install the theme update" }
            } finally {
                installing = false
                _prompt.value = null
            }
        }
    }
}
