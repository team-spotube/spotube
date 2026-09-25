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

package dev.krtirtho.spotube.core.server

import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.remote.RemoteControlHandler
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent

class LocalServer(
    settingsViewModel: SettingsViewModel,
    private val streamingUrlRepository: StreamingUrlRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val cacheManager: CacheManager,
    private val remoteControlHandler: RemoteControlHandler,
) : KoinComponent {

    val logger by injectLogger<LocalServer>()
    private val httpClient = HttpClient {
        // A stalled upstream connection must not wedge the CIO dispatcher thread
        // forever. Only the connect phase is bounded — the proxy streams long
        // audio bodies, so request/socket timeouts would cut playback short.
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val serverMutex = Mutex()
    private var portWatcher: Job? = null
    private val serverState = MutableStateFlow<EmbeddedServer<*, *>?>(null)

    val server: StateFlow<EmbeddedServer<*, *>?> = serverState.asStateFlow()

    private val activePort = MutableStateFlow<Int?>(null)
    val port = activePort.asStateFlow()

    private val activeHost = MutableStateFlow<String?>(null)

    val baseUrl = activePort.map { port ->
        port?.let { "http://$HOST_LOCAL:$it" }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    private val cachedCacheEnabled = MutableStateFlow(false)

    private val streamProxy by lazy {
        StreamProxy(
            httpClient = httpClient,
            streamingUrlRepository = streamingUrlRepository,
            cacheManager = cacheManager,
            audioPlayerQueue = audioPlayerQueue,
            isCachingEnabled = { cachedCacheEnabled.value },
            activePort = { activePort.value },
            scope = scope,
        )
    }

    companion object {
        private const val HOST_LOCAL = "127.0.0.1"
        private const val HOST_LAN = "0.0.0.0"
    }

    init {
        logger.d { "Starting playback proxy port watcher" }
        portWatcher = scope.launch {
            settingsViewModel.settingsState
                .mapNotNull { it?.let { s -> s.playbackProxyServerPort to s.allowRemoteControl } }
                .distinctUntilChanged()
                .collectLatest { (port, allowRemoteControl) ->
                    logger.d { "Observed server config change: port=$port, allowRemoteControl=$allowRemoteControl" }
                    restartServer(port, allowRemoteControl)
                }
        }
        scope.launch {
            settingsViewModel.settingsState
                .collect { settings ->
                    if (settings != null) {
                        cachedCacheEnabled.value = settings.enableMusicCaching
                    }
                }
        }
    }

    @Suppress("unused")
    suspend fun stop() {
        logger.i { "Stopping playback proxy server and watcher" }
        serverMutex.withLock {
            stopServerLocked()
        }
        portWatcher?.cancel()
        portWatcher = null
        runCatching { httpClient.close() }
            .onFailure { throwable ->
                logger.w(throwable) { "Failed to close proxy HTTP client cleanly" }
            }
        scope.cancel()
        logger.d { "Playback proxy server stopped" }
    }

    private suspend fun restartServer(port: Int, allowRemoteControl: Boolean) {
        val host = if (allowRemoteControl) HOST_LAN else HOST_LOCAL
        serverMutex.withLock {
            if (serverState.value != null && activePort.value == port && activeHost.value == host) {
                logger.v { "Playback proxy server already running on $host:$port; skipping restart" }
                return
            }

            logger.d { "Restarting playback proxy server on $host:$port (remoteControl=$allowRemoteControl)" }
            stopServerLocked()

            serverState.value = embeddedServer(
                factory = CIO,
                host = host,
                port = port,
                module = { configureRoutes() }
            ).also { engine ->
                engine.start(wait = false)
            }
            activePort.value = port
            activeHost.value = host
            logger.i { "Playback proxy server started at ${baseUrl.value ?: "http://$host:$port"}" }
        }
    }

    private fun stopServerLocked() {
        serverState.value?.let { engine ->
            logger.d { "Stopping playback proxy server on port ${activePort.value}" }
            runCatching { engine.stop(gracePeriodMillis = 1_000, timeoutMillis = 3_000) }
                .onSuccess {
                    logger.d { "Playback proxy server stopped cleanly" }
                }
                .onFailure { throwable ->
                    logger.w(throwable) { "Failed to stop playback proxy server cleanly" }
                }
        }
        serverState.value = null
        activePort.value = null
        activeHost.value = null
    }

    private fun Application.configureRoutes() {
        install(WebSockets) {
            pingPeriodMillis = 30_000L
            timeoutMillis = 60_000L
            maxFrameSize = 10L * 1024 * 1024
            masking = false
        }
        routing {
            get("/health") {
                call.respondText("ok")
            }

            head("/stream/{trackId}") {
                streamProxy.handleStreamRequest(call, HttpMethod.Head)
            }

            get("/stream/{trackId}") {
                streamProxy.handleStreamRequest(call, HttpMethod.Get)
            }

            get("/manifest/{trackId}") {
                streamProxy.handleManifestRequest(call)
            }

            get("/segment/{trackId}") {
                streamProxy.handleSegmentRequest(call)
            }

            webSocket("/control") {
                remoteControlHandler.handleConnection(this)
            }
        }
    }
}
