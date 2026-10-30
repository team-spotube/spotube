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
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_error_invalid_code
import spotube.composeapp.generated.resources.jam_error_no_broker
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * A jam session over MQTT (star topology, host-authoritative queue).
 *
 * Sync rules:
 * - The queue list and current index are global. [skipNext]/[skipPrevious]/[jumpTo]
 *   from anyone are applied by the host, then broadcast via the retained state topic.
 * - Play/pause is global: anyone's play/pause is sent to the host, which applies it
 *   and broadcasts it; guests follow every change of the host's play state.
 * - Seek is now global: host broadcasts currentPositionMs; guests seek on resume/sync.
 * - Volume and loop are local to each device — never broadcast.
 * - Shuffle is host-only; guests mirror the host's shuffle setting.
 * - Guests can only add to the queue (suggest); the host applies suggestions.
 * - If the host leaves, the participant with the lowest client id takes over.
 */
private data class PlaybackBroadcast(
    val queue: List<QueueEntry>,
    val current: QueueEntry?,
    val shuffle: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long
)

class JamRoomService(
    private val jamClient: JamRoomClient,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsRepository: SettingsRepository) {
    private val log = Logger.withTag("JamRoomService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Throttle host broadcasts to max 1 per 500ms to reduce MQTT traffic & CPU (fixes #3186)
    private val hostBroadcastChannel = Channel<PlaybackBroadcast>(capacity = 10)

    /**
     * Play/pause intent. Transient states (buffering between tracks, loading, completion)
     * keep the last value, so they never pause or resume the room.
     */
    private val playIntent: StateFlow<Boolean> = audioPlayer.playerStateFlow
        .mapNotNull { state ->
            when (state) {
                PlayerState.PLAYING -> true
                PlayerState.PAUSED -> false
                else -> null
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, audioPlayer.playerStateFlow.value == PlayerState.PLAYING)

    private val _role = MutableStateFlow<JamRole?>(null)
    val role: StateFlow<JamRole?> = _role.asStateFlow()

    private val _participants = MutableStateFlow<List<JamParticipant>>(emptyList())
    val participants: StateFlow<List<JamParticipant>> = _participants.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private var localClientId: String = ""
    private var localDisplayName: String = ""

    /** Display name of the local participant. */
    val participantDisplayName: String
        get() = localDisplayName

    /** Stable id of the local participant (stamped on items this device adds). */
    val participantClientId: String
        get() = localClientId

    private var hostBroadcastJob: Job? = null
    private var hostBroadcastProcessorJob: Job? = null

    /** Guest side: last queue snapshot applied to the local player. */
    private var lastAppliedItems: List<JamMediaItem> = emptyList()
    private var lastAppliedIndex = -1

    /**
     * Host side: a song completed, so index changes within this window are
     * auto-advance (guests must not follow). The completion event, the player
     * state change and the media transition arrive as separate flow emissions,
     * so the window covers the whole sequence instead of a single flag.
     */
    private var autoAdvanceDeadlineMs = 0L

    /** Guest side: the room's play state from the last host broadcast (null until the first one). */
    private var lastHostIsPlaying: Boolean? = null
    /** Guest side: last known host position to avoid redundant seeks. */
    private var lastHostPositionMs: Long = -1

    /** Host side: client ids banned for this session. */
    private val bannedClientIds = mutableSetOf<String>()

    private var leaving = false

    init {
        jamClient.isConnected
            .onEach { _isConnected.value = it }
            .launchIn(scope)
        jamClient.connectionError
            .onEach { _connectionError.value = it }
            .launchIn(scope)
        jamClient.state
            .onEach { onRemoteState(it) }
            .launchIn(scope)
        jamClient.commands
            .onEach { onCommand(it) }
            .launchIn(scope)
        jamClient.presence
            .onEach { onPresence(it) }
            .launchIn(scope)
        // A naturally-completed song means the host's next index change is an
        // auto-advance — guests must NOT follow those, only manual skips.
        audioPlayer.completionFlow
            .onEach { autoAdvanceDeadlineMs = now() + AUTO_ADVANCE_WINDOW_MS }
            .launchIn(scope)
        // A guest's own play/pause (UI, notification, headset) goes to the host, which
        // applies it for the whole room. Intent that matches the room's state came from
        // following the host, so it isn't echoed back.
        playIntent
            .onEach { playing ->
                val roomPlaying = lastHostIsPlaying
                if (_role.value == JamRole.Guest && !leaving && roomPlaying != null && playing != roomPlaying) {
                    publishCommand(PlaybackCmd.SetPlaying(playing))
                }
            }
            .launchIn(scope)
    }

    // ---------- Session lifecycle ----------

    suspend fun createRoom(): Result<String> {
        val broker = settingsRepository.userSettings.value.jamBroker
        if (broker.host.isBlank()) {
            return Result.failure(IllegalStateException(getString(Res.string.jam_error_no_broker)))
        }
        val code = JamRoomCode.generate()
        localClientId = newClientId(broker)
        val name = participantName("Host")
        localDisplayName = name

        return jamClient.connect(
            broker = broker,
            code = code,
            clientId = localClientId,
            displayName = name,
            isHost = true).map {
            _role.value = JamRole.Host
            _roomCode.value = code
            _participants.value = listOf(JamParticipant(localClientId, name, isHost = true))
            lastAppliedItems = emptyList()
            lastAppliedIndex = -1
            bannedClientIds.clear()
            leaving = false
            autoAdvanceDeadlineMs = 0L
            lastHostIsPlaying = null
            lastHostPositionMs = -1
            startHostBroadcast()
            // IMMEDIATELY broadcast current playback state so guests sync instantly (fixes #3184)
            broadcastCurrentStateImmediately()
            persistLastCode(code)
            code
        }
    }

    suspend fun joinRoom(code: String): Result<Unit> {
        val broker = settingsRepository.userSettings.value.jamBroker
        if (broker.host.isBlank()) {
            return Result.failure(IllegalStateException(getString(Res.string.jam_error_no_broker)))
        }
        val normalized = JamRoomCode.normalize(code)
        if (!JamRoomCode.isValid(normalized)) {
            return Result.failure(IllegalArgumentException(getString(Res.string.jam_error_invalid_code)))
        }
        localClientId = newClientId(broker)
        val name = participantName("Guest")
        localDisplayName = name

        return jamClient.connect(
            broker = broker,
            code = normalized,
            clientId = localClientId,
            displayName = name,
            isHost = false).map {
            _role.value = JamRole.Guest
            _roomCode.value = normalized
            _participants.value = emptyList()
            lastAppliedItems = emptyList()
            lastAppliedIndex = -1
            leaving = false
            lastHostIsPlaying = null
            lastHostPositionMs = -1
            persistLastCode(normalized)
        }
    }

    suspend fun leaveRoom() {
        leaving = true
        stopHostBroadcast()
        runCatching { jamClient.leavePresence() }
        jamClient.disconnect()
        _role.value = null
        _roomCode.value = null
        _participants.value = emptyList()
        _shuffleEnabled.value = false
        _isConnected.value = false
        lastAppliedItems = emptyList()
        lastAppliedIndex = -1
        bannedClientIds.clear()
        lastHostIsPlaying = null
        lastHostPositionMs = -1
    }

    // ---------- Controls (called from the UI) ----------

    fun skipNext() {
        publishCommand(PlaybackCmd.SkipNext)
    }

    fun skipPrevious() {
        publishCommand(PlaybackCmd.SkipPrevious)
    }

    fun jumpTo(index: Int) {
        publishCommand(PlaybackCmd.JumpTo(index))
    }

    /** Host-only. Applied locally; the queue broadcast carries the new shuffle flag. */
    fun toggleShuffle() {
        if (_role.value != JamRole.Host) return
        scope.launch {
            runCatching { audioPlayer.shuffle(!_shuffleEnabled.value) }
        }
    }

    suspend fun suggestTrack(track: MetadataTrack) {
        jamClient.publishCommand(
            JamMessage.SuggestTrack(
                mediaItem = JamMediaItem.fromTrack(track),
                addedBy = localClientId)
        )
    }

    suspend fun suggestPlaylist(tracks: List<MetadataTrack>) {
        if (tracks.isEmpty()) return
        jamClient.publishCommand(
            JamMessage.SuggestPlaylist(
                tracks = tracks.map(JamMediaItem::fromTrack),
                addedBy = localClientId)
        )
    }

    suspend fun kickParticipant(participantId: String, reason: String = "kicked by host") {
        if (_role.value != JamRole.Host) return
        jamClient.publishCommand(JamMessage.Kick(participantId, reason))
    }

    suspend fun banParticipant(participantId: String) {
        if (_role.value != JamRole.Host) return
        bannedClientIds += participantId
        kickParticipant(participantId, "banned by host")
    }

    // ---------- Host: broadcast ----------

    private fun startHostBroadcast() {
        if (hostBroadcastJob?.isActive == true) return

        // Producer: collect playback state changes and send to channel (debounced)
        hostBroadcastJob = scope.launch {
            combine(
                audioPlayerQueue.queueFlow,
                audioPlayerQueue.currentQueueEntryFlow,
                audioPlayer.shuffleModeFlow,
                playIntent,
                audioPlayer.positionFlow) { queue, current, shuffle, isPlaying, position ->
                PlaybackBroadcast(queue, current, shuffle, isPlaying, position.inWholeMilliseconds)
            }
                .distinctUntilChanged { old, new ->
                    // Only emit if meaningfully changed: queue content, index, play state, or position > 1s diff
                    old.queue == new.queue &&
                    old.current == new.current &&
                    old.shuffle == new.shuffle &&
                    old.isPlaying == new.isPlaying &&
                    kotlin.math.abs(old.positionMs - new.positionMs) < 1000
                }
                .debounce(500) // max 2 broadcasts/sec (fixes #3186 overheating)
                .onEach { broadcast ->
                    if (_role.value != JamRole.Host) return@onEach
                    hostBroadcastChannel.trySend(broadcast)
                }
                .launchIn(this)
        }

        // Consumer: process broadcasts sequentially, serialize MQTT publishes
        hostBroadcastProcessorJob = scope.launch {
            for (broadcast in hostBroadcastChannel) {
                if (_role.value != JamRole.Host) break
                val index = if (broadcast.current != null) {
                    broadcast.queue.indexOfFirst { it.matchesEntry(broadcast.current) }
                } else {
                    -1
                }
                _shuffleEnabled.value = broadcast.shuffle
                jamClient.publishState(
                    JamMessage.QueueState(
                        items = broadcast.queue.map(JamMediaItem::fromQueueEntry),
                        currentIndex = index.coerceAtLeast(0),
                        shuffleEnabled = broadcast.shuffle,
                        follow = now() > autoAdvanceDeadlineMs,
                        isPlaying = broadcast.isPlaying,
                        currentPositionMs = broadcast.positionMs)
                )
            }
        }
    }

    private fun stopHostBroadcast() {
        hostBroadcastJob?.cancel()
        hostBroadcastJob = null
        hostBroadcastProcessorJob?.cancel()
        hostBroadcastProcessorJob = null
        hostBroadcastChannel.close()
    }

    /** Immediately broadcast current state (used on room creation and host takeover) */
    private fun broadcastCurrentStateImmediately() {
        scope.launch {
            val currentQueue = audioPlayerQueue.queueFlow.value
            val currentEntry = audioPlayerQueue.currentQueueEntryFlow.value
            val shuffle = audioPlayer.shuffleModeFlow.value
            val isPlaying = audioPlayer.playerStateFlow.value == PlayerState.PLAYING
            val positionMs = audioPlayer.positionFlow.value.inWholeMilliseconds

            if (currentEntry != null) {
                val index = currentQueue.indexOfFirst { it.matchesEntry(currentEntry) }
                jamClient.publishState(
                    JamMessage.QueueState(
                        items = currentQueue.map(JamMediaItem::fromQueueEntry),
                        currentIndex = index.coerceAtLeast(0),
                        shuffleEnabled = shuffle,
                        follow = true,
                        isPlaying = isPlaying,
                        currentPositionMs = positionMs)
                )
                // If playing, also send explicit seek to ensure guests start at exact position
                if (isPlaying) {
                    jamClient.publishCommand(JamMessage.PlaybackCommand(PlaybackCmd.SeekTo(positionMs)))
                }
            }
        }
    }

    // ---------- Guest: apply remote state ----------

    private suspend fun onRemoteState(state: JamMessage.QueueState) {
        if (_role.value != JamRole.Guest) return
        if (leaving) return

        _shuffleEnabled.value = state.shuffleEnabled
        runCatching { audioPlayer.shuffle(state.shuffleEnabled) }

        // Play/pause is room-wide: follow every change of the host's play state.
        // On resume (isPlaying becomes true), seek to host's currentPositionMs first (fixes #3181).
        if (state.isPlaying != lastHostIsPlaying) {
            lastHostIsPlaying = state.isPlaying
            if (state.isPlaying) {
                // Host resumed: seek to their position BEFORE playing
                if (state.currentPositionMs > 0 && state.currentPositionMs != lastHostPositionMs) {
                    lastHostPositionMs = state.currentPositionMs
                    runCatching {
                        audioPlayer.seekTo(Duration.milliseconds(state.currentPositionMs))
                    }.onFailure { log.w(it) { "Failed to seek to host position on resume" } }
                }
                if (state.isPlaying != playIntent.value) {
                    runCatching { audioPlayer.play() }
                        .onFailure { log.w(it) { "Failed to follow the host's play state" } }
                }
            } else {
                // Host paused: just pause locally
                if (state.isPlaying != playIntent.value) {
                    runCatching { audioPlayer.pause() }
                        .onFailure { log.w(it) { "Failed to follow the host's pause" } }
                }
            }
        }

        val items = state.items.filter { it.trackId.isNotBlank() || it.url.isNotBlank() }

        if (items != lastAppliedItems) {
            val previous = lastAppliedItems
            lastAppliedItems = items
            val guestCurrent = audioPlayerQueue.currentQueueEntryFlow.value
            val guestIndex = items.indexOfFirst { it.matchesEntry(guestCurrent) }

            // The host only appended items (e.g. accepted suggestions): merge
            // them in without resetting playback or the guest's position.
            if (previous.isNotEmpty() && items.size > previous.size &&
                items.take(previous.size) == previous && guestIndex >= 0
            ) {
                lastAppliedIndex = guestIndex
                val appended = items.drop(previous.size)
                runCatching {
                    audioPlayerQueue.addAllToQueue(appended.map { it.toQueueEntry() })
                }.onFailure { log.w(it) { "Failed to append jam queue items" } }
                return
            }

            // Full re-sync. Keep the guest's current track when it still exists
            // in the synced queue; otherwise take the host's position.
            val startIndex = if (guestIndex >= 0) guestIndex
                else state.currentIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
            lastAppliedIndex = startIndex
            runCatching {
                audioPlayerQueue.load(
                    entries = items.map { it.toQueueEntry() },
                    autoPlay = state.isPlaying,
                    startPosition = startIndex)
            }.onFailure { log.w(it) { "Failed to apply jam queue" } }
            return
        }

        // Same queue content: only follow the host when it moved manually
        // (skip/jump). Natural auto-advance keeps everyone where they are.
        if (state.follow && state.currentIndex != lastAppliedIndex) {
            lastAppliedIndex = state.currentIndex
            runCatching {
                audioPlayerQueue.jumpTo(state.currentIndex.coerceAtLeast(0), autoPlay = false)
            }.onFailure { log.w(it) { "Failed to follow jam queue index" } }
        }

        // Periodic position sync: if host position drifted > 2s, correct it (fixes #3181 drift)
        if (state.isPlaying && state.currentPositionMs > 0) {
            val guestPosition = audioPlayer.positionFlow.value.inWholeMilliseconds
            if (kotlin.math.abs(guestPosition - state.currentPositionMs) > 2000) {
                lastHostPositionMs = state.currentPositionMs
                runCatching {
                    audioPlayer.seekTo(Duration.milliseconds(state.currentPositionMs))
                }.onFailure { log.w(it) { "Failed to correct position drift" } }
            }
        }
    }

    // ---------- Incoming commands ----------

    private suspend fun onCommand(message: JamMessage) {
        when (message) {
            is JamMessage.PlaybackCommand -> {
                if (_role.value != JamRole.Host) return
                applyCommand(message.command)
            }

            is JamMessage.SuggestTrack -> {
                if (_role.value == JamRole.Host) {
                    acceptSuggestion(listOf(message.mediaItem.copy(addedBy = message.addedBy)))
                }
            }

            is JamMessage.SuggestPlaylist -> {
                if (_role.value == JamRole.Host) {
                    acceptSuggestion(message.tracks.map { it.copy(addedBy = message.addedBy) })
                }
            }

            is JamMessage.Kick -> {
                if (_role.value == JamRole.Guest && message.participantId == localClientId) {
                    log.i { "Kicked from jam room: ${message.reason}" }
                    leaveRoom()
                }
            }

            else -> Unit
        }
    }

    private suspend fun applyCommand(command: PlaybackCmd) {
        when (command) {
            // Applied locally; the host broadcast carries the new play state to every guest
            is PlaybackCmd.SetPlaying -> runCatching {
                if (command.playing) audioPlayer.play() else audioPlayer.pause()
            }
            PlaybackCmd.SkipNext -> runCatching { audioPlayer.skipToNext() }
            PlaybackCmd.SkipPrevious -> runCatching { audioPlayer.skipToPrevious() }
            is PlaybackCmd.JumpTo -> runCatching { audioPlayer.jumpTo(command.index) }
            is PlaybackCmd.SeekTo -> runCatching {
                audioPlayer.seekTo(Duration.milliseconds(command.positionMs))
            }
        }
    }

    private suspend fun acceptSuggestion(items: List<JamMediaItem>) {
        if (items.isEmpty()) return
        log.i { "Accepting ${items.size} suggested item(s) into the jam queue" }
        runCatching {
            audioPlayerQueue.addAllToQueue(items.map { it.toQueueEntry() })
        }
    }

    // ---------- Presence & host takeover ----------

    private fun onPresence(all: Map<String, JamPresence>) {
        if (_role.value == null) return
        val live = all.values.filter { !it.left }
        _participants.value = live
            .sortedBy { it.clientId }
            .map { JamParticipant(it.clientId, it.displayName, it.isHost) }

        // Host-side: auto-kick banned participants that rejoin.
        if (_role.value == JamRole.Host) {
            live.filter { it.clientId in bannedClientIds }.forEach { banned ->
                scope.launch { kickParticipant(banned.clientId, "banned by host") }
            }
            return
        }

        val host = live.firstOrNull { it.isHost }
        if (host != null) return

        // Host left: the lowest client id takes over (deterministic, clock-free).
        val candidate = live.minByOrNull { it.clientId } ?: return
        if (candidate.clientId != localClientId) return

        scope.launch {
            delay(HOST_TAKEOVER_DELAY_MS)
            val stillNoHost = jamClient.presence.value.values.none { !it.left && it.isHost }
            if (!stillNoHost || leaving || _role.value != JamRole.Guest) return@launch
            log.i { "Taking over as jam host (previous host left)" }
            _role.value = JamRole.Host
            jamClient.claimHost()
            startHostBroadcast()
            broadcastCurrentStateImmediately()
        }
    }

    // ---------- Helpers ----------

    private fun publishCommand(command: PlaybackCmd) {
        scope.launch {
            jamClient.publishCommand(JamMessage.PlaybackCommand(command))
        }
    }

    private fun participantName(fallbackPrefix: String): String {
        val configured = settingsRepository.userSettings.value.jamParticipantName
        return configured.ifBlank { "$fallbackPrefix-${Random.nextInt(1000, 9999)}" }
    }

    private fun newClientId(broker: org.spotube.spotube.modules.settings.JamBroker): String {
        val suffix = buildString(6) {
            val chars = "0123456789abcdef"
            repeat(6) { append(chars[Random.nextInt(chars.length)]) }
        }
        return "${broker.clientIdPrefix.ifBlank { "spotube" }}-$suffix"
    }

    private fun persistLastCode(code: String) {
        scope.launch {
            runCatching {
                val settings = settingsRepository.userSettings.value
                if (settings.lastJamCode != code) {
                    settingsRepository.updateSettings(settings.copy(lastJamCode = code))
                }
            }
        }
    }

    private fun JamMediaItem.toQueueEntry(): QueueEntry = when {
        trackId.isNotBlank() -> QueueEntry.StreamingTrack(
            track = MetadataTrack(
                id = trackId,
                title = title,
                durationMs = durationMs,
                trackNumber = null,
                discNumber = null,
                artists = listOf(
                    MetadataArtist.Basic(id = "", name = artist, thumbnails = emptyList(), externalUri = null)
                ),
                album = null,
                thumbnails = coverUrl.takeIf { it.isNotBlank() }
                    ?.let { url -> listOf(Thumbnail(url = url, width = 0, height = 0)) },
                explicit = null,
                popularity = null,
                isrcCode = null,
                externalUri = null),
            url = "",
            protocol = runCatching { StreamProtocol.valueOf(protocol.ifBlank { "PROGRESSIVE" }) }
                .getOrDefault(StreamProtocol.PROGRESSIVE),
            addedBy = addedBy)

        else -> QueueEntry.LocalTrack(
            name = title,
            artists = artist.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            duration = durationMs,
            album = album.ifBlank { null },
            coverBytes = null,
            url = url,
            addedBy = addedBy)
    }

    private fun JamMediaItem.matchesEntry(entry: QueueEntry?): Boolean {
        if (entry == null) return false
        return when (entry) {
            is QueueEntry.StreamingTrack ->
                trackId.isNotBlank() && entry.track.id == trackId

            is QueueEntry.LocalTrack ->
                url.isNotBlank() && entry.url == url && entry.name == title
        }
    }

    private fun QueueEntry.matchesEntry(other: QueueEntry): Boolean = when {
        this is QueueEntry.StreamingTrack && other is QueueEntry.StreamingTrack ->
            this.track.id == other.track.id

        this is QueueEntry.LocalTrack && other is QueueEntry.LocalTrack ->
            this.url == other.url && this.name == other.name

        else -> false
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        private const val HOST_TAKEOVER_DELAY_MS = 1_500L
        private const val AUTO_ADVANCE_WINDOW_MS = 2_000L
    }
}