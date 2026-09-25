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

package dev.krtirtho.spotube.core.zipline

import app.cash.zipline.Zipline
import app.cash.zipline.ZiplineService
import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.LoadResult
import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineLoader
import dev.krtirtho.plugin_interfaces.core.Initializer
import dev.krtirtho.plugin_interfaces.core.Initializer_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.host_apis.CryptoAPI
import dev.krtirtho.plugin_interfaces.host_apis.CryptoAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.host_apis.HttpClientAPI
import dev.krtirtho.plugin_interfaces.host_apis.HttpClientAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.host_apis.PersistedStorageAPI
import dev.krtirtho.plugin_interfaces.host_apis.PersistedStorageAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.host_apis.SystemInformationAPI
import dev.krtirtho.plugin_interfaces.host_apis.SystemInformationAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.host_apis.WebViewAPI
import dev.krtirtho.plugin_interfaces.host_apis.WebViewAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.core.CoreAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.core.CoreAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.lyrics.LyricsAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI_SERVICE_NAME
import dev.krtirtho.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI_SERVICE_NAME
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.webview.WebViewController
import dev.krtirtho.spotube.core.zipline.host_apis.RealCryptoAPI
import dev.krtirtho.spotube.core.zipline.host_apis.RealHttpClientAPI
import dev.krtirtho.spotube.core.zipline.host_apis.RealPersistedStorageAPI
import dev.krtirtho.spotube.core.zipline.host_apis.RealSystemInformationAPI
import dev.krtirtho.spotube.core.zipline.host_apis.RealWebViewAPI
import dev.krtirtho.spotube.modules.plugin.PluginAbility
import dev.krtirtho.spotube.modules.plugin.PluginCapability
import dev.krtirtho.spotube.modules.plugin.PluginEntry
import io.ktor.http.URLBuilder
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass


/**
 * Manages the lifecycle of a Zipline plugin.
 * It runs everything on its own dispatcher (different thread) as per Zipline's requirements.
 * Anything it provides, must be called within that dispatcher context.
 * The [use] function must be used.
 *
 * The host bindings are called by plugins in the supplied [ZiplineDispatcher] as well,
 * so if they are calling something on [Dispatchers.Main], those calls should be wrapped in
 * `withContext(Dispatchers.Main)` to avoid blocking the zipline thread. It can cause stack-overflows.
 *
 * The plugin is loaded lazily when [start] is called, and all services are closed when [stop] is called.
 */
open class ZiplinePluginService(
    val applicationName: String,
    private val manifestUrl: String,
    private val pluginInfo: PluginEntry,
) : PluginService, KoinComponent {

    override val pluginId: String = pluginInfo.id

    // QuickJS compile() uses deep C-level recursion on the native thread stack.
    // Zipline.create() sets maxStackSize to only 6 MiB, but compiling large JS modules
    // (e.g. kotlin-stdlib at ~491 KB) can exceed that during AST parsing.
    // We use a custom EventListener to increase maxStackSize right after the Zipline
    // instance is created, before any modules are loaded.
    private val ziplineDispatcher = createZiplineDispatcher()

    private fun trace(event: String) {
        logger.d { "[$applicationName] $event" }
    }

    private val logger by injectLogger<ZiplinePluginService>()
    private val webViewController: WebViewController by inject()

    private val scope = CoroutineScope(SupervisorJob() + ziplineDispatcher.dispatcher)
    private val ziplineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e(throwable) { "Zipline Engine Error" }
    }
    private val lifecycleMutex = Mutex()
    private var ziplineLoader: ZiplineLoader
    private var ziplineInstance: Zipline? = null
    private val serviceRegistry = mutableMapOf<KClass<*>, ZiplineService>()

    init {
        val manifestPath = URLBuilder(manifestUrl)
        val baseDir =
            manifestPath.encodedParameters["path"]?.decodeURLQueryComponent()?.toPath()?.parent
                ?: throw IllegalArgumentException("Invalid manifest URL: $manifestUrl. Expected a 'path' query parameter pointing to the manifest file.")
        ziplineLoader = ZiplineLoader(
            dispatcher = ziplineDispatcher.dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = FileSystemHTTPClient(baseDir)
        )
    }

    private val realHttpClientAPI = RealHttpClientAPI()
    private val realWebViewAPI = RealWebViewAPI(scope, webViewController, pluginInfo.id)

    private val persistedStorageAPI = RealPersistedStorageAPI(pluginInfo)
    private val cryptoAPI = RealCryptoAPI(scope.coroutineContext)
    private val systemInformationAPI = RealSystemInformationAPI()

    private val loggedInStateFlow = MutableStateFlow(false)
    override val loggedInFlow: StateFlow<Boolean> = loggedInStateFlow.asStateFlow()

    private fun bindHostServices(zipline: Zipline) {
        trace("initializer(): binding host APIs")
        logger.d { "[$applicationName] Binding host APIs in initializer" }
        try {
            // Basic APIs
            zipline.bind<CryptoAPI>(CryptoAPI_SERVICE_NAME, cryptoAPI)
            zipline.bind<SystemInformationAPI>(
                SystemInformationAPI_SERVICE_NAME,
                systemInformationAPI
            )

            // Conditional APIs based on plugin capabilities
            if (PluginCapability.NETWORK_REQUESTS in pluginInfo.capabilities) {
                zipline.bind<HttpClientAPI>(
                    HttpClientAPI_SERVICE_NAME,
                    realHttpClientAPI
                )
            }
            if (PluginCapability.WEBVIEW in pluginInfo.capabilities) {
                zipline.bind<WebViewAPI>(WebViewAPI_SERVICE_NAME, realWebViewAPI)
            }
            if (PluginCapability.PERSISTENT_STORAGE in pluginInfo.capabilities) {
                zipline.bind<PersistedStorageAPI>(
                    PersistedStorageAPI_SERVICE_NAME,
                    persistedStorageAPI
                )
            }
        } catch (e: Exception) {
            logger.e(e) { "[$applicationName] Failed to bind host APIs: ${e.message}" }
            throw e
        }
    }

    private fun consumePluginServices(result: LoadResult.Success) {
        trace("start(): loadOnce success")
        val apiMap =
            buildMap<KClass<*>, ZiplineService> {
                put(CoreAPI::class, result.zipline.take<CoreAPI>(CoreAPI_SERVICE_NAME))

                if (PluginAbility.METADATA in pluginInfo.abilities) {
                    put(
                        MetadataUserAPI::class,
                        result.zipline.take<MetadataUserAPI>(
                            MetadataUserAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataTrackAPI::class,
                        result.zipline.take<MetadataTrackAPI>(
                            MetadataTrackAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataAlbumAPI::class,
                        result.zipline.take<MetadataAlbumAPI>(
                            MetadataAlbumAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataArtistAPI::class,
                        result.zipline.take<MetadataArtistAPI>(
                            MetadataArtistAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataPlaylistAPI::class,
                        result.zipline.take<MetadataPlaylistAPI>(
                            MetadataPlaylistAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataBrowseAPI::class,
                        result.zipline.take<MetadataBrowseAPI>(
                            MetadataBrowseAPI_SERVICE_NAME
                        )
                    )
                    put(
                        MetadataSearchAPI::class,
                        result.zipline.take<MetadataSearchAPI>(
                            MetadataSearchAPI_SERVICE_NAME
                        )
                    )
                }
                if (PluginAbility.AUDIO in pluginInfo.abilities) {
                    put(
                        AudioAPI::class,
                        result.zipline.take<AudioAPI>(AudioAPI_SERVICE_NAME)
                    )
                }
                if (PluginAbility.LYRICS in pluginInfo.abilities) {
                    put(
                        LyricsAPI::class,
                        result.zipline.take<LyricsAPI>(LyricsAPI_SERVICE_NAME)
                    )
                }
                if (PluginAbility.SCROBBLE in pluginInfo.abilities) {
                    put(
                        ScrobbleAPI::class,
                        result.zipline.take<ScrobbleAPI>(ScrobbleAPI_SERVICE_NAME)
                    )
                }
            }

        serviceRegistry.putAll(apiMap)
        trace("start(): API ready")
    }

    private fun runLogInFlowObservers() = scope.launch {
        val coreAPI = serviceRegistry[CoreAPI::class] as CoreAPI
        coreAPI.loggedInFlow.collect { isLoggedIn ->
            loggedInStateFlow.value = isLoggedIn
        }
    }

    override suspend fun start() {
        lifecycleMutex.withLock {
            trace("start(): entered")
            if (serviceRegistry.isNotEmpty()) {
                trace("start(): already started, skipping")
                return
            }

            logger.d { "[$applicationName] start(): loading plugin from $manifestUrl" }
            withContext(ziplineDispatcher.dispatcher) {
                trace("start(): inside zipline dispatcher before loadOnce")
                val result = ziplineLoader.loadOnce(
                    applicationName = applicationName,
                    manifestUrl = manifestUrl,
                    freshnessChecker = DefaultFreshnessCheckerNotFresh,
                )
                when (result) {
                    is LoadResult.Success -> {
                        logger.d { "[$applicationName] start(): loadOnce succeeded, consuming services" }
                        ziplineInstance = result.zipline
                        // Now we consume the initializer
                        val initializer = result.zipline.take<Initializer>(Initializer_SERVICE_NAME)
                        // Bind host services before initialization, so plugins can use them in their initializer
                        bindHostServices(result.zipline)
                        trace("start(): calling initializer.initialize()")
                        runCatching { initializer.initialize() }
                            .onSuccess {
                                consumePluginServices(result)
                                runLogInFlowObservers()
                            }
                            .onFailure { e ->
                                logger.e(e) { "[$applicationName] Initializer failed: ${e.message}" }
                                throw e
                            }
                    }

                    is LoadResult.Failure -> {
                        trace("start(): loadOnce failure: ${result.exception}")
                        logger.e(result.exception) { "[$applicationName] Failed to load plugin: ${result.exception.message}" }
                        throw result.exception
                    }
                }
            }

        }
    }

    override suspend fun stop() {
        lifecycleMutex.withLock {
            trace("stop(): entered")
            withContext(ziplineDispatcher.dispatcher) {
                ziplineInstance?.close()
                ziplineInstance = null
                for (service in serviceRegistry.values) {
                    try {
                        trace("stop(): closing service ${service::class.simpleName}")
                        service.close()
                    } catch (_: Exception) {
                        trace("stop(): error closing service ${service::class.simpleName}")
                    }
                }
            }
            serviceRegistry.clear()
            scope.cancel()
            loggedInStateFlow.value = false
            ziplineDispatcher.close()
            trace("stop(): completed")
        }
    }

    override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
        return withContext(ziplineDispatcher.dispatcher + ziplineExceptionHandler) {
            // Create the scope with the current registry
            val scope = PluginServiceScope(serviceRegistry)
            // Execute the block with 'scope' as 'this'
            scope.block()
        }
    }
}