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

import dev.krtirtho.spotube.core.db.Database
import dev.krtirtho.spotube.core.db.DatabaseKeys
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.paths.Paths
import dev.krtirtho.spotube.core.zipline.BuiltInPluginService
import dev.krtirtho.spotube.core.zipline.PluginService
import dev.krtirtho.spotube.core.zipline.ZiplinePluginService
import io.ktor.client.HttpClient
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
import org.koin.core.component.KoinComponent

const val PLUGIN_API_VERSION = "0.0.1"

interface PluginProvider {
    val selectedMetadataPlugin: StateFlow<PluginService?>
}

/** Narrow read-only view of the active audio plugin, used by consumers that only need to
 * key caches or make calls against the currently selected audio plugin without depending
 * on the full [PluginManager] (which also owns plugin installation/lifecycle machinery). */
interface AudioPluginSource {
    val selectedAudioPlugin: StateFlow<PluginService?>
}


class PluginManager(
    val database: Database,
    val paths: Paths,
) : KoinComponent, PluginProvider, AudioPluginSource {
    private val logger by injectLogger<PluginManager>()
    private val pluginExceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "Plugin runtime threw an unhandled exception. Intercepted safely." }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + pluginExceptionHandler)
    private val pluginsDir = "${paths.getApplicationDataDirPath()}/plugins".toPath()
    val pluginsDirPath: Path get() = pluginsDir
    private val httpClient = HttpClient()


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
    )

    val pendingPlugin = MutableStateFlow<PendingPlugin?>(null)

    val state: StateFlow<PluginManagerStates> = database.settingsDataStore.data
        .map { p ->
            val json = p[DatabaseKeys.PLUGINS_STATE_KEY]
            val defaultSelectedPlugins = mapOf(
                PluginAbility.AUDIO to NEWPIPE_YOUTUBE_BUILT_IN_PLUGIN,
                PluginAbility.SCROBBLE to LRCLIB_BUILT_IN_PLUGIN,
            )
            if (json == null) {
                PluginManagerStates.Data(
                    plugins = BUILT_IN_PLUGINS,
                    selectedPlugins = defaultSelectedPlugins
                )
            } else {
                try {
                    val res = Json.decodeFromString<PluginManagerStates.Data>(json)
                    val plugins = res.plugins.map {
                        // Replace plugins that match built-in plugin IDs with the built-in
                        // plugin entries. This ensures that any updates to built-in plugins
                        // are reflected in the UI, while still allowing user-installed plugins
                        // to be loaded from disk.
                        if (it in BUILT_IN_PLUGINS) {
                            BUILT_IN_PLUGINS.first { builtIn -> builtIn.id == it.id }
                        } else it
                    }.toSet()

                    res.copy(
                        plugins = (plugins union BUILT_IN_PLUGINS).toList(),
                        selectedPlugins = res.selectedPlugins.ifEmpty {
                            defaultSelectedPlugins
                        }
                    )
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
                        } else if (plugin in BUILT_IN_PLUGINS) {
                            val service = BuiltInPluginService(plugin)
                            ziplineServices[ability] = service
                            ziplineServicesByPluginID[plugin.id] = service
                            service.start()
                        } else {
                            logger.d { "ziplineServices: creating new ZiplinePluginService for ${plugin.name} (${plugin.id})" }
                            val service = ZiplinePluginService(
                                applicationName = plugin.name,
                                manifestUrl = "http://localhost?path=${(pluginsDir / plugin.id.toPath() / "manifest.zipline.json")}",
                                pluginInfo = plugin,
                            )
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
    val selectedScrobblePlugin =
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
                    throw IllegalArgumentException("plugin.json not found in the zip file")
                }

                val pluginJson =
                    FileSystem.SYSTEM.source(pluginJsonPath).buffer().use { it.readUtf8() }
                val pluginEntry = try {
                    Json.decodeFromString<PluginEntry>(pluginJson)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid plugin.json format: ${e.message}")
                }

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
                throw Exception("Failed to read plugin: ${e.message}", e)
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

    /** Called when the user confirms installation/update/replacement in the dialog. */
    fun confirmInstall() {
        val pending = pendingPlugin.value ?: return
        val bytes = pending.bytes ?: return dismissInstall()
        val allowReplacingInstalled = when (pending.kind) {
            InstallPromptKind.INSTALL -> false
            InstallPromptKind.UPDATE, InstallPromptKind.REPLACE -> true
            InstallPromptKind.INFO -> return dismissInstall()
            InstallPromptKind.REPLACE -> true
        }
        pendingPlugin.value = null
        scope.launch {
            addPluginFromByteArray(
                bytes = bytes,
                allowReplacingInstalled = allowReplacingInstalled
            )
        }
    }

    /** Called when the user closes or denies the dialog. */
    fun dismissInstall() {
        pendingPlugin.value = null
    }

    suspend fun addPluginFromByteArray(
        bytes: ByteArray,
        allowReplacingInstalled: Boolean = false,
    ) {
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
                    throw IllegalArgumentException("plugin.json not found in the zip file")
                }
                val pluginManifestPath = tempDir / "manifest.zipline.json".toPath()
                if (!FileSystem.SYSTEM.exists(pluginManifestPath)) {
                    throw IllegalArgumentException("manifest.zipline.json not found in the zip file")
                }

                val pluginJson = FileSystem.SYSTEM.source(pluginJsonPath).buffer().use { source ->
                    source.readUtf8()
                }

                val pluginEntry = try {
                    Json.decodeFromString<PluginEntry>(pluginJson)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid plugin.json format: ${e.message}")
                }

                ensurePluginApiCompatible(pluginEntry)

                val installedPlugin = findInstalledPlugin(pluginEntry.id)
                if (installedPlugin != null && !allowReplacingInstalled) {
                    throw IllegalArgumentException(
                        when (compareVersions(installedPlugin, pluginEntry)) {
                            VersionRelation.Update -> "A newer version of ${pluginEntry.name} is available. Confirm the update before installing."
                            VersionRelation.Replace -> "${pluginEntry.name} is already installed. Confirm replacing it before installing."
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
                throw Exception("Failed to install plugin: ${e.message}", e)
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
        if (plugin in BUILT_IN_PLUGINS) {
            throw IllegalArgumentException("Built-in plugins are already included and can't be added again.")
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
        if (plugin in BUILT_IN_PLUGINS) {
            throw IllegalArgumentException("Built-in plugins can't be removed.")
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

    private fun buildPendingPlugin(pluginEntry: PluginEntry, bytes: ByteArray): PendingPlugin {
        val apiError = getPluginApiCompatibilityError(pluginEntry)
        if (apiError != null) {
            return PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.INFO,
                title = "Plugin API not compatible",
                message = apiError,
            )
        }

        val installedPlugin = findInstalledPlugin(pluginEntry.id) ?: return PendingPlugin(
            entry = pluginEntry,
            kind = InstallPromptKind.INSTALL,
            title = "Install plugin?",
            message = "${pluginEntry.name} will be added to your installed plugins.",
            bytes = bytes,
            confirmLabel = "Install",
        )

        return when (compareVersions(installedPlugin, pluginEntry)) {
            VersionRelation.Update -> PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.UPDATE,
                title = "Update plugin?",
                message = "An older version is installed (${installedPlugin.version}). Update to ${pluginEntry.version}?",
                bytes = bytes,
                existingEntry = installedPlugin,
                confirmLabel = "Update",
            )

            VersionRelation.Replace -> PendingPlugin(
                entry = pluginEntry,
                kind = InstallPromptKind.REPLACE,
                title = "Replace installed plugin?",
                message = buildReplaceMessage(installedPlugin, pluginEntry),
                bytes = bytes,
                existingEntry = installedPlugin,
                confirmLabel = "Replace",
            )
        }
    }

    private fun buildReplaceMessage(
        installedPlugin: PluginEntry,
        incomingPlugin: PluginEntry
    ): String {
        val incomingVersion = parseSemVerOrNull(incomingPlugin.version)
        val installedVersion = parseSemVerOrNull(installedPlugin.version)

        return when {
            incomingVersion == null -> "${incomingPlugin.name} uses an invalid semantic version (${incomingPlugin.version}) and can't be compared as an update. Replace the installed plugin anyway?"
            installedVersion == null -> "The installed version (${installedPlugin.version}) can't be compared using semantic versioning. Replace it with ${incomingPlugin.version}?"
            incomingVersion == installedVersion -> "Version ${incomingPlugin.version} is already installed. Replace the existing plugin with the supplied copy?"
            incomingVersion < installedVersion -> "Installed version ${installedPlugin.version} is newer than the supplied version ${incomingPlugin.version}. Replace it anyway?"
            else -> "Replace the installed plugin with the supplied copy?"
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

    private fun ensurePluginApiCompatible(pluginEntry: PluginEntry) {
        getPluginApiCompatibilityError(pluginEntry)?.let { error ->
            throw IllegalArgumentException(error)
        }
    }

    private fun getPluginApiCompatibilityError(pluginEntry: PluginEntry): String? {
        val pluginApiVersion = parseSemVerOrNull(pluginEntry.apiVersion)
            ?: return "${pluginEntry.name} declares an invalid apiVersion (${pluginEntry.apiVersion}). Expected semantic versioning compatible with $PLUGIN_API_VERSION."
        val appApiVersion = parseSemVerOrNull(PLUGIN_API_VERSION)
            ?: return "Spotube plugin API version $PLUGIN_API_VERSION is invalid."

        val compatible = if (pluginApiVersion.major == 0 || appApiVersion.major == 0) {
            pluginApiVersion.major == appApiVersion.major && pluginApiVersion.minor == appApiVersion.minor
        } else {
            pluginApiVersion.major == appApiVersion.major
        }

        return if (compatible) {
            null
        } else {
            "${pluginEntry.name} targets plugin API ${pluginEntry.apiVersion}, but Spotube supports breaking plugin API $PLUGIN_API_VERSION. Install a plugin built for the same breaking API version."
        }
    }
}