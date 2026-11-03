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
import org.spotube.spotube.core.audioplayer.DeviceAudioPlayerQueue
import org.spotube.spotube.core.audioplayer.FakeAudioPlayerQueueRepository
import org.spotube.spotube.core.audioplayer.FakeBlacklistRepository
import org.spotube.spotube.core.audioplayer.FakePluginProvider
import org.spotube.spotube.core.audioplayer.FakeSettingsProvider
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

            // Mashing skip stays within the server's flood limits (the host isn't cut off).
            repeat(20) { if (it % 2 == 0) host.service.skipNext() else host.service.skipPrevious() }
            delay(6_000)
            assertEquals(true, host.service.isConnected.value, "host still connected after mashing skip")
            assertEquals(host.player.index.value, guest.player.index.value, "guest followed the skips")
            assertInSync(host, guest, "after mashing skip")

            // Steady playback costs (almost) nothing: count the anchors a listener receives.
            val listener = JamRoomClient(http)
            try {
                listener.connect(url, code, JamRoomCode.newClientKey(), "Listener", create = false).getOrThrow()
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

    /**
     * Songs that end on their own. The guest has its own uploads of every song (a shorter
     * intro) and its player loads the next song halfway through the current one, so it
     * must have the host's recording before then, or it plays a different one.
     */
    @Test
    fun usesTheHostsRecordingForEverySong() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val tracks = List(3) { track("song-$it", durationMs = SONG_MS) }
        val host = Participant(http, url, speedError = 1.0, realQueue = true)
        val guest = Participant(http, url, speedError = 1.01, realQueue = true)
        tracks.forEach {
            host.sources.seed(it, source("host-${it.id}"), PLUGIN)
            guest.sources.seed(it, source("own-${it.id}"), PLUGIN)
        }
        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(500)
            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            delay(6_000)
            assertEquals("host-song-0", guest.player.currentSource, "guest plays the host's recording of song 0")
            assertInSync(host, guest, "during song 0")

            for (song in 1..2) {
                while (host.player.index.value < song) delay(100)
                delay(5_000)
                assertEquals(song, guest.player.index.value, "guest on song $song")
                assertEquals("host-song-$song", guest.player.currentSource, "guest plays the host's recording of song $song")
                assertInSync(host, guest, "5 s into song $song after the previous one ended")
            }
        } finally {
            guest.close()
            host.close()
            http.close()
        }
    }

    /**
     * A guest that can't use the host's recordings (another audio plugin) finishes each
     * song early. It must wait for the host, not jump back to the song that just ended.
     */
    @Test
    fun aGuestThatFinishesASongFirstWaitsForTheHost() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val tracks = List(3) { track("song-$it", durationMs = SONG_MS) }
        val host = Participant(http, url, speedError = 1.0)
        val guest = Participant(http, url, speedError = 1.0, pluginId = "other-plugin")
        tracks.forEach { guest.sources.seed(it, source("own-${it.id}"), "other-plugin") }
        val jumpedBack = CopyOnWriteArrayList<String>()
        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(500)
            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            delay(4_000)
            val watcher = launch {
                var last = guest.player.index.value
                guest.player.index.collect { now ->
                    if (now < last) jumpedBack += "guest went back from song $last to $now"
                    last = now
                }
            }
            for (song in 1..2) {
                while (host.player.index.value < song) delay(100)
                delay(5_000)
                assertEquals(song, guest.player.index.value, "guest on song $song")
            }
            watcher.cancel()
            assertTrue(jumpedBack.isEmpty(), jumpedBack.toString())
        } finally {
            guest.close()
            host.close()
            http.close()
        }
    }

    /**
     * A jam must not keep the CPU busy (a spinning loop allocates nonstop and freezes the
     * app in GC). Measures the process's CPU time while the room sits in each state.
     */
    @Test
    fun idleStatesDoNotBurnCpu() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val tracks = List(3) { track("cpu-$it") }
        val host = Participant(http, url, speedError = 1.0, realQueue = true)
        val guest = Participant(http, url, speedError = 1.01, realQueue = true)
        val os = java.lang.management.ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
        suspend fun cpuDuring(label: String, block: suspend () -> Unit) {
            val before = os.processCpuTime
            val start = System.nanoTime()
            block()
            val share = (os.processCpuTime - before).toDouble() / (System.nanoTime() - start)
            println("Jam simulation: CPU ${"%.0f".format(share * 100)}% of a core while $label")
            assertTrue(share < 0.5, "CPU ${"%.0f".format(share * 100)}% of a core while $label")
        }
        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(500)
            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            delay(5_000)
            cpuDuring("playing in sync") { delay(8_000) }
            host.player.pause()
            delay(2_000)
            cpuDuring("the room is paused") { delay(8_000) }
            host.player.play()
            delay(3_000)
            host.player.stall()
            delay(1_000)
            cpuDuring("the host is buffering") { delay(8_000) }
            host.player.unstall()
            delay(3_000)
            guest.player.stall()
            cpuDuring("the guest is buffering") { delay(8_000) }
            guest.player.unstall()
        } finally {
            guest.close()
            host.close()
            http.close()
        }
    }

    /**
     * Several minutes of jam (songs ending, recordings switching, seeks) must not grow the
     * heap. Opt-in, since it takes minutes: set `JAM_SOAK=1`.
     */
    @Test
    fun longSessionDoesNotLeak() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        if (System.getenv("JAM_SOAK") == null) return@runBlocking
        val http = HttpClientFactory()
        val tracks = List(8) { track("soak-$it", durationMs = SONG_MS) }
        val host = Participant(http, url, speedError = 1.0, realQueue = true)
        val guest = Participant(http, url, speedError = 1.01, realQueue = true)
        tracks.forEach {
            host.sources.seed(it, source("host-${it.id}"), PLUGIN)
            guest.sources.seed(it, source("own-${it.id}"), PLUGIN)
        }
        val runtime = Runtime.getRuntime()
        fun usedMb(): Long {
            repeat(3) { System.gc(); Thread.sleep(200) }
            return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        }
        try {
            host.queue.load(tracks.map { QueueEntry.StreamingTrack(it, url = "") }, autoPlay = true, startPosition = 0)
            delay(500)
            val code = host.service.createRoom().getOrThrow()
            guest.service.joinRoom(code).getOrThrow()
            delay(20_000)
            val baseline = usedMb()
            val samples = mutableListOf(baseline)
            repeat(6) { round ->
                if (round % 2 == 0) host.player.seekTo(10_000.milliseconds) else guest.player.seekTo(20_000.milliseconds)
                delay(30_000)
                samples += usedMb()
            }
            println("Jam simulation: heap after GC over ${samples.size} samples (MB): $samples")
            assertTrue(samples.last() - baseline < 30, "heap grew from $baseline MB to ${samples.last()} MB")
            assertInSync(host, guest, "after a long session")
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
        val gap = guest.player.songPosition() - host.player.songPosition()
        println("Jam simulation: guest is ${gap}ms off $phase")
        assertTrue(abs(gap) < MAX_GAP_MS, "guest is ${gap}ms off $phase")
    }

    private class Participant(
        http: HttpClientFactory,
        serverUrl: String,
        speedError: Double,
        pluginId: String = PLUGIN,
        /** The app's own queue (stateIn flows and all) instead of [SimQueue]. */
        realQueue: Boolean = false) {
        val sources = FakeTrackSourceRepository()
        val player = SimPlayer(speedError)
        val queue: AudioPlayerQueue = if (realQueue) {
            player.resolveSource = { i ->
                val id = player.playlistFlow.value.getOrNull(i)?.url?.substringAfterLast('/')
                id?.let { sources.rawEntryFor(it)?.source?.id } ?: "default-$id"
            }
            DeviceAudioPlayerQueue(
                player, FakeSettingsProvider(), FakeAudioPlayerQueueRepository(),
                FakePluginProvider(), FakeBlacklistRepository())
        } else {
            SimQueue(player, sources)
        }
        private val plugins = FakeAudioPluginSource(FakeAudioPluginService(pluginId))
        val service = JamRoomService(
            jamClient = JamRoomClient(http),
            audioPlayer = player,
            audioPlayerQueue = queue,
            settingsRepository = FakeSettingsStore(UserSettings(jamServerUrl = serverUrl)),
            audioPluginSource = plugins,
            trackSources = sources,
            streamingUrlRepository = StreamingUrlRepository(
                plugins, FakeStreamingAudioPlayerQueue(), sources, FakeUserSettingsSource()),
            evictCachedAudio = {})

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
     *
     * Like ExoPlayer, it picks a song's recording (source) when it loads the song: the next
     * song is loaded halfway through the current one, and jumping to the song already
     * playing keeps its recording. Recordings differ in intro length ([introMs]).
     */
    private class SimPlayer(private val speedError: Double) : AudioPlayerInterface {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var durations: List<Long> = emptyList()
        /** The source the queue would resolve for a song now. */
        var resolveSource: (Int) -> String = { "default" }
        private val loadedSources = mutableMapOf<Int, String>()
        val currentSource: String? get() = loadedSources[index.value]
        private var basePosition = 0L
        private var baseAt = now()
        private var speed = 1f
        private var playWhenReady = false

        val index = MutableStateFlow(-1)
        private val _state = MutableStateFlow(PlayerState.IDLE)
        private val _position = MutableStateFlow(Duration.ZERO)
        private val _duration = MutableStateFlow(Duration.ZERO)
        private val _speed = MutableStateFlow(1f)
        private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
        private val _currentItem = MutableStateFlow<MediaItem?>(null)
        private val _completion = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

        init {
            scope.launch {
                while (isActive) {
                    delay(250)
                    val position = truePosition()
                    val duration = currentDuration() ?: continue
                    val next = index.value + 1
                    if (position >= duration / 2 && next <= durations.lastIndex && next !in loadedSources) {
                        loadedSources[next] = resolveSource(next)
                    }
                    if (position >= duration) {
                        _completion.tryEmit(Unit)
                        if (index.value < durations.lastIndex) advanceGapless()
                        continue
                    }
                    _position.value = (position / 100 * 100).milliseconds
                }
            }
        }

        /** Where in the song (not the recording) playback is. */
        fun songPosition(): Long = truePosition() - introMs(currentSource)

        private fun currentDuration(): Long? =
            durations.getOrNull(index.value)?.let { it + introMs(currentSource) }

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
            loadedSources.clear()
            startTrack(start)
        }

        /**
         * A song ending on its own, like ExoPlayer's gapless transition: no buffering, and
         * the position keeps the old song's value until the next report.
         */
        private fun advanceGapless() {
            val next = index.value + 1
            loadedSources.getOrPut(next) { resolveSource(next) }
            index.value = next
            _currentItem.value = _playlist.value.getOrNull(next)
            _duration.value = currentDuration()!!.milliseconds
            rebase(0)
        }

        fun startTrack(i: Int) {
            // Jumping to the song already playing seeks within it (ExoPlayer's
            // seekToDefaultPosition): the loaded recording stays.
            if (i != index.value || i !in loadedSources) loadedSources[i] = loadedSources[i] ?: resolveSource(i)
            index.value = i
            _currentItem.value = _playlist.value.getOrNull(i)
            _duration.value = currentDuration()!!.milliseconds
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

        /** Network stall: buffering until [unstall]. */
        fun stall() {
            rebase()
            _state.value = PlayerState.BUFFERING
        }

        fun unstall() {
            rebase()
            _state.value = if (playWhenReady) PlayerState.PLAYING else PlayerState.PAUSED
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
        override val currentMediaItemFlow: StateFlow<MediaItem?> = _currentItem
        override val playlistFlow: StateFlow<List<MediaItem>> = _playlist
        override val bufferingPositionFlow: StateFlow<Duration> = MutableStateFlow(Duration.ZERO)
        override val loopStateFlow: StateFlow<LoopState> = MutableStateFlow(LoopState.NONE)
        override val volumeFlow: StateFlow<Float> = MutableStateFlow(1f)
        override val errorFlow: Flow<Throwable> = MutableSharedFlow()

        override suspend fun stop() = pause()
        override suspend fun loop(state: LoopState) = Unit
        override suspend fun shuffle(enabled: Boolean) = Unit
        override suspend fun load(playlist: List<MediaItem>, autoPlay: Boolean, startPosition: Int) {
            _playlist.value = playlist
            loadTracks(playlist.map { it.duration.inWholeMilliseconds }, startPosition, autoPlay)
        }

        override suspend fun addMediaItem(mediaItem: MediaItem) {
            _playlist.value += mediaItem
            durations = durations + mediaItem.duration.inWholeMilliseconds
        }
        override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) = Unit
        override suspend fun removeMediaItem(mediaItem: MediaItem) = Unit
        override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) = Unit
        override suspend fun setVolume(volume: Float) = Unit
        override fun isDisposed(): Boolean = !scope.isActive
        override fun dispose() = scope.cancel()
    }

    private class SimQueue(private val player: SimPlayer, sources: FakeTrackSourceRepository) : AudioPlayerQueue {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val entries = MutableStateFlow<List<QueueEntry>>(emptyList())

        init {
            player.resolveSource = { i ->
                val id = (entries.value.getOrNull(i) as? QueueEntry.StreamingTrack)?.track?.id
                id?.let { sources.rawEntryFor(it)?.source?.id } ?: "default-$id"
            }
        }

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
        private const val SONG_MS = 30_000L

        /** How much longer a recording's intro is than the original's ("own" uploads cut 2.5 s). */
        private fun introMs(source: String?): Long = if (source?.startsWith("own-") == true) -2_500 else 0

        private fun now() = Clock.System.now().toEpochMilliseconds()

        private fun track(id: String, durationMs: Long = 180_000) = MetadataTrack(
            id = id, title = id, durationMs = durationMs, trackNumber = null, discNumber = null,
            artists = emptyList(), album = null, thumbnails = null, explicit = null,
            popularity = null, isrcCode = null, externalUri = null)

        private fun source(id: String) = AudioSource.Basic(
            id = id, title = id, artist = null, album = null, thumbnails = emptyList(),
            externalUri = null, confidence = 1f)
    }
}
