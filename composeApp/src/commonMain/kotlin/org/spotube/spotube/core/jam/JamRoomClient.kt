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

package org.spotube.spotube.core.jam

import co.touchlab.kermit.Logger
import com.ditchoom.buffer.Charset
import com.ditchoom.buffer.codec.asReadBuffer
import com.ditchoom.buffer.toReadBuffer
import com.ditchoom.mqtt.client.ConnectionState
import com.ditchoom.mqtt.client.MqttClient
import com.ditchoom.mqtt.connection.MqttConnectionOptions
import com.ditchoom.mqtt.controlpacket.OpaquePublishPayloadCodec
import com.ditchoom.mqtt.controlpacket.QualityOfService
import com.ditchoom.mqtt.controlpacket.TopicName
import com.ditchoom.mqtt.controlpacket.WillConfig
import com.ditchoom.mqtt5.controlpacket.ConnectionRequest
import org.spotube.spotube.modules.settings.JamBroker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_error_broker_host_empty
import spotube.composeapp.generated.resources.jam_error_broker_unreachable
import spotube.composeapp.generated.resources.jam_error_connection_failed
import spotube.composeapp.generated.resources.jam_error_connection_lost
import spotube.composeapp.generated.resources.jam_error_no_broker
import spotube.composeapp.generated.resources.settings_jam_broker_test_ok
import org.koin.core.component.KoinComponent
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Thin facade over the Ditchoom MQTT 5 client for one jam room.
 *
 * Topics (room code `C`):
 * - `spotube/jam/{C}/state`   retained  — [JamMessage.QueueState] (host -> everyone)
 * - `spotube/jam/{C}/cmd`     volatile  — commands/suggestions (everyone -> host, host -> guest)
 * - `spotube/jam/{C}/presence/{clientId}` retained — [JamPresence], with a Last Will
 *   (`left = true`) so a dropped client disappears from the room automatically.
 *
 * The library keeps the connection alive (auto-reconnect + backoff); this class only
 * re-establishes the room subscription and re-publishes presence after a reconnect.
 * Exponential backoff on reconnect prevents CPU/battery drain on flaky networks (fixes #3186).
 */
class JamRoomClient : KoinComponent {
    private val log = Logger.withTag("JamRoomClient")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private var client: MqttClient? = null
    private var reconnectJob: Job? = null

    private var roomCode: String? = null
    private var localPresence: JamPresence? = null
    private var currentBroker: JamBroker? = null
    private var currentClientId: String = ""

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _state = MutableSharedFlow<JamMessage.QueueState>(replay = 1, extraBufferCapacity = 8)
    val state: SharedFlow<JamMessage.QueueState> = _state.asSharedFlow()

    private val _commands = MutableSharedFlow<JamMessage>(extraBufferCapacity = 32)
    val commands: SharedFlow<JamMessage> = _commands.asSharedFlow()

    private val _presence = MutableStateFlow<Map<String, JamPresence>>(emptyMap())
    val presence: StateFlow<Map<String, JamPresence>> = _presence.asStateFlow()

    // ---------- Public API ----------

    /** One-off connection check used by the settings screen. Returns latency description. */
    suspend fun testConnection(broker: JamBroker): Result<String> {
        if (broker.host.isBlank()) return Result.failure(IllegalArgumentException(getString(Res.string.jam_error_broker_host_empty)))
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        return runCatching {
            val client = startClient(broker, clientId = "${broker.clientIdPrefix}-test")
            try {
                withTimeout(broker.connectionTimeoutSeconds.seconds) {
                    client.awaitConnectivity()
                }
                getString(Res.string.settings_jam_broker_test_ok, started.elapsedNow().inWholeMilliseconds)
            } finally {
                runCatching { client.shutdown(sendDisconnect = true, drain = false) }
            }
        }
    }

    /** Connects to [code] on [broker] and starts routing room messages. */
    suspend fun connect(
        broker: JamBroker,
        code: String,
        clientId: String,
        displayName: String,
        isHost: Boolean): Result<Unit> {
        disconnect()
        if (broker.host.isBlank()) {
            return Result.failure(IllegalArgumentException(getString(Res.string.jam_error_no_broker)))
        }
        currentBroker = broker
        currentClientId = clientId
        return runCatching {
            roomCode = code
            localPresence = JamPresence(
                clientId = clientId,
                displayName = displayName,
                isHost = isHost,
                left = false)

            val mqtt = startClient(broker, clientId)
            client = mqtt
            try {
                withTimeout(broker.connectionTimeoutSeconds.seconds) {
                    // connectionState replays its current value, unlike awaitConnectivity's
                    // broadcast flow, which loses a CONNACK that lands before it is collected.
                    mqtt.connectionState.first { it is ConnectionState.Connected }
                }
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException(
                    getString(Res.string.jam_error_broker_unreachable, broker.host, broker.port), e)
            }
            _connectionError.value = null
            log.i { "Connected to ${broker.host}:${broker.port} room=$code as $clientId" }

            mqtt.connectionState
                .onEach { onConnectionStateChanged(it) }
                .launchIn(scope)
            Unit
        }.onFailure { e ->
            log.w(e) { "Failed to connect to jam broker" }
            _connectionError.value = e.message ?: getString(Res.string.jam_error_connection_failed)
            _isConnected.value = false
            runCatching { client?.shutdown(sendDisconnect = true, drain = false) }
            client = null
        }
    }

    suspend fun publishState(state: JamMessage.QueueState) {
        val code = roomCode ?: return
        publishJson(stateTopic(code), json.encodeToString(JamMessage.QueueState.serializer(), state), retain = true)
    }

    suspend fun publishCommand(message: JamMessage) {
        val code = roomCode ?: return
        publishJson(cmdTopic(code), json.encodeToString(JamMessage.serializer(), message), retain = false)
    }

    /** Publishes our own (retained) presence. Re-published after every reconnect. */
    suspend fun publishPresence() {
        val code = roomCode ?: return
        val presence = localPresence ?: return
        publishJson(
            presenceTopic(code, presence.clientId),
            json.encodeToString(JamPresence.serializer(), presence),
            retain = true)
    }

    /** Marks us as the host in presence (host takeover). */
    suspend fun claimHost() {
        val presence = localPresence ?: return
        localPresence = presence.copy(isHost = true)
        publishPresence()
    }

    /** Graceful leave: publish `left = true` before disconnecting. */
    suspend fun leavePresence() {
        val presence = localPresence ?: return
        val code = roomCode ?: return
        runCatching {
            publishJson(
                presenceTopic(code, presence.clientId),
                json.encodeToString(JamPresence.serializer(), presence.copy(left = true)),
                retain = true)
        }
    }

    suspend fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        roomCode = null
        localPresence = null
        currentBroker = null
        currentClientId = ""
        _isConnected.value = false
        _presence.value = emptyMap()
        val current = client
        client = null
        runCatching { current?.shutdown(sendDisconnect = true, drain = false) }
    }

    // ---------- Internals ----------

    private suspend fun startClient(broker: JamBroker, clientId: String): MqttClient {
        val connection = MqttConnectionOptions.SocketConnection(
            host = broker.host,
            port = broker.port,
            tlsEnabled = broker.useTls,
            connectionTimeout = broker.connectionTimeoutSeconds.seconds)
        val code = roomCode ?: "unset"
        val will = WillConfig.Enabled(
            topic = TopicName.fromOrThrow(presenceTopic(code, clientId)),
            payload = json.encodeToString(
                JamPresence.serializer(),
                JamPresence(clientId, localPresence?.displayName ?: clientId, isHost = false, left = true)).toReadBuffer(Charset.UTF8),
            qos = QualityOfService.AT_LEAST_ONCE,
            retain = true)
        val request = ConnectionRequest(
            clientId = clientId,
            keepAliveSeconds = broker.keepAliveSeconds,
            cleanStart = true,
            userName = broker.username,
            password = broker.password,
            will = will)
        val persistence = request.controlPacketFactory.defaultPersistence(inMemory = true)
        val brokerRef = persistence.addBroker(connection, request)
        return MqttClient.start(scope = scope, broker = brokerRef, persistence = persistence)
    }

    private fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                _isConnected.value = true
                _connectionError.value = null
                // Reset reconnect attempt counter on successful connection
                reconnectAttempt = 0
                scope.launch {
                    // Every connection (initial + reconnects) must re-establish the
                    // broker-side subscription (clean session) and re-publish our
                    // retained presence to clear any Last Will. Re-subscribing with
                    // the same filter replaces the previous dispatcher handler.
                    subscribeRoom()
                    publishPresence()
                }
            }

            ConnectionState.Disconnected, ConnectionState.Handshaking -> {
                _isConnected.value = false
                // Schedule exponential backoff reconnect (fixes #3186 reconnection storm)
                if (state == ConnectionState.Disconnected && roomCode != null && currentBroker != null) {
                    scheduleReconnect()
                }
            }

            else -> {
                _isConnected.value = false
                scope.launch {
                    val message = getString(Res.string.jam_error_connection_lost)
                    if (!_isConnected.value) _connectionError.value = message
                }
            }
        }
    }

    private var reconnectAttempt = 0
    private const val MAX_RECONNECT_ATTEMPTS = 10
    private const val BASE_RECONNECT_DELAY_MS = 1000L
    private const val MAX_RECONNECT_DELAY_MS = 30_000L

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
                log.w { "Max reconnect attempts reached, giving up" }
                _connectionError.value = getString(Res.string.jam_error_connection_lost)
                return@launch
            }

            val delayMs = (BASE_RECONNECT_DELAY_MS * (2.0.pow(reconnectAttempt))).toLong().coerceAtMost(MAX_RECONNECT_DELAY_MS)
            // Add jitter (±20%) to avoid thundering herd
            val jitter = (delayMs * 0.2 * (Random.nextDouble() * 2 - 1)).toLong()
            val finalDelay = (delayMs + jitter).coerceAtMost(MAX_RECONNECT_DELAY_MS)

            log.i { "Scheduling reconnect attempt ${reconnectAttempt + 1} in ${finalDelay}ms" }
            delay(finalDelay)

            if (roomCode == null || currentBroker == null) return@launch

            reconnectAttempt++
            val broker = currentBroker!!
            val clientId = currentClientId

            runCatching {
                val mqtt = startClient(broker, clientId)
                client = mqtt
                mqtt.connectionState
                    .onEach { onConnectionStateChanged(it) }
                    .launchIn(scope)
                mqtt.connectionState.first { it is ConnectionState.Connected }
            }.onFailure { e ->
                log.w(e) { "Reconnect attempt $reconnectAttempt failed" }
                // Schedule next attempt
                scheduleReconnect()
            }
        }
    }

    private suspend fun subscribeRoom() {
        val mqtt = client ?: return
        val code = roomCode ?: return
        val operation = mqtt.subscribe(
            roomFilter(code),
            OpaquePublishPayloadCodec,
            QualityOfService.AT_LEAST_ONCE) { publish, payload ->
            route(publish.topic.toString(), payload)
        }
        runCatching { operation.subAck.await() }
            .onFailure { log.w(it) { "Subscribe ack failed for room $code" } }
    }

    private fun route(topic: String, payload: com.ditchoom.mqtt.controlpacket.OpaquePublishPayload) {
        val text = runCatching {
            val buffer = payload.handle.asReadBuffer()
            buffer.readString(buffer.remaining(), Charset.UTF8)
        }.getOrElse { e ->
            log.w(e) { "Failed to read jam payload on $topic" }
            return
        }

        runCatching {
            when {
                topic.endsWith("/state") -> {
                    _state.tryEmit(json.decodeFromString(JamMessage.QueueState.serializer(), text))
                }

                topic.endsWith("/cmd") -> {
                    _commands.tryEmit(json.decodeFromString(JamMessage.serializer(), text))
                }

                topic.contains("/presence/") -> {
                    val presence = json.decodeFromString(JamPresence.serializer(), text)
                    _presence.value = _presence.value + (presence.clientId to presence)
                }
            }
        }.onFailure { e ->
            log.w(e) { "Failed to decode jam message on $topic: $text" }
        }
    }

    private suspend fun publishJson(topic: String, payload: String, retain: Boolean) {
        val mqtt = client ?: return
        runCatching {
            mqtt.publish(
                topicName = topic,
                qos = QualityOfService.AT_LEAST_ONCE,
                payload = payload.toReadBuffer(Charset.UTF8),
                retain = retain)
        }.onFailure { e ->
            log.w(e) { "Failed to publish to $topic" }
        }
    }

    private fun stateTopic(code: String) = "spotube/jam/$code/state"
    private fun cmdTopic(code: String) = "spotube/jam/$code/cmd"
    private fun presenceTopic(code: String, clientId: String) = "spotube/jam/$code/presence/$clientId"
    private fun roomFilter(code: String) = "spotube/jam/$code/#"
}
