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

package dev.krtirtho.spotube.core.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * WebSocket client for controlling a remote Spotube instance.
 * Connects to the remote device's `/control` endpoint and sends commands.
 */
class RemoteControlClient {
    private val logger = Logger.withTag("RemoteControlClient")
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = HttpClient {
        install(WebSockets)
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }

    private var session: WebSocketSession? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latestPlayerState = MutableStateFlow<RemoteControlEvent.PlayerState?>(null)
    val latestPlayerState: StateFlow<RemoteControlEvent.PlayerState?> = _latestPlayerState.asStateFlow()

    private val _latestQueue = MutableStateFlow<RemoteControlEvent.QueueUpdated?>(null)
    val latestQueue: StateFlow<RemoteControlEvent.QueueUpdated?> = _latestQueue.asStateFlow()

    suspend fun connect(host: String, port: Int, deviceId: String, deviceName: String) {
        if (_connectionState.value is ConnectionState.Connected) {
            logger.w { "Already connected" }
            return
        }

        _connectionState.value = ConnectionState.Connecting
        try {
            session = httpClient.webSocketSession {
                url("ws://$host:$port/control")
                header("X-Device-Id", deviceId)
                header("X-Device-Name", deviceName)
            }

            logger.i { "WebSocket connected to $host:$port, waiting for authorization..." }

            // Start receiving messages in a separate coroutine
            scope.launch {
                receiveLoop(host, port)
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to connect to $host:$port" }
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            disconnect()
        }
    }

    private suspend fun receiveLoop(host: String, port: Int) {
        val currentSession = session ?: return
        logger.d { "Starting receive loop for $host:$port" }
        try {
            for (frame in currentSession.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.d { "Received frame: $text" }
                        try {
                            val event = json.decodeFromString(RemoteControlEvent.serializer(), text)
                            logger.d { "Parsed event: $event" }
                            when (event) {
                                is RemoteControlEvent.Connected -> {
                                    logger.i { "Connection authorized by server" }
                                    _connectionState.value = ConnectionState.Connected(host, port)
                                }
                                is RemoteControlEvent.WaitingForPermission -> {
                                    logger.i { "Waiting for permission: ${event.message}" }
                                    // Keep showing connecting state
                                }
                                else -> {
                                    // Only store state updates after connection is established
                                    if (_connectionState.value is ConnectionState.Connected) {
                                        when (event) {
                                            is RemoteControlEvent.PlayerState -> {
                                                _latestPlayerState.value = event
                                            }
                                            is RemoteControlEvent.QueueUpdated -> {
                                                _latestQueue.value = event
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.w(e) { "Failed to parse message: $text" }
                        }
                    }
                    is Frame.Close -> {
                        logger.i { "WebSocket closed by server" }
                        _connectionState.value = ConnectionState.Disconnected
                        break
                    }
                    else -> {}
                }
            }
            logger.d { "Receive loop exited normally" }
        } catch (e: Exception) {
            logger.e(e) { "Error in receive loop" }
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection lost")
        }
    }

    suspend fun sendCommand(command: RemoteControlCommand) {
        val currentSession = session ?: run {
            logger.w { "Not connected" }
            return
        }

        val envelope = CommandEnvelope(
            commandId = randomShortId(),
            command = command,
        )

        try {
            val text = json.encodeToString(CommandEnvelope.serializer(), envelope)
            currentSession.send(Frame.Text(text))
            logger.d { "Sent command: $command" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to send command" }
            _connectionState.value = ConnectionState.Error(e.message ?: "Send failed")
        }
    }

    suspend fun disconnect() {
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnecting"))
        session = null
        _connectionState.value = ConnectionState.Disconnected
        logger.i { "Disconnected" }
    }

    private fun randomShortId(): String {
        val chars = "0123456789abcdef"
        return buildString(8) {
            repeat(8) {
                append(chars[kotlin.random.Random.nextInt(chars.length)])
            }
        }
    }
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val host: String, val port: Int) : ConnectionState
    data class Error(val message: String) : ConnectionState
}