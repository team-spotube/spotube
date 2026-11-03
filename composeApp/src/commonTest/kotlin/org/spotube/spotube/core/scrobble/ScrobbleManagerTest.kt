package org.spotube.spotube.core.scrobble

import co.touchlab.kermit.Logger
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.FakeAudioPlayer
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.zipline.PluginService
import org.spotube.spotube.core.zipline.PluginServiceScope
import org.spotube.spotube.modules.plugin.ScrobblePluginSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ScrobbleManagerTest {

    private lateinit var player: FakeAudioPlayer
    private lateinit var queue: FakeQueue
    private lateinit var plugin: FakeScrobblePluginService
    private lateinit var pluginSource: FakeScrobblePluginSource

    /** Virtual playback position advanced by [playSeconds]; mirrors a real player clock. */
    private var currentPositionMs = 0L

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                factory { (tag: String?) -> Logger.withTag(tag ?: "test") }
            })
        }
        player = FakeAudioPlayer()
        queue = FakeQueue()
        plugin = FakeScrobblePluginService()
        pluginSource = FakeScrobblePluginSource(plugin)
        currentPositionMs = 0L
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    private fun TestScope.createManager(): ScrobbleManager {
        return ScrobbleManager(
            audioPlayer = player,
            audioPlayerQueue = queue,
            scrobblePluginSource = pluginSource,
            scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
    }

    private fun playSeconds(seconds: Int) {
        repeat(seconds) {
            currentPositionMs += 1_000
            player.setPosition(currentPositionMs.milliseconds)
        }
    }

    @Test
    fun `scrobbles once half of the track has been played`() = runTest {
        createManager()
        queue.setCurrentEntry(
            streamingEntry(
                id = "t1",
                durationMs = 200_000,
                externalUri = "https://open.spotify.com/track/t1")
        )
        player.setPlayerState(PlayerState.PLAYING)
        player.setDuration(200.seconds)

        playSeconds(99)
        assertEquals(0, plugin.scrobbled.size)

        playSeconds(1)
        assertEquals(1, plugin.scrobbled.size)

        val scrobble = plugin.scrobbled.single()
        assertEquals("t1", scrobble.trackId)
        assertEquals("artist-t1", scrobble.artistId)
        assertEquals("album-t1", scrobble.albumId)
        assertEquals("Track t1", scrobble.trackName)
        assertEquals("Artist t1", scrobble.artistName)
        assertEquals("Album t1", scrobble.albumName)
        assertEquals("spotify", scrobble.streamingProvider)
        assertEquals(200_000L, scrobble.durationMs)
        assertTrue(scrobble.timestamp > 0)
    }

    @Test
    fun `does not scrobble tracks shorter than 30 seconds`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "short", durationMs = 20_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(20)

        assertEquals(0, plugin.scrobbled.size)
    }

    @Test
    fun `caps the listening threshold at four minutes`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "long", durationMs = 1_800_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(239)
        assertEquals(0, plugin.scrobbled.size)

        playSeconds(1)
        assertEquals(1, plugin.scrobbled.size)
    }

    @Test
    fun `only scrobbles a track once per play`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 40_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(20)
        playSeconds(15)

        assertEquals(1, plugin.scrobbled.size)
    }

    @Test
    fun `does not credit forward seeks`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 100_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(10)
        currentPositionMs = 60_000
        player.setPosition(currentPositionMs.milliseconds)

        playSeconds(39)
        assertEquals(0, plugin.scrobbled.size)

        playSeconds(1)
        assertEquals(1, plugin.scrobbled.size)
    }

    @Test
    fun `does not credit time while paused`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 100_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(30)

        player.setPlayerState(PlayerState.PAUSED)
        currentPositionMs = 80_000
        player.setPosition(currentPositionMs.milliseconds)

        player.setPlayerState(PlayerState.PLAYING)
        playSeconds(19)
        assertEquals(0, plugin.scrobbled.size)

        playSeconds(1)
        assertEquals(1, plugin.scrobbled.size)
    }

    @Test
    fun `restarting the track allows scrobbling it again`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 40_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(20)
        assertEquals(1, plugin.scrobbled.size)

        // Repeat-one restarts playback from the beginning without a queue entry change.
        currentPositionMs = 0
        player.setPosition(0.milliseconds)
        playSeconds(20)

        assertEquals(2, plugin.scrobbled.size)
    }

    @Test
    fun `switching tracks starts a fresh scrobble session`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 40_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(20)
        assertEquals(1, plugin.scrobbled.size)

        queue.setCurrentEntry(streamingEntry(id = "t2", durationMs = 40_000))
        currentPositionMs = 0
        player.setPosition(0.milliseconds)
        playSeconds(20)

        assertEquals(2, plugin.scrobbled.size)
        assertEquals("t2", plugin.scrobbled.last().trackId)
    }

    @Test
    fun `waits for a scrobble plugin to be selected`() = runTest {
        pluginSource.setPlugin(null)
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 40_000))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(25)
        assertEquals(0, plugin.scrobbled.size)

        pluginSource.setPlugin(plugin)
        playSeconds(1)

        assertEquals(1, plugin.scrobbled.size)
    }

    @Test
    fun `scrobbles local tracks`() = runTest {
        createManager()
        queue.setCurrentEntry(
            localEntry(
                name = "Local Song",
                url = "file:///music/local.mp3",
                duration = 100_000)
        )
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(50)

        val scrobble = plugin.scrobbled.single()
        assertEquals("file:///music/local.mp3", scrobble.trackId)
        assertEquals("Local Song", scrobble.trackName)
        assertEquals("Local Artist", scrobble.artistName)
        assertEquals("Local Album", scrobble.albumName)
        assertEquals("local", scrobble.streamingProvider)
        assertEquals(100_000L, scrobble.durationMs)
    }

    @Test
    fun `uses unknown provider when the track has no external uri`() = runTest {
        createManager()
        queue.setCurrentEntry(streamingEntry(id = "t1", durationMs = 40_000, externalUri = null))
        player.setPlayerState(PlayerState.PLAYING)

        playSeconds(20)

        assertEquals("unknown", plugin.scrobbled.single().streamingProvider)
    }

    // --- fixtures ---

    private fun streamingTrack(
        id: String,
        durationMs: Long = 200_000,
        externalUri: String? = null): MetadataTrack {
        val artist = MetadataArtist.Basic(
            id = "artist-$id",
            name = "Artist $id",
            thumbnails = emptyList(),
            externalUri = null)
        val album = MetadataAlbum.Detailed(
            releaseDate = "2024",
            genres = emptyList(),
            trackCount = 1,
            id = "album-$id",
            title = "Album $id",
            description = null,
            thumbnails = emptyList(),
            albumType = MetadataAlbumType.Album,
            artists = listOf(artist),
            externalUri = null)
        return MetadataTrack(
            id = id,
            title = "Track $id",
            durationMs = durationMs,
            trackNumber = 1,
            discNumber = 1,
            artists = listOf(artist),
            album = album,
            thumbnails = null,
            explicit = false,
            popularity = null,
            isrcCode = null,
            externalUri = externalUri)
    }

    private fun streamingEntry(
        id: String,
        durationMs: Long = 200_000,
        externalUri: String? = null): QueueEntry.StreamingTrack {
        return QueueEntry.StreamingTrack(
            track = streamingTrack(id, durationMs, externalUri),
            url = "http://127.0.0.1:14769/stream/$id")
    }

    private fun localEntry(
        name: String = "Local Track",
        url: String = "file:///music/local.mp3",
        duration: Long = 180_000): QueueEntry.LocalTrack {
        return QueueEntry.LocalTrack(
            name = name,
            artists = listOf("Local Artist"),
            duration = duration,
            album = "Local Album",
            coverBytes = null,
            url = url)
    }

    private class FakeQueue : AudioPlayerQueue {
        private val queueState = MutableStateFlow<List<QueueEntry>>(emptyList())
        private val currentEntryState = MutableStateFlow<QueueEntry?>(null)

        override val queueFlow: StateFlow<List<QueueEntry>> = queueState
        override val currentQueueEntryFlow: StateFlow<QueueEntry?> = currentEntryState
        override val currentCollectionEntryFlow: StateFlow<QueueCollectionEntry?> =
            MutableStateFlow(null)
        override val collectionHistoryFlow: StateFlow<List<QueueCollectionEntry>> =
            MutableStateFlow(emptyList())

        fun setCurrentEntry(entry: QueueEntry?) {
            currentEntryState.value = entry
        }

        override suspend fun load(
            entries: List<QueueEntry>,
            autoPlay: Boolean,
            startPosition: Int,
            collectionEntry: QueueCollectionEntry?) = error("not used")

        override suspend fun addToQueue(entry: QueueEntry) = error("not used")
        override suspend fun addAllToQueue(
            entries: List<QueueEntry>,
            collectionEntry: QueueCollectionEntry?) = error("not used")

        override suspend fun addAllAfterCurrent(entries: List<QueueEntry>) = error("not used")
        override suspend fun removeFromQueue(entry: QueueEntry) = error("not used")
        override suspend fun removeFromQueueByMediaUrl(mediaUrl: String) = error("not used")
        override suspend fun move(fromIndex: Int, toIndex: Int) = error("not used")
        override suspend fun jumpTo(index: Int, autoPlay: Boolean) = error("not used")
        override suspend fun reloadCurrent() = error("not used")
        override suspend fun clear() = error("not used")
        override suspend fun getQueue(): List<QueueEntry> = queueState.value
        override suspend fun getCurrentQueueEntry(): QueueEntry? = currentEntryState.value
        override suspend fun getCurrentCollectionEntry(): QueueCollectionEntry? = null
        override suspend fun getCollectionHistory(): List<QueueCollectionEntry> = emptyList()
    }

    private class FakeScrobblePluginSource(
        initialPlugin: PluginService? = null) : ScrobblePluginSource {
        private val selected = MutableStateFlow(initialPlugin)
        override val selectedScrobblePlugin: StateFlow<PluginService?> = selected

        fun setPlugin(service: PluginService?) {
            selected.value = service
        }
    }

    private class FakeScrobblePluginService : PluginService {
        val scrobbled = mutableListOf<ScrobbleTrack>()
        override val pluginId: String = "fake-scrobble-plugin"
        override val loggedInFlow: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun start() = Unit
        override suspend fun stop() = Unit

        override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
            val api = object : ScrobbleAPI {
                override suspend fun scrobble(track: ScrobbleTrack) {
                    scrobbled.add(track)
                }
            }
            val scope = PluginServiceScope(mapOf(ScrobbleAPI::class to api))
            return scope.block()
        }
    }
}
