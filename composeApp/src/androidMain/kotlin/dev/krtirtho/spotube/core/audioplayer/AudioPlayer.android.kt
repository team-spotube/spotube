/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.core.audioplayer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.krtirtho.spotube.media.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(context: Any) : AudioPlayerInterface {

    actual val context: Any = context

    private val appContext: Context = (context as Context).applicationContext

    private fun ensureServiceStarted() {
        // On Android 12+ starting a foreground service from the background throws
        // (ForegroundServiceStartNotAllowedException) — e.g. when a jam session or
        // remote control applies playback while the app is backgrounded. Never let
        // that crash the app; playback itself runs in-process without the service.
        try {
            val intent = Intent(appContext, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to start playback service", e)
        }
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var disposed = false

    private val currentPlaylist = mutableListOf<MediaItem>()
    private val urlIndexMap = mutableMapOf<String, Int>()

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _duration = MutableStateFlow(Duration.ZERO)
    private val _position = MutableStateFlow(Duration.ZERO)
    private val _bufferingPosition = MutableStateFlow(Duration.ZERO)
    private val _loopState = MutableStateFlow(LoopState.NONE)
    private val _shuffleMode = MutableStateFlow(false)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _volume = MutableStateFlow(1.0f)
    private val _completion = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _error = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)

    actual override val playerStateFlow: StateFlow<PlayerState> = _playerState.asStateFlow()
    actual override val currentMediaItemFlow: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()
    actual override val playlistFlow: StateFlow<List<MediaItem>> = _playlist.asStateFlow()
    actual override val durationFlow: StateFlow<Duration> = _duration.asStateFlow()
    actual override val positionFlow: StateFlow<Duration> = _position.asStateFlow()
    actual override val bufferingPositionFlow: StateFlow<Duration> = _bufferingPosition.asStateFlow()
    actual override val loopStateFlow: StateFlow<LoopState> = _loopState.asStateFlow()
    actual override val shuffleModeFlow: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    actual override val playbackSpeedFlow: StateFlow<Float> = _playbackSpeed.asStateFlow()
    actual override val volumeFlow: StateFlow<Float> = _volume.asStateFlow()
    actual override val completionFlow: Flow<Unit> = _completion.asSharedFlow()
    actual override val errorFlow: Flow<Throwable> = _error.asSharedFlow()

    private var lastPlaybackState = Player.STATE_IDLE

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPlaybackState(playbackState)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                syncPlayWhenReady(playWhenReady)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                syncCurrentItem()
            }

            override fun onPlayerError(error: PlaybackException) {
                _error.tryEmit(error)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _loopState.tryEmit(
                    when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> LoopState.NONE
                        Player.REPEAT_MODE_ONE -> LoopState.ONE
                        Player.REPEAT_MODE_ALL -> LoopState.ALL
                        else -> LoopState.NONE
                    }
                )
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.tryEmit(shuffleModeEnabled)
            }

            override fun onVolumeChanged(volume: Float) {
                _volume.tryEmit(volume)
            }
        })

        scope.launch {
            while (isActive && !disposed) {
                _position.tryEmit(exoPlayer.currentPosition.milliseconds)
                _duration.tryEmit(
                    if (exoPlayer.duration > 0) exoPlayer.duration.milliseconds else Duration.ZERO
                )
                _bufferingPosition.tryEmit(exoPlayer.bufferedPosition.milliseconds)
                delay(250)
            }
        }
    }

    private fun syncPlaybackState(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && lastPlaybackState != Player.STATE_ENDED) {
            _completion.tryEmit(Unit)
            _playerState.tryEmit(PlayerState.COMPLETED)
        }
        lastPlaybackState = playbackState
        if (playbackState != Player.STATE_ENDED) {
            syncPlayWhenReady(exoPlayer.playWhenReady)
        }
    }

    private fun syncPlayWhenReady(playWhenReady: Boolean) {
        val state = when {
            exoPlayer.playbackState == Player.STATE_IDLE -> PlayerState.IDLE
            exoPlayer.playbackState == Player.STATE_BUFFERING -> PlayerState.BUFFERING
            playWhenReady -> PlayerState.PLAYING
            else -> PlayerState.PAUSED
        }
        _playerState.tryEmit(state)
    }

    private fun syncCurrentItem() {
        val index = exoPlayer.currentMediaItemIndex
        _currentMediaItem.tryEmit(
            if (index in currentPlaylist.indices) currentPlaylist[index] else null
        )
    }

    internal val player: ExoPlayer get() = exoPlayer

    private fun MediaItem.toExoMediaItem(): androidx.media3.common.MediaItem {
        return androidx.media3.common.MediaItem.Builder()
            .setMediaId(url)
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .apply {
                        if (coverURL.isNotBlank()) {
                            setArtworkUri(android.net.Uri.parse(coverURL))
                        }
                    }
                    .build()
            )
            .build()
    }

    actual override suspend fun play() {
        withContext(Dispatchers.Main) {
            ensureServiceStarted()
            exoPlayer.play()
        }
    }

    actual override suspend fun pause() {
        withContext(Dispatchers.Main) {
            exoPlayer.pause()
        }
    }

    actual override suspend fun stop() {
        withContext(Dispatchers.Main) {
            exoPlayer.stop()
            _playerState.tryEmit(PlayerState.IDLE)
        }
    }

    actual override suspend fun seekTo(position: Duration) {
        withContext(Dispatchers.Main) {
            val duration = exoPlayer.duration
            val targetMs = if (duration > 0) {
                position.inWholeMilliseconds.coerceIn(0, duration)
            } else {
                position.inWholeMilliseconds.coerceAtLeast(0)
            }
            exoPlayer.seekTo(targetMs)
            _position.tryEmit(exoPlayer.currentPosition.milliseconds)
        }
    }

    actual override suspend fun loop(state: LoopState) {
        withContext(Dispatchers.Main) {
            val repeatMode = when (state) {
                LoopState.NONE -> Player.REPEAT_MODE_OFF
                LoopState.ONE -> Player.REPEAT_MODE_ONE
                LoopState.ALL -> Player.REPEAT_MODE_ALL
            }
            exoPlayer.repeatMode = repeatMode
            _loopState.tryEmit(state)
        }
    }

    actual override suspend fun shuffle(enabled: Boolean) {
        withContext(Dispatchers.Main) {
            exoPlayer.shuffleModeEnabled = enabled
            _shuffleMode.tryEmit(enabled)
        }
    }

    actual override suspend fun load(
        playlist: List<MediaItem>,
        autoPlay: Boolean,
        startPosition: Int
    ) {
        withContext(Dispatchers.Main) {
            currentPlaylist.clear()
            currentPlaylist.addAll(playlist)
            urlIndexMap.clear()
            playlist.forEachIndexed { index, item ->
                urlIndexMap[item.url] = index
            }
            _playlist.tryEmit(currentPlaylist.toList())

            if (playlist.isEmpty()) {
                _playerState.tryEmit(PlayerState.IDLE)
                _currentMediaItem.tryEmit(null)
                _position.tryEmit(Duration.ZERO)
                _duration.tryEmit(Duration.ZERO)
                return@withContext
            }

            val exoItems = playlist.map { it.toExoMediaItem() }
            val safeIndex = startPosition.coerceIn(0, exoItems.lastIndex)

            exoPlayer.setMediaItems(exoItems, safeIndex, 0L)
            exoPlayer.prepare()

            if (autoPlay) {
                ensureServiceStarted()
                exoPlayer.playWhenReady = true
            }

            _currentMediaItem.tryEmit(playlist[safeIndex])
        }
    }

    actual override suspend fun addMediaItem(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            currentPlaylist.add(mediaItem)
            urlIndexMap[mediaItem.url] = currentPlaylist.lastIndex
            _playlist.tryEmit(currentPlaylist.toList())
            exoPlayer.addMediaItem(mediaItem.toExoMediaItem())
        }
    }

    actual override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            val currentIndex = exoPlayer.currentMediaItemIndex
            val insertIndex = (currentIndex + 1).coerceAtMost(currentPlaylist.size)

            currentPlaylist.add(insertIndex, mediaItem)
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { index, item ->
                urlIndexMap[item.url] = index
            }
            _playlist.tryEmit(currentPlaylist.toList())

            exoPlayer.addMediaItem(insertIndex, mediaItem.toExoMediaItem())
        }
    }

    actual override suspend fun removeMediaItem(mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            val index = urlIndexMap[mediaItem.url] ?: return@withContext
            currentPlaylist.removeAt(index)
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { i, item ->
                urlIndexMap[item.url] = i
            }
            _playlist.tryEmit(currentPlaylist.toList())
            exoPlayer.removeMediaItem(index)
        }
    }

    actual override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        withContext(Dispatchers.Main) {
            if (fromIndex !in currentPlaylist.indices || toIndex !in currentPlaylist.indices || fromIndex == toIndex) return@withContext

            val item = currentPlaylist.removeAt(fromIndex)
            currentPlaylist.add(toIndex, item)
            urlIndexMap.clear()
            currentPlaylist.forEachIndexed { i, it ->
                urlIndexMap[it.url] = i
            }
            _playlist.tryEmit(currentPlaylist.toList())

            exoPlayer.moveMediaItem(fromIndex, toIndex)
        }
    }

    actual override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    actual override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    actual override suspend fun jumpTo(index: Int) {
        withContext(Dispatchers.Main) {
            if (index in currentPlaylist.indices) {
                exoPlayer.seekToDefaultPosition(index)
            }
        }
    }

    actual override suspend fun setVolume(volume: Float) {
        withContext(Dispatchers.Main) {
            val clamped = volume.coerceIn(0f, 1f)
            exoPlayer.setVolume(clamped)
            _volume.tryEmit(clamped)
        }
    }

    actual override suspend fun setPlaybackSpeed(speed: Float) {
        withContext(Dispatchers.Main) {
            val clamped = speed.coerceIn(0.25f, 4f)
            exoPlayer.setPlaybackSpeed(clamped)
            _playbackSpeed.tryEmit(clamped)
        }
    }

    actual override fun isDisposed(): Boolean = disposed

    actual override fun dispose() {
        disposed = true
        exoPlayer.release()
        currentPlaylist.clear()
        urlIndexMap.clear()

        _playerState.tryEmit(PlayerState.IDLE)
        _currentMediaItem.tryEmit(null)
        _playlist.tryEmit(emptyList())
        _duration.tryEmit(Duration.ZERO)
        _position.tryEmit(Duration.ZERO)
        _bufferingPosition.tryEmit(Duration.ZERO)
    }
}
