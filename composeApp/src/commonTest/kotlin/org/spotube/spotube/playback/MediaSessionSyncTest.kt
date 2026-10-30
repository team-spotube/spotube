package org.spotube.spotube.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.mockk.every
import kotlin.test.mockk.mockk
import kotlin.test.mockk.verify
import kotlin.test.mockk.exactly

@ExperimentalCoroutinesApi
class MediaSessionSyncTest {

    @Test
    fun `publishes position after delay`() = runTest {
        val testClock = TestClock()
        val player = mockk<PlayerController> {
            every { currentState } returns PlaybackState(
                playing = true,
                tracks = listOf(Track(id = "1", title = "Test", artist = "A", album = "Alb", durationMs = 10000)),
                currentIndex = 0,
                position = 5000
            )
            every { position } returns 5000L
        }
        val mediaSession = mockk<MediaSessionPublisher>()

        val sync = MediaSessionSync(player, mediaSession)
        sync.publishPosition()

        // Advance time past delay
        testClock.advanceTime(300)

        verify(exactly = 1) { mediaSession.publishPlaybackState(any()) }
    }

    @Test
    fun `discards stale publish when new seek initiated`() = runTest {
        val testClock = TestClock()
        val player = mockk<PlayerController> {
            every { currentState } returns PlaybackState(
                playing = true,
                tracks = listOf(Track(id = "1", title = "Test", artist = "A", album = "Alb", durationMs = 10000)),
                currentIndex = 0
            )
            every { position } returns 5000L
        }
        val mediaSession = mockk<MediaSessionPublisher>()

        val sync = MediaSessionSync(player, mediaSession)

        // Start first publish
        val job1 = kotlinx.coroutines.launch { sync.publishPosition() }

        // Immediately start a seek (new generation)
        sync.seek(8000)

        // Advance time
        testClock.advanceTime(300)

        // First publish should be discarded, second should happen
        verify(exactly = 1) { mediaSession.publishPlaybackState(any()) }

        job1.cancel()
    }

    @Test
    fun `seek executes and publishes`() = runTest {
        val testClock = TestClock()
        val player = mockk<PlayerController> {
            every { currentState } returns PlaybackState(
                playing = true,
                tracks = listOf(Track(id = "1", title = "Test", artist = "A", album = "Alb", durationMs = 10000)),
                currentIndex = 0
            )
            every { position } returns 5000L
            every { seekTo(any()) } just Runs
        }
        val mediaSession = mockk<MediaSessionPublisher>()

        val sync = MediaSessionSync(player, mediaSession)
        sync.seek(8000)

        testClock.advanceTime(300)

        verify(exactly = 1) { player.seekTo(8000L) }
        verify(exactly = 1) { mediaSession.publishPlaybackState(any()) }
    }

    @Test
    fun `discards stale seek when newer seek initiated`() = runTest {
        val testClock = TestClock()
        val player = mockk<PlayerController> {
            every { currentState } returns PlaybackState(
                playing = true,
                tracks = listOf(Track(id = "1", title = "Test", artist = "A", album = "Alb", durationMs = 10000)),
                currentIndex = 0
            )
            every { position } returns 5000L
            every { seekTo(any()) } just Runs
        }
        val mediaSession = mockk<MediaSessionPublisher>()

        val sync = MediaSessionSync(player, mediaSession)

        // Start first seek
        val job1 = kotlinx.coroutines.launch { sync.seek(3000) }

        // Immediately start second seek
        sync.seek(8000)

        testClock.advanceTime(300)

        // First seek should be discarded (but second executes)
        verify(exactly = 1) { player.seekTo(8000L) }

        job1.cancel()
    }

    @Test
    fun `onStateChanged publishes immediately`() = runTest {
        val testClock = TestClock()
        val player = mockk<PlayerController> {
            every { currentState } returns PlaybackState(
                playing = true,
                tracks = listOf(Track(id = "1", title = "Test", artist = "A", album = "Alb", durationMs = 10000)),
                currentIndex = 0,
                position = 5000
            )
            every { position } returns 5000L
        }
        val mediaSession = mockk<MediaSessionPublisher>()

        val sync = MediaSessionSync(player, mediaSession)
        sync.onStateChanged()

        testClock.advanceTime(100)

        verify(exactly = 1) { mediaSession.publishPlaybackState(any()) }
    }
}