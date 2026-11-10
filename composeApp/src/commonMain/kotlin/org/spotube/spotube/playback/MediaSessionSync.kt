package org.spotube.spotube.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Synchronizes player position with MediaSession using generation counter.
 *
 * Prevents race conditions between concurrent seeks and position publications:
 * - When a seek is initiated, a new generation counter is incremented
 * - Position publications are delayed and checked against current generation
 * - Stale publications (from older seeks) are discarded
 *
 * Critical for Android Auto where head unit seeks race with app-initiated seeks.
 *
 * Based on DuranMusic's MediaSessionSync in `media_session_sync.dart`.
 */
@ExperimentalCoroutinesApi
class MediaSessionSync(
    private val player: PlayerController,
    private val mediaSession: MediaSessionPublisher
) {

    private var _syncGeneration = 0
    private val lock = Any()

    /**
     * Publishes current position to MediaSession, clamped to duration.
     * Discards if a newer seek has been initiated.
     */
    suspend fun publishPosition() {
        val generation = synchronized(lock) { ++_syncGeneration }

        // Small delay to batch rapid position updates
        kotlinx.coroutines.delay(200)

        // Discard if stale (newer seek initiated)
        synchronized(lock) {
            if (generation != _syncGeneration) return
        }

        val state = player.currentState
        val track = state.currentTrack
        val duration = track?.durationMs.toLong() ?: 0L
        val clamped = PositionClamp.clampPosition(player.position, duration)

        // Publish clamped position
        val updatedState = state.copy(
            position = clamped,
            duration = duration
        )
        mediaSession.publishPlaybackState(updatedState)
    }

    /**
     * Executes a seek and publishes new position atomically.
     * Discards if a newer seek has been initiated.
     */
    suspend fun seek(position: Long) {
        val generation = synchronized(lock) { ++_syncGeneration }

        player.seekTo(position)

        // Discard if stale
        synchronized(lock) {
            if (generation != _syncGeneration) return
        }

        await publishPosition()
    }

    /**
     * Called when player state changes externally (e.g., track change).
     * Publishes new state immediately.
     */
    suspend fun onStateChanged() {
        // Use a new generation to ensure this publishes
        val generation = synchronized(lock) { ++_syncGeneration }
        kotlinx.coroutines.delay(50)

        synchronized(lock) {
            if (generation != _syncGeneration) return
        }

        await publishPosition()
    }
}

/**
 * Interface for controlling player (seeking, position).
 */
@ExperimentalCoroutinesApi
interface PlayerController {
    val currentState: PlaybackState
    val position: Long
    suspend fun seekTo(position: Long)
}

/**
 * Interface for publishing to MediaSession.
 */
@ExperimentalCoroutinesApi
interface MediaSessionPublisher {
    suspend fun publishPlaybackState(state: PlaybackState)
}