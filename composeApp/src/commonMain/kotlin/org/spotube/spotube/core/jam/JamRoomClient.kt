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
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import org.jetbrains.compose.resources.getString
import org.spotube.spotube.core.network.HttpClientFactory
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_error_banned
import spotube.composeapp.generated.resources.jam_error_connection_failed
import spotube.composeapp.generated.resources.jam_error_connection_lost
import spotube.composeapp.generated.resources.jam_error_kicked
import spotube.composeapp.generated.resources.jam_error_replaced
import spotube.composeapp.generated.resources.jam_error_room_full
import spotube.composeapp.generated.resources.jam_error_room_not_found
import spotube.composeapp.generated.resources.jam_error_server_incompatible
import spotube.composeapp.generated.resources.jam_error_server_unreachable
import spotube.composeapp.generated.resources.settings_jam_server_test_ok
import kotlin.time.Clock

/**
 * WebSocket client for one jam room on the room server (`jam-server/`, a Cloudflare
 * Durable Object per room code).
 *
 * The server keeps the host's latest queue and anchor for late joiners, routes guest
 * commands to the host and is the clock every participant syncs playback against
 * ([clock]). A dropped connection is retried with the same client id, which the
 * server treats as the same participant (a host keeps the room for a grace period).
 *
 * The room is billed per incoming message, and on phones every transmission wakes the
 * cellular radio for seconds, so the steady state sends almost nothing: one keep-alive
 * a minute (answered by the server runtime for free) and a clock re-sync every
 * [CLOCK_RESYNC_INTERVAL_MS]. Reconnect attempts back off to [RECONNECT_BACKOFF_MS]'s
 * last step while the network stays away.
 */
class JamRoomClient(httpClientFactory: HttpClientFactory) {
    private val log = Logger.withTag("JamRoomClient")
    // Nothing here may take the app down: a network failure is a reconnect, not a crash.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e -> log.w(e) { "Unexpected jam connection failure" } })
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }
    private val http = httpClientFactory.create {
        install(WebSockets)
        install(HttpTimeout) { connectTimeoutMillis = CONNECT_TIMEOUT_MS }
    }
    private val plainHttp = httpClientFactory.create {
        install(HttpTimeout) { requestTimeoutMillis = CONNECT_TIMEOUT_MS }
    }

    val clock = JamClock()

    private var session: DefaultWebSocketSession? = null
    private var connectionJob: Job? = null
    private var reader: Deferred<String?>? = null
    private var pingId = 0
    /** When the current socket last received anything; a silent socket is a dead one. */
    private var lastFrameAtMs = 0L
    /** The room's host as last reported by the server. */
    private var hostId: String? = null
    private val pongs = MutableStateFlow(0)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _events = MutableSharedFlow<JamRoomEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<JamRoomEvent> = _events.asSharedFlow()

    // ---------- Public API ----------

    /** Checks that [serverUrl] runs a compatible room server. Returns a latency description. */
    suspend fun testConnection(serverUrl: String): Result<String> = runCatching {
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val response = plainHttp.get(healthUrl(serverUrl))
        if (!response.status.isSuccess()) error(getString(Res.string.jam_error_server_unreachable))
        val health = json.decodeFromString(Health.serializer(), response.bodyAsText())
        if (!health.ok || health.v != PROTOCOL_VERSION) error(getString(Res.string.jam_error_server_incompatible))
        getString(Res.string.settings_jam_server_test_ok, started.elapsedNow().inWholeMilliseconds)
    }

    /**
     * Opens room [code] on [serverUrl] and syncs the clock. Fails with [JamRoomException]
     * when the server refuses (code taken, room not found, banned, …). Once connected,
     * drops are retried in the background until [disconnect] or a refusal.
     */
    suspend fun connect(
        serverUrl: String,
        code: String,
        clientId: String,
        displayName: String,
        create: Boolean): Result<JamRoomEvent.Welcome> {
        disconnect()
        clock.reset()
        val welcome = runCatching { open(serverUrl, code, clientId, displayName, create) }
            .onFailure { e ->
                log.w(e) { "Failed to open jam room $code" }
                _connectionError.value = e.message ?: getString(Res.string.jam_error_connection_failed)
            }
            .getOrElse { return Result.failure(it) }

        connectionJob = scope.launch {
            var current = welcome.second
            var attempt = 0
            while (isActive) {
                val refusal = runCatching { current.await() }.getOrNull()
                ensureActive()
                _isConnected.value = false
                if (refusal != null) {
                    fail(refusal)
                    return@launch
                }
                _connectionError.value = getString(Res.string.jam_error_connection_lost)
                // Rejoin: the room exists now, so even a former host comes back with "join".
                while (isActive) {
                    delay(RECONNECT_BACKOFF_MS[attempt.coerceAtMost(RECONNECT_BACKOFF_MS.lastIndex)])
                    attempt++
                    var reopened = runCatching { open(serverUrl, code, clientId, displayName, create = false) }
                    // Everyone was gone long enough for the room to expire: its host brings it back.
                    if ((reopened.exceptionOrNull() as? JamRoomException)?.code == "room_not_found" && hostId == clientId) {
                        log.i { "Jam room $code expired while offline; recreating it" }
                        reopened = runCatching { open(serverUrl, code, clientId, displayName, create = true) }
                    }
                    val error = reopened.exceptionOrNull()
                    if (error is JamRoomException) {
                        fail(error.code)
                        return@launch
                    }
                    val (event, socket) = reopened.getOrNull() ?: continue
                    attempt = 0
                    current = socket
                    _events.emit(event)
                    break
                }
            }
        }
        return Result.success(welcome.first)
    }

    fun sendQueue(queue: JamQueue) = send(ClientFrame.Queue(queue))

    fun sendAnchor(anchor: JamAnchor) = send(ClientFrame.Anchor(anchor))

    fun sendCommand(command: JamCommand) = send(ClientFrame.Cmd(command))

    fun kick(participantId: String, ban: Boolean) = send(ClientFrame.Kick(participantId, ban))

    suspend fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        hostId = null
        reader?.cancel()
        reader = null
        val current = session
        session = null
        _isConnected.value = false
        _connectionError.value = null
        runCatching { current?.close() }
    }

    // ---------- Internals ----------

    /**
     * Connects, waits for the welcome (or refusal) and syncs the clock. Returns the welcome
     * and the socket's reader, which completes with the server's refusal (if any) on close.
     */
    private suspend fun open(
        serverUrl: String,
        code: String,
        clientId: String,
        displayName: String,
        create: Boolean): Pair<JamRoomEvent.Welcome, Deferred<String?>> {
        val socket = runCatching { http.webSocketSession(roomUrl(serverUrl, code, clientId, displayName, create)) }
            .getOrElse { throw IllegalStateException(getString(Res.string.jam_error_server_unreachable), it) }
        try {
            val welcome = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { awaitWelcome(socket) }
                ?: throw IllegalStateException(getString(Res.string.jam_error_server_unreachable))
            if (welcome.v != PROTOCOL_VERSION) {
                throw IllegalStateException(getString(Res.string.jam_error_server_incompatible))
            }

            session = socket
            hostId = welcome.host
            lastFrameAtMs = now()
            _isConnected.value = true
            _connectionError.value = null
            // Read from here on: a pong's arrival time is part of the clock measurement.
            val socketReader = scope.async { pump(socket) }
            reader = socketReader
            syncClock(socket)
            log.i { "Joined jam room $code as $clientId (host=${welcome.host}, rtt=${clock.rttMs}ms)" }
            return welcome.toEvent() to socketReader
        } catch (e: Throwable) {
            runCatching { socket.close() }
            throw e
        }
    }

    private suspend fun awaitWelcome(socket: DefaultWebSocketSession): ServerFrame.Welcome {
        while (true) {
            val frame = socket.incoming.receiveCatching().getOrNull()
                ?: throw IllegalStateException(getString(Res.string.jam_error_connection_failed))
            when (val decoded = decode(frame)) {
                is ServerFrame.Welcome -> return decoded
                is ServerFrame.Error -> throw JamRoomException(decoded.code, refusalMessage(decoded.code))
                else -> Unit
            }
        }
    }

    /**
     * A burst of clock pings (playback can't sync before the clock is known); waits for
     * their pongs, so the first sync is done when this returns.
     */
    private suspend fun syncClock(socket: DefaultWebSocketSession, pings: Int = INITIAL_PINGS) {
        val target = pongs.value + pings
        repeat(pings) {
            ping(socket)
            delay(PING_SPACING_MS)
        }
        withTimeoutOrNull(CLOCK_SYNC_TIMEOUT_MS) { pongs.first { it >= target } }
    }

    /**
     * Reads frames until the socket closes or fails (a dropped network surfaces here as
     * an exception). Returns the server's refusal, if any.
     */
    private suspend fun pump(socket: DefaultWebSocketSession): String? = withKeepAlive(socket) {
        var refusal: String? = null
        try {
            for (frame in socket.incoming) {
                lastFrameAtMs = now()
                refusal = handle(frame) ?: refusal
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.i { "Jam connection dropped: ${e.message}" }
        }
        refusal
    }

    /**
     * Keeps the socket alive (and NAT mappings open) with free keep-alives, gives up on a
     * socket that has gone silent (a half-open connection never errors by itself), and
     * re-syncs the clock now and then.
     */
    private suspend fun <T> withKeepAlive(
        socket: DefaultWebSocketSession,
        block: suspend () -> T): T {
        val keeper = scope.launch {
            var lastClockSyncMs = now()
            while (isActive) {
                delay(KEEPALIVE_INTERVAL_MS)
                if (now() - lastFrameAtMs > DEAD_AFTER_MS) {
                    log.i { "Jam connection went silent; reconnecting" }
                    runCatching { socket.close() }
                    socket.cancel()
                    return@launch
                }
                runCatching { socket.send(Frame.Text(KEEPALIVE_PING)) }
                if (now() - lastClockSyncMs >= CLOCK_RESYNC_INTERVAL_MS) {
                    lastClockSyncMs = now()
                    runCatching { syncClock(socket, RESYNC_PINGS) }
                }
            }
        }
        return try {
            block()
        } finally {
            keeper.cancel()
        }
    }

    /** Routes one frame; returns an error code when the server refused us. */
    private suspend fun handle(frame: Frame): String? {
        if (frame is Frame.Text && frame.readText() == KEEPALIVE_PONG) return null
        when (val decoded = decode(frame)) {
            is ServerFrame.Pong -> {
                clock.addSample(decoded.c, decoded.s, now())
                pongs.value++
            }
            is ServerFrame.Welcome -> {
                hostId = decoded.host
                _events.emit(decoded.toEvent())
            }
            is ServerFrame.Presence -> {
                hostId = decoded.host
                _events.emit(JamRoomEvent.Presence(decoded.host, decoded.participants.toParticipants(decoded.host)))
            }
            is ServerFrame.Queue -> _events.emit(JamRoomEvent.Queue(decoded.body))
            is ServerFrame.Anchor -> _events.emit(JamRoomEvent.Anchor(decoded.body))
            is ServerFrame.Cmd -> _events.emit(JamRoomEvent.Command(decoded.from, decoded.body))
            is ServerFrame.Error -> return decoded.code
            null -> Unit
        }
        return null
    }

    private suspend fun fail(code: String) {
        val message = refusalMessage(code)
        log.i { "Jam room closed by the server: $code" }
        session = null
        _connectionError.value = message
        _events.emit(JamRoomEvent.Closed(code, message))
    }

    private suspend fun ping(socket: DefaultWebSocketSession) {
        socket.send(Frame.Text(json.encodeToString(ClientFrame.serializer(), ClientFrame.Ping(++pingId, now()))))
    }

    private fun send(frame: ClientFrame) {
        val socket = session ?: return
        val text = json.encodeToString(ClientFrame.serializer(), frame)
        scope.launch {
            runCatching { socket.send(Frame.Text(text)) }
                .onFailure { log.w(it) { "Failed to send jam frame" } }
        }
    }

    private fun decode(frame: Frame): ServerFrame? {
        if (frame !is Frame.Text) return null
        val text = frame.readText()
        return runCatching { json.decodeFromString(ServerFrame.serializer(), text) }
            .onFailure { log.w(it) { "Failed to decode jam frame: ${text.take(200)}" } }
            .getOrNull()
    }

    private suspend fun refusalMessage(code: String): String = when (code) {
        "room_not_found" -> getString(Res.string.jam_error_room_not_found)
        "room_full" -> getString(Res.string.jam_error_room_full)
        "banned" -> getString(Res.string.jam_error_banned)
        "kicked" -> getString(Res.string.jam_error_kicked)
        "replaced" -> getString(Res.string.jam_error_replaced)
        else -> getString(Res.string.jam_error_connection_failed)
    }

    private fun roomUrl(serverUrl: String, code: String, clientId: String, displayName: String, create: Boolean) =
        URLBuilder(serverUrl.trim()).apply {
            protocol = if (protocol == URLProtocol.HTTP || protocol == URLProtocol.WS) URLProtocol.WS else URLProtocol.WSS
            appendPathSegments("v1", "rooms", code)
            parameters.append("id", clientId)
            parameters.append("name", displayName)
            parameters.append("mode", if (create) "create" else "join")
        }.buildString()

    private fun healthUrl(serverUrl: String) =
        URLBuilder(serverUrl.trim()).apply {
            if (protocol == URLProtocol.WS) protocol = URLProtocol.HTTP
            if (protocol == URLProtocol.WSS) protocol = URLProtocol.HTTPS
            appendPathSegments("v1", "health")
        }.buildString()

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        const val PROTOCOL_VERSION = 1
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val INITIAL_PINGS = 6
        private const val RESYNC_PINGS = 3
        private const val PING_SPACING_MS = 100L
        private const val CLOCK_SYNC_TIMEOUT_MS = 3_000L
        /** Clocks drift by a few ms per minute at most; this keeps sync error well under 50 ms. */
        private const val CLOCK_RESYNC_INTERVAL_MS = 10 * 60_000L
        /**
         * As rare as possible (each one wakes a phone's radio) while staying under the
         * 100 s idle timeout of Cloudflare's proxy. A dropped idle socket would only cost
         * a reconnect, so there's no need for margin beyond that.
         */
        private const val KEEPALIVE_INTERVAL_MS = 60_000L
        /** No pong to two keep-alives in a row: the connection is gone. */
        private const val DEAD_AFTER_MS = 2 * KEEPALIVE_INTERVAL_MS + 10_000L
        private const val KEEPALIVE_PING = "ping"
        private const val KEEPALIVE_PONG = "pong"
        /** Quick at first (most drops are a network switch), then slower to spare the battery. */
        private val RECONNECT_BACKOFF_MS = longArrayOf(500, 1_000, 2_000, 4_000, 8_000, 8_000, 15_000, 15_000, 30_000, 60_000)
    }
}

/** Something that happened in the room, for [JamRoomService]. */
sealed interface JamRoomEvent {
    /** (Re)joined the room; carries the retained queue and anchor. */
    data class Welcome(
        val you: String,
        val host: String?,
        val participants: List<JamParticipant>,
        val queue: JamQueue?,
        val anchor: JamAnchor?) : JamRoomEvent

    data class Presence(val host: String?, val participants: List<JamParticipant>) : JamRoomEvent
    data class Queue(val queue: JamQueue) : JamRoomEvent
    data class Anchor(val anchor: JamAnchor) : JamRoomEvent
    data class Command(val from: String, val command: JamCommand) : JamRoomEvent

    /** The server ended our membership ([code]: kicked, banned, …); no retry follows. */
    data class Closed(val code: String, val message: String) : JamRoomEvent
}

/** The server refused to let us into the room. */
class JamRoomException(val code: String, message: String) : Exception(message)

// ---------- Wire frames (see jam-server/src/protocol.ts) ----------

@Serializable
private data class Health(val ok: Boolean = false, val v: Int = 0)

@Serializable
private data class WireParticipant(val id: String, val name: String)

private fun List<WireParticipant>.toParticipants(host: String?) =
    map { JamParticipant(it.id, it.name, isHost = it.id == host) }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("t")
private sealed class ClientFrame {
    @Serializable
    @SerialName("ping")
    data class Ping(val id: Int, val c: Long) : ClientFrame()

    @Serializable
    @SerialName("queue")
    data class Queue(val body: JamQueue) : ClientFrame()

    @Serializable
    @SerialName("anchor")
    data class Anchor(val body: JamAnchor) : ClientFrame()

    @Serializable
    @SerialName("cmd")
    data class Cmd(val body: JamCommand) : ClientFrame()

    @Serializable
    @SerialName("kick")
    data class Kick(val target: String, val ban: Boolean) : ClientFrame()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("t")
private sealed class ServerFrame {
    @Serializable
    @SerialName("welcome")
    data class Welcome(
        val v: Int,
        val you: String,
        val host: String? = null,
        val participants: List<WireParticipant> = emptyList(),
        val queue: JamQueue? = null,
        val anchor: JamAnchor? = null) : ServerFrame() {
        fun toEvent() = JamRoomEvent.Welcome(you, host, participants.toParticipants(host), queue, anchor)
    }

    @Serializable
    @SerialName("pong")
    data class Pong(val id: Int, val c: Long, val s: Long) : ServerFrame()

    @Serializable
    @SerialName("presence")
    data class Presence(val host: String? = null, val participants: List<WireParticipant> = emptyList()) : ServerFrame()

    @Serializable
    @SerialName("queue")
    data class Queue(val body: JamQueue) : ServerFrame()

    @Serializable
    @SerialName("anchor")
    data class Anchor(val body: JamAnchor) : ServerFrame()

    @Serializable
    @SerialName("cmd")
    data class Cmd(val from: String, val body: JamCommand) : ServerFrame()

    @Serializable
    @SerialName("error")
    data class Error(val code: String) : ServerFrame()
}
