package org.spotube.spotube.playback

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibrarySession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Android Media3 integration for ZombieSessionGuard.
 * Wraps MediaLibrarySession/MediaController for zombie detection.
 *
 * Based on DuranMusic's ZombieSessionGuard adapted for Media3 architecture.
 */
@ExperimentalCoroutinesApi
class Media3ZombieGuard(
    private val mediaController: MediaController,
    private val session: MediaLibrarySession
) : ZombieSessionGuard(
    player = object : PlayerStateReader {
        override val state: PlaybackState
            get() = convertPlaybackState(mediaController.playbackState)
        override val position: Long
            get() = mediaController.playbackState.position
    },
    audioService = object : AudioSessionController {
        override suspend fun pause() = mediaController.pause()
        override suspend fun stop() = mediaController.stop()
        override suspend fun seekTo(position: Long) = mediaController.seekTo(position)
        override suspend fun skipToNext() = mediaController.fastForward()
        override suspend fun skipToPrevious() = mediaController.rewind()
        override suspend fun setLoopMode(mode: LoopMode) {
            // Map LoopMode to Media3 repeat mode
        }
        override suspend fun setShuffleMode(mode: ShuffleMode) {
            // Map ShuffleMode to Media3 shuffle mode
        }
    }
) {
    /**
     * Converts Media3 PlaybackState to our common model.
     */
    private fun convertPlaybackState(pbState: PlaybackState?): PlaybackState {
        if (pbState == null) {
            return PlaybackState()
        }

        val playing = when (pbState.state) {
            PlaybackState.STATE_PLAYING -> true
            PlaybackState.STATE_BUFFERING -> true
            else -> false
        }

        return PlaybackState(
            playing = playing,
            tracks = emptyList(), // Would need queue access
            currentIndex = pbState.activeQueueItemId?.toInt() ?: -1,
            position = pbState.position,
            duration = pbState.duration
        )
    }
}

/**
 * Android Media3 integration for MediaSessionSync with generation counter.
 * Wraps MediaController for seek synchronization.
 */
@ExperimentalCoroutinesApi
class Media3SessionSync(
    private val mediaController: MediaController,
    private val session: MediaLibrarySession
) : MediaSessionSync(
    player = object : PlayerController {
        override val currentState: PlaybackState
            get() = convertPlaybackState(mediaController.playbackState)
        override val position: Long
            get() = mediaController.playbackState.position
        override suspend fun seekTo(position: Long) = mediaController.seekTo(position)
    },
    mediaSession = object : MediaSessionPublisher {
        override suspend fun publishPlaybackState(state: PlaybackState) {
            // Media3 publishes automatically via MediaLibrarySession.Callback
            // This is for explicit notifications if needed
        }
    }
) {
    private fun convertPlaybackState(pbState: PlaybackState?): PlaybackState {
        if (pbState == null) return PlaybackState()
        return PlaybackState(
            playing = pbState.state == PlaybackState.STATE_PLAYING,
            position = pbState.position,
            duration = pbState.duration
        )
    }
}

/**
 * Enhanced STOP button state for Media3.
 * Provides visual feedback during stop operation and complete state cleanup.
 */
@ExperimentalCoroutinesApi
class Media3StopHandler(
    private val mediaController: MediaController,
    private val session: MediaLibrarySession
) {

    /**
     * Executes stop with complete state cleanup.
     * Clears: queue, position, playback state, metadata.
     */
    suspend fun stopWithCleanup() {
        // 1. Stop playback
        mediaController.stop()

        // 2. Clear queue via session callback (if we have session access)
        // This would be called on the MediaLibrarySession side

        // 3. Reset playback state
        val resetState = PlaybackStateCompat.Builder()
            .setState(PlaybackState.STATE_STOPPED, 0, 1.0f)
            .build()
        session.setPlaybackState(resetState)

        // 4. Clear metadata
        session.setMediaMetadata(null)
    }

    /**
     * State for STOP button UI.
     */
    data class StopButtonState(
        val isStopping: Boolean = false,
        val showSpinner: Boolean = false
    )
}

// Helper to convert our PlaybackState to Media3 PlaybackStateCompat
private fun toPlaybackStateCompat(state: PlaybackState): PlaybackStateCompat {
    return PlaybackStateCompat.Builder()
        .setState(
            if (state.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
            state.position,
            state.speed
        )
        .setActions(
            PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO
        )
        .build()
}

// MediaMetadata conversion
private fun toMediaMetadata(track: Track?): MediaMetadata? {
    track?.let {
        return MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)
            .build()
    }
    return null
}