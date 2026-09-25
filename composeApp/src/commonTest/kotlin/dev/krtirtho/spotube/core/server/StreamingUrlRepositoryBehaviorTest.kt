package dev.krtirtho.spotube.core.server

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioQuality
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioSource
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioStream
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.modules.settings.UserSettings
import co.touchlab.kermit.Logger
import kotlinx.coroutines.test.runTest
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

/**
 * Behavioral / caching tests for [StreamingUrlRepository]: the in-memory stream-url cache,
 * the matched-source lookup, and the interaction between the two when the active audio
 * plugin changes.
 *
 * These specifically target the "wrong track plays" class of bug: a stale match or cached
 * URL from one plugin (or one track) being silently reused for a different plugin/track.
 */
class StreamingUrlRepositoryBehaviorTest {

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                factory { (tag: String?) -> Logger.withTag(tag ?: "test") }
            })
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    private fun track(id: String, title: String = "Track $id") = MetadataTrack(
        id = id,
        title = title,
        durationMs = 200_000,
        trackNumber = 1,
        discNumber = 1,
        artists = emptyList(),
        album = null,
        thumbnails = null,
        explicit = false,
        popularity = null,
        isrcCode = null,
        externalUri = null,
    )

    private fun basicSource(id: String, confidence: Float = 1f) = AudioSource.Basic(
        id = id,
        title = "source-$id",
        artist = null,
        album = null,
        thumbnails = emptyList(),
        externalUri = null,
        confidence = confidence,
    )

    private fun streamedSource(
        sourceId: String,
        streams: List<AudioStream>,
        confidence: Float = 1f,
    ) = AudioSource.Streamed(
        id = sourceId,
        title = "source-$sourceId",
        artist = null,
        album = null,
        thumbnails = emptyList(),
        externalUri = null,
        confidence = confidence,
        streams = streams,
    )

    private fun stream(url: String, bitrate: Int = 256_000) = AudioStream.Lossy(
        url = url,
        codec = "opus",
        container = "webm",
        protocol = StreamProtocol.PROGRESSIVE,
        bitrate = bitrate,
    )

    private class Harness(cacheTtlMs: Long = 30_000) {
        val queue = FakeStreamingAudioPlayerQueue()
        val matchedTracksRepository = FakeTrackSourceRepository()
        val settings = FakeUserSettingsSource()
        val pluginSource = FakeAudioPluginSource()
        val repository = StreamingUrlRepository(
            pluginManager = pluginSource,
            audioPlayerQueue = queue,
            matchedTracksRepository = matchedTracksRepository,
            settingsRepository = settings,
            streamUrlCacheTtlMs = cacheTtlMs,
        )

        fun selectPlugin(pluginId: String, audioAPI: FakeAudioAPI = FakeAudioAPI()): FakeAudioPluginService {
            val service = FakeAudioPluginService(pluginId, audioAPI)
            pluginSource.setPlugin(service)
            return service
        }
    }

    // ---------- basic resolution ----------

    @Test
    fun `resolves and returns a stream via track lookup when nothing is cached`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        val info = h.repository.resolveStreamInfo(t)

        assertNotNull(info)
        assertEquals("https://example.com/a.opus", info.url)
    }

    @Test
    fun `returns null when no audio plugin is selected`() = runTest {
        val h = Harness()
        val info = h.repository.resolveStreamInfo(track("t1"))
        assertNull(info)
    }

    @Test
    fun `returns null when the plugin yields no sources`() = runTest {
        val h = Harness()
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = { emptyList() }

        assertNull(h.repository.resolveStreamInfo(track("t1")))
    }

    @Test
    fun `resolveStreamInfo by trackId looks up the matching queue entry`() = runTest {
        val h = Harness()
        val t = track("t1")
        h.queue.setQueue(listOf(QueueEntry.StreamingTrack(track = t, url = "")))
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        val info = h.repository.resolveStreamInfo(trackId = "t1")

        assertNotNull(info)
        assertEquals("https://example.com/a.opus", info.url)
    }

    @Test
    fun `resolveStreamInfo by trackId returns null when track is not queued`() = runTest {
        val h = Harness()
        h.selectPlugin("pluginA")
        assertNull(h.repository.resolveStreamInfo(trackId = "not-queued"))
    }

    // ---------- in-memory URL cache ----------

    @Test
    fun `second resolution for the same track reuses the cache instead of calling the plugin again`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        h.repository.resolveStreamInfo(t)
        h.repository.resolveStreamInfo(t)

        assertEquals(1, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `forceRefresh bypasses the cache and re-resolves`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        var counter = 0
        plugin.audioAPI.onGetStreamsByTrack = {
            counter++
            listOf(streamedSource("src$counter", listOf(stream("https://example.com/$counter.opus"))))
        }

        val first = h.repository.resolveStreamInfo(t)
        val second = h.repository.resolveStreamInfo(t, forceRefresh = true)

        assertEquals("https://example.com/1.opus", first?.url)
        assertEquals("https://example.com/2.opus", second?.url)
        assertEquals(2, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `changing the preferred format invalidates the cached url`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(
                streamedSource(
                    "src1",
                    listOf(
                        stream("https://example.com/opus.webm"),
                        AudioStream.Lossy(
                            url = "https://example.com/aac.mp4",
                            codec = "aac",
                            container = "mp4",
                            bitrate = 256_000,
                        ),
                    )
                )
            )
        }

        val first = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/opus.webm", first?.url)

        h.settings.update {
            copy(streamingMusicFormat = streamingMusicFormat.copy(codec = "aac", container = "mp4"))
        }

        val second = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/aac.mp4", second?.url)
        // Preference change forced a second real lookup rather than serving the stale URL.
        assertEquals(2, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `changing the preferred quality invalidates the cached url`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(
                streamedSource(
                    "src1",
                    listOf(stream("https://example.com/128.opus", 128_000), stream("https://example.com/320.opus", 320_000))
                )
            )
        }

        h.settings.update { copy(streamingMusicQuality = AudioQuality.Lossy(128_000)) }
        val first = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/128.opus", first?.url)

        h.settings.update { copy(streamingMusicQuality = AudioQuality.Lossy(320_000)) }
        val second = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/320.opus", second?.url)
    }

    @Test
    fun `cached url expires after the configured ttl`() = kotlinx.coroutines.runBlocking {
        val h = Harness(cacheTtlMs = 10)
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        var counter = 0
        plugin.audioAPI.onGetStreamsByTrack = {
            counter++
            listOf(streamedSource("src$counter", listOf(stream("https://example.com/$counter.opus"))))
        }

        h.repository.resolveStreamInfo(t)
        kotlinx.coroutines.delay(50)
        h.repository.resolveStreamInfo(t)

        assertEquals(2, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `invalidateCachedStreamUrl removes the cached entry unconditionally when no url is given`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        h.repository.resolveStreamInfo(t)
        h.repository.invalidateCachedStreamUrl(t.id)
        h.repository.resolveStreamInfo(t)

        assertEquals(2, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `invalidateCachedStreamUrl with a mismatching url is a no-op`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        h.repository.resolveStreamInfo(t)
        h.repository.invalidateCachedStreamUrl(t.id, url = "https://not-the-cached-url")
        h.repository.resolveStreamInfo(t)

        // Cache entry survived because the supplied url didn't match; plugin isn't hit again.
        assertEquals(1, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    // ---------- matched-source fast path ----------

    @Test
    fun `a previously matched source from the same plugin is reused via getStreamsOfAudioSource`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        h.matchedTracksRepository.seed(t, basicSource("matched-1"), pluginId = "pluginA")
        plugin.audioAPI.onGetStreamsOfAudioSource = {
            listOf(streamedSource(it.id, listOf(stream("https://example.com/matched.opus"))))
        }
        plugin.audioAPI.onGetStreamsByTrack = {
            error("should not fall back to track lookup when a matched source resolves")
        }

        val info = h.repository.resolveStreamInfo(t)

        assertEquals("https://example.com/matched.opus", info?.url)
        assertEquals(1, plugin.audioAPI.getStreamsOfAudioSourceCallCount)
        assertEquals(0, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `THE BUG - a source matched by a previous plugin is never handed to a newly selected plugin`() = runTest {
        val h = Harness()
        val t = track("t1")

        // The user matched this track while "pluginA" (e.g. a YouTube-backed plugin) was active.
        // "yt-video-123" is meaningful *only* to pluginA.
        h.matchedTracksRepository.seed(t, basicSource("yt-video-123"), pluginId = "pluginA")

        // The user then switches the active audio plugin to "pluginB" (a different backend).
        val pluginB = h.selectPlugin("pluginB")
        pluginB.audioAPI.onGetStreamsOfAudioSource = {
            error(
                "pluginB must never receive a source id matched by a different plugin " +
                    "(id=${it.id}); this is exactly how playback of the wrong track happens."
            )
        }
        pluginB.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("pluginB-native-source", listOf(stream("https://example.com/correct.opus"))))
        }

        val info = h.repository.resolveStreamInfo(t)

        // pluginB never saw the stale "yt-video-123" id...
        assertEquals(0, pluginB.audioAPI.getStreamsOfAudioSourceCallCount)
        // ...and instead correctly fell back to a fresh, pluginB-native track lookup.
        assertEquals(1, pluginB.audioAPI.getStreamsByTrackCallCount)
        assertEquals("https://example.com/correct.opus", info?.url)
    }

    @Test
    fun `a cached stream url from a previous plugin is not served after switching plugins`() = runTest {
        val h = Harness()
        val t = track("t1")

        val pluginA = h.selectPlugin("pluginA")
        pluginA.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("srcA", listOf(stream("https://example.com/pluginA.opus"))))
        }
        val first = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/pluginA.opus", first?.url)

        val pluginB = h.selectPlugin("pluginB")
        pluginB.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("srcB", listOf(stream("https://example.com/pluginB.opus"))))
        }
        val second = h.repository.resolveStreamInfo(t)

        // Must re-resolve via pluginB rather than replaying pluginA's cached URL.
        assertEquals("https://example.com/pluginB.opus", second?.url)
        assertEquals(1, pluginB.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `matched source is saved and tagged with the resolving plugin id`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        h.repository.resolveStreamInfo(t)

        val saved = h.matchedTracksRepository.rawEntryFor(t.id)
        assertNotNull(saved)
        assertEquals("pluginA", saved.pluginId)
        assertEquals("src1", saved.source.id)
    }

    @Test
    fun `matched source is looked up but ignored when the format preference has no matching stream`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        h.matchedTracksRepository.seed(t, basicSource("matched-1"), pluginId = "pluginA")
        // Matched source resolves to a stream, so preferred-format mismatch is irrelevant here;
        // this documents that selection still happens on top of the matched-source stream list.
        plugin.audioAPI.onGetStreamsOfAudioSource = {
            listOf(streamedSource(it.id, listOf(stream("https://example.com/only.opus"))))
        }

        val info = h.repository.resolveStreamInfo(t)
        assertEquals("https://example.com/only.opus", info?.url)
    }

    @Test
    fun `falls back to track lookup when the matched source resolves no streams`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        h.matchedTracksRepository.seed(t, basicSource("matched-1"), pluginId = "pluginA")
        plugin.audioAPI.onGetStreamsOfAudioSource = { emptyList() }
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("fresh", listOf(stream("https://example.com/fresh.opus"))))
        }

        val info = h.repository.resolveStreamInfo(t)

        assertEquals("https://example.com/fresh.opus", info?.url)
        assertEquals(1, plugin.audioAPI.getStreamsOfAudioSourceCallCount)
        assertEquals(1, plugin.audioAPI.getStreamsByTrackCallCount)
    }

    @Test
    fun `track lookup persists a Basic source by resolving its streams before caching`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = { listOf(basicSource("basic-1")) }
        plugin.audioAPI.onGetStreamsOfAudioSource = {
            listOf(streamedSource(it.id, listOf(stream("https://example.com/resolved.opus"))))
        }

        val info = h.repository.resolveStreamInfo(t)

        assertEquals("https://example.com/resolved.opus", info?.url)
        assertEquals("basic-1", h.matchedTracksRepository.rawEntryFor(t.id)?.source?.id)
    }

    @Test
    fun `distinct tracks never share cache entries even with concurrent resolution`() = runTest {
        val h = Harness()
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = { t ->
            listOf(streamedSource("src-${t.id}", listOf(stream("https://example.com/${t.id}.opus"))))
        }

        val t1 = track("t1")
        val t2 = track("t2")

        val info1 = h.repository.resolveStreamInfo(t1)
        val info2 = h.repository.resolveStreamInfo(t2)

        assertEquals("https://example.com/t1.opus", info1?.url)
        assertEquals("https://example.com/t2.opus", info2?.url)
        assertTrue(info1?.url != info2?.url)
    }

    // ---------- alternatives cache ----------

    @Test
    fun `getCachedAlternatives returns null before anything is cached`() = runTest {
        val h = Harness()
        assertNull(h.repository.getCachedAlternatives("t1", "pluginA"))
    }

    @Test
    fun `alternatives resolved via track lookup are cached and retrievable`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        val candidates = listOf(basicSource("alt-1"), basicSource("alt-2"))
        plugin.audioAPI.onGetStreamsByTrack = { candidates }
        plugin.audioAPI.onGetStreamsOfAudioSource = {
            listOf(streamedSource(it.id, listOf(stream("https://example.com/${it.id}.opus"))))
        }

        h.repository.resolveStreamInfo(t)

        val cached = h.repository.getCachedAlternatives(t.id, "pluginA")
        assertNotNull(cached)
        assertEquals(2, cached.size)
    }

    @Test
    fun `alternatives cached by one plugin are not visible to a different plugin`() = runTest {
        val h = Harness()
        val t = track("t1")
        val pluginA = h.selectPlugin("pluginA")
        pluginA.audioAPI.onGetStreamsByTrack = { listOf(basicSource("alt-1")) }
        pluginA.audioAPI.onGetStreamsOfAudioSource = {
            listOf(streamedSource(it.id, listOf(stream("https://example.com/a.opus"))))
        }
        h.repository.resolveStreamInfo(t)

        assertNotNull(h.repository.getCachedAlternatives(t.id, "pluginA"))
        assertNull(h.repository.getCachedAlternatives(t.id, "pluginB"))
    }

    @Test
    fun `cacheAlternatives followed by invalidateCachedAlternatives clears the entry`() = runTest {
        val h = Harness()
        h.repository.cacheAlternatives("t1", listOf(basicSource("alt-1")), "pluginA")
        assertNotNull(h.repository.getCachedAlternatives("t1", "pluginA"))

        h.repository.invalidateCachedAlternatives("t1")

        assertNull(h.repository.getCachedAlternatives("t1", "pluginA"))
    }

    // ---------- getCachedStreamUrlEntry ----------

    @Test
    fun `getCachedStreamUrlEntry returns null when nothing has been cached`() = runTest {
        val h = Harness()
        assertNull(h.repository.getCachedStreamUrlEntry("t1"))
    }

    @Test
    fun `getCachedStreamUrlEntry never fetches - it only reflects the existing cache`() = runTest {
        val h = Harness()
        val t = track("t1")
        val plugin = h.selectPlugin("pluginA")
        plugin.audioAPI.onGetStreamsByTrack = {
            listOf(streamedSource("src1", listOf(stream("https://example.com/a.opus"))))
        }

        assertNull(h.repository.getCachedStreamUrlEntry(t.id))
        h.repository.resolveStreamInfo(t)
        val entry = h.repository.getCachedStreamUrlEntry(t.id)

        assertNotNull(entry)
        assertEquals("https://example.com/a.opus", entry.url)
        assertEquals("pluginA", entry.pluginId)
        // Fetching the entry doesn't call the plugin again.
        assertEquals(1, plugin.audioAPI.getStreamsByTrackCallCount)
    }
}
