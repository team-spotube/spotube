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
import org.spotube.spotube.core.server.PluginTaggedAudioSource
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.core.server.TrackSourceRepository
import org.spotube.spotube.modules.plugin.AudioPluginSource
import org.spotube.spotube.modules.settings.UserSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_error_invalid_code
import spotube.composeapp.generated.resources.jam_error_no_server
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * A jam session: one host, any number of guests, all playing the same thing at the same
 * moment.
 *
 * Sync model:
 * - The host owns the queue and broadcasts it ([JamQueue]), plus a playback [JamAnchor]
 *   ("track X was at position P at server time T") whenever playback changes. Times are
 *   in the room server's clock, which every device estimates with [JamClock], so guests
 *   extrapolate an anchor on their own; the host only re-anchors when its player drifts
 *   from its last anchor (and rarely, as a safety net). Rooms are billed per message,
 *   so nothing is sent periodically.
 * - Battery: nothing polls while there's nothing to follow. A paused room, a buffering
 *   host or a missing queue puts the guest's sync loop to sleep until the room or the
 *   player changes, and the host only checks its own drift while playing.
 * - Guests follow the anchor's track and play state, and keep their position within
 *   ~150 ms of the room by nudging their playback rate a few percent ([JamDriftCorrector]),
 *   seeking only when far off.
 * - Anyone's play/pause, skip, jump and seek go to the host, which applies them for the
 *   whole room. Volume and loop stay local.
 * - Guests stream each track from the host's audio source when they use the same audio
 *   plugin, so everyone hears the same recording (different uploads don't line up).
 * - The server hands hosting to the longest-connected guest when the host leaves.
 */
class JamRoomService(
    private val jamClient: JamRoomClient,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsRepository: UserSettingsStore,
    private val audioPluginSource: AudioPluginSource,
    private val trackSources: TrackSourceRepository,
    private val streamingUrlRepository: StreamingUrlRepository,
    /** Deletes a track's audio from the disk cache (`CacheManager.invalidateCacheEntry`). */
    private val evictCachedAudio: suspend (trackId: String) -> Unit) {
    private val log = Logger.withTag("JamRoomService")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private var localDisplayName: String = ""

    /** Display name of the local participant. */
    val participantDisplayName: String
        get() = localDisplayName

    /** Stable id of the local participant (stamped on items this device adds). */
    val participantClientId: String
        get() = jamClient.selfId.orEmpty()

    private var roleJob: Job? = null
    private var leaving = false
    /** Joining or in a room: room events apply (some arrive before join returns). */
    private var inRoom = false

    /** Room-wide play state from the last anchor (null until the first one). */
    private var lastHostIsPlaying: Boolean? = null

    /** Our playback speed before we joined, restored when sync stops changing it. */
    private var speedBeforeJam: Float? = null

    // Latest room state from the host (kept in any role, applied while a guest)
    private var roomQueue: JamQueue? = null
    private var anchor: JamAnchor? = null
    private var lastAppliedItems: List<JamMediaItem> = emptyList()
    private val corrector = JamDriftCorrector()
    private val syncMutex = Mutex()
    private var appliedRate: Float? = null
    /** How far ahead to aim a seek so playback resumes on time; learned from each seek. */
    private var seekLeadMs = INITIAL_SEEK_LEAD_MS
    private var lastCorrectionWasSeek = false
    /** Don't correct before this (a seek or track change is still settling). */
    private var holdSyncUntilMs = 0L
    private val pinnedSources = mutableSetOf<String>()
    /** Bumped on every anchor and queue from the host; wakes an idle guest sync loop. */
    private val roomVersion = MutableStateFlow(0)

    // Position tracking, used to extrapolate the player's coarse position reports
    // and to tell user seeks apart from playback.
    private var positionSampleMs = 0L
    private var positionSampledAtMs = 0L
    private var positionAdvancing = false
    private var ownSeekUntilMs = 0L
    private var entryChangedAtMs = 0L
    /** The song [positionSampleMs] belongs to; a new song starts the tracking from zero. */
    private var positionEntryKey: String? = null

    /** Host: its sources for the next songs, sent with every anchor. */
    private var upcomingSources: List<JamTrackSource> = emptyList()

    private val anchorRequests = Channel<Unit>(Channel.CONFLATED)

    // The host's last anchor, to notice its own player drifting away from it.
    private var sentAnchor: JamAnchor? = null
    private var sentAnchorAtMs = 0L
    private val hostDrifts = ArrayDeque<Long>()

    init {
        jamClient.isConnected
            .onEach { _isConnected.value = it }
            .launchIn(scope)
        jamClient.connectionError
            .onEach { _connectionError.value = it }
            .launchIn(scope)
        jamClient.events
            .onEach { onEvent(it) }
            .launchIn(scope)
        audioPlayerQueue.currentQueueEntryFlow
            .onEach { onEntryChanged(it?.jamKey()) }
            .launchIn(scope)
        audioPlayer.positionFlow
            .onEach { onPosition(it.inWholeMilliseconds) }
            .launchIn(scope)
        // Extrapolation restarts from wherever the player was when it started or stopped
        // moving. (Not from positionFlow's value: a seek's report must stay a jump for onPosition.)
        audioPlayer.playerStateFlow
            .onEach { state ->
                positionSampleMs = localPositionMs()
                positionSampledAtMs = now()
                positionAdvancing = state == PlayerState.PLAYING
            }
            .launchIn(scope)
        // A guest's own play/pause (UI, notification, headset) goes to the host, which
        // applies it for the whole room. Intent that matches the room's state came from
        // following the host, so it isn't echoed back.
        playIntent
            .onEach { playing ->
                val roomPlaying = lastHostIsPlaying
                if (_role.value == JamRole.Guest && !leaving && roomPlaying != null && playing != roomPlaying) {
                    jamClient.sendCommand(JamCommand.SetPlaying(playing))
                }
            }
            .launchIn(scope)
    }

    // ---------- Session lifecycle ----------

    suspend fun createRoom(): Result<String> {
        val serverUrl = serverUrl() ?: return Result.failure(IllegalStateException(getString(Res.string.jam_error_no_server)))
        val deviceKey = deviceKey()
        localDisplayName = participantName(deviceKey)
        enter()

        // A fresh code is almost never taken; retry the rare collision with another one.
        var result: Result<JamRoomEvent.Welcome>
        var code: String
        var attempts = 0
        do {
            code = JamRoomCode.generate()
            result = jamClient.connect(
                serverUrl, code, jamParticipantKey(deviceKey, code), localDisplayName, create = true,
                deviceDigest = jamDeviceDigest(code))
            val taken = (result.exceptionOrNull() as? JamRoomException)?.code == "room_taken"
        } while (taken && ++attempts < CREATE_ATTEMPTS)

        val welcome = result.getOrElse {
            inRoom = false
            return Result.failure(it)
        }
        start(code, welcome)
        return Result.success(code)
    }

    suspend fun joinRoom(code: String): Result<Unit> {
        val serverUrl = serverUrl() ?: return Result.failure(IllegalStateException(getString(Res.string.jam_error_no_server)))
        val normalized = JamRoomCode.normalize(code)
        if (!JamRoomCode.isValid(normalized)) {
            return Result.failure(IllegalArgumentException(getString(Res.string.jam_error_invalid_code)))
        }
        val deviceKey = deviceKey()
        localDisplayName = participantName(deviceKey)
        enter()

        val welcome = jamClient.connect(
            serverUrl, normalized, jamParticipantKey(deviceKey, normalized), localDisplayName, create = false,
            deviceDigest = jamDeviceDigest(normalized))
            .getOrElse {
                inRoom = false
                return Result.failure(it)
            }
        start(normalized, welcome)
        return Result.success(Unit)
    }

    suspend fun leaveRoom() {
        leaving = true
        jamClient.disconnect()
        reset()
    }

    private fun enter() {
        inRoom = true
        roomQueue = null
        anchor = null
    }

    private suspend fun start(code: String, welcome: JamRoomEvent.Welcome) {
        leaving = false
        lastHostIsPlaying = null
        speedBeforeJam = audioPlayer.playbackSpeedFlow.value
        _roomCode.value = code
        _connectionError.value = null
        persistLastCode(code)
        // Frames that arrived while the clock synced are newer than the welcome's snapshot.
        onWelcome(welcome.copy(queue = roomQueue ?: welcome.queue, anchor = anchor ?: welcome.anchor))
    }

    private suspend fun reset() {
        inRoom = false
        audioPlayerQueue.setEndlessPlaybackSuppressed(false)
        upcomingSources = emptyList()
        roleJob?.cancel()
        roleJob = null
        restoreSpeed()
        _role.value = null
        _roomCode.value = null
        _participants.value = emptyList()
        _shuffleEnabled.value = false
        _isConnected.value = false
        roomQueue = null
        anchor = null
        lastAppliedItems = emptyList()
        lastHostIsPlaying = null
        pinnedSources.clear()
        corrector.reset()
        speedBeforeJam = null
    }

    // ---------- Controls (called from the UI) ----------

    fun skipNext() = control(JamCommand.SkipNext)

    fun skipPrevious() = control(JamCommand.SkipPrevious)

    fun jumpTo(index: Int) = control(JamCommand.JumpTo(index))

    /** Host-only. Applied locally; the queue broadcast carries the new shuffle flag. */
    fun toggleShuffle() {
        if (_role.value != JamRole.Host) return
        scope.launch {
            runCatching { audioPlayer.shuffle(!_shuffleEnabled.value) }
        }
    }

    suspend fun suggestTrack(track: MetadataTrack) {
        jamClient.sendCommand(JamCommand.Suggest(listOf(JamMediaItem.fromTrack(track)), addedBy = participantClientId))
    }

    suspend fun suggestPlaylist(tracks: List<MetadataTrack>) {
        if (tracks.isEmpty()) return
        jamClient.sendCommand(JamCommand.Suggest(tracks.map(JamMediaItem::fromTrack), addedBy = participantClientId))
    }

    suspend fun kickParticipant(participantId: String) {
        if (_role.value != JamRole.Host) return
        jamClient.kick(participantId, ban = false)
    }

    suspend fun banParticipant(participantId: String) {
        if (_role.value != JamRole.Host) return
        jamClient.kick(participantId, ban = true)
    }

    /** The host applies its own controls; a guest's go to the host. */
    private fun control(command: JamCommand) {
        scope.launch {
            if (_role.value == JamRole.Host) applyCommand(command, from = participantClientId) else jamClient.sendCommand(command)
        }
    }

    // ---------- Room events ----------

    private suspend fun onEvent(event: JamRoomEvent) {
        if (!inRoom) return
        when (event) {
            is JamRoomEvent.Welcome -> onWelcome(event)
            is JamRoomEvent.Presence -> {
                onMembership(event.host, event.participants)
                // The server keeps anchors in memory only; a joiner of a room that was
                // asleep gets none in its welcome, so the host answers every join.
                if (_role.value == JamRole.Host) publishAnchor()
            }
            is JamRoomEvent.Queue -> {
                roomQueue = event.queue
                if (_role.value == JamRole.Guest) applyQueue(event.queue)
            }
            is JamRoomEvent.Anchor -> {
                anchor = event.anchor
                if (_role.value == JamRole.Guest) onAnchor(event.anchor)
            }
            // [JamRoomEvent.Command.from] is the server's word for who sent it; the body is the sender's.
            is JamRoomEvent.Command -> if (_role.value == JamRole.Host) applyCommand(event.command, event.from)
            is JamRoomEvent.Closed -> {
                log.i { "Left jam room: ${event.code}" }
                reset()
                _connectionError.value = event.message
            }
        }
    }

    /** First join and every reconnect: the server's view of the room replaces ours. */
    private fun onWelcome(welcome: JamRoomEvent.Welcome) {
        welcome.queue?.let { roomQueue = it }
        welcome.anchor?.let { anchor = it }
        onMembership(welcome.host, welcome.participants, force = true)
    }

    private fun onMembership(host: String?, participants: List<JamParticipant>, force: Boolean = false) {
        _participants.value = participants
        val role = if (host != null && host == jamClient.selfId) JamRole.Host else JamRole.Guest
        // A (re)welcome restarts the role so a guest re-applies the room's state.
        if (role == _role.value && !force) return
        log.i { "Jam role: ${_role.value} -> $role" }
        _role.value = role
        roleJob?.cancel()
        roleJob = scope.launch {
            if (role == JamRole.Host) runHost() else runGuest()
        }
    }

    // ---------- Host ----------

    private suspend fun runHost() {
        restoreSpeed()
        audioPlayerQueue.setEndlessPlaybackSuppressed(false)
        sentAnchor = null
        upcomingSources = emptyList()
        kotlinx.coroutines.coroutineScope {
            combine(audioPlayerQueue.queueFlow, audioPlayer.shuffleModeFlow) { queue, shuffle ->
                JamQueue(queue.map(JamMediaItem::fromQueueEntry), shuffle)
            }
                .distinctUntilChanged()
                // At most one upload a second (the server's limit), always the latest queue.
                .conflate()
                .onEach { queue ->
                    _shuffleEnabled.value = queue.shuffleEnabled
                    // After a reconnect the server already has it: skip the re-upload.
                    if (queue != roomQueue) {
                        roomQueue = queue
                        jamClient.sendQueue(queue)
                        delay(QUEUE_MIN_GAP_MS)
                    }
                }
                .launchIn(this)

            // Anchors are built when sent, so coalescing a burst of requests (rapid skips)
            // loses nothing; it keeps the host under the server's frame budget.
            launch {
                for (request in anchorRequests) {
                    sendAnchor()
                    delay(ANCHOR_MIN_GAP_MS)
                }
            }

            combine(
                audioPlayerQueue.currentQueueEntryFlow,
                playIntent,
                audioPlayer.playerStateFlow,
                audioPlayer.playbackSpeedFlow) { _, _, _, _ -> }
                .onEach { publishAnchor() }
                .launchIn(this)

            // Share the next songs' sources as soon as a song starts: a guest's player loads
            // the next song long before it plays, and must find the host's recording by then.
            launch {
                combine(
                    audioPlayerQueue.currentQueueEntryFlow.map { it?.jamKey() }.distinctUntilChanged(),
                    audioPlayerQueue.queueFlow) { key, queue -> key to queue }
                    .collectLatest { (key, queue) ->
                        val start = queue.indexOfFirst { it.jamKey() == key } + 1
                        if (start <= 0) return@collectLatest
                        val upcoming = queue.drop(start).take(UPCOMING_SOURCES)
                            .filterIsInstance<QueueEntry.StreamingTrack>()
                            .mapNotNull { entry ->
                                hostSource(entry.track, match = true)?.let { JamTrackSource(entry.track.id, it) }
                            }
                        if (upcoming != upcomingSources) {
                            upcomingSources = upcoming
                            publishAnchor()
                        }
                    }
            }

            // Drift only builds up while playing; a paused host checks nothing.
            launch {
                while (isActive) {
                    audioPlayer.playerStateFlow.first { it == PlayerState.PLAYING }
                    delay(HOST_DRIFT_CHECK_INTERVAL_MS)
                    checkHostDrift()
                }
            }
        }
    }

    /**
     * Guests extrapolate the last anchor with the shared clock, so it stays valid as long
     * as the host's player keeps pace with its own clock. When it doesn't (audio clock
     * drift, a stall the player didn't report as buffering), re-anchor. Readings are
     * smoothed like a guest's, since players report position coarsely.
     */
    private suspend fun checkHostDrift() {
        val last = sentAnchor ?: return
        val now = now()
        if (now - sentAnchorAtMs >= HOST_ANCHOR_HEARTBEAT_MS) {
            publishAnchor()
            return
        }
        if (!last.advancing || audioPlayer.playerStateFlow.value != PlayerState.PLAYING) {
            hostDrifts.clear()
            return
        }
        hostDrifts.addLast(localPositionMs() - last.positionAt(jamClient.clock.serverTime(now)))
        while (hostDrifts.size > HOST_DRIFT_WINDOW) hostDrifts.removeFirst()
        val smoothed = hostDrifts.sorted()[hostDrifts.size / 2]
        if (hostDrifts.size == HOST_DRIFT_WINDOW &&
            abs(smoothed) > HOST_REANCHOR_DRIFT_MS &&
            now - sentAnchorAtMs >= HOST_REANCHOR_MIN_INTERVAL_MS
        ) {
            log.d { "Jam host drifted ${smoothed}ms from its anchor; re-anchoring" }
            publishAnchor()
        }
    }

    /** Asks the host loop to send a fresh anchor (coalesced, see [runHost]). */
    private fun publishAnchor() {
        anchorRequests.trySend(Unit)
    }

    private suspend fun sendAnchor() {
        if (_role.value != JamRole.Host) return
        val current = audioPlayerQueue.currentQueueEntryFlow.value ?: return
        onEntryChanged(current.jamKey())
        val playing = playIntent.value
        val next = JamAnchor(
            trackKey = current.jamKey(),
            index = audioPlayerQueue.queueFlow.value.indexOfFirst { it.jamKey() == current.jamKey() },
            positionMs = localPositionMs(),
            serverTimeMs = jamClient.clock.serverTime(now()),
            playing = playing,
            advancing = playing && audioPlayer.playerStateFlow.value == PlayerState.PLAYING,
            rate = audioPlayer.playbackSpeedFlow.value,
            source = (current as? QueueEntry.StreamingTrack)?.let { hostSource(it.track, match = false) },
            upcoming = upcomingSources)
        sentAnchor = next
        sentAnchorAtMs = now()
        hostDrifts.clear()
        jamClient.sendAnchor(next)
    }

    /**
     * The source the host's audio plugin matched to [track], so guests can use the same one.
     * With [match], a track not matched yet is matched now (as the player would when it got
     * there); the current song is being matched by the player already.
     */
    private suspend fun hostSource(track: MetadataTrack, match: Boolean): PluginTaggedAudioSource? {
        val plugin = audioPluginSource.selectedAudioPlugin.value ?: return null
        suspend fun saved() = runCatching { trackSources.getTrackSource(track, plugin.pluginId) }.getOrNull()
        val source = saved() ?: if (match) {
            runCatching { streamingUrlRepository.resolveStreamInfo(track) }
            saved()
        } else {
            null
        }
        return source?.let { PluginTaggedAudioSource(plugin.pluginId, it) }
    }

    private suspend fun applyCommand(command: JamCommand, from: String) {
        runCatching {
            when (command) {
                // Applied locally; the anchor carries the change to every guest.
                is JamCommand.SetPlaying -> if (command.playing) audioPlayer.play() else audioPlayer.pause()
                JamCommand.SkipNext -> audioPlayer.skipToNext()
                JamCommand.SkipPrevious -> audioPlayer.skipToPrevious()
                is JamCommand.JumpTo -> audioPlayer.jumpTo(command.index)
                is JamCommand.SeekTo -> {
                    seek(command.positionMs)
                    publishAnchor()
                }
                is JamCommand.Suggest -> acceptSuggestion(command.items, from)
            }
        }.onFailure { log.w(it) { "Failed to apply jam command $command" } }
    }

    /** Suggestions come from other devices: only clean streaming items, credited to the actual sender. */
    private suspend fun acceptSuggestion(items: List<JamMediaItem>, from: String) {
        val accepted = items.asSequence()
            .mapNotNull { it.sanitized() }
            .take(MAX_SUGGESTED_ITEMS)
            .map { it.copy(addedBy = from) }
            .toList()
        if (accepted.isEmpty()) return
        log.i { "Accepting ${accepted.size} suggested item(s) into the jam queue" }
        audioPlayerQueue.addAllToQueue(accepted.map { it.toQueueEntry() })
    }

    // ---------- Guest ----------

    private suspend fun runGuest() {
        // Guests follow the host's order through anchors; a local shuffle would only
        // make auto-advance pick a different track for a moment.
        runCatching { audioPlayer.shuffle(false) }
        audioPlayerQueue.setEndlessPlaybackSuppressed(true)
        corrector.reset()
        // The host's recordings first, so the queue's first load already uses them.
        anchor?.let { pinSources(it) }
        roomQueue?.let { applyQueue(it) }
        anchor?.let { onAnchor(it) }
        while (true) {
            // Captured before the check, so a change during it still wakes the wait below.
            val seenRoom = roomVersion.value
            val seenState = audioPlayer.playerStateFlow.value
            val seenQueue = audioPlayerQueue.queueFlow.value
            val status = syncMutex.withLock {
                runCatching { syncOnce() }
                    .onFailure { log.w(it) { "Jam sync failed" } }
                    .getOrDefault(SyncStatus.Busy)
            }
            when (status) {
                // While playing the CPU is awake for audio anyway, so a fast check is free
                // and catches drift early (slower checks made seeks settle visibly later).
                SyncStatus.Busy, SyncStatus.InSync -> delay(SYNC_INTERVAL_MS)
                // Sleep until the room or our player changes (the timeout is a safety net).
                SyncStatus.Idle -> withTimeoutOrNull(IDLE_RECHECK_MS) {
                    merge(
                        roomVersion.filter { it != seenRoom },
                        audioPlayer.playerStateFlow.filter { it != seenState },
                        audioPlayerQueue.queueFlow.filter { it !== seenQueue }).first()
                }
            }
        }
    }

    private enum class SyncStatus {
        /** Correcting or settling: check again soon. */
        Busy,
        /** Playing in sync: drift builds up slowly. */
        InSync,
        /** Nothing to follow until the room or the player changes. */
        Idle,
    }

    private suspend fun onAnchor(next: JamAnchor) {
        anchor = next
        roomVersion.value++
        // Play/pause is room-wide: follow every change of the room's play state. The room
        // state is recorded first, so the resulting local intent isn't sent back as a command.
        if (next.playing != lastHostIsPlaying) {
            lastHostIsPlaying = next.playing
            if (next.playing != playIntent.value) {
                runCatching { if (next.playing) audioPlayer.play() else audioPlayer.pause() }
                    .onFailure { log.w(it) { "Failed to follow the host's play state" } }
            }
        }
        pinSources(next)
    }

    private suspend fun pinSources(anchor: JamAnchor) {
        anchor.source?.let { pinSource(anchor.trackKey, it) }
        anchor.upcoming.forEach { pinSource(it.trackKey, it.source) }
    }

    private suspend fun applyQueue(queue: JamQueue) {
        roomVersion.value++
        _shuffleEnabled.value = queue.shuffleEnabled
        // The host's local files can't play here, and anything else is cleaned first.
        val items = queue.items.mapNotNull { it.sanitized() }
        if (items == lastAppliedItems) return
        val previous = lastAppliedItems
        lastAppliedItems = items
        // Sources that arrived before the queue had their tracks, before the queue loads them.
        anchor?.let { pinSources(it) }

        // The host only appended items (e.g. accepted suggestions): merge them in
        // without disturbing playback.
        val current = audioPlayerQueue.currentQueueEntryFlow.value
        if (previous.isNotEmpty() && items.size > previous.size &&
            items.take(previous.size) == previous && current != null
        ) {
            runCatching { audioPlayerQueue.addAllToQueue(items.drop(previous.size).map { it.toQueueEntry() }) }
                .onFailure { log.w(it) { "Failed to append jam queue items" } }
            return
        }

        val room = anchor
        val startIndex = room?.let { a -> items.indexOfFirst { it.key == a.trackKey } }?.takeIf { it >= 0 }
            ?: items.indexOfFirst { it.key == current?.jamKey() }.takeIf { it >= 0 }
            ?: 0
        holdSync(TRACK_CHANGE_HOLD_MS)
        runCatching {
            audioPlayerQueue.load(
                entries = items.map { it.toQueueEntry() },
                autoPlay = room?.playing ?: false,
                startPosition = startIndex)
        }.onFailure { log.w(it) { "Failed to apply jam queue" } }
    }

    /** One step of following the room: right track, then right position. */
    private suspend fun syncOnce(): SyncStatus {
        if (_role.value != JamRole.Guest || leaving) return SyncStatus.Idle
        val room = anchor ?: return SyncStatus.Idle
        val now = now()
        if (now < holdSyncUntilMs) return SyncStatus.Busy

        val current = audioPlayerQueue.currentQueueEntryFlow.value
        onEntryChanged(current?.jamKey())
        if (current?.jamKey() != room.trackKey) {
            val queue = audioPlayerQueue.queueFlow.value
            val index = queue.indexOfFirst { it.jamKey() == room.trackKey }
            if (index < 0) return SyncStatus.Idle // The queue with this track hasn't arrived yet.
            if (finishedSongFirst(queue, current, index, room, now)) return SyncStatus.Busy
            log.d { "Jam sync: switching to queue index $index" }
            holdSync(TRACK_CHANGE_HOLD_MS)
            corrector.reset()
            audioPlayerQueue.jumpTo(index, autoPlay = room.playing)
            return SyncStatus.Busy
        }

        val expected = room.positionAt(jamClient.clock.serverTime(now))
        val actual = localPositionMs()

        if (!room.playing) {
            // Paused room: line up once, so everyone resumes from the same spot.
            if (abs(expected - actual) > PAUSED_ALIGN_THRESHOLD_MS) {
                seek(expected)
                return SyncStatus.Busy
            }
            return SyncStatus.Idle
        }
        // A buffering host isn't moving; guests wait for its next anchor instead of
        // chasing a position that is about to be corrected. A buffering guest wakes
        // up when its player starts playing.
        if (!room.advancing || audioPlayer.playerStateFlow.value != PlayerState.PLAYING) return SyncStatus.Idle
        val durationMs = audioPlayer.durationFlow.value.inWholeMilliseconds
        if (expected < 0 || (durationMs > 0 && expected >= durationMs - END_GUARD_MS)) return SyncStatus.Busy

        val drift = expected - actual
        if (lastCorrectionWasSeek) {
            // Whatever is left after a seek is how far off the lead was.
            seekLeadMs = (seekLeadMs + drift / 2).coerceIn(0L, MAX_SEEK_LEAD_MS)
            lastCorrectionWasSeek = false
        }

        return when (val correction = corrector.correct(expected, actual, room.rate, seekLeadMs)) {
            JamCorrection.None -> {
                // Exactly the room's rate (normally 1x): any other rate makes the player
                // time-stretch, which costs CPU for as long as it lasts.
                setRate(room.rate)
                SyncStatus.InSync
            }
            is JamCorrection.Nudge -> {
                setRate(correction.rate)
                SyncStatus.Busy
            }
            is JamCorrection.Seek -> {
                log.d { "Jam sync: ${drift}ms off, seeking (lead ${seekLeadMs}ms)" }
                setRate(room.rate)
                seek(correction.positionMs)
                holdSync(SEEK_HOLD_MS)
                lastCorrectionWasSeek = true
                SyncStatus.Busy
            }
        }
    }

    /**
     * Our song ended a moment before the host's (players and recordings differ slightly in
     * length), so we're on the song after the room's. Wait for the host to get there
     * instead of going back to replay the ending. The room has to be near the end of its
     * song: a host that went back a song is followed right away.
     */
    private fun finishedSongFirst(
        queue: List<QueueEntry>,
        current: QueueEntry?,
        roomIndex: Int,
        room: JamAnchor,
        now: Long): Boolean {
        if (!room.playing || current == null) return false
        if (queue.indexOfFirst { it.jamKey() == current.jamKey() } != roomIndex + 1) return false
        if (now - entryChangedAtMs > NEXT_SONG_WAIT_MS) return false
        val roomSongMs = roomQueue?.items?.firstOrNull { it.key == room.trackKey }?.durationMs
            ?: (queue[roomIndex] as? QueueEntry.StreamingTrack)?.track?.durationMs
            ?: return false
        return roomSongMs > 0 && room.positionAt(jamClient.clock.serverTime(now)) >= roomSongMs - NEAR_SONG_END_MS
    }

    /**
     * Uses the host's audio source for [trackKey] when we use the same audio plugin, so
     * both sides stream the same recording. Uploads of one song differ in intro length,
     * which no amount of clock sync can fix. Upcoming songs are pinned well before our
     * player loads them; a song already playing from another recording is reloaded.
     */
    private suspend fun pinSource(trackKey: String, tagged: PluginTaggedAudioSource) {
        val pin = "$trackKey|${tagged.source.id}"
        if (pin in pinnedSources) return
        val plugin = audioPluginSource.selectedAudioPlugin.value ?: return
        if (plugin.pluginId != tagged.pluginId) return
        // Not in the room's queue yet: tried again when the queue arrives (applyQueue).
        val track = roomTrack(trackKey) ?: return
        pinnedSources += pin
        val mine = runCatching { trackSources.getTrackSource(track, plugin.pluginId) }.getOrNull()
        if (mine == tagged.source) return

        log.i { "Jam: using the host's source ${tagged.source.id} for $trackKey" }
        runCatching {
            trackSources.saveTrackSource(track, tagged.source, plugin.pluginId)
            streamingUrlRepository.invalidateCachedStreamUrl(trackKey)
            // A cached copy of another recording would be played instead.
            evictCachedAudio(trackKey)
            if (audioPlayerQueue.currentQueueEntryFlow.value?.jamKey() == trackKey) reloadForNewSource(trackKey)
        }.onFailure { log.w(it) { "Failed to switch to the host's audio source" } }
    }

    /** [trackKey]'s track, from our queue or (before it has loaded) the room's. */
    private fun roomTrack(trackKey: String): MetadataTrack? =
        audioPlayerQueue.queueFlow.value
            .firstNotNullOfOrNull { (it as? QueueEntry.StreamingTrack)?.track?.takeIf { t -> t.id == trackKey } }
            ?: roomQueue?.items?.firstOrNull { it.trackId == trackKey }?.sanitized()
                ?.let { (it.toQueueEntry() as? QueueEntry.StreamingTrack)?.track }

    /**
     * Plays the current song again from the newly pinned source. Jumping to the song that's
     * playing only seeks within what's already loaded on Android (ExoPlayer), so the queue
     * is reloaded at this song instead.
     */
    private suspend fun reloadForNewSource(trackKey: String) = syncMutex.withLock {
        val queue = audioPlayerQueue.queueFlow.value
        val index = queue.indexOfFirst { it.jamKey() == trackKey }.takeIf { it >= 0 } ?: return@withLock
        holdSync(TRACK_CHANGE_HOLD_MS)
        positionEntryKey = null
        audioPlayerQueue.load(queue, autoPlay = anchor?.playing ?: true, startPosition = index)
        onEntryChanged(trackKey)
    }

    private suspend fun setRate(rate: Float) {
        val rounded = (rate * 200).toInt() / 200f
        if (appliedRate == rounded) return
        // Only while playing: on iOS a non-zero rate also starts playback.
        if (audioPlayer.playerStateFlow.value != PlayerState.PLAYING) return
        runCatching { audioPlayer.setPlaybackSpeed(rounded) }
            .onSuccess { appliedRate = rounded }
    }

    private suspend fun restoreSpeed() {
        if (appliedRate == null) return
        appliedRate = null
        val speed = speedBeforeJam ?: 1f
        runCatching { audioPlayer.setPlaybackSpeed(speed) }
    }

    private fun holdSync(durationMs: Long) {
        holdSyncUntilMs = maxOf(holdSyncUntilMs, now() + durationMs)
    }

    // ---------- Position tracking ----------

    /**
     * A new song starts from zero. Without this, the old song's last position would carry
     * over: a song that ends on its own doesn't make the player report a jump like a skip
     * does, and its next report can still be the old song's.
     */
    private fun onEntryChanged(key: String?) {
        if (key == positionEntryKey) return
        positionEntryKey = key
        entryChangedAtMs = now()
        positionSampleMs = 0
        positionSampledAtMs = entryChangedAtMs
        positionAdvancing = audioPlayer.playerStateFlow.value == PlayerState.PLAYING
        corrector.reset()
    }

    /** The player's position now, extrapolated from its last (coarse) report. */
    private fun localPositionMs(): Long {
        if (!positionAdvancing) return positionSampleMs
        val elapsed = now() - positionSampledAtMs
        return positionSampleMs + (elapsed * audioPlayer.playbackSpeedFlow.value).toLong()
    }

    private suspend fun seek(positionMs: Long) {
        ownSeekUntilMs = now() + OWN_SEEK_WINDOW_MS
        audioPlayer.seekTo(positionMs.coerceAtLeast(0).milliseconds)
        positionSampleMs = positionMs
        positionSampledAtMs = now()
    }

    private fun onPosition(positionMs: Long) {
        val now = now()
        onEntryChanged(audioPlayerQueue.currentQueueEntryFlow.value?.jamKey())
        // Right after a song change, a report can still be the old song's position.
        val sinceChange = now - entryChangedAtMs
        if (sinceChange < TRACK_CHANGE_HOLD_MS &&
            positionMs > sinceChange * audioPlayer.playbackSpeedFlow.value + STALE_REPORT_SLACK_MS
        ) return
        // A seek moves the position away from both the last report and the extrapolation;
        // a stall (or a long pause) only from the extrapolation.
        val jumped = abs(positionMs - positionSampleMs) > USER_SEEK_THRESHOLD_MS &&
            abs(positionMs - localPositionMs()) > USER_SEEK_THRESHOLD_MS &&
            now > ownSeekUntilMs &&
            now - entryChangedAtMs > TRACK_CHANGE_HOLD_MS
        positionSampleMs = positionMs
        positionSampledAtMs = now
        if (!jumped || _roomCode.value == null || leaving) return

        // The user seeked: it applies to the whole room.
        when (_role.value) {
            JamRole.Host -> publishAnchor()
            JamRole.Guest -> {
                holdSync(SEEK_HOLD_MS * 2)
                jamClient.sendCommand(JamCommand.SeekTo(positionMs))
            }
            null -> Unit
        }
    }

    // ---------- Helpers ----------

    private fun serverUrl(): String? =
        settingsRepository.userSettings.value.jamServerUrl.trim().ifBlank { null }

    private suspend fun participantName(deviceKey: String): String =
        JamNames.normalize(settingsRepository.userSettings.value.jamParticipantName)
            .take(JamNames.MAX_LENGTH)
            .ifBlank { JamNames.generate(deviceKey) }

    /** The name this device gets when the user hasn't picked one; the same every time. */
    suspend fun defaultParticipantName(): String = JamNames.generate(deviceKey())

    /** Saves the user's own name; blank goes back to [defaultParticipantName]. */
    suspend fun setParticipantName(name: String) {
        val settings = settingsRepository.userSettings.value
        settingsRepository.updateSettings(
            settings.copy(jamParticipantName = JamNames.normalize(name).take(JamNames.MAX_LENGTH)))
    }

    /** This install's jam secret, created on first use. */
    private suspend fun deviceKey(): String {
        val settings = settingsRepository.userSettings.value
        if (settings.jamDeviceKey.isNotBlank()) return settings.jamDeviceKey
        val generated = JamRoomCode.newClientKey()
        runCatching { settingsRepository.updateSettings(settings.copy(jamDeviceKey = generated)) }
            .onFailure { log.w(it) { "Failed to save the jam device key" } }
        return generated
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

    private fun QueueEntry.jamKey(): String = when (this) {
        is QueueEntry.StreamingTrack -> track.id
        is QueueEntry.LocalTrack -> JamMediaItem.localTrackKey(url)
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

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        private const val CREATE_ATTEMPTS = 3
        /** Keeps the host under the server's budgets (see `src/protocol.ts` in the jam-server repository). */
        private const val QUEUE_MIN_GAP_MS = 1_000L
        private const val ANCHOR_MIN_GAP_MS = 250L
        private const val MAX_SUGGESTED_ITEMS = 500
        /** How many next songs the host shares its sources for. */
        private const val UPCOMING_SOURCES = 2
        /** How long a guest that finished a song first waits for the host to finish it too. */
        private const val NEXT_SONG_WAIT_MS = 15_000L
        /** "Near the end" of the room's song, allowing for recordings of different lengths. */
        private const val NEAR_SONG_END_MS = 15_000L
        private const val STALE_REPORT_SLACK_MS = 1_500L
        private const val HOST_DRIFT_CHECK_INTERVAL_MS = 2_000L
        private const val HOST_DRIFT_WINDOW = 5
        /** Below the guests' dead band, so the host's own error never adds up to a correction. */
        private const val HOST_REANCHOR_DRIFT_MS = 80L
        private const val HOST_REANCHOR_MIN_INTERVAL_MS = 10_000L
        /** A safety net only (e.g. a lost frame); costs one message a minute. */
        private const val HOST_ANCHOR_HEARTBEAT_MS = 60_000L
        private const val SYNC_INTERVAL_MS = 500L
        private const val IDLE_RECHECK_MS = 30_000L
        private const val INITIAL_SEEK_LEAD_MS = 300L
        private const val MAX_SEEK_LEAD_MS = 3_000L
        private const val SEEK_HOLD_MS = 1_500L
        private const val TRACK_CHANGE_HOLD_MS = 2_000L
        private const val PAUSED_ALIGN_THRESHOLD_MS = 1_500L
        private const val USER_SEEK_THRESHOLD_MS = 2_500L
        private const val OWN_SEEK_WINDOW_MS = 1_500L
        /** Near a track's end the next anchor (new track) is due; don't seek into the outro. */
        private const val END_GUARD_MS = 1_000L
    }
}
