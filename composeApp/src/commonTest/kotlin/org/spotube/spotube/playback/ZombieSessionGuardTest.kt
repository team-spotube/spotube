package org.spotube.spotube.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.mockk.every
import kotlin.test.mockk.mockk
import kotlin.test.mockk.verify
import kotlin.test.mockk.exactly

@ExperimentalCoroutinesApi
class ZombieSessionGuardTest {

    @Test
    fun `detects zombie: playing with empty tracks`() = runTest {
        val player = mockk<PlayerStateReader> {
            every { state } returns PlaybackState(playing = true, tracks = emptyList())
            every { position } returns 0L
        }
        val audioService = mockk<AudioSessionController>()

        val guard = ZombieSessionGuard(player, audioService)
        val fixed = guard.checkAndFix()

        assertTrue(fixed)
        verify(exactly = 1) { audioService.pause() }
        verify(exactly = 1) { audioService.stop() }
    }

    @Test
    fun `detects zombie: position exceeds duration`() = runTest {
        val track = Track(id = "1", title = "Test", artist = "Artist", album = "Album", durationMs = 10000)
        val player = mockk<PlayerStateReader> {
            every { state } returns PlaybackState(
                playing = true,
                tracks = listOf(track),
                currentIndex = 0,
                position = 15000 // 15s > 10s duration
            )
            every { position } returns 15000L
        }
        val audioService = mockk<AudioSessionController>()

        val guard = ZombieSessionGuard(player, audioService)
        val fixed = guard.checkAndFix()

        assertTrue(fixed)
        verify(exactly = 1) { audioService.pause() }
        verify(exactly = 0) { audioService.stop() } // Only pause for position overrun
    }

    @Test
    fun `no zombie when not playing`() = runTest {
        val track = Track(id = "1", title = "Test", artist = "Artist", album = "Album", durationMs = 10000)
        val player = mockk<PlayerStateReader> {
            every { state } returns PlaybackState(
                playing = false,
                tracks = listOf(track),
                currentIndex = 0,
                position = 15000
            )
            every { position } returns 15000L
        }
        val audioService = mockk<AudioSessionController>()

        val guard = ZombieSessionGuard(player, audioService)
        val fixed = guard.checkAndFix()

        assertFalse(fixed)
        verify(exactly = 0) { audioService.pause() }
        verify(exactly = 0) { audioService.stop() }
    }

    @Test
    fun `no zombie when position within duration`() = runTest {
        val track = Track(id = "1", title = "Test", artist = "Artist", album = "Album", durationMs = 20000)
        val player = mockk<PlayerStateReader> {
            every { state } returns PlaybackState(
                playing = true,
                tracks = listOf(track),
                currentIndex = 0,
                position = 15000 // 15s < 20s duration
            )
            every { position } returns 15000L
        }
        val audioService = mockk<AudioSessionController>()

        val guard = ZombieSessionGuard(player, audioService)
        val fixed = guard.checkAndFix()

        assertFalse(fixed)
    }

    @Test
    fun `no zombie when currentIndex out of bounds`() = runTest {
        val player = mockk<PlayerStateReader> {
            every { state } returns PlaybackState(
                playing = true,
                tracks = emptyList(),
                currentIndex = 5, // Out of bounds
                position = 1000
            )
            every { position } returns 1000L
        }
        val audioService = mockk<AudioSessionController>()

        val guard = ZombieSessionGuard(player, audioService)
        val fixed = guard.checkAndFix()

        // Should detect empty tracks zombie
        assertTrue(fixed)
    }
}