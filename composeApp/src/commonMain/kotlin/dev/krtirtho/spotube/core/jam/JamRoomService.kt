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

package dev.krtirtho.spotube.core.jam

import co.touchlab.kermit.Logger
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.PlayerState
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

/**
 * A jam session over MQTT (star topology, host-authoritative queue).
 *
 * Sync rules:
 * - The queue list and current index are global. [skipNext]/[skipPrevious]/[jumpTo]
 *   from anyone are applied by the host, then broadcast via the retained state topic.
 * - Play/pause, seek, volume and loop are local to each device — never broadcast.
 *   When the queue moves on, a paused participant stays paused; a playing one
 *   keeps playing the new current item.
 * - Shuffle is host-only; guests mirror the host's shuffle setting.
 * - Guests can only add to the queue (suggest); the host applies suggestions.
 * - If the host leaves, the participant with the lowest client id takes over.
 */
private data class PlaybackBroadcast(
    val queue: List<QueueEntry>,
    val current: QueueEntry?,
    val shuffle: Boolean,
    val playerState: PlayerState,
)

class JamRoomService(
    private val jamClient: JamRoomClient,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsRepository: SettingsRepository,
) {
    private val log = Logger.withTag("JamRoomService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    /** Guest side: playback has started at least once (local control is the guest's own). */
    private var hasStartedPlayback = false

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
        // Once a guest has played on its own (or was started by the host), its
        // play/pause is its own — the host's play state only starts fresh joiners.
        audioPlayer.playerStateFlow
            .onEach { state ->
                if (_role.value == JamRole.Guest && state == PlayerState.PLAYING) {
                    hasStartedPlayback = true
                }
            }
            .launchIn(scope)
    }

    // ---------- Session lifecycle ----------

    suspend fun createRoom(): Result<String> {
        val broker = settingsRepository.userSettings.value.jamBroker
        if (broker.host.isBlank()) {
            return Result.failure(IllegalStateException("No jam broker configured"))
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
            isHost = true,
        ).map {
            _role.value = JamRole.Host
            _roomCode.value = code
            _participants.value = listOf(JamParticipant(localClientId, name, isHost = true))
            lastAppliedItems = emptyList()
            lastAppliedIndex = -1
            bannedClientIds.clear()
            leaving = false
            autoAdvanceDeadlineMs = 0L
            hasStartedPlayback = false
            startHostBroadcast()
            persistLastCode(code)
            code
        }
    }

    suspend fun joinRoom(code: String): Result<Unit> {
        val broker = settingsRepository.userSettings.value.jamBroker
        if (broker.host.isBlank()) {
            return Result.failure(IllegalStateException("No jam broker configured"))
        }
        val normalized = JamRoomCode.normalize(code)
        if (!JamRoomCode.isValid(normalized)) {
            return Result.failure(IllegalArgumentException("Invalid room code"))
        }
        localClientId = newClientId(broker)
        val name = participantName("Guest")
        localDisplayName = name

        return jamClient.connect(
            broker = broker,
            code = normalized,
            clientId = localClientId,
            displayName = name,
            isHost = false,
        ).map {
            _role.value = JamRole.Guest
            _roomCode.value = normalized
            _participants.value = emptyList()
            lastAppliedItems = emptyList()
            lastAppliedIndex = -1
            leaving = false
            hasStartedPlayback = false
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
        hasStartedPlayback = false
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
                addedBy = localClientId,
            )
        )
    }

    suspend fun suggestPlaylist(tracks: List<MetadataTrack>) {
        if (tracks.isEmpty()) return
        jamClient.publishCommand(
            JamMessage.SuggestPlaylist(
                tracks = tracks.map(JamMediaItem::fromTrack),
                addedBy = localClientId,
            )
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
        hostBroadcastJob = scope.launch {
            combine(
                audioPlayerQueue.queueFlow,
                audioPlayerQueue.currentQueueEntryFlow,
                audioPlayer.shuffleModeFlow,
                audioPlayer.playerStateFlow,
            ) { queue, current, shuffle, playerState ->
                PlaybackBroadcast(queue, current, shuffle, playerState)
            }
                .onEach { broadcast ->
                    if (_role.value != JamRole.Host) return@onEach
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
                            isPlaying = broadcast.playerState == PlayerState.PLAYING,
                        )
                    )
                }
                .launchIn(this)
        }
    }

    private fun stopHostBroadcast() {
        hostBroadcastJob?.cancel()
        hostBroadcastJob = null
    }

    // ---------- Guest: apply remote state ----------

    private suspend fun onRemoteState(state: JamMessage.QueueState) {
        if (_role.value != JamRole.Guest) return
        if (leaving) return

        _shuffleEnabled.value = state.shuffleEnabled
        runCatching { audioPlayer.shuffle(state.shuffleEnabled) }

        // Late joiner: the host is already playing, so start immediately. Once
        // this guest has played on its own, the host's play state is ignored.
        if (state.isPlaying && !hasStartedPlayback) {
            runCatching { audioPlayer.play() }
                .onFailure { log.w(it) { "Failed to start playback on host play state" } }
        }

        val items = state.items.filter { it.trackId.isNotBlank() || it.url.isNotBlank() }
        val wasPlaying = audioPlayer.playerStateFlow.value == PlayerState.PLAYING

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
                    autoPlay = wasPlaying,
                    startPosition = startIndex,
                )
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
            PlaybackCmd.SkipNext -> runCatching { audioPlayer.skipToNext() }
            PlaybackCmd.SkipPrevious -> runCatching { audioPlayer.skipToPrevious() }
            is PlaybackCmd.JumpTo -> runCatching { audioPlayer.jumpTo(command.index) }
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

    private fun newClientId(broker: dev.krtirtho.spotube.modules.settings.JamBroker): String {
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
                // Keep the cover art flowing to participants — the wire carries it
                // as coverUrl, so mirror it back into the reconstructed thumbnails.
                thumbnails = coverUrl.takeIf { it.isNotBlank() }
                    ?.let { url -> listOf(Thumbnail(url = url, width = 0, height = 0)) },
                explicit = null,
                popularity = null,
                isrcCode = null,
                externalUri = null,
            ),
            url = "",
            protocol = runCatching { StreamProtocol.valueOf(protocol.ifBlank { "PROGRESSIVE" }) }
                .getOrDefault(StreamProtocol.PROGRESSIVE),
            addedBy = addedBy,
        )

        else -> QueueEntry.LocalTrack(
            name = title,
            artists = artist.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            duration = durationMs,
            album = album.ifBlank { null },
            coverBytes = null,
            url = url,
            addedBy = addedBy,
        )
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