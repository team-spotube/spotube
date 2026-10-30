package org.spotube.spotube.core.audioplayer

import co.touchlab.kermit.Logger
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DeviceAudioPlayerQueueTest {

    private lateinit var fakePlayer: FakeAudioPlayer
    private lateinit var fakeSettings: FakeSettingsProvider
    private lateinit var fakeRepo: FakeAudioPlayerQueueRepository
    private lateinit var fakePlugin: FakePluginProvider
    private lateinit var fakeBlacklist: FakeBlacklistRepository

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                factory { (tag: String?) -> Logger.withTag(tag ?: "test") }
            })
        }
        fakePlayer = FakeAudioPlayer()
        fakeSettings = FakeSettingsProvider()
        fakeRepo = FakeAudioPlayerQueueRepository()
        fakePlugin = FakePluginProvider()
        fakeBlacklist = FakeBlacklistRepository()
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    private fun createQueue(): DeviceAudioPlayerQueue {
        return DeviceAudioPlayerQueue(fakePlayer, fakeSettings, fakeRepo, fakePlugin, fakeBlacklist)
    }

    private fun streamingTrack(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist $id"
    ): MetadataTrack {
        val artistBasic = MetadataArtist.Basic(
            id = "artist-$id",
            name = artist,
            thumbnails = emptyList(),
            externalUri = null
        )
        val album = MetadataAlbum.Detailed(
            releaseDate = "2024",
            genres = emptyList(),
            trackCount = 1,
            id = "album-$id",
            title = "Album $title",
            description = null,
            thumbnails = listOf(Thumbnail(url = "https://cover/$id", width = 300, height = 300)),
            albumType = MetadataAlbumType.Album,
            artists = listOf(artistBasic),
            externalUri = null
        )
        return MetadataTrack(
            id = id,
            title = title,
            durationMs = 200_000L,
            trackNumber = 1,
            discNumber = 1,
            artists = listOf(artistBasic),
            album = album,
            thumbnails = album.thumbnails,
            explicit = false,
            popularity = 50,
            isrcCode = null,
            externalUri = null
        )
    }

    private fun streamingEntry(
        trackId: String,
        url: String = "http://127.0.0.1:14769/stream/$trackId"
    ): QueueEntry.StreamingTrack {
        return QueueEntry.StreamingTrack(
            track = streamingTrack(trackId),
            url = url,
            protocol = StreamProtocol.PROGRESSIVE
        )
    }

    private fun localEntry(
        name: String = "Local Track",
        url: String = "file:///music/$name.mp3"
    ): QueueEntry.LocalTrack {
        return QueueEntry.LocalTrack(
            name = name,
            artists = listOf("Local Artist"),
            duration = 180_000L,
            album = "Local Album",
            coverBytes = null,
            url = url
        )
    }

    // --- load ---

    @Test
    fun `load populates player playlist with media items`() = runTest {
        val queue = createQueue()
        val entries = listOf(streamingEntry("t1"), streamingEntry("t2"))

        queue.load(entries)

        assertEquals(1, fakePlayer.loadCallCount)
        assertEquals(2, fakePlayer.lastLoadPlaylist?.size)
        assertEquals("Track t1", fakePlayer.lastLoadPlaylist?.get(0)?.title)
        assertEquals("Track t2", fakePlayer.lastLoadPlaylist?.get(1)?.title)
    }

    @Test
    fun `load respects autoPlay parameter`() = runTest {
        val queue = createQueue()
        val entries = listOf(streamingEntry("t1"))

        queue.load(entries, autoPlay = false)

        assertEquals(false, fakePlayer.lastLoadAutoPlay)
    }

    @Test
    fun `load respects startPosition`() = runTest {
        val queue = createQueue()
        val entries = listOf(streamingEntry("t1"), streamingEntry("t2"))

        queue.load(entries, startPosition = 1)

        assertEquals(1, fakePlayer.lastLoadStartPosition)
    }

    @Test
    fun `load sets collection context`() = runTest {
        val queue = createQueue()
        val colEntry = QueueCollectionEntry.Playlist("pl-1")

        queue.load(
            entries = listOf(streamingEntry("t1")),
            collectionEntry = colEntry
        )

        assertEquals(colEntry, queue.getCurrentCollectionEntry())
    }

    @Test
    fun `load with empty entries does not crash`() = runTest {
        val queue = createQueue()

        queue.load(emptyList())

        assertEquals(1, fakePlayer.loadCallCount)
        assertTrue { fakePlayer.lastLoadPlaylist?.isEmpty() == true }
    }

    // --- addToQueue ---

    @Test
    fun `addToQueue adds entry and calls player`() = runTest {
        val queue = createQueue()
        val entry = streamingEntry("t1")

        queue.addToQueue(entry)

        assertEquals(1, fakePlayer.addMediaItemCallCount)
        assertNotNull(fakePlayer.lastAddedMediaItem)
        assertEquals("Track t1", fakePlayer.lastAddedMediaItem?.title)
    }

    @Test
    fun `addToQueue skips local entry with blank url`() = runTest {
        val queue = createQueue()
        val entry = localEntry(url = "")

        queue.addToQueue(entry)

        assertEquals(0, fakePlayer.addMediaItemCallCount)
    }

    // --- addAllToQueue ---

    @Test
    fun `addAllToQueue adds multiple entries`() = runTest {
        val queue = createQueue()
        val entries = listOf(streamingEntry("t1"), streamingEntry("t2"))

        queue.addAllToQueue(entries)

        assertEquals(2, fakePlayer.addMediaItemCallCount)
    }

    @Test
    fun `addAllToQueue sets collection context`() = runTest {
        val queue = createQueue()
        val colEntry = QueueCollectionEntry.Album("al-1")

        queue.addAllToQueue(
            entries = listOf(streamingEntry("t1")),
            collectionEntry = colEntry
        )

        assertEquals(colEntry, queue.getCurrentCollectionEntry())
    }

    // --- addAllAfterCurrent ---

    @Test
    fun `addAllAfterCurrent inserts after current track`() = runTest {
        val queue = createQueue()
        val entries = listOf(streamingEntry("t1"))

        queue.addAllAfterCurrent(entries)

        assertEquals(1, fakePlayer.insertAtNextCallCount)
    }

    // --- removeFromQueue ---

    @Test
    fun `removeFromQueue removes entry by media item url`() = runTest {
        val queue = createQueue()
        val entry = streamingEntry("t1")
        queue.addToQueue(entry)

        fakePlayer.resetCallCounts()
        queue.removeFromQueue(entry)

        assertEquals(1, fakePlayer.removeMediaItemCallCount)
    }

    @Test
    fun `removeFromQueueByMediaUrl calls player remove`() = runTest {
        val queue = createQueue()
        val entry = streamingEntry("t1", url = "http://example.com/t1")
        queue.addToQueue(entry)

        fakePlayer.resetCallCounts()
        queue.removeFromQueueByMediaUrl("http://example.com/t1")

        assertEquals(1, fakePlayer.removeMediaItemCallCount)
    }

    // --- move ---

    @Test
    fun `move delegates to player`() = runTest {
        val queue = createQueue()

        queue.move(0, 2)

        assertEquals(1, fakePlayer.moveCallCount)
        assertEquals(0, fakePlayer.lastMoveFromIndex)
        assertEquals(2, fakePlayer.lastMoveToIndex)
    }

    // --- jumpTo ---

    @Test
    fun `jumpTo with autoPlay calls player jumpTo and play`() = runTest {
        val queue = createQueue()
        fakePlayer.setPlaylist(listOf(
            MediaItem("T1", "A1", "Al1", 100.milliseconds, "", "url1", StreamProtocol.PROGRESSIVE),
            MediaItem("T2", "A2", "Al2", 200.milliseconds, "", "url2", StreamProtocol.PROGRESSIVE)))

        queue.jumpTo(1, autoPlay = true)

        assertEquals(1, fakePlayer.jumpToCallCount)
        assertEquals(1, fakePlayer.lastJumpToIndex)
        assertEquals(1, fakePlayer.playCallCount)
    }

    @Test
    fun `jumpTo without autoPlay does not call play`() = runTest {
        val queue = createQueue()
        fakePlayer.setPlaylist(listOf(
            MediaItem("T1", "A1", "Al1", 100.milliseconds, "", "url1", StreamProtocol.PROGRESSIVE)))

        queue.jumpTo(0, autoPlay = false)

        assertEquals(1, fakePlayer.jumpToCallCount)
        assertEquals(0, fakePlayer.playCallCount)
    }

    @Test
    fun `jumpTo clamps out of range index`() = runTest {
        val queue = createQueue()
        fakePlayer.setPlaylist(listOf(
            MediaItem("T1", "A1", "Al1", 100.milliseconds, "", "url1", StreamProtocol.PROGRESSIVE)))

        queue.jumpTo(999)

        assertEquals(1, fakePlayer.jumpToCallCount)
        assertEquals(0, fakePlayer.lastJumpToIndex)
    }

    // --- reloadCurrent ---

    @Test
    fun `reloadCurrent does nothing when no current entry`() = runTest {
        val queue = createQueue()
        fakePlayer.setCurrentItem(null)

        queue.reloadCurrent()

        assertEquals(0, fakePlayer.jumpToCallCount)
    }

    // --- clear ---

    @Test
    fun `clear resets everything`() = runTest {
        val queue = createQueue()
        queue.load(listOf(streamingEntry("t1")), collectionEntry = QueueCollectionEntry.Playlist("pl-1"))

        queue.clear()

        val lastLoad = fakePlayer.lastLoadPlaylist
        assertTrue { lastLoad?.isEmpty() == true }
        assertEquals(false, fakePlayer.lastLoadAutoPlay)
        assertNull(queue.getCurrentCollectionEntry())
        assertTrue { queue.getCollectionHistory().isEmpty() }
    }

    // --- getQueue ---

    @Test
    fun `getQueue returns entries with metadata`() = runTest {
        val queue = createQueue()
        val entry = streamingEntry("t1", url = "url1")
        queue.addToQueue(entry)

        val result = queue.getQueue()

        assertEquals(1, result.size)
        val qEntry = result[0] as? QueueEntry.StreamingTrack
        assertNotNull(qEntry)
        assertEquals("t1", qEntry.track.id)
    }

    @Test
    fun `getCurrentQueueEntry returns current media as queue entry`() = runTest {
        val queue = createQueue()
        fakePlayer.setCurrentItem(MediaItem("Test", "Artist", "Album", 100.milliseconds, "", "url1", StreamProtocol.PROGRESSIVE))

        val result = queue.getCurrentQueueEntry()

        assertNotNull(result)
        val localEntry = result as? QueueEntry.LocalTrack
        assertNotNull(localEntry)
        assertEquals("Test", localEntry.name)
    }

    // --- collection history ---

    @Test
    fun `collection history tracks plays in order`() = runTest {
        val queue = createQueue()
        queue.addAllToQueue(
            listOf(streamingEntry("t1")),
            collectionEntry = QueueCollectionEntry.Playlist("pl-1")
        )
        queue.addAllToQueue(
            listOf(streamingEntry("t2")),
            collectionEntry = QueueCollectionEntry.Album("al-1")
        )

        val history = queue.getCollectionHistory()
        assertEquals(2, history.size)
        assertEquals(QueueCollectionEntry.Album("al-1"), history[0])
        assertEquals(QueueCollectionEntry.Playlist("pl-1"), history[1])
    }

    @Test
    fun `collection history deduplicates`() = runTest {
        val queue = createQueue()
        queue.addAllToQueue(
            listOf(streamingEntry("t1")),
            collectionEntry = QueueCollectionEntry.Playlist("pl-1")
        )
        queue.addAllToQueue(
            listOf(streamingEntry("t2")),
            collectionEntry = QueueCollectionEntry.Playlist("pl-1")
        )

        val history = queue.getCollectionHistory()
        assertEquals(1, history.size)
    }

    // --- persistence ---

    @Test
    fun `restorePersistedState loads from repository on init`() = runTest {
        val entries = listOf(streamingEntry("t1"))
        fakeRepo.setState(
            PersistedQueueState(
                entries = entries,
                currentIndex = 0,
                currentCollectionEntry = QueueCollectionEntry.Playlist("pl-1"),
                collectionHistory = listOf(QueueCollectionEntry.Playlist("pl-1"))
            )
        )

        val queue = createQueue()
        withContext(Dispatchers.Default) { } // yield to let init coroutine run

        assertEquals(1, fakePlayer.loadCallCount)
        assertEquals(QueueCollectionEntry.Playlist("pl-1"), queue.getCurrentCollectionEntry())
        assertEquals(1, queue.getCollectionHistory().size)
    }

    @Test
    fun `restorePersistedState skips when no persisted state`() = runTest {
        fakeRepo.setState(null)

        val queue = createQueue()
        withContext(Dispatchers.Default) { }

        assertEquals(0, fakePlayer.loadCallCount)
    }

    // --- toMediaItem / toFallbackQueueEntry ---

    @Test
    fun `streaming track converts to media item`() = runTest {
        val queue = createQueue()
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(playbackProxyServerPort = 14769)
        )

        val entry = streamingEntry("t1")
        queue.addToQueue(entry)

        val mediaItem = fakePlayer.lastAddedMediaItem
        assertNotNull(mediaItem)
        assertEquals("Track t1", mediaItem.title)
        assertEquals("Artist t1", mediaItem.artist)
        assertTrue { mediaItem.url.contains("/stream/t1") }
    }

    @Test
    fun `local track converts to media item`() = runTest {
        val queue = createQueue()
        val entry = localEntry()

        queue.addToQueue(entry)

        val mediaItem = fakePlayer.lastAddedMediaItem
        assertNotNull(mediaItem)
        assertEquals("Local Track", mediaItem.title)
        assertEquals("file:///music/Local Track.mp3", mediaItem.url)
    }

    @Test
    fun `fallback queue entry preserves media item fields`() = runTest {
        val queue = createQueue()
        val mediaItem = MediaItem(
            title = "Fallback",
            artist = "Artist1, Artist2",
            album = "Album",
            duration = 300.milliseconds,
            coverURL = "",
            url = "file:///test.mp3",
            protocol = StreamProtocol.PROGRESSIVE
        )
        fakePlayer.setCurrentItem(mediaItem)

        val result = queue.getCurrentQueueEntry()

        assertNotNull(result)
        val local = result as? QueueEntry.LocalTrack
        assertNotNull(local)
        assertEquals("Fallback", local.name)
        assertEquals(listOf("Artist1", "Artist2"), local.artists)
    }

    private suspend fun waitUntil(maxAttempts: Int = 100, condition: suspend () -> Boolean) {
        var attempts = 0
        while (attempts < maxAttempts) {
            if (condition()) return
            withContext(Dispatchers.IO) {
                Thread.sleep(25)
            }
            attempts++
        }
        assertTrue { condition() }
    }

    // --- endless playback ---

    @Test
    fun `endless playback appends recommendations when queue finishes`() = runTest {
        val recTrack = streamingTrack("rec1")
        val service = FakePluginService(
            metadataTrackAPI = FakeMetadataTrackAPI(recommendations = listOf(recTrack))
        )
        fakePlugin.setPlugin(service)
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(
                playbackProxyServerPort = 14769,
                enableEndlessPlayback = true
            )
        )

        val queue = createQueue()
        val seed = streamingEntry("t1")
        queue.load(listOf(seed))

        waitUntil {
            queue.getQueue().any { (it as? QueueEntry.StreamingTrack)?.track?.id == "rec1" }
        }
    }

    @Test
    fun `endless playback cascades to album tracks when recommendations empty`() = runTest {
        val albumTrack = streamingTrack("album-track-1")
        val service = FakePluginService(
            metadataTrackAPI = FakeMetadataTrackAPI(recommendations = emptyList()),
            metadataAlbumAPI = FakeMetadataAlbumAPI(
                albumTracks = mapOf("album-t1" to listOf(streamingTrack("t1"), albumTrack))
            )
        )
        fakePlugin.setPlugin(service)
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(
                playbackProxyServerPort = 14769,
                enableEndlessPlayback = true
            )
        )

        val queue = createQueue()
        val seed = streamingEntry("t1")
        queue.load(listOf(seed))

        waitUntil {
            queue.getQueue().any { (it as? QueueEntry.StreamingTrack)?.track?.id == "album-track-1" }
        }
        val queueTracks = queue.getQueue()
        assertEquals(1, queueTracks.count { (it as? QueueEntry.StreamingTrack)?.track?.id == "t1" })
    }

    @Test
    fun `endless playback cascades to artist top tracks when album tracks empty`() = runTest {
        val artistTrack = streamingTrack("top-track-1")
        val service = FakePluginService(
            metadataTrackAPI = FakeMetadataTrackAPI(recommendations = emptyList()),
            metadataAlbumAPI = FakeMetadataAlbumAPI(albumTracks = emptyMap()),
            metadataArtistAPI = FakeMetadataArtistAPI(
                topTracks = mapOf("artist-t1" to listOf(streamingTrack("t1"), artistTrack))
            )
        )
        fakePlugin.setPlugin(service)
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(
                playbackProxyServerPort = 14769,
                enableEndlessPlayback = true
            )
        )

        val queue = createQueue()
        val seed = streamingEntry("t1")
        queue.load(listOf(seed))

        waitUntil {
            queue.getQueue().any { (it as? QueueEntry.StreamingTrack)?.track?.id == "top-track-1" }
        }
        val queueTracks = queue.getQueue()
        assertEquals(1, queueTracks.count { (it as? QueueEntry.StreamingTrack)?.track?.id == "t1" })
    }

    @Test
    fun `endless playback filters out blacklisted tracks`() = runTest {
        val recTrack = streamingTrack("rec1")
        fakeBlacklist.toggleTrack(recTrack)

        val service = FakePluginService(
            metadataTrackAPI = FakeMetadataTrackAPI(recommendations = listOf(recTrack))
        )
        fakePlugin.setPlugin(service)
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(
                playbackProxyServerPort = 14769,
                enableEndlessPlayback = true
            )
        )

        val queue = createQueue()
        val seed = streamingEntry("t1")
        queue.load(listOf(seed))

        withContext(Dispatchers.IO) { Thread.sleep(200) }

        val queueTracks = queue.getQueue()
        assertTrue { queueTracks.none { (it as? QueueEntry.StreamingTrack)?.track?.id == "rec1" } }
    }

    @Test
    fun `endless playback does not fetch when setting is disabled`() = runTest {
        val recTrack = streamingTrack("rec1")
        val service = FakePluginService(
            metadataTrackAPI = FakeMetadataTrackAPI(recommendations = listOf(recTrack))
        )
        fakePlugin.setPlugin(service)
        fakeSettings.setSettings(
            org.spotube.spotube.modules.settings.UserSettings(
                playbackProxyServerPort = 14769,
                enableEndlessPlayback = false
            )
        )

        val queue = createQueue()
        val seed = streamingEntry("t1")
        queue.load(listOf(seed))

        withContext(Dispatchers.IO) { Thread.sleep(200) }

        val queueTracks = queue.getQueue()
        assertEquals(1, queueTracks.size)
        assertEquals("t1", (queueTracks[0] as? QueueEntry.StreamingTrack)?.track?.id)
    }
}
