package org.spotube.spotube.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Detects and prevents "zombie sessions" in MediaSession.
 *
 * A zombie session occurs when MediaSession reports PLAYING state but:
 * - No track is loaded (empty queue)
 * - Position exceeds track duration (stale position)
 *
 * This leaves the app in an inconsistent state: UI says "playing" but no audio.
 *
 * Based on DuranMusic's ZombieSessionGuard in `zombie_session_guard.dart`.
 * Call periodically or after player state changes.
 */
@ExperimentalCoroutinesApi
class ZombieSessionGuard(
    private val player: PlayerStateReader,
    private val audioService: AudioSessionController
) {

    /**
     * Checks for zombie session and corrects it by pausing/stopping.
     * Returns true if a zombie session was detected and fixed.
     */
    suspend fun checkAndFix(): Boolean {
        // Atomic snapshot to avoid race conditions between state reads
        val state = player.state
        val playing = state.playing
        val tracks = state.tracks
        val currentIndex = state.currentIndex
        val position = player.position

        // Zombie: playing but no tracks loaded
        if (playing && tracks.isEmpty()) {
            audioService.pause()
            audioService.stop()
            return true
        }

        // Zombie: position exceeded track duration
        if (currentIndex in tracks.indices) {
            val track = tracks[currentIndex]
            if (playing) {
                val duration = track.durationMs
                if (duration > 0 && position > duration) {
                    // Position overran track duration: possible stale session
                    audioService.pause()
                    return true
                }
            }
        }

        return false
    }

    /**
     * Starts a periodic check coroutine.
     * Call from a lifecycle-aware scope (e.g., viewModelScope).
     */
    fun startPeriodicCheck(
        intervalMillis: Long = 5000,
        scope: kotlinx.coroutines.CoroutineScope
    ): kotlinx.coroutines.Job {
        return scope.launch {
            while (true) {
                checkAndFix()
                kotlinx.coroutines.delay(intervalMillis)
            }
        }
    }
}