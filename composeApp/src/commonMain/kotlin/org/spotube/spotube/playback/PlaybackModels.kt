package org.spotube.spotube.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable

/**
 * Playback state model for MediaSession resilience utilities.
 * Shared between commonMain and platform-specific implementations.
 */
@Serializable
data class PlaybackState(
    val playing: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val loopMode: LoopMode = LoopMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val position: Long = 0, // milliseconds
    val duration: Long = 0, // milliseconds
    val bufferedPosition: Long = 0, // milliseconds
    val speed: Float = 1.0f
) {
    val currentTrack: Track? get() = if (currentIndex in tracks.indices) tracks[currentIndex] else null
    val progress: Float get() = if (duration > 0) position.toFloat() / duration else 0f
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val externalUri: String = ""
)

enum class LoopMode {
    OFF,      // No repeat
    ALL,      // Repeat playlist/queue
    ONE       // Repeat current track
}

enum class ShuffleMode {
    OFF, ON
}

/**
 * Interface for reading player state (testable/mockable).
 */
@ExperimentalCoroutinesApi
interface PlayerStateReader {
    val state: PlaybackState
    val position: Long
}

/**
 * Interface for controlling audio session (testable/mockable).
 */
@ExperimentalCoroutinesApi
interface AudioSessionController {
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(position: Long)
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun setLoopMode(mode: LoopMode)
    suspend fun setShuffleMode(mode: ShuffleMode)
}

/**
 * MediaSession publisher interface for platform-specific implementations.
 */
@ExperimentalCoroutinesApi
interface MediaSessionPublisher {
    suspend fun publishPlaybackState(state: PlaybackState)
}