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

package dev.krtirtho.spotube.modules.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.webview.WebViewController
import dev.krtirtho.spotube.core.zipline.PluginService
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.core.component.KoinComponent

data class PluginAuthState(
    val requiresAuth: Boolean = false,
    val isLoggedIn: Boolean = false,
)

data class PluginListItem(
    val plugin: PluginEntry,
    val isSelected: Boolean,
    val authState: PluginAuthState,
    val logoPath: Path?,
)

data class AbilitySelection(
    val ability: PluginAbility,
    val plugins: List<PluginEntry>,
    val selectedPlugin: PluginEntry?,
    val pluginLogoPaths: Map<String, Path?> = emptyMap(),
)

data class PluginDiscoverState(
    val repos: List<GitHubRepo> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val installingRepoId: Long? = null,
    val isInitialLoaded: Boolean = false,
)

sealed interface UrlError {
    data object Empty : UrlError
    data object InvalidScheme : UrlError
    data object DownloadFailed : UrlError
    data class Message(val message: String) : UrlError
}

sealed interface PluginUiState {
    data object Loading : PluginUiState

    data class Data(
        val onboardingDiscover: Boolean = true,
        val plugins: List<PluginListItem>,
        val abilitySelections: List<AbilitySelection>,
        val pendingPlugin: PluginManager.PendingPlugin? = null,
        val pendingPluginLogoPath: Path? = null,
        val urlInput: String = "",
        val urlError: UrlError? = null,
        val isLoadingUrl: Boolean = false,
        val showInstallSheet: Boolean = false,
        val showPluginInfo: PluginEntry? = null,
        val pluginInfoLogoPath: Path? = null,
        val showPluginSupport: PluginEntry? = null,
        val supportText: String? = null,
        val isLoadingSupport: Boolean = false,
        val installDialogRepo: GitHubRepo? = null,
        val releases: List<GitHubRelease> = emptyList(),
        val isLoadingReleases: Boolean = false,
        val discover: PluginDiscoverState = PluginDiscoverState(),
    ) : PluginUiState
}

private data class PluginManagerSnapshot(
    val state: PluginManagerStates,
    val pendingPlugin: PluginManager.PendingPlugin?,
    val metadataPlugins: List<PluginEntry>,
    val audioPlugins: List<PluginEntry>,
    val lyricsPlugins: List<PluginEntry>,
    val scrobblePlugins: List<PluginEntry>,
    val activeServices: Map<PluginAbility, PluginService?>?,
)

private data class PluginScreenState(
    val onboardingDiscover: Boolean = true,
    val urlInput: String = "",
    val urlError: UrlError? = null,
    val isLoadingUrl: Boolean = false,
    val showInstallSheet: Boolean = false,
    val showPluginInfo: PluginEntry? = null,
    val pluginInfoLogoPath: Path? = null,
    val showPluginSupport: PluginEntry? = null,
    val supportText: String? = null,
    val isLoadingSupport: Boolean = false,
    val installDialogRepo: GitHubRepo? = null,
    val releases: List<GitHubRelease> = emptyList(),
    val isLoadingReleases: Boolean = false,
    val discover: PluginDiscoverState = PluginDiscoverState(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PluginViewModel(
    private val pluginManager: PluginManager,
    private val webViewController: WebViewController,
) : ViewModel(), KoinComponent {

    private val logger by injectLogger<PluginViewModel>()
    private val gitHubRepo = GitHubPluginRepository()

    private val _screenState = MutableStateFlow(PluginScreenState())
    private val _allRepos = mutableListOf<GitHubRepo>()
    private val _paginationInfo = PaginationInfo()

    init {
        viewModelScope.launch {
            pluginManager.state.collect { pluginState ->
                if (pluginState is PluginManagerStates.Data) {
                    val installedUrls = pluginState.plugins
                        .mapNotNull { it.repository.takeIf { r -> r.isNotBlank() } }
                        .toSet()
                    _screenState.update {
                        it.copy(
                            discover = it.discover.copy(
                                repos = _allRepos.filter { repo -> repo.htmlUrl !in installedUrls }
                            )
                        )
                    }
                }
            }
        }
        loadFirstDiscoverPage()
    }

    private class PaginationInfo(
        var currentPage: Int = 1,
        var hasMore: Boolean = true,
        var totalCount: Int = 0,
    )

    @Suppress("UNCHECKED_CAST")
    private val pluginManagerSnapshot: StateFlow<PluginManagerSnapshot> = combine(
        pluginManager.state,
        pluginManager.pendingPlugin,
        pluginManager.metadataPlugins,
        pluginManager.audioPlugins,
        pluginManager.lyricsPlugins,
        pluginManager.scrobblePlugins,
        pluginManager.ziplineServices,
    ) { values ->
        PluginManagerSnapshot(
            state = values[0] as PluginManagerStates,
            pendingPlugin = values[1] as PluginManager.PendingPlugin?,
            metadataPlugins = values[2] as List<PluginEntry>,
            audioPlugins = values[3] as List<PluginEntry>,
            lyricsPlugins = values[4] as List<PluginEntry>,
            scrobblePlugins = values[5] as List<PluginEntry>,
            activeServices = values[6] as Map<PluginAbility, PluginService?>?,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PluginManagerSnapshot(
            state = PluginManagerStates.Loading,
            pendingPlugin = null,
            metadataPlugins = emptyList(),
            audioPlugins = emptyList(),
            lyricsPlugins = emptyList(),
            scrobblePlugins = emptyList(),
            activeServices = null,
        )
    )

    private val pluginLogoPaths: StateFlow<Map<String, Path?>> = pluginManager.state
        .filterIsInstance<PluginManagerStates.Data>()
        .map { state ->
            state.plugins.associate { plugin ->
                plugin.id to getLogoPath(plugin.id)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val pluginAuthStates: StateFlow<Map<String, PluginAuthState>> = pluginManager.state
        .filterIsInstance<PluginManagerStates.Data>()
        .map { it.plugins }
        .distinctUntilChanged()
        .flatMapLatest { plugins ->
            if (plugins.isEmpty()) return@flatMapLatest flowOf(emptyMap())
            val entries = plugins.associate { plugin ->
                plugin.id to pluginAuthFlow(plugin)
            }
            combine(entries.values.toList()) { states ->
                entries.keys.zip(states).toMap()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<PluginUiState> = combine(
        pluginManagerSnapshot,
        _screenState,
        pluginAuthStates,
        pluginLogoPaths,
    ) { snapshot, screen, authStates, logoPaths ->
        when (val state = snapshot.state) {
            is PluginManagerStates.Loading -> PluginUiState.Loading
            is PluginManagerStates.Data -> PluginUiState.Data(
                onboardingDiscover = screen.onboardingDiscover,
                plugins = state.plugins.map { plugin ->
                    PluginListItem(
                        plugin = plugin,
                        isSelected = state.selectedPlugins.containsValue(plugin),
                        authState = authStates[plugin.id] ?: PluginAuthState(),
                        logoPath = logoPaths[plugin.id],
                    )
                },
                abilitySelections = listOf(
                    AbilitySelection(
                        PluginAbility.METADATA,
                        snapshot.metadataPlugins,
                        state.selectedPlugins[PluginAbility.METADATA],
                        logoPaths,
                    ),
                    AbilitySelection(
                        PluginAbility.AUDIO,
                        snapshot.audioPlugins,
                        state.selectedPlugins[PluginAbility.AUDIO],
                        logoPaths,
                    ),
                    AbilitySelection(
                        PluginAbility.LYRICS,
                        snapshot.lyricsPlugins,
                        state.selectedPlugins[PluginAbility.LYRICS],
                        logoPaths,
                    ),
                    AbilitySelection(
                        PluginAbility.SCROBBLE,
                        snapshot.scrobblePlugins,
                        state.selectedPlugins[PluginAbility.SCROBBLE],
                        logoPaths,
                    ),
                ),
                pendingPlugin = snapshot.pendingPlugin,
                pendingPluginLogoPath = snapshot.pendingPlugin?.entry?.id?.let { getLogoPath(it) },
                urlInput = screen.urlInput,
                urlError = screen.urlError,
                isLoadingUrl = screen.isLoadingUrl,
                showInstallSheet = screen.showInstallSheet,
                showPluginInfo = screen.showPluginInfo,
                pluginInfoLogoPath = screen.pluginInfoLogoPath,
                showPluginSupport = screen.showPluginSupport,
                supportText = screen.supportText,
                isLoadingSupport = screen.isLoadingSupport,
                installDialogRepo = screen.installDialogRepo,
                releases = screen.releases,
                isLoadingReleases = screen.isLoadingReleases,
                discover = screen.discover,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PluginUiState.Loading)

    private fun pluginAuthFlow(plugin: PluginEntry): Flow<PluginAuthState> {
        return combine(pluginManager.state, pluginManager.ziplineServices) { state, services ->
            val data = state as? PluginManagerStates.Data
            val ability = data?.selectedPlugins?.entries
                ?.firstOrNull { (_, selected) -> selected.id == plugin.id }
                ?.key
            ability?.let { services?.get(it) }
        }.distinctUntilChanged()
            .flatMapLatest { service ->
                if (service == null) return@flatMapLatest flowOf(PluginAuthState())
                flow {
                    val requiresAuth = try {
                        service.use { coreAPI.requiresAuthentication }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        // Service may have been stopped/closed concurrently when the
                        // plugin selection changed. Fall back to a default state and wait
                        // for the next service emission.
                        false
                    }
                    emit(PluginAuthState(requiresAuth = requiresAuth, isLoggedIn = false))
                    if (requiresAuth) {
                        service.loggedInFlow.collect { loggedIn ->
                            emit(PluginAuthState(requiresAuth = true, isLoggedIn = loggedIn))
                        }
                    }
                }
            }
    }

    private fun getLogoPath(pluginId: String): Path? {
        val path = pluginManager.pluginsDirPath / pluginId.toPath() / "logo.png".toPath()
        return if (FileSystem.SYSTEM.exists(path)) path else null
    }

    private fun findAbilityForPlugin(pluginId: String): PluginAbility? {
        val state = pluginManagerSnapshot.value.state as? PluginManagerStates.Data
        return state?.selectedPlugins?.entries
            ?.firstOrNull { (_, plugin) -> plugin.id == pluginId }
            ?.key
    }

    private fun findServiceForPlugin(pluginId: String): PluginService? {
        val ability = findAbilityForPlugin(pluginId) ?: return null
        return pluginManagerSnapshot.value.activeServices?.get(ability)
    }

    fun onUrlInputChange(input: String) {
        _screenState.update { it.copy(urlInput = input, urlError = null) }
    }

    fun submitUrl() {
        val url = _screenState.value.urlInput.trim()
        when {
            url.isBlank() -> {
                _screenState.update { it.copy(urlError = UrlError.Empty) }
                return
            }

            !url.startsWith("http://") && !url.startsWith("https://") -> {
                _screenState.update { it.copy(urlError = UrlError.InvalidScheme) }
                return
            }
        }

        _screenState.update { it.copy(urlError = null, isLoadingUrl = true) }
        viewModelScope.launch {
            runCatching {
                pluginManager.addPluginFromURL(url)
            }.onSuccess {
                _screenState.update { it.copy(urlInput = "", isLoadingUrl = false) }
            }.onFailure { e ->
                logger.e(e) { "Failed to add plugin from URL" }
                _screenState.update {
                    it.copy(
                        urlError = e.message?.let { message -> UrlError.Message(message) }
                            ?: UrlError.DownloadFailed,
                        isLoadingUrl = false,
                    )
                }
            }
        }
    }

    fun onFileSelected(file: PlatformFile?) {
        if (file == null) return
        viewModelScope.launch {
            runCatching {
                pluginManager.preparePlugin(file.readBytes())
            }.onFailure { e ->
                logger.e(e) { "Failed to prepare plugin from file" }
            }
        }
    }

    fun showInstallSheet() {
        _screenState.update { it.copy(showInstallSheet = true) }
    }

    fun dismissInstallSheet() {
        _screenState.update { it.copy(showInstallSheet = false) }
    }

    fun showPluginInfo(plugin: PluginEntry) {
        _screenState.update {
            it.copy(
                showPluginInfo = plugin,
                pluginInfoLogoPath = getLogoPath(plugin.id),
            )
        }
    }

    fun dismissPluginInfo() {
        _screenState.update { it.copy(showPluginInfo = null, pluginInfoLogoPath = null) }
    }

    fun loadSupport(plugin: PluginEntry) {
        _screenState.update {
            it.copy(
                showPluginSupport = plugin,
                isLoadingSupport = true,
                supportText = null,
            )
        }
        viewModelScope.launch {
            val service = findServiceForPlugin(plugin.id)
            if (service == null) {
                _screenState.update { it.copy(isLoadingSupport = false) }
                return@launch
            }
            runCatching {
                service.use { coreAPI.supportMarkdownText(SemVer.parse(plugin.version)) }
            }.onSuccess { text ->
                _screenState.update { it.copy(supportText = text, isLoadingSupport = false) }
            }.onFailure { e ->
                logger.e(e) { "Failed to load support text" }
                _screenState.update { it.copy(isLoadingSupport = false) }
            }
        }
    }

    fun dismissSupport() {
        _screenState.update {
            it.copy(
                showPluginSupport = null,
                supportText = null,
                isLoadingSupport = false,
            )
        }
    }

    fun showInstallDialog(repo: GitHubRepo) {
        _screenState.update {
            it.copy(
                installDialogRepo = repo,
                isLoadingReleases = true,
                releases = emptyList(),
            )
        }
        viewModelScope.launch {
            runCatching {
                val parts = repo.fullName.split("/")
                gitHubRepo.getReleases(parts[0], parts[1])
            }.onSuccess { releases ->
                _screenState.update { it.copy(releases = releases, isLoadingReleases = false) }
            }.onFailure { e ->
                logger.e(e) { "Failed to load releases" }
                _screenState.update { it.copy(isLoadingReleases = false) }
            }
        }
    }

    fun dismissInstallDialog() {
        _screenState.update { it.copy(installDialogRepo = null, releases = emptyList()) }
    }

    fun selectPlugin(ability: PluginAbility, plugin: PluginEntry?) {
        pluginManager.setSelectedPlugin(ability, plugin)
    }

    fun showOnboardingDiscovery(show: Boolean) {
        _screenState.update { it.copy(onboardingDiscover = show) }
    }

    fun removePlugin(plugin: PluginEntry) {
        viewModelScope.launch {
            runCatching {
                pluginManager.removePlugin(plugin)
            }.onFailure { e ->
                logger.e(e) { "Failed to remove plugin" }
            }
        }
    }

    fun login(plugin: PluginEntry) {
        val service = findServiceForPlugin(plugin.id) ?: return
        pluginManager.launchTask {
            service.use { coreAPI.login() }
        }
    }

    fun logout(plugin: PluginEntry) {
        val service = findServiceForPlugin(plugin.id) ?: return
        pluginManager.launchTask {
            service.use { coreAPI.logout() }
        }
        viewModelScope.launch {
            webViewController.clearData(plugin.id)
        }
    }

    fun confirmInstall() {
        pluginManager.confirmInstall()
    }

    fun dismissInstall() {
        pluginManager.dismissInstall()
    }

    private fun filterInstalledRepos(repos: List<GitHubRepo>): List<GitHubRepo> {
        val pluginState = pluginManager.state.value
        if (pluginState !is PluginManagerStates.Data) return repos
        val installedUrls = pluginState.plugins
            .mapNotNull { it.repository.takeIf { r -> r.isNotBlank() } }
            .toSet()
        return repos.filter { it.htmlUrl !in installedUrls }
    }

    fun retryDiscovery() {
        if (_screenState.value.discover.isLoading) return
        loadFirstDiscoverPage()
    }

    private fun loadFirstDiscoverPage() {
        viewModelScope.launch {
            _screenState.update {
                it.copy(discover = it.discover.copy(isLoading = true, error = null))
            }
            runCatching {
                gitHubRepo.searchSpotubePlugins(page = 1)
            }.onSuccess { response ->
                _allRepos.clear()
                _allRepos.addAll(response.items)
                _paginationInfo.currentPage = 1
                _paginationInfo.totalCount = response.totalCount
                _paginationInfo.hasMore = _allRepos.size < response.totalCount
                _screenState.update {
                    it.copy(
                        discover = it.discover.copy(
                            repos = filterInstalledRepos(response.items),
                            currentPage = 1,
                            hasMore = _paginationInfo.hasMore,
                            isLoading = false,
                            isInitialLoaded = true,
                        )
                    )
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load plugins" }
                _screenState.update {
                    it.copy(
                        discover = it.discover.copy(
                            isLoading = false,
                            error = e.message,
                            isInitialLoaded = true,
                        )
                    )
                }
            }
        }
    }

    fun loadNextDiscoverPage() {
        val current = _screenState.value.discover
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            val nextPage = _paginationInfo.currentPage + 1
            _screenState.update {
                it.copy(discover = it.discover.copy(isLoadingMore = true, error = null))
            }
            runCatching {
                gitHubRepo.searchSpotubePlugins(page = nextPage)
            }.onSuccess { response ->
                _allRepos.addAll(response.items)
                _paginationInfo.currentPage = nextPage
                _paginationInfo.hasMore = _allRepos.size < response.totalCount
                _screenState.update {
                    it.copy(
                        discover = it.discover.copy(
                            repos = filterInstalledRepos(_allRepos),
                            currentPage = nextPage,
                            hasMore = _paginationInfo.hasMore,
                            isLoadingMore = false,
                        )
                    )
                }
            }.onFailure { e ->
                logger.e(e) { "Failed to load more plugins" }
                _screenState.update {
                    it.copy(
                        discover = it.discover.copy(
                            isLoadingMore = false,
                            error = e.message,
                        )
                    )
                }
            }
        }
    }

    fun installPlugin(release: GitHubRelease, repoId: Long) {
        val smplugUrl =
            release.assets.firstOrNull { it.name.endsWith(".smplug") }?.browserDownloadUrl
        if (smplugUrl != null) {
            installPluginFromUrl(smplugUrl, repoId)
        }
    }

    private fun installPluginFromUrl(url: String, repoId: Long) {
        if (_screenState.value.discover.installingRepoId != null) return
        _screenState.update {
            it.copy(discover = it.discover.copy(installingRepoId = repoId, error = null))
        }
        viewModelScope.launch {
            runCatching {
                pluginManager.addPluginFromURL(url)
            }.onFailure { e ->
                logger.e(e) { "Failed to install plugin" }
                _screenState.update {
                    it.copy(discover = it.discover.copy(error = e.message))
                }
            }
            _screenState.update {
                it.copy(discover = it.discover.copy(installingRepoId = null))
            }
        }
    }

    override fun onCleared() {
        gitHubRepo.close()
        super.onCleared()
    }
}
