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

package org.spotube.spotube.core.audioplayer

import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.generated.VLCBundleLoaderGenerated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.DebugConfig
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.medialist.MediaList
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.component.AudioListPlayerComponent
import uk.co.caprica.vlcj.player.list.MediaListPlayer
import uk.co.caprica.vlcj.player.list.PlaybackMode
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(context: Any) : AudioPlayerInterface, KoinComponent {
    actual val context: Any = context

    private val logger by injectLogger<AudioPlayer>()
    private val lock = ReentrantLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var mediaPlayerFactory: MediaPlayerFactory

    private var audioListPlayerComponent: AudioListPlayerComponent

    private var mediaListPlayer: MediaListPlayer
    private var mediaPlayer: MediaPlayer
    private var mediaList: MediaList

    private val originalPlaylist = mutableListOf<MediaItem>()
    private val currentPlaylist = mutableListOf<MediaItem>()

    private var currentIndex = -1
    private var pendingNextIndex: Int? = null
    private var shuffleEnabled = false
    private var disposed = false
    private var hasStartedPlayback = false
    private var positionPollingJob: Job? = null

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
    actual override val currentMediaItemFlow: StateFlow<MediaItem?> =
        _currentMediaItem.asStateFlow()
    actual override val playlistFlow: StateFlow<List<MediaItem>> = _playlist.asStateFlow()
    actual override val durationFlow: StateFlow<Duration> = _duration.asStateFlow()
    actual override val positionFlow: StateFlow<Duration> = _position.asStateFlow()
    actual override val bufferingPositionFlow: StateFlow<Duration> =
        _bufferingPosition.asStateFlow()
    actual override val loopStateFlow: StateFlow<LoopState> = _loopState.asStateFlow()
    actual override val shuffleModeFlow: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    actual override val playbackSpeedFlow: StateFlow<Float> = _playbackSpeed.asStateFlow()
    actual override val volumeFlow: StateFlow<Float> = _volume.asStateFlow()
    actual override val completionFlow: Flow<Unit> = _completion.asSharedFlow()
    actual override val errorFlow: Flow<Throwable> = _error.asSharedFlow()

    init {
        VLCBundleLoaderGenerated.getVerifiedPath()
        val vlcLogArgs = if (DebugConfig.FORCE_VLC_LOGS) arrayOf("--verbose=2") else emptyArray()
        mediaPlayerFactory = MediaPlayerFactory(
            null as NativeDiscovery?,
            "--no-video",
            "--vout=dummy",
            "--aout=any",
            "--no-osd",
            "--no-snapshot-preview",
            *vlcLogArgs
        )
        audioListPlayerComponent = object : AudioListPlayerComponent(mediaPlayerFactory) {
            override fun opening(mediaPlayer: MediaPlayer) {
                _playerState.tryEmit(PlayerState.BUFFERING)
            }

            override fun buffering(
                mediaPlayer: MediaPlayer,
                newCache: Float
            ) {
                val durationMs = mediaPlayer.status().length()
                _bufferingPosition.tryEmit(
                    if (durationMs > 0) (durationMs * (newCache / 100f)).toLong().milliseconds else Duration.ZERO
                )
                _playerState.tryEmit(if (newCache < 100f) PlayerState.BUFFERING else PlayerState.READY)
            }

            override fun playing(mediaPlayer: MediaPlayer) {
                hasStartedPlayback = true
                _playerState.tryEmit(PlayerState.PLAYING)
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _playerState.tryEmit(PlayerState.PAUSED)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _playerState.tryEmit(PlayerState.IDLE)
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                _playerState.tryEmit(PlayerState.COMPLETED)
                _completion.tryEmit(Unit)
            }

            override fun timeChanged(
                mediaPlayer: MediaPlayer,
                newTime: Long
            ) {
                if (newTime >= 0) {
                    _position.tryEmit(newTime.milliseconds)
                }
            }

            override fun mediaDurationChanged(
                mediaPlayer: uk.co.caprica.vlcj.media.Media,
                newDuration: Long
            ) {
                logger.d { "Media duration changed: ${newDuration}ms" }
                if (newDuration > 0) {
                    _duration.tryEmit(newDuration.milliseconds)
                }
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                logger.d { "Media length changed: ${newLength}ms" }
                if (newLength > 0) {
                    _duration.tryEmit(newLength.milliseconds)
                }
            }

            override fun volumeChanged(
                mediaPlayer: MediaPlayer,
                volume: Float
            ) {
                _volume.tryEmit(normalizeVlcVolume(volume))
            }

            override fun error(mediaPlayer: MediaPlayer) {
                _playerState.tryEmit(PlayerState.IDLE)
                _error.tryEmit(IllegalStateException("VLC encountered a playback error"))
            }

            override fun nextItem(
                mediaListPlayer: uk.co.caprica.vlcj.player.list.MediaListPlayer,
                item: uk.co.caprica.vlcj.media.MediaRef
            ) {
                lock.withLock {
                    val targetIndex = pendingNextIndex ?: run {
                        val currentMrl = runCatching {
                            val media = item.newMedia()
                            try {
                                media.info().mrl()
                            } finally {
                                media.release()
                            }
                        }.getOrNull()

                        if (currentMrl != null) {
                            currentPlaylist.indexOfFirst { it.toMrl() == currentMrl }
                        } else {
                            -1
                        }
                    }

                    if (targetIndex >= 0 && targetIndex < currentPlaylist.size) {
                        logger.d { "nextItem: updating currentIndex from $currentIndex to $targetIndex" }
                        currentIndex = targetIndex
                        _currentMediaItem.tryEmit(currentPlaylist[targetIndex])
                    } else {
                        logger.w { "nextItem: invalid targetIndex=$targetIndex, currentIndex remains $currentIndex" }
                    }

                    pendingNextIndex = null
                }
            }

            override fun mediaListPlayerFinished(mediaListPlayer: uk.co.caprica.vlcj.player.list.MediaListPlayer) {
                _playerState.tryEmit(PlayerState.COMPLETED)
                _completion.tryEmit(Unit)
            }

            override fun stopped(mediaListPlayer: uk.co.caprica.vlcj.player.list.MediaListPlayer) {
                _playerState.tryEmit(PlayerState.IDLE)
            }

            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                _duration.tryEmit(
                    mediaPlayer?.media()?.info()?.duration()?.milliseconds ?: Duration.ZERO
                )
            }
        }

        mediaListPlayer = audioListPlayerComponent.mediaListPlayer()
        mediaPlayer = audioListPlayerComponent.mediaPlayer()
        mediaList = mediaPlayerFactory.media().newMediaList()

        mediaListPlayer.list().setMediaList(mediaList.newMediaListRef())
        startPositionPolling()
    }

    actual override suspend fun play() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            if (currentIndex !in currentPlaylist.indices) {
                currentIndex = 0
            }
            if (hasStartedPlayback) {
                mediaListPlayer.controls().setPause(false)
            } else {
                mediaListPlayer.controls().play(currentIndex)
            }
        }
    }

    actual override suspend fun pause() {
        lock.withLock {
            if (disposed) return
            mediaListPlayer.controls().pause()
        }
    }

    actual override suspend fun stop() {
        lock.withLock {
            if (disposed) return
            mediaListPlayer.controls().stop()
            hasStartedPlayback = false
            _position.tryEmit(Duration.ZERO)
        }
    }

    actual override suspend fun seekTo(position: Duration) {
        lock.withLock {
            if (disposed) return
            val maxMs = _duration.value.inWholeMilliseconds
            val targetMs = if (maxMs > 0) {
                position.inWholeMilliseconds.coerceIn(0, maxMs)
            } else {
                position.inWholeMilliseconds.coerceAtLeast(0)
            }
            mediaPlayer.controls().setTime(targetMs)
            if (maxMs > 0) {
                mediaPlayer.controls()
                    .setPosition((targetMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f))
            }
            _position.tryEmit(targetMs.milliseconds)
        }
    }

    actual override suspend fun loop(state: LoopState) {
        lock.withLock {
            if (disposed) return
            val vlcMode = when (state) {
                LoopState.NONE -> PlaybackMode.DEFAULT
                LoopState.ONE -> PlaybackMode.REPEAT
                LoopState.ALL -> PlaybackMode.LOOP
            }
            mediaListPlayer.controls().setMode(vlcMode)
            _loopState.tryEmit(state)
        }
    }

    actual override suspend fun shuffle(enabled: Boolean) {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty() || shuffleEnabled == enabled) return

            val currentItem = _currentMediaItem.value

            val rebuilt = if (enabled) {
                if (currentItem != null) {
                    val rest = originalPlaylist.filterNot { it.url == currentItem.url }.shuffled()
                    listOf(currentItem) + rest
                } else {
                    originalPlaylist.shuffled()
                }
            } else {
                originalPlaylist.toList()
            }

            currentPlaylist.clear()
            currentPlaylist.addAll(rebuilt)
            rebuildVlcMediaListLocked()

            shuffleEnabled = enabled
            _shuffleMode.tryEmit(enabled)
            _playlist.tryEmit(currentPlaylist.toList())

            val nextIndex = when {
                currentItem == null -> if (currentPlaylist.isEmpty()) -1 else 0
                else -> currentPlaylist.indexOfFirst { it.url == currentItem.url }
                    .takeIf { it >= 0 } ?: 0
            }

            currentIndex = nextIndex
            _currentMediaItem.tryEmit(currentPlaylist.getOrNull(nextIndex))
        }
    }

    actual override suspend fun load(
        playlist: List<MediaItem>,
        autoPlay: Boolean,
        startPosition: Int
    ) {
        lock.withLock {
            if (disposed) return

            originalPlaylist.clear()
            originalPlaylist.addAll(playlist)
            currentPlaylist.clear()
            currentPlaylist.addAll(playlist)
            shuffleEnabled = false

            _shuffleMode.tryEmit(false)
            rebuildVlcMediaListLocked()
            _playlist.tryEmit(currentPlaylist.toList())

            if (currentPlaylist.isEmpty()) {
                currentIndex = -1
                _currentMediaItem.tryEmit(null)
                _playerState.tryEmit(PlayerState.IDLE)
                _duration.tryEmit(Duration.ZERO)
                _position.tryEmit(Duration.ZERO)
                _bufferingPosition.tryEmit(Duration.ZERO)
                return
            }

            val normalizedStartPosition = startPosition.coerceIn(0, currentPlaylist.lastIndex)
            currentIndex = normalizedStartPosition
            _currentMediaItem.tryEmit(currentPlaylist[normalizedStartPosition])

            hasStartedPlayback = false
            if (autoPlay) {
                mediaListPlayer.controls().play(normalizedStartPosition)
            } else {
                _playerState.tryEmit(PlayerState.PAUSED)
            }
        }
    }

    actual override suspend fun addMediaItem(mediaItem: MediaItem) {
        lock.withLock {
            if (disposed) return
            originalPlaylist.add(mediaItem)
            currentPlaylist.add(mediaItem)
            mediaList.media().add(mediaItem.toMrl())
            _playlist.tryEmit(currentPlaylist.toList())
            if (currentIndex == -1) {
                currentIndex = 0
                _currentMediaItem.tryEmit(currentPlaylist.firstOrNull())
            }
        }
    }

    actual override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) {
        lock.withLock {
            if (disposed) return
            val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
            originalPlaylist.add(insertIndex, mediaItem)
            currentPlaylist.add(insertIndex, mediaItem)
            mediaList.media().clear()
            currentPlaylist.forEach { item ->
                mediaList.media().add(item.toMrl())
            }
            _playlist.tryEmit(currentPlaylist.toList())
            if (currentIndex == -1) {
                currentIndex = 0
                _currentMediaItem.tryEmit(currentPlaylist.firstOrNull())
            }
        }
    }

    actual override suspend fun removeMediaItem(mediaItem: MediaItem) {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return

            originalPlaylist.removeAll { it.url == mediaItem.url }
            val removalIndices = currentPlaylist.withIndex()
                .filter { it.value.url == mediaItem.url }
                .map { it.index }

            if (removalIndices.isEmpty()) return

            removalIndices.asReversed().forEach { index ->
                mediaList.media().remove(index)
                currentPlaylist.removeAt(index)
            }

            if (currentPlaylist.isEmpty()) {
                currentIndex = -1
                _currentMediaItem.tryEmit(null)
                mediaListPlayer.controls().stop()
                _playerState.tryEmit(PlayerState.IDLE)
            } else {
                currentIndex = currentIndex.coerceIn(0, currentPlaylist.lastIndex)
                _currentMediaItem.tryEmit(currentPlaylist.getOrNull(currentIndex))
            }

            _playlist.tryEmit(currentPlaylist.toList())
        }
    }

    actual override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        lock.withLock {
            if (disposed) return
            if (fromIndex !in currentPlaylist.indices || toIndex !in currentPlaylist.indices || fromIndex == toIndex) return

            val item = currentPlaylist.removeAt(fromIndex)
            currentPlaylist.add(toIndex, item)

            if (!shuffleEnabled && fromIndex in originalPlaylist.indices && toIndex in originalPlaylist.indices) {
                val originalItem = originalPlaylist.removeAt(fromIndex)
                originalPlaylist.add(toIndex, originalItem)
            }

            val currentItemUrl = _currentMediaItem.value?.url
            rebuildVlcMediaListLocked()
            currentIndex = currentItemUrl?.let { url ->
                currentPlaylist.indexOfFirst { it.url == url }.takeIf { it >= 0 }
            } ?: currentIndex.coerceIn(0, currentPlaylist.lastIndex)

            _playlist.tryEmit(currentPlaylist.toList())
            _currentMediaItem.tryEmit(currentPlaylist.getOrNull(currentIndex))
        }
    }

    actual override suspend fun skipToNext() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            val nextIndex = (currentIndex + 1).coerceAtMost(currentPlaylist.lastIndex)
            logger.d { "skipToNext: currentIndex=$currentIndex, targetIndex=$nextIndex" }
            currentIndex = nextIndex
            _currentMediaItem.value = currentPlaylist[nextIndex]
            pendingNextIndex = nextIndex
            mediaListPlayer.controls().play(nextIndex)
        }
    }

    actual override suspend fun skipToPrevious() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            val prevIndex = (currentIndex - 1).coerceAtLeast(0)
            logger.d { "skipToPrevious: currentIndex=$currentIndex, targetIndex=$prevIndex" }
            currentIndex = prevIndex
            _currentMediaItem.value = currentPlaylist[prevIndex]
            pendingNextIndex = prevIndex
            mediaListPlayer.controls().play(prevIndex)
        }
    }

    actual override suspend fun jumpTo(index: Int) {
        lock.withLock {
            if (disposed || index !in currentPlaylist.indices) return
            currentIndex = index
            _currentMediaItem.tryEmit(currentPlaylist[index])
            pendingNextIndex = index
            mediaListPlayer.controls().play(index)
        }
    }

    actual override suspend fun setVolume(volume: Float) {
        lock.withLock {
            if (disposed) return
            val clamped = volume.coerceIn(0f, 1f)
            mediaPlayer.audio().setVolume((clamped * 100).toInt())
            _volume.tryEmit(clamped)
        }
    }

    actual override suspend fun setPlaybackSpeed(speed: Float) {
        lock.withLock {
            if (disposed) return
            val clamped = speed.coerceIn(0.25f, 4f)
            val updated = mediaPlayer.controls().setRate(clamped)
            if (updated) {
                _playbackSpeed.tryEmit(clamped)
            } else {
                logger.w { "Failed to set VLC playback speed to $clamped" }
            }
        }
    }

    actual override fun isDisposed(): Boolean = disposed

    actual override fun dispose() {
        lock.withLock {
            if (disposed) return
            disposed = true
        }

        positionPollingJob?.cancel()
        scope.cancel()

        runCatching { mediaListPlayer.controls().stop() }
        runCatching { mediaList.release() }
        runCatching { audioListPlayerComponent.release() }
        runCatching { mediaPlayerFactory.release() }

        lock.withLock {
            originalPlaylist.clear()
            currentPlaylist.clear()
            currentIndex = -1
            hasStartedPlayback = false
        }

        _playerState.tryEmit(PlayerState.IDLE)
        _currentMediaItem.tryEmit(null)
        _playlist.tryEmit(emptyList())
        _duration.tryEmit(Duration.ZERO)
        _position.tryEmit(Duration.ZERO)
        _bufferingPosition.tryEmit(Duration.ZERO)
    }

    private fun rebuildVlcMediaListLocked() {
        val oldList = mediaList
        mediaList = mediaPlayerFactory.media().newMediaList()
        currentPlaylist.forEach { mediaItem ->
            mediaList.media().add(mediaItem.toMrl())
        }
        mediaListPlayer.list().setMediaList(mediaList.newMediaListRef())
        oldList.release()
    }

    private fun startPositionPolling() {
        positionPollingJob = scope.launch {
            try {
                while (isActive) {
                    if (!disposed) {
                        val status = mediaPlayer.status()
                        val time = status.time()
                        val duration = status.length()
                        if (time >= 0) {
                            _position.tryEmit(time.milliseconds)
                        }
                        if (duration > 0) {
                            _duration.tryEmit(duration.milliseconds)
                        }
                        _playerState.tryEmit(status.state().toPlayerState())
                    }
                    delay(250.milliseconds)
                }
            } catch (e: Throwable) {
                if (!disposed) {
                    logger.e(e) { "Error in position polling loop" }
                }
            }
        }
    }

    private fun State.toPlayerState(): PlayerState {
        return when (this) {
            State.NOTHING_SPECIAL -> PlayerState.IDLE
            State.OPENING -> PlayerState.BUFFERING
            State.BUFFERING -> PlayerState.BUFFERING
            State.PLAYING -> PlayerState.PLAYING
            State.PAUSED -> PlayerState.PAUSED
            State.STOPPED -> PlayerState.IDLE
            State.ENDED -> PlayerState.COMPLETED
            State.ERROR -> PlayerState.IDLE
        }
    }

    private fun MediaItem.toMrl(): String {
        return url
    }

    private fun normalizeVlcVolume(rawVolume: Float): Float {
        return if (rawVolume <= 1f) {
            rawVolume.coerceIn(0f, 1f)
        } else {
            (rawVolume / 100f).coerceIn(0f, 1f)
        }
    }
}
