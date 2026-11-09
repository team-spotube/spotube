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
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.db.DatabaseKeys
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.core.zipline.BuiltInPluginService
import org.spotube.spotube.core.zipline.PluginService
import org.spotube.spotube.core.zipline.PluginServiceScope
import org.spotube.spotube.core.zipline.ZiplinePluginService
import org.spotube.spotube.core.zipline.permissions.PluginPermissionValidator
import org.spotube.spotube.core.zipline.permissions.PublicSuffixList
import org.spotube.spotube.core.zipline.permissions.PublicSuffixListRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import net.swiftzer.semver.SemVer
import no.synth.kmpzip.okio.ZipInputStream
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import org.spotube.spotube.core.i18n.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plugin_action_install
import spotube.composeapp.generated.resources.plugin_action_replace
import spotube.composeapp.generated.resources.plugin_error_api_incompatible
import spotube.composeapp.generated.resources.plugin_error_api_webview_outdated
import spotube.composeapp.generated.resources.plugin_error_api_version_invalid
import spotube.composeapp.generated.resources.plugin_error_app_api_version_invalid
import spotube.composeapp.generated.resources.plugin_error_builtin_add
import spotube.composeapp.generated.resources.plugin_error_builtin_remove
import spotube.composeapp.generated.resources.plugin_error_install_failed
import spotube.composeapp.generated.resources.plugin_error_invalid_cookie_domains
import spotube.composeapp.generated.resources.plugin_error_suffix_list_unavailable
import spotube.composeapp.generated.resources.plugin_error_invalid_plugin_json
import spotube.composeapp.generated.resources.plugin_error_plugin_json_missing
import spotube.composeapp.generated.resources.plugin_error_read_failed
import spotube.composeapp.generated.resources.plugin_error_replace_needs_confirmation
import spotube.composeapp.generated.resources.plugin_error_update_needs_confirmation
import spotube.composeapp.generated.resources.plugin_error_zipline_manifest_missing
import spotube.composeapp.generated.resources.plugin_prompt_incompatible_title
import spotube.composeapp.generated.resources.plugin_prompt_install_message
import spotube.composeapp.generated.resources.plugin_prompt_install_title
import spotube.composeapp.generated.resources.plugin_prompt_invalid_title
import spotube.composeapp.generated.resources.plugin_prompt_replace_downgrade
import spotube.composeapp.generated.resources.plugin_prompt_replace_generic
import spotube.composeapp.generated.resources.plugin_prompt_replace_invalid_incoming
import spotube.composeapp.generated.resources.plugin_prompt_replace_invalid_installed
import spotube.composeapp.generated.resources.plugin_prompt_replace_same_version
import spotube.composeapp.generated.resources.plugin_prompt_replace_title
import spotube.composeapp.generated.resources.plugin_permissions_update_adds
import spotube.composeapp.generated.resources.plugin_prompt_update_message
import spotube.composeapp.generated.resources.plugin_prompt_update_title
import spotube.composeapp.generated.resources.update_action_update

/**
 * 0.1.0: WebView plugins must declare `cookieDomains`, and cookie reads and scripts are limited to
 * them. Nothing else changed, so 0.0.x plugins without the WebView capability still work.
 */
const val PLUGIN_API_VERSION = "0.1.0"

interface PluginProvider {
    val selectedMetadataPlugin: StateFlow<PluginService?>
}

/** Narrow read-only view of the active audio plugin, used by consumers that only need to
 * key caches or make calls against the currently selected audio plugin without depending
 * on the full [PluginManager] (which also owns plugin installation/lifecycle machinery). */
interface AudioPluginSource {
    val selectedAudioPlugin: StateFlow<PluginService?>
}

/** Narrow read-only view of the active scrobble plugin, used by consumers that only need
 * to submit scrobbles against the currently selected plugin without depending on the full
 * [PluginManager] (which also owns plugin installation/lifecycle machinery). */
interface ScrobblePluginSource {
    val selectedScrobblePlugin: StateFlow<PluginService?>
}

class PluginManager(
    val database: Database,
    val paths: Paths,
    private val httpClientFactory: HttpClientFactory) : KoinComponent, PluginProvider, AudioPluginSource, ScrobblePluginSource {
    private val logger by injectLogger<PluginManager>()
    private val publicSuffixes: PublicSuffixListRepository by inject()

    // plugin.json may carry fields newer than this app knows about (e.g. `entryPoint`).
    private val pluginJsonParser = Json { ignoreUnknownKeys = true }
    private val pluginExceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "Plugin runtime threw an unhandled exception. Intercepted safely." }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + pluginExceptionHandler)
    private val pluginsDir = "${paths.getApplicationDataDirPath()}/plugins".toPath()
    val pluginsDirPath: Path get() = pluginsDir
    private val httpClient get() = httpClientFactory.default

    enum class InstallPromptKind {
        INSTALL,
        UPDATE,
        REPLACE,
        INFO,
    }

    // Holds a parsed plugin awaiting user confirmation or acknowledgement.
    class PendingPlugin(
        val entry: PluginEntry,
        val kind: InstallPromptKind,
        val title: String,
        val message: String,
        val bytes: ByteArray? = null,
        val existingEntry: PluginEntry? = null,
        val confirmLabel: String? = null,
        /** What the incoming build asks for beyond [existingEntry], highlighted in the prompt. */
        val permissionChanges: PluginPermissionChanges = PluginPermissionChanges(),
        /** [entry]'s permissions, ready to render. */
        val permissions: PluginPermissionsSummary = entry.permissionsSummary(permissionChanges)) {
        fun withMessage(message: String) = PendingPlugin(
            entry, kind, title, message, bytes, existingEntry, confirmLabel, permissionChanges, permissions)
    }

    /** Outcome of [updatePluginFromURL]. */
    sealed interface UpdateResult {
        data object Installed : UpdateResult

        /** The update asks for more permissions, so it wasn't installed; show [pending] to the user. */
        data class NeedsPermissionReview(val pending: PendingPlugin) : UpdateResult
    }

    val pendingPlugin = MutableStateFlow<PendingPlugin?>(null)

    val state: StateFlow<PluginManagerStates> = database.settingsDataStore.data
        .map { p ->
            val json = p[DatabaseKeys.PLUGINS_STATE_KEY]
            // Each category starts out with the built-in plugin that provides it.
            val defaultSelectedPlugins = BUILT_IN_PLUGINS
                .flatMap { plugin -> plugin.abilities.map { it to plugin } }
                .distinctBy { it.first }
                .toMap()
            if (json == null) {
                PluginManagerStates.Data(
                    plugins = BUILT_IN_PLUGINS,
                    selectedPlugins = defaultSelectedPlugins
                )
            } else {
                try {
                    val res = Json.decodeFromString<PluginManagerStates.Data>(json)
                    // Built-in plugins always come from the current app build, never from the
                    // saved state: a saved copy can be from an older version and differ from
                    // today's definition, so match by id and drop it, then add the current
                    // built-ins. Saved user plugins are deduplicated by id too.
                    val resolvedPlugins = BUILT_IN_PLUGINS +
                        res.plugins.filterNot { it.isBuiltIn }.distinctBy { it.id }
                    val resolvedPluginsById = resolvedPlugins.associateBy { it.id }
                    // Drop selections for plugins that don't actually declare the ability
                    // (e.g. a scrobble selection left over from a removed built-in plugin)
                    // and always point selections at the resolved plugin entry.
                    val selectedPlugins = res.selectedPlugins.ifEmpty { defaultSelectedPlugins }
                        .mapNotNull { (ability, selected) ->
                            val plugin = resolvedPluginsById[selected.id] ?: return@mapNotNull null
                            if (ability in plugin.abilities) ability to plugin else null
                        }
                        .toMap()

                    res.copy(
                        plugins = resolvedPlugins,
                        selectedPlugins = selectedPlugins)
                } catch (_: Exception) {
                    PluginManagerStates.Data(
                        BUILT_IN_PLUGINS,
                        selectedPlugins = defaultSelectedPlugins
                    ) // Fallback on error
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = PluginManagerStates.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val ziplineServices = state
        .filterIsInstance<PluginManagerStates.Data>()
        .map { it.selectedPlugins to it.generation }
        // Paired with generation so same-version replaces (structurally equal
        // PluginEntry) still trigger a restart. Without it the DataStore skips
        // the emission (identical JSON) and the flow stays alive with stale code.
        .distinctUntilChanged()
        .flatMapLatest { (plugins, gen) ->
            logger.d { "ziplineServices: flow restarting with generation=$gen, selectedPlugins=${plugins.keys}" }
            flow {
                val ziplineServices: MutableMap<PluginAbility, PluginService?> =
                    mutableMapOf()
                val ziplineServicesByPluginID = mutableMapOf<String, PluginService>()
                try {
                    for (ability in PluginAbility.entries) {
                        val plugin = plugins[ability] ?: continue

                        if (ziplineServicesByPluginID.containsKey(plugin.id)) {
                            ziplineServices[ability] = ziplineServicesByPluginID[plugin.id]
                        } else {
                            logger.d { "ziplineServices: creating service for ${plugin.name} (${plugin.id})" }
                            val service = createService(plugin)
                            ziplineServices[ability] = service
                            ziplineServicesByPluginID[plugin.id] = service
                            service.start()
                        }
                    }
                    emit(ziplineServices)
                    // Keep the flow alive until the next plugin is selected.
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        logger.d { "ziplineServices: finally block stopping ${ziplineServicesByPluginID.size} services" }
                        ziplineServicesByPluginID.forEach { (_, service) ->
                            try {
                                service.stop()
                            } catch (_: Exception) {
                                logger.e { "ziplineServices: error stopping service" }
                            }
                        }
                        logger.d { "ziplineServices: all services stopped" }
                    }
                }
            }
        }
        .stateIn(
            scope = scope,
            // Keep plugin runtime alive across screen/navigation transitions.
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private fun createService(plugin: PluginEntry): PluginService =
        if (plugin.isBuiltIn) {
            BuiltInPluginService(plugin)
        } else {
            ZiplinePluginService(
                applicationName = plugin.name,
                manifestUrl = "http://localhost?path=${(pluginsDir / plugin.id.toPath() / "manifest.zipline.json")}",
                pluginInfo = plugin)
        }

    /**
     * Runs [block] against [plugin]: on its running service when it's selected for an ability,
     * otherwise on a short-lived one that is stopped again afterwards. Lets the plugin screen
     * ask any installed plugin (e.g. for its support text) without making it a default.
     */
    suspend fun <T> withPluginService(plugin: PluginEntry, block: suspend PluginServiceScope.() -> T): T {
        ziplineServices.value?.values?.firstOrNull { it?.pluginId == plugin.id }?.let { running ->
            return running.use(block)
        }
        val service = createService(plugin)
        try {
            service.start()
            return service.use(block)
        } finally {
            withContext(NonCancellable) {
                runCatching { service.stop() }.onFailure { logger.e(it) { "Failed to stop temporary service for ${plugin.name}" } }
            }
        }
    }

    private fun filterPluginByType(ability: PluginAbility): StateFlow<List<PluginEntry>> {
        return state
            .filterIsInstance<PluginManagerStates.Data>()
            .map { state ->
                state.plugins.filter {
                    it.abilities.contains(ability)
                }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )
    }

    private fun filterSelectedPluginByType(ability: PluginAbility): StateFlow<PluginService?> {
        return ziplineServices
            .mapNotNull { state ->
                state?.get(ability)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )
    }

    val metadataPlugins: StateFlow<List<PluginEntry>> = filterPluginByType(PluginAbility.METADATA)
    val audioPlugins = filterPluginByType(PluginAbility.AUDIO)
    val lyricsPlugins = filterPluginByType(PluginAbility.LYRICS)
    val scrobblePlugins = filterPluginByType(PluginAbility.SCROBBLE)

    override val selectedMetadataPlugin =
        filterSelectedPluginByType(PluginAbility.METADATA)
    override val selectedAudioPlugin =
        filterSelectedPluginByType(PluginAbility.AUDIO)
    val selectedLyricsPlugin =
        filterSelectedPluginByType(PluginAbility.LYRICS)
    override val selectedScrobblePlugin =
        filterSelectedPluginByType(PluginAbility.SCROBBLE)

    fun launchTask(block: suspend CoroutineScope.() -> Unit): Job {
        return scope.launch(block = block)
    }

    fun <T> asyncTask(block: suspend CoroutineScope.() -> T) =
        scope.async(context = Dispatchers.IO, block = block)
    suspend fun <T> withScope(block: suspend CoroutineScope.() -> T) = withContext(
        scope.coroutineContext.minusKey(Job) + Dispatchers.IO
    ) {
        block()
    }

    private suspend fun updatePluginsState(newState: PluginManagerStates.Data) {
        database.settingsDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[DatabaseKeys.PLUGINS_STATE_KEY] = Json.encodeToString(newState)
            }
        }
    }

    suspend fun addPluginFromURL(url: String) {
        withContext(Dispatchers.IO) {
            val response = httpClient.get(url)
            val bytes = response.bodyAsChannel().readRemaining().readByteArray()
            preparePlugin(bytes)
        }
    }

    /**
     * Downloads a newer plugin build and installs it over the installed one without prompting,
     * unless it asks for permissions (capabilities or cookie domains) the installed build didn't
     * have. Then nothing is installed and the caller must let the user review it first.
     */
    suspend fun updatePluginFromURL(url: String): UpdateResult = withContext(Dispatchers.IO) {
        val response = httpClient.get(url)
        val bytes = response.bodyAsChannel().readRemaining().readByteArray()
        val incoming = readPluginEntry(bytes)
        val installed = findInstalledPlugin(incoming.id)
        if (installed != null && !incoming.permissionChangesSince(installed).isEmpty) {
            val pending = buildPendingPlugin(incoming, bytes)
            // An INFO prompt (incompatible or invalid build) already says why it can't be installed.
            return@withContext UpdateResult.NeedsPermissionReview(
                if (pending.kind == InstallPromptKind.INFO) {
                    pending
                } else {
                    pending.withMessage(getString(Res.string.plugin_permissions_update_adds))
                })
        }
        addPluginFromByteArray(bytes = bytes, allowReplacingInstalled = true)
        UpdateResult.Installed
    }

    /** Reads plugin.json straight from a plugin archive, without extracting anything else. */
    private suspend fun readPluginEntry(bytes: ByteArray): PluginEntry {
        val pluginJson = okio.Buffer().apply { write(bytes) }.use { source ->
            val zipIn = ZipInputStream(source)
            var entry = zipIn.nextEntry
            var json: String? = null
            while (entry != null && json == null) {
                if (!entry.isDirectory && entry.name.trimStart('/') == "plugin.json") {
                    json = zipIn.readBytes().decodeToString()
                }
                entry = zipIn.nextEntry
            }
            json
        } ?: throw IllegalArgumentException(getString(Res.string.plugin_error_plugin_json_missing))
        return decodePluginEntry(pluginJson)
    }

    private suspend fun decodePluginEntry(pluginJson: String): PluginEntry = try {
        pluginJsonParser.decodeFromString<PluginEntry>(pluginJson)
    } catch (e: Exception) {
        throw IllegalArgumentException(getString(Res.string.plugin_error_invalid_plugin_json, e.message.orEmpty()))
    }

    /** Parses the zip, reads plugin.json, then surfaces a PendingPlugin for the UI to confirm. */
    suspend fun preparePlugin(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val tempDir = "${paths.getApplicationCacheDirPath()}/temp-plugin-preview".toPath()
            try {
                if (FileSystem.SYSTEM.exists(tempDir)) FileSystem.SYSTEM.deleteRecursively(tempDir)
                FileSystem.SYSTEM.createDirectories(tempDir)

                val okioBuffer = okio.Buffer().apply { write(bytes) }
                okioBuffer.use { bufferedSource ->
                    val zipIn = ZipInputStream(bufferedSource)
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val entryPath = tempDir / entry.name.toPath()
                        if (entry.isDirectory) {
                            FileSystem.SYSTEM.createDirectories(entryPath)
                        } else {
                            entryPath.parent?.let {
                                if (!FileSystem.SYSTEM.exists(it)) FileSystem.SYSTEM.createDirectories(
                                    it
                                )
                            }
                            FileSystem.SYSTEM.sink(entryPath).buffer().use { sink ->
                                sink.write(zipIn.readBytes())
                            }
                        }
                        entry = zipIn.nextEntry
                    }
                }

                val pluginJsonPath = tempDir / "plugin.json".toPath()
                if (!FileSystem.SYSTEM.exists(pluginJsonPath)) {
                    throw IllegalArgumentException(getString(Res.string.plugin_error_plugin_json_missing))
                }

                val pluginJson =
                    FileSystem.SYSTEM.source(pluginJsonPath).buffer().use { it.readUtf8() }
                val pluginEntry = decodePluginEntry(pluginJson)

                // Preserve logo.png before deleting temp dir so the permission dialog can show it
                val logoPngPath = tempDir / "logo.png".toPath()
                if (FileSystem.SYSTEM.exists(logoPngPath)) {
                    val logoDir = pluginsDirPath / pluginEntry.id.toPath()
                    if (!FileSystem.SYSTEM.exists(logoDir)) FileSystem.SYSTEM.createDirectories(logoDir)
                    val destLogo = logoDir / "logo.png".toPath()
                    FileSystem.SYSTEM.copy(logoPngPath, destLogo)
                }

                pendingPlugin.value = buildPendingPlugin(pluginEntry, bytes)
            } catch (e: Exception) {
                throw Exception(getString(Res.string.plugin_error_read_failed, e.message.orEmpty()), e)
            } finally {
                if (FileSystem.SYSTEM.exists(tempDir)) {
                    try {
                        FileSystem.SYSTEM.deleteRecursively(tempDir)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    /**
     * Called when the user confirms installation/update/replacement in the dialog. The prompt stays
     * up until the install finishes, so the dialog can show progress.
     *
     * @return The install job, or `null` when there was nothing to install.
     */
    fun confirmInstall(): Job? {
        val pending = pendingPlugin.value ?: return null
        val bytes = pending.bytes ?: run { dismissInstall(); return null }
        val allowReplacingInstalled = when (pending.kind) {
            InstallPromptKind.INSTALL -> false
            InstallPromptKind.UPDATE, InstallPromptKind.REPLACE -> true
            InstallPromptKind.INFO -> run { dismissInstall(); return null }
        }
        return scope.launch {
            try {
                addPluginFromByteArray(
                    bytes = bytes,
                    allowReplacingInstalled = allowReplacingInstalled
                )
            } finally {
                // Only close our own prompt; another plugin may have been queued meanwhile.
                pendingPlugin.compareAndSet(pending, null)
            }
        }
    }

    /** Called when the user closes or denies the dialog. */
    fun dismissInstall() {
        pendingPlugin.value = null
    }

    suspend fun addPluginFromByteArray(
        bytes: ByteArray,
        allowReplacingInstalled: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (!FileSystem.SYSTEM.exists(pluginsDir)) {
                FileSystem.SYSTEM.createDirectories(pluginsDir)
            }

            val tempDir = "${paths.getApplicationCacheDirPath()}/temp-plugin".toPath()

            try {
                if (FileSystem.SYSTEM.exists(tempDir)) {
                    FileSystem.SYSTEM.deleteRecursively(tempDir)
                }
                FileSystem.SYSTEM.createDirectories(tempDir)

                val okioBuffer = okio.Buffer()
                okioBuffer.write(bytes)

                okioBuffer.use { bufferedSource ->
                    val zipIn = ZipInputStream(bufferedSource)
                    var entry = zipIn.nextEntry

                    while (entry != null) {
                        val entryPath = tempDir / entry.name.toPath()

                        if (entry.isDirectory) {
                            FileSystem.SYSTEM.createDirectories(entryPath)
                        } else {
                            entryPath.parent?.let { parent ->
                                if (!FileSystem.SYSTEM.exists(parent)) {
                                    FileSystem.SYSTEM.createDirectories(parent)
                                }
                            }

                            FileSystem.SYSTEM.sink(entryPath).buffer().use { sink ->
                                val entryBytes = zipIn.readBytes()
                                val hash = entryBytes.toByteString().sha256().hex()
                                logger.d { "Extracting ${entry.name} (${entryBytes.size / 1024.0} KB, SHA-256: $hash)" }
                                sink.write(entryBytes)
                                sink.flush()
                            }
                        }

                        entry = zipIn.nextEntry
                    }
                }

                val pluginJsonPath = tempDir / "plugin.json".toPath()
                if (!FileSystem.SYSTEM.exists(pluginJsonPath)) {
                    throw IllegalArgumentException(getString(Res.string.plugin_error_plugin_json_missing))
                }
                val pluginManifestPath = tempDir / "manifest.zipline.json".toPath()
                if (!FileSystem.SYSTEM.exists(pluginManifestPath)) {
                    throw IllegalArgumentException(getString(Res.string.plugin_error_zipline_manifest_missing))
                }

                val pluginJson = FileSystem.SYSTEM.source(pluginJsonPath).buffer().use { source ->
                    source.readUtf8()
                }

                val pluginEntry = decodePluginEntry(pluginJson)

                ensurePluginApiCompatible(pluginEntry)
                getPluginPermissionError(pluginEntry)?.let { throw IllegalArgumentException(it) }

                val installedPlugin = findInstalledPlugin(pluginEntry.id)
                if (installedPlugin != null && !allowReplacingInstalled) {
                    throw IllegalArgumentException(
                        when (compareVersions(installedPlugin, pluginEntry)) {
                            VersionRelation.Update -> getString(Res.string.plugin_error_update_needs_confirmation, pluginEntry.name)
                            VersionRelation.Replace -> getString(Res.string.plugin_error_replace_needs_confirmation, pluginEntry.name)
                        }
                    )
                }

                val finalPluginDir = pluginsDir / pluginEntry.id.toPath()
                if (FileSystem.SYSTEM.exists(finalPluginDir)) {
                    FileSystem.SYSTEM.deleteRecursively(finalPluginDir)
                }
                FileSystem.SYSTEM.createDirectories(finalPluginDir)

                FileSystem.SYSTEM.list(tempDir).forEach { sourcePath ->
                    val relativePath =
                        sourcePath.toString().removePrefix(tempDir.toString()).trimStart('/', '\\')
                    val targetPath = finalPluginDir / relativePath.toPath()

                    if (FileSystem.SYSTEM.metadata(sourcePath).isDirectory) {
                        copyDirectory(sourcePath, targetPath)
                    } else {
                        targetPath.parent?.let { parent ->
                            if (!FileSystem.SYSTEM.exists(parent)) {
                                FileSystem.SYSTEM.createDirectories(parent)
                            }
                        }
                        FileSystem.SYSTEM.copy(sourcePath, targetPath)
                    }
                }

                addPlugin(pluginEntry)
            } catch (e: Exception) {
                throw Exception(getString(Res.string.plugin_error_install_failed, e.message.orEmpty()), e)
            } finally {
                if (FileSystem.SYSTEM.exists(tempDir)) {
                    try {
                        FileSystem.SYSTEM.deleteRecursively(tempDir)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun copyDirectory(source: Path, target: Path) {
        if (!FileSystem.SYSTEM.exists(target)) {
            FileSystem.SYSTEM.createDirectories(target)
        }

        FileSystem.SYSTEM.list(source).forEach { sourcePath ->
            val fileName = sourcePath.name
            val targetPath = target / fileName.toPath()

            if (FileSystem.SYSTEM.metadata(sourcePath).isDirectory) {
                copyDirectory(sourcePath, targetPath)
            } else {
                FileSystem.SYSTEM.copy(sourcePath, targetPath)
            }
        }
    }

    private suspend fun addPlugin(plugin: PluginEntry) {
        if (plugin.isBuiltIn) {
            throw IllegalArgumentException(getString(Res.string.plugin_error_builtin_add))
        }

        val currentState = state.value
        if (currentState is PluginManagerStates.Data) {
            val updatedPlugins = currentState.plugins.filterNot { it.id == plugin.id } + plugin
            val updatedSelectedPlugins =
                currentState.selectedPlugins.mapValues { (_, selectedPlugin) ->
                    if (selectedPlugin.id == plugin.id) plugin else selectedPlugin
                }
            val newState = PluginManagerStates.Data(
                plugins = updatedPlugins,
                selectedPlugins = updatedSelectedPlugins,
                generation = currentState.generation + 1
            )
            logger.d { "addPlugin: bumped generation to ${newState.generation} for plugin ${plugin.id}" }
            updatePluginsState(newState)
        }
    }

    suspend fun removePlugin(plugin: PluginEntry) {
        if (plugin.isBuiltIn) {
            throw IllegalArgumentException(getString(Res.string.plugin_error_builtin_remove))
        }

        val currentState = state.value
        if (currentState is PluginManagerStates.Data) {
            val updatedPlugins = currentState.plugins.filterNot { it.id == plugin.id }
            val updatedSelectedPlugins =
                currentState.selectedPlugins.filterValues { it.id != plugin.id }
            val newState = PluginManagerStates.Data(
                plugins = updatedPlugins,
                selectedPlugins = updatedSelectedPlugins,
                generation = currentState.generation
            )

            withContext(Dispatchers.IO) {
                val pluginDir = pluginsDir / plugin.id.toPath()
                if (FileSystem.SYSTEM.exists(pluginDir)) {
                    FileSystem.SYSTEM.deleteRecursively(pluginDir)
                }
            }

            updatePluginsState(newState)
        }
    }

    fun setSelectedPlugin(ability: PluginAbility, plugin: PluginEntry?) {
        val currentState = state.value
        if (currentState is PluginManagerStates.Data) {
            val newState = currentState.copy(
                selectedPlugins = if (plugin == null) {
                    currentState.selectedPlugins - ability
                } else {
                    currentState.selectedPlugins + (ability to plugin)
                }
            )
            scope.launch {
                updatePluginsState(newState)
            }
        }
    }

    private enum class VersionRelation {
        Update,
        Replace,
    }

    private suspend fun buildPendingPlugin(pluginEntry: PluginEntry, bytes: ByteArray): PendingPlugin {
        val apiError = getPluginApiCompatibilityError(pluginEntry)
        if (apiError != null) {
            return PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.INFO,
                title = getString(Res.string.plugin_prompt_incompatible_title),
                message = apiError)
        }

        val permissionError = getPluginPermissionError(pluginEntry)
        if (permissionError != null) {
            return PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.INFO,
                title = getString(Res.string.plugin_prompt_invalid_title),
                message = permissionError)
        }

        val installedPlugin = findInstalledPlugin(pluginEntry.id) ?: return PendingPlugin(
            entry = pluginEntry,
            kind = InstallPromptKind.INSTALL,
            title = getString(Res.string.plugin_prompt_install_title),
            message = getString(Res.string.plugin_prompt_install_message, pluginEntry.name),
            bytes = bytes,
            confirmLabel = getString(Res.string.plugin_action_install))

        return when (compareVersions(installedPlugin, pluginEntry)) {
            VersionRelation.Update -> PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.UPDATE,
                title = getString(Res.string.plugin_prompt_update_title),
                message = getString(
                    Res.string.plugin_prompt_update_message,
                    installedPlugin.version,
                    pluginEntry.version),
                bytes = bytes,
                existingEntry = installedPlugin,
                confirmLabel = getString(Res.string.update_action_update),
                permissionChanges = pluginEntry.permissionChangesSince(installedPlugin))

            VersionRelation.Replace -> PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.REPLACE,
                title = getString(Res.string.plugin_prompt_replace_title),
                message = buildReplaceMessage(installedPlugin, pluginEntry),
                bytes = bytes,
                existingEntry = installedPlugin,
                confirmLabel = getString(Res.string.plugin_action_replace),
                permissionChanges = pluginEntry.permissionChangesSince(installedPlugin))
        }
    }

    private suspend fun buildReplaceMessage(
        installedPlugin: PluginEntry,
        incomingPlugin: PluginEntry
    ): String {
        val incomingVersion = parseSemVerOrNull(incomingPlugin.version)
        val installedVersion = parseSemVerOrNull(installedPlugin.version)

        return when {
            incomingVersion == null -> getString(
                Res.string.plugin_prompt_replace_invalid_incoming,
                incomingPlugin.name,
                incomingPlugin.version)
            installedVersion == null -> getString(
                Res.string.plugin_prompt_replace_invalid_installed,
                installedPlugin.version,
                incomingPlugin.version)
            incomingVersion == installedVersion -> getString(
                Res.string.plugin_prompt_replace_same_version,
                incomingPlugin.version)
            incomingVersion < installedVersion -> getString(
                Res.string.plugin_prompt_replace_downgrade,
                installedPlugin.version,
                incomingPlugin.version)
            else -> getString(Res.string.plugin_prompt_replace_generic)
        }
    }

    private fun compareVersions(
        installedPlugin: PluginEntry,
        incomingPlugin: PluginEntry
    ): VersionRelation {
        val installedVersion = parseSemVerOrNull(installedPlugin.version)
        val incomingVersion = parseSemVerOrNull(incomingPlugin.version)

        return if (installedVersion != null && incomingVersion != null && incomingVersion > installedVersion) {
            VersionRelation.Update
        } else {
            VersionRelation.Replace
        }
    }

    private fun findInstalledPlugin(id: String): PluginEntry? {
        val currentState = state.value as? PluginManagerStates.Data
        return currentState?.plugins?.find { it.id == id }
    }

    private fun parseSemVerOrNull(version: String): SemVer? {
        return try {
            SemVer.parse(version)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getPluginPermissionError(pluginEntry: PluginEntry): String? {
        val suffixes = publicSuffixes.get()
        val invalid = PluginPermissionValidator.invalidCookieDomains(pluginEntry.cookieDomains, suffixes)
        if (invalid.isEmpty()) return null
        // Wildcards are only checkable against a downloaded list; say so instead of calling them invalid.
        if (!suffixes.isAvailable &&
            PluginPermissionValidator.invalidCookieDomains(invalid, PublicSuffixList.TopLevelOnly).isEmpty()) {
            return getString(
                Res.string.plugin_error_suffix_list_unavailable,
                pluginEntry.name,
                invalid.joinToString { "\"$it\"" })
        }
        return getString(
            Res.string.plugin_error_invalid_cookie_domains,
            pluginEntry.name,
            invalid.joinToString { "\"$it\"" })
    }

    private suspend fun ensurePluginApiCompatible(pluginEntry: PluginEntry) {
        getPluginApiCompatibilityError(pluginEntry)?.let { error ->
            throw IllegalArgumentException(error)
        }
    }

    private suspend fun getPluginApiCompatibilityError(pluginEntry: PluginEntry): String? {
        val pluginApiVersion = parseSemVerOrNull(pluginEntry.apiVersion)
            ?: return getString(
                Res.string.plugin_error_api_version_invalid,
                pluginEntry.name,
                pluginEntry.apiVersion,
                PLUGIN_API_VERSION)
        val appApiVersion = parseSemVerOrNull(PLUGIN_API_VERSION)
            ?: return getString(Res.string.plugin_error_app_api_version_invalid, PLUGIN_API_VERSION)

        val builtBeforeCookieDomains = pluginApiVersion.major == 0 && pluginApiVersion.minor == 0
        if (builtBeforeCookieDomains && appApiVersion.major == 0 && appApiVersion.minor == 1) {
            return if (PluginCapability.WEBVIEW in pluginEntry.capabilities) {
                getString(Res.string.plugin_error_api_webview_outdated, pluginEntry.name, pluginEntry.apiVersion)
            } else {
                null
            }
        }

        val compatible = if (pluginApiVersion.major == 0 || appApiVersion.major == 0) {
            pluginApiVersion.major == appApiVersion.major && pluginApiVersion.minor == appApiVersion.minor
        } else {
            pluginApiVersion.major == appApiVersion.major
        }

        return if (compatible) {
            null
        } else {
            getString(
                Res.string.plugin_error_api_incompatible,
                pluginEntry.name,
                pluginEntry.apiVersion,
                PLUGIN_API_VERSION)
        }
    }
}
