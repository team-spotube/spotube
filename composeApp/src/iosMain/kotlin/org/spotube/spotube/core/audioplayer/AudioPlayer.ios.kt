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

@file:OptIn(ExperimentalForeignApi::class)

package org.spotube.spotube.core.audioplayer

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
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
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerTimeControlStatus
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.loadedTimeRanges
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSURL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(context: Any) : AudioPlayerInterface {

    actual val context: Any = context

    private val avPlayer: AVPlayer = AVPlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private var lastTimeControlStatus: AVPlayerTimeControlStatus? = null

    init {
        avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMake(1, 4),
            queue = null
        ) { time ->
            if (!disposed) {
                val seconds = CMTimeGetSeconds(time)
                _position.tryEmit(seconds.seconds)

                avPlayer.currentItem?.let { item ->
                    val durationSeconds = CMTimeGetSeconds(item.duration)
                    if (durationSeconds > 0) {
                        _duration.tryEmit(durationSeconds.seconds)
                    }
                    val loadedRanges = item.loadedTimeRanges
//                    if (loadedRanges.count() > 0u) {
//                        val range = loadedRanges.objectAtIndex(0u)
//                    }
                }
            }
        }

        scope.launch {
            while (!disposed) {
                syncTimeControlStatus()
                delay(500)
            }
        }
    }

    private fun syncTimeControlStatus() {
        val status = avPlayer.timeControlStatus
        if (status == lastTimeControlStatus) return
        lastTimeControlStatus = status

        val playerState = when (status) {
            AVPlayerTimeControlStatusPlaying -> PlayerState.PLAYING
            AVPlayerTimeControlStatusPaused -> PlayerState.PAUSED
            AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> PlayerState.BUFFERING
            else -> PlayerState.IDLE
        }
        _playerState.tryEmit(playerState)

        if (avPlayer.currentItem == null) {
            _playerState.tryEmit(PlayerState.IDLE)
        }
    }

    private fun resolveState(): PlayerState {
        return when (avPlayer.timeControlStatus) {
            AVPlayerTimeControlStatusPlaying -> PlayerState.PLAYING
            AVPlayerTimeControlStatusPaused -> PlayerState.PAUSED
            AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> PlayerState.BUFFERING
            else -> PlayerState.IDLE
        }
    }

    private fun buildAVPlayerItem(url: String): AVPlayerItem? {
        val nsUrl = NSURL.URLWithString(url) ?: return null
        return AVPlayerItem(nsUrl)
    }

    actual override suspend fun play() {
        avPlayer.play()
    }

    actual override suspend fun pause() {
        avPlayer.pause()
    }

    actual override suspend fun stop() {
        avPlayer.pause()
        avPlayer.seekToTime(CMTimeMake(0, 1))
        _playerState.tryEmit(PlayerState.IDLE)
    }

    actual override suspend fun seekTo(position: Duration) {
        val seconds = position.inWholeMilliseconds / 1000.0
        val cmTime = CMTimeMakeWithSeconds(seconds, 1000)
        avPlayer.seekToTime(cmTime)
        _position.tryEmit(position)
    }

    actual override suspend fun loop(state: LoopState) {
        _loopState.tryEmit(state)
    }

    actual override suspend fun shuffle(enabled: Boolean) {
        _shuffleMode.tryEmit(enabled)
    }

    actual override suspend fun load(
        playlist: List<MediaItem>,
        autoPlay: Boolean,
        startPosition: Int
    ) {
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
            return
        }

        val safeIndex = startPosition.coerceIn(0, playlist.lastIndex)
        val item = playlist[safeIndex]
        val avItem = buildAVPlayerItem(item.url)

        if (avItem == null) {
            _error.tryEmit(IllegalArgumentException("Invalid URL: ${item.url}"))
            _playerState.tryEmit(PlayerState.IDLE)
            return
        }

        avPlayer.replaceCurrentItemWithPlayerItem(avItem)
        _currentMediaItem.tryEmit(item)

        if (autoPlay) {
            avPlayer.play()
        }
    }

    actual override suspend fun addMediaItem(mediaItem: MediaItem) {
        currentPlaylist.add(mediaItem)
        urlIndexMap[mediaItem.url] = currentPlaylist.lastIndex
        _playlist.tryEmit(currentPlaylist.toList())
    }

    actual override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) {
        val currentIndex = currentPlaylist.indexOfFirst {
            it.url == _currentMediaItem.value?.url
        }
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else currentPlaylist.size
        currentPlaylist.add(insertIndex, mediaItem)
        urlIndexMap.clear()
        currentPlaylist.forEachIndexed { index, item ->
            urlIndexMap[item.url] = index
        }
        _playlist.tryEmit(currentPlaylist.toList())
    }

    actual override suspend fun removeMediaItem(mediaItem: MediaItem) {
        val index = urlIndexMap[mediaItem.url] ?: return
        currentPlaylist.removeAt(index)
        urlIndexMap.clear()
        currentPlaylist.forEachIndexed { i, item ->
            urlIndexMap[item.url] = i
        }
        _playlist.tryEmit(currentPlaylist.toList())
    }

    actual override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentPlaylist.indices || toIndex !in currentPlaylist.indices || fromIndex == toIndex) return
        val item = currentPlaylist.removeAt(fromIndex)
        currentPlaylist.add(toIndex, item)
        urlIndexMap.clear()
        currentPlaylist.forEachIndexed { i, it ->
            urlIndexMap[it.url] = i
        }
        _playlist.tryEmit(currentPlaylist.toList())
    }

    actual override suspend fun skipToNext() {
        val currentIndex = currentPlaylist.indexOfFirst {
            it.url == _currentMediaItem.value?.url
        }
        val nextIndex = currentIndex + 1
        if (nextIndex < currentPlaylist.size) {
            val nextItem = currentPlaylist[nextIndex]
            val avItem = buildAVPlayerItem(nextItem.url)
            if (avItem != null) {
                avPlayer.replaceCurrentItemWithPlayerItem(avItem)
                _currentMediaItem.tryEmit(nextItem)
                avPlayer.play()
            }
        }
    }

    actual override suspend fun skipToPrevious() {
        val currentIndex = currentPlaylist.indexOfFirst {
            it.url == _currentMediaItem.value?.url
        }
        val prevIndex = currentIndex - 1
        if (prevIndex >= 0) {
            val prevItem = currentPlaylist[prevIndex]
            val avItem = buildAVPlayerItem(prevItem.url)
            if (avItem != null) {
                avPlayer.replaceCurrentItemWithPlayerItem(avItem)
                _currentMediaItem.tryEmit(prevItem)
                avPlayer.play()
            }
        }
    }

    actual override suspend fun jumpTo(index: Int) {
        if (index in currentPlaylist.indices) {
            val item = currentPlaylist[index]
            val avItem = buildAVPlayerItem(item.url)
            if (avItem != null) {
                avPlayer.replaceCurrentItemWithPlayerItem(avItem)
                _currentMediaItem.tryEmit(item)
            }
        }
    }

    actual override suspend fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        avPlayer.volume = clamped
        _volume.tryEmit(clamped)
    }

    actual override suspend fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4f)
        avPlayer.rate = clamped
        _playbackSpeed.tryEmit(clamped)
    }

    actual override fun isDisposed(): Boolean = disposed

    actual override fun dispose() {
        disposed = true
        avPlayer.pause()
        avPlayer.replaceCurrentItemWithPlayerItem(null)
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

private fun CMTimeMake(value: Long, timescale: Int): CValue<CMTime> {
    return platform.CoreMedia.CMTimeMake(value, timescale)
}

private fun CMTimeMakeWithSeconds(seconds: Double, preferredTimescale: Int): CValue<CMTime> {
    return platform.CoreMedia.CMTimeMakeWithSeconds(seconds, preferredTimescale)
}
