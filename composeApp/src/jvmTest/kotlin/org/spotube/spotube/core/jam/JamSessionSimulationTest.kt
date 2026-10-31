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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.MediaItem
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.server.FakeAudioPluginService
import org.spotube.spotube.core.server.FakeAudioPluginSource
import org.spotube.spotube.core.server.FakeStreamingAudioPlayerQueue
import org.spotube.spotube.core.server.FakeTrackSourceRepository
import org.spotube.spotube.core.server.FakeUserSettingsSource
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.UserSettingsStore
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A host and a guest [JamRoomService], each driving a simulated player, in one room on a
 * real room server. The guest's player runs 1% fast, the host's 0.1% slow (so it has to
 * re-anchor), and both report positions coarsely, like real players. Skipped unless `JAM_TEST_URL` is set (see [JamRoomClientIntegrationTest]).
 */
class JamSessionSimulationTest {
    private val serverUrl: String? = System.getenv("JAM_TEST_URL")

    @BeforeTest
    fun setup() {
        startKoin { modules(module { factory { (tag: String?) -> Logger.withTag(tag ?: "test") } }) }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun guestFollowsTheHost() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val tracks = List(3) { track("track-$it") }
        val hostSource = source("host-upload")
        val host = Participant(http, url, speedError = 0.999)
        val guest = Participant(http, url, speedError = 1.01)
        host.sources.seed(tracks[0], hostSource, PLUGIN)
        guest.sources.seed(tracks[0], source("other-upload"), PLUGIN)

        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(2_000)

            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            assertEquals(JamRole.Host, host.service.role.value)
            assertEquals(JamRole.Guest, guest.service.role.value)

            delay(7_000)
            assertInSync(host, guest, "after joining")
            assertEquals(hostSource, guest.sources.rawEntryFor(tracks[0].id)?.source, "guest uses the host's source")

            host.player.seekTo(60_000.milliseconds)
            delay(5_000)
            assertInSync(host, guest, "after the host seeked")

            // A guest's seek moves the whole room.
            guest.player.seekTo(90_000.milliseconds)
            // Two hops (guest -> host -> guest) and two seeks: allow longer to settle.
            delay(8_000)
            assertTrue(host.player.truePosition() in 96_000..100_000, "host followed the guest's seek: ${host.player.truePosition()}")
            assertInSync(host, guest, "after the guest seeked")

            guest.player.pause()
            delay(1_500)
            assertEquals(PlayerState.PAUSED, host.player.playerStateFlow.value, "guest paused the room")

            host.player.play()
            host.service.skipNext()
            delay(6_000)
            assertEquals(1, host.player.index.value)
            assertEquals(1, guest.player.index.value, "guest followed the skip")
            assertEquals(PlayerState.PLAYING, guest.player.playerStateFlow.value)
            assertInSync(host, guest, "after the skip")

            // Steady playback costs (almost) nothing: count the anchors a listener receives.
            val listener = JamRoomClient(http)
            try {
                listener.connect(url, code, "it-listener", "Listener", create = false).getOrThrow()
                delay(1_000) // The host answers the join with an anchor.
                var anchors = 0
                val counting = launch {
                    listener.events.collect { if (it is JamRoomEvent.Anchor) anchors++ }
                }
                delay(STEADY_WINDOW_MS)
                counting.cancel()
                println("Jam simulation: $anchors anchor(s) in ${STEADY_WINDOW_MS / 1000}s of steady playback")
                assertTrue(anchors <= 2, "steady playback sent $anchors anchors")
                assertInSync(host, guest, "after steady playback")
            } finally {
                listener.disconnect()
            }
        } finally {
            guest.close()
            host.close()
            http.close()
        }
    }

    /** Losing the network (guest, then host) must not crash, and the room picks up again. */
    @Test
    fun roomSurvivesNetworkLoss() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val hostNet = TcpProxy(url)
        val guestNet = TcpProxy(url)
        val tracks = List(3) { track("track-$it") }
        val host = Participant(http, hostNet.url, speedError = 1.0)
        val guest = Participant(http, guestNet.url, speedError = 1.01)
        val crashes = CopyOnWriteArrayList<Throwable>()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> crashes += e }

        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(1_000)
            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            delay(5_000)
            assertInSync(host, guest, "before the guest lost its network")

            guestNet.goOffline()
            delay(1_000)
            assertEquals(false, guest.service.isConnected.value, "guest noticed the drop")
            assertEquals(code, guest.service.roomCode.value, "guest stays in the room while offline")
            // Meanwhile the room moves on without the guest.
            host.player.seekTo(40_000.milliseconds)
            delay(4_000)
            assertEquals(PlayerState.PLAYING, guest.player.playerStateFlow.value, "guest keeps playing offline")

            guestNet.goOnline()
            delay(10_000) // Up to one backoff step, then resync.
            assertEquals(true, guest.service.isConnected.value, "guest reconnected")
            assertEquals(JamRole.Guest, guest.service.role.value)
            assertInSync(host, guest, "after the guest reconnected")

            // A host that drops for less than the grace period keeps the room.
            hostNet.goOffline()
            delay(5_000)
            assertEquals(PlayerState.PLAYING, guest.player.playerStateFlow.value, "guest keeps playing while the host is away")
            hostNet.goOnline()
            delay(10_000)
            assertEquals(true, host.service.isConnected.value, "host reconnected")
            assertEquals(JamRole.Host, host.service.role.value, "host kept the room")
            host.player.seekTo(100_000.milliseconds)
            delay(5_000)
            assertInSync(host, guest, "after the host reconnected and seeked")

            assertTrue(crashes.isEmpty(), "uncaught exceptions: $crashes")
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
            guest.close()
            host.close()
            hostNet.close()
            guestNet.close()
            http.close()
        }
    }

    private fun assertInSync(host: Participant, guest: Participant, phase: String) {
        assertEquals(host.player.index.value, guest.player.index.value, "same track $phase")
        val gap = guest.player.truePosition() - host.player.truePosition()
        println("Jam simulation: guest is ${gap}ms off $phase")
        assertTrue(abs(gap) < MAX_GAP_MS, "guest is ${gap}ms off $phase")
    }

    private class Participant(http: HttpClientFactory, serverUrl: String, speedError: Double) {
        val player = SimPlayer(speedError)
        val queue = SimQueue(player)
        val sources = FakeTrackSourceRepository()
        private val plugins = FakeAudioPluginSource(FakeAudioPluginService(PLUGIN))
        val service = JamRoomService(
            jamClient = JamRoomClient(http),
            audioPlayer = player,
            audioPlayerQueue = queue,
            settingsRepository = FakeSettingsStore(UserSettings(jamServerUrl = serverUrl)),
            audioPluginSource = plugins,
            trackSources = sources,
            streamingUrlRepository = StreamingUrlRepository(
                plugins, FakeStreamingAudioPlayerQueue(), sources, FakeUserSettingsSource()))

        suspend fun close() {
            service.leaveRoom()
            player.dispose()
        }
    }

    private class FakeSettingsStore(initial: UserSettings) : UserSettingsStore {
        override val userSettings = MutableStateFlow(initial)
        override suspend fun updateSettings(newSettings: UserSettings) {
            userSettings.value = newSettings
        }
    }

    /**
     * Plays a list of tracks in real time. Its clock runs [speedError] times true speed (as
     * a drifting device would), positions are reported every 250 ms rounded to 100 ms, and
     * every seek or track change buffers briefly.
     */
    private class SimPlayer(private val speedError: Double) : AudioPlayerInterface {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var durations: List<Long> = emptyList()
        private var basePosition = 0L
        private var baseAt = now()
        private var speed = 1f
        private var playWhenReady = false

        val index = MutableStateFlow(-1)
        private val _state = MutableStateFlow(PlayerState.IDLE)
        private val _position = MutableStateFlow(Duration.ZERO)
        private val _duration = MutableStateFlow(Duration.ZERO)
        private val _speed = MutableStateFlow(1f)
        private val _completion = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

        init {
            scope.launch {
                while (isActive) {
                    delay(250)
                    val position = truePosition()
                    val duration = durations.getOrNull(index.value) ?: continue
                    if (position >= duration) {
                        _completion.tryEmit(Unit)
                        if (index.value < durations.lastIndex) startTrack(index.value + 1)
                        continue
                    }
                    _position.value = (position / 100 * 100).milliseconds
                }
            }
        }

        @Synchronized
        fun truePosition(): Long =
            if (_state.value == PlayerState.PLAYING) {
                basePosition + ((now() - baseAt) * speed * speedError).toLong()
            } else {
                basePosition
            }

        @Synchronized
        private fun rebase(position: Long = truePosition()) {
            basePosition = position
            baseAt = now()
        }

        fun loadTracks(trackDurations: List<Long>, start: Int, autoPlay: Boolean) {
            durations = trackDurations
            playWhenReady = autoPlay
            startTrack(start)
        }

        fun startTrack(i: Int) {
            index.value = i
            _duration.value = durations[i].milliseconds
            rebase(0)
            _position.value = Duration.ZERO
            buffer()
        }

        private fun buffer() {
            rebase()
            _state.value = PlayerState.BUFFERING
            scope.launch {
                delay(BUFFER_MS)
                rebase()
                _state.value = if (playWhenReady) PlayerState.PLAYING else PlayerState.PAUSED
            }
        }

        override suspend fun play() {
            playWhenReady = true
            rebase()
            if (_state.value != PlayerState.BUFFERING) _state.value = PlayerState.PLAYING
        }

        override suspend fun pause() {
            playWhenReady = false
            rebase()
            if (_state.value != PlayerState.BUFFERING) _state.value = PlayerState.PAUSED
        }

        override suspend fun seekTo(position: Duration) {
            rebase(position.inWholeMilliseconds)
            _position.value = position
            buffer()
        }

        override suspend fun setPlaybackSpeed(speed: Float) {
            rebase()
            this.speed = speed
            _speed.value = speed
        }

        override suspend fun skipToNext() = startTrack((index.value + 1).coerceAtMost(durations.lastIndex))
        override suspend fun skipToPrevious() = startTrack((index.value - 1).coerceAtLeast(0))
        override suspend fun jumpTo(index: Int) = startTrack(index)

        override val playerStateFlow: StateFlow<PlayerState> = _state
        override val positionFlow: StateFlow<Duration> = _position
        override val durationFlow: StateFlow<Duration> = _duration
        override val playbackSpeedFlow: StateFlow<Float> = _speed
        override val completionFlow: Flow<Unit> = _completion
        override val shuffleModeFlow: StateFlow<Boolean> = MutableStateFlow(false)
        override val currentMediaItemFlow: StateFlow<MediaItem?> = MutableStateFlow(null)
        override val playlistFlow: StateFlow<List<MediaItem>> = MutableStateFlow(emptyList())
        override val bufferingPositionFlow: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
        override val loopStateFlow: StateFlow<LoopState> = MutableStateFlow(LoopState.NONE)
        override val volumeFlow: StateFlow<Float> = MutableStateFlow(1f)
        override val errorFlow: Flow<Throwable> = MutableSharedFlow()

        override suspend fun stop() = pause()
        override suspend fun loop(state: LoopState) = Unit
        override suspend fun shuffle(enabled: Boolean) = Unit
        override suspend fun load(playlist: List<MediaItem>, autoPlay: Boolean, startPosition: Int) = error("use SimQueue")
        override suspend fun addMediaItem(mediaItem: MediaItem) = Unit
        override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) = Unit
        override suspend fun removeMediaItem(mediaItem: MediaItem) = Unit
        override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) = Unit
        override suspend fun setVolume(volume: Float) = Unit
        override fun isDisposed(): Boolean = !scope.isActive
        override fun dispose() = scope.cancel()
    }

    private class SimQueue(private val player: SimPlayer) : AudioPlayerQueue {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val entries = MutableStateFlow<List<QueueEntry>>(emptyList())

        override val queueFlow: StateFlow<List<QueueEntry>> = entries
        override val currentQueueEntryFlow: StateFlow<QueueEntry?> =
            combine(entries, player.index) { list, i -> list.getOrNull(i) }
                .stateIn(scope, SharingStarted.Eagerly, null)
        override val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?> = MutableStateFlow(null)
        override val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>> = MutableStateFlow(emptyList())

        override suspend fun load(
            entries: List<QueueEntry>,
            autoPlay: Boolean,
            startPosition: Int,
            collectionEntry: QueueCollectionEntry?) {
            this.entries.value = entries
            player.loadTracks(entries.map { (it as QueueEntry.StreamingTrack).track.durationMs }, startPosition, autoPlay)
        }

        override suspend fun jumpTo(index: Int, autoPlay: Boolean) {
            if (autoPlay) player.play() else player.pause()
            player.startTrack(index)
        }

        override suspend fun reloadCurrent() = jumpTo(player.index.value, autoPlay = true)
        override suspend fun addToQueue(entry: QueueEntry) = addAllToQueue(listOf(entry), null)
        override suspend fun addAllToQueue(entries: List<QueueEntry>, collectionEntry: QueueCollectionEntry?) {
            this.entries.value += entries
        }

        override suspend fun addAllAfterCurrent(entries: List<QueueEntry>) = addAllToQueue(entries, null)
        override suspend fun removeFromQueue(entry: QueueEntry) = Unit
        override suspend fun removeFromQueueByMediaUrl(mediaUrl: String) = Unit
        override suspend fun move(fromIndex: Int, toIndex: Int) = Unit
        override suspend fun clear() = Unit
        override suspend fun getQueue(): List<QueueEntry> = entries.value
        override suspend fun getCurrentQueueEntry(): QueueEntry? = currentQueueEntryFlow.value
        override suspend fun getCurrentCollectionEntry(): QueueCollectionEntry? = null
        override suspend fun getCollectionHistory(): List<QueueCollectionEntry> = emptyList()
    }

    companion object {
        private const val PLUGIN = "test-audio-plugin"
        private const val BUFFER_MS = 200L
        private const val MAX_GAP_MS = 200L
        private const val STEADY_WINDOW_MS = 40_000L

        private fun now() = Clock.System.now().toEpochMilliseconds()

        private fun track(id: String) = MetadataTrack(
            id = id, title = id, durationMs = 180_000, trackNumber = null, discNumber = null,
            artists = emptyList(), album = null, thumbnails = null, explicit = null,
            popularity = null, isrcCode = null, externalUri = null)

        private fun source(id: String) = AudioSource.Basic(
            id = id, title = id, artist = null, album = null, thumbnails = emptyList(),
            externalUri = null, confidence = 1f)
    }
}
