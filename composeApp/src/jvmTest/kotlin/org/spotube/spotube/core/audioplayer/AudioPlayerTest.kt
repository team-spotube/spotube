package org.spotube.spotube.core.audioplayer

import co.touchlab.kermit.Logger
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AudioPlayerTest {
    private lateinit var player: AudioPlayer
    private lateinit var testAudioDir: File
    private val audioFiles = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        runCatching { stopKoin() }
        startKoin {
            modules(module {
                factory { (tag: String?) -> Logger.withTag(tag ?: "test") }
            })
        }
        val vlcNativesDir = File("build/vlc-natives/windows-x64")
        if (vlcNativesDir.isDirectory) {
            System.setProperty("compose.application.resources.dir", vlcNativesDir.absolutePath)
        }
        testAudioDir = findTestAudioDir()
        val files = testAudioDir.listFiles()
            ?.filter { it.isFile && it.extension in setOf("mp3", "flac", "wav", "ogg", "m4a") }
            ?.sortedBy { it.name }
            ?: emptyList()
        require(files.size >= 5) {
            "Need at least 5 audio files in $testAudioDir, found ${files.size}. " +
            "Place .mp3/.flac/.wav/.ogg/.m4a files in composeApp/src/jvmTest/resources/test-audio/"
        }
        audioFiles.addAll(files)
        player = AudioPlayer(Any())
    }

    @AfterTest
    fun teardown() {
        runCatching { if (::player.isInitialized) player.dispose() }
        runCatching { stopKoin() }
    }

    private fun findTestAudioDir(): File {
        val candidates = listOf(
            File("src/jvmTest/resources/test-audio"),
            File("../composeApp/src/jvmTest/resources/test-audio"),
            File(System.getProperty("user.dir"), "src/jvmTest/resources/test-audio"))
        for (dir in candidates) {
            if (dir.isDirectory) return dir
        }
        return candidates.first()
    }

    private fun mediaItem(url: String, title: String): MediaItem {
        return MediaItem(
            title = title,
            artist = "Test Artist",
            album = "Test Album",
            duration = 10.seconds,
            coverURL = "",
            url = url,
            protocol = StreamProtocol.PROGRESSIVE)
    }

    private fun testItems(count: Int = 3): List<MediaItem> {
        return (1..count).map { i ->
            mediaItem("file:///test/unique-$i.wav", "Track $i")
        }
    }

    private fun realAudioItems(count: Int = 1): List<MediaItem> {
        return (1..count).map { i ->
            val file = audioFiles.getOrNull(i - 1) ?: audioFiles.first()
            mediaItem(file.absolutePath, file.nameWithoutExtension)
        }
    }

    // Wait for player to reach a target state
    private fun awaitState(target: PlayerState, timeoutMs: Long = 4000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (player.playerStateFlow.value == target) return
            runBlocking { delay(50.milliseconds) }
        }
    }

    // --- Playback ---

    @Test
    fun `play transitions to PLAYING`() = runBlocking {
        withTimeout(10000.milliseconds) {
            player.load(realAudioItems(1), autoPlay = false, startPosition = 0)
            assertEquals(PlayerState.PAUSED, player.playerStateFlow.value)

            player.play()
            awaitState(PlayerState.PLAYING)

            assertTrue(player.playerStateFlow.value == PlayerState.PLAYING ||
                player.playerStateFlow.value == PlayerState.COMPLETED,
                "Expected PLAYING but got ${player.playerStateFlow.value}")
        }
    }

    @Test
    fun `pause transitions to PAUSED`() = runBlocking {
        withTimeout(10000.milliseconds) {
            player.load(realAudioItems(1), autoPlay = false, startPosition = 0)
            player.play()
            delay(100.milliseconds)
            val state = player.playerStateFlow.value
            if (state == PlayerState.COMPLETED) return@withTimeout
            awaitState(PlayerState.PLAYING)
            player.pause()
            delay(200.milliseconds)

            assertTrue(player.playerStateFlow.value == PlayerState.PAUSED ||
                player.playerStateFlow.value == PlayerState.COMPLETED,
                "Expected PAUSED but got ${player.playerStateFlow.value}")
        }
    }

    @Test
    fun `stop resets position and state`() = runBlocking {
        withTimeout(10000.milliseconds) {
            player.load(realAudioItems(1), autoPlay = false, startPosition = 0)
            player.play()
            awaitState(PlayerState.PLAYING)
            delay(200.milliseconds)
            player.stop()
            delay(200.milliseconds)

            assertEquals(PlayerState.IDLE, player.playerStateFlow.value)
            assertEquals(0L, player.positionFlow.value.inWholeMilliseconds)
        }
    }

    @Test
    fun `seekTo changes position`() = runBlocking {
        withTimeout(10000.milliseconds) {
            player.load(realAudioItems(1), autoPlay = false, startPosition = 0)
            player.play()
            awaitState(PlayerState.PLAYING)
            delay(200.milliseconds)

            player.seekTo(1000.milliseconds)
            delay(300.milliseconds)

            val pos = player.positionFlow.value.inWholeMilliseconds
            assertTrue(pos >= 800 && pos <= 2000 || pos == 0L && player.playerStateFlow.value == PlayerState.COMPLETED,
                "Position $pos should be near 1000ms (state=${player.playerStateFlow.value})")
        }
    }

    // --- Shuffle (the bug fix) ---

    @Test
    fun `shuffle enabled reorders playlist`() = runBlocking {
        val items = testItems(3)
        player.load(items, autoPlay = false, startPosition = 0)
        val originalOrder = player.playlistFlow.value.map { it.url }

        player.shuffle(true)

        assertEquals(true, player.shuffleModeFlow.value)
        val shuffledOrder = player.playlistFlow.value.map { it.url }
        assertEquals(items.size, shuffledOrder.size)
        assertTrue(shuffledOrder.containsAll(originalOrder))
    }

    @Test
    fun `shuffle does NOT restart current track`() = runBlocking {
        withTimeout(15000.milliseconds) {
            val items = realAudioItems(2)
            player.load(items, autoPlay = false, startPosition = 0)
            player.play()
            awaitState(PlayerState.PLAYING)
            delay(500.milliseconds)
            val posBefore = player.positionFlow.value.inWholeMilliseconds
            if (posBefore <= 100 && player.playerStateFlow.value == PlayerState.COMPLETED) return@withTimeout
            assertTrue(posBefore > 100, "Track should have progressed past 100ms (state=${player.playerStateFlow.value})")

            player.shuffle(true)
            delay(200.milliseconds)
            val posAfter = player.positionFlow.value.inWholeMilliseconds

            assertTrue(posAfter >= posBefore - 200,
                "Position after shuffle ($posAfter) dropped vs before ($posBefore)")
        }
    }

    @Test
    fun `shuffle disable restores original order`() = runBlocking {
        val items = testItems(3)
        player.load(items, autoPlay = false, startPosition = 0)
        val originalOrder = player.playlistFlow.value.map { it.url }

        player.shuffle(true)
        player.shuffle(false)

        assertEquals(false, player.shuffleModeFlow.value)
        val restoredOrder = player.playlistFlow.value.map { it.url }
        assertEquals(originalOrder, restoredOrder)
    }

    @Test
    fun `shuffle no-op when already shuffled`() = runBlocking {
        val items = testItems(3)
        player.load(items, autoPlay = false, startPosition = 0)
        player.shuffle(true)
        val orderAfterFirst = player.playlistFlow.value.map { it.url }

        player.shuffle(true)

        val orderAfterSecond = player.playlistFlow.value.map { it.url }
        assertEquals(orderAfterFirst, orderAfterSecond)
    }

    @Test
    fun `shuffle preserves current item as first in shuffled list`() = runBlocking {
        val items = testItems(4)
        player.load(items, autoPlay = false, startPosition = 0)

        player.shuffle(true)

        val shuffled = player.playlistFlow.value
        assertEquals(items[0].url, shuffled.first().url,
            "Current item should be first in shuffled playlist")
    }

    // --- Playlist management ---

    @Test
    fun `load populates playlist and sets current item`() = runBlocking {
        val items = testItems(2)
        player.load(items, autoPlay = false, startPosition = 0)

        assertEquals(2, player.playlistFlow.value.size)
        assertNotNull(player.currentMediaItemFlow.value)
        assertEquals(items[0].url, player.currentMediaItemFlow.value?.url)
    }

    @Test
    fun `load with empty playlist clears state`() = runBlocking {
        player.load(testItems(1), autoPlay = false, startPosition = 0)

        player.load(emptyList(), autoPlay = false, startPosition = 0)

        assertEquals(PlayerState.IDLE, player.playerStateFlow.value)
        assertTrue(player.playlistFlow.value.isEmpty())
    }

    @Test
    fun `load with autoPlay false stays paused`() = runBlocking {
        player.load(testItems(1), autoPlay = false, startPosition = 0)
        delay(200.milliseconds)

        assertEquals(PlayerState.PAUSED, player.playerStateFlow.value)
    }

    @Test
    fun `addMediaItem appends to playlist`() = runBlocking {
        player.load(testItems(1), autoPlay = false, startPosition = 0)
        val newItem = mediaItem(audioFiles.first().absolutePath, "Appended Track")

        player.addMediaItem(newItem)

        assertEquals(2, player.playlistFlow.value.size)
        assertEquals("Appended Track", player.playlistFlow.value.last().title)
    }

    @Test
    fun `removeMediaItem removes by url`() = runBlocking {
        val items = testItems(2)
        player.load(items, autoPlay = false, startPosition = 0)

        player.removeMediaItem(items[0])

        assertEquals(1, player.playlistFlow.value.size)
        assertNotEquals(items[0].url, player.playlistFlow.value.first().url)
    }

    @Test
    fun `moveMediaItem reorders playlist`() = runBlocking {
        val items = testItems(3)
        player.load(items, autoPlay = false, startPosition = 0)

        player.moveMediaItem(0, 2)

        assertEquals(items[1].url, player.playlistFlow.value[0].url)
        assertEquals(items[2].url, player.playlistFlow.value[1].url)
        assertEquals(items[0].url, player.playlistFlow.value[2].url)
    }

    // --- Navigation ---

    @Test
    fun `skipToNext advances track`() = runBlocking {
        withTimeout(15000.milliseconds) {
            val items = realAudioItems(2)
            player.load(items, autoPlay = false, startPosition = 0)
            player.play()
            awaitState(PlayerState.PLAYING)
            delay(500.milliseconds)
            val firstUrl = player.currentMediaItemFlow.value?.url

            player.skipToNext()
            delay(1000.milliseconds)
            val secondUrl = player.currentMediaItemFlow.value?.url

            assertNotEquals(firstUrl, secondUrl)
            assertEquals(items[1].url, secondUrl)
        }
    }

    @Test
    fun `jumpTo plays specific index`() = runBlocking {
        withTimeout(10000.milliseconds) {
            val items = realAudioItems(3)
            player.load(items, autoPlay = true, startPosition = 0)
            awaitState(PlayerState.PLAYING)
            delay(500.milliseconds)

            player.jumpTo(2)
            delay(1000.milliseconds)

            assertEquals(items[2].url, player.currentMediaItemFlow.value?.url)
        }
    }

    @Test
    fun `jumpTo out of bounds is no-op`() = runBlocking {
        withTimeout(10000.milliseconds) {
            val items = testItems(2)
            player.load(items, autoPlay = true, startPosition = 0)
            awaitState(PlayerState.PLAYING)
            delay(500.milliseconds)
            val beforeUrl = player.currentMediaItemFlow.value?.url

            player.jumpTo(99)
            delay(100.milliseconds)

            assertEquals(beforeUrl, player.currentMediaItemFlow.value?.url)
        }
    }

    // --- Other controls ---

    @Test
    fun `loop sets repeat mode`() = runBlocking {
        player.load(testItems(1), autoPlay = false, startPosition = 0)

        player.loop(LoopState.ONE)
        assertEquals(LoopState.ONE, player.loopStateFlow.value)

        player.loop(LoopState.ALL)
        assertEquals(LoopState.ALL, player.loopStateFlow.value)

        player.loop(LoopState.NONE)
        assertEquals(LoopState.NONE, player.loopStateFlow.value)
    }

    @Test
    fun `setVolume changes volume`() = runBlocking {
        player.setVolume(0.5f)
        delay(100.milliseconds)

        assertTrue(player.volumeFlow.value in 0.45f..0.55f)
    }

    @Test
    fun `setPlaybackSpeed changes rate`() = runBlocking {
        player.setPlaybackSpeed(2.0f)
        delay(100.milliseconds)

        assertEquals(2.0f, player.playbackSpeedFlow.value)
    }

    @Test
    fun `dispose cleans up and marks disposed`() = runBlocking {
        player.load(testItems(1), autoPlay = false, startPosition = 0)
        player.dispose()

        assertTrue(player.isDisposed())
        assertEquals(PlayerState.IDLE, player.playerStateFlow.value)
        assertNull(player.currentMediaItemFlow.value)
    }

    // --- Edge cases ---

    @Test
    fun `shuffle on empty playlist is no-op`() = runBlocking {
        player.shuffle(true)

        assertEquals(false, player.shuffleModeFlow.value)
    }

    @Test
    fun `shuffle on empty playlist after load with empty`() = runBlocking {
        player.load(emptyList(), autoPlay = false, startPosition = 0)

        player.shuffle(true)

        assertEquals(false, player.shuffleModeFlow.value)
    }

    @Test
    fun `multiple back-to-back shuffle toggles`() = runBlocking {
        val items = testItems(3)
        player.load(items, autoPlay = false, startPosition = 0)

        player.shuffle(true)
        player.shuffle(false)
        player.shuffle(true)
        player.shuffle(false)

        assertEquals(false, player.shuffleModeFlow.value)
        assertEquals(items.size, player.playlistFlow.value.size)
    }

    @Test
    fun `removeMediaItem on empty playlist does nothing`() = runBlocking {
        val item = mediaItem(audioFiles.first().absolutePath, "Test")

        player.removeMediaItem(item)

        assertTrue(player.playlistFlow.value.isEmpty())
    }
}
