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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * iOS player. Playback is done by MobileVLCKit through [NativePlayerEngine]; AVPlayer misreports the
 * duration of some fragmented MP4 streams (doubling it, which leaves the second half silent).
 * The engine plays a single URL, so the playlist, loop, shuffle and auto-advance live here.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(context: Any) : AudioPlayerInterface {

    actual val context: Any = context

    private val engine: NativePlayerEngine = checkNotNull(NativePlayerRegistry.factory) {
        "NativePlayerRegistry.factory must be set from Swift before the first AudioPlayer is created"
    }.create()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    private var disposed = false

    private val originalPlaylist = mutableListOf<MediaItem>()
    private val currentPlaylist = mutableListOf<MediaItem>()
    private var currentIndex = -1
    private var shuffleEnabled = false

    // The engine holds media only after the first play(); until then a requested start position
    // is kept here and handed over with the media.
    private var engineHasMedia = false
    private var pendingStartMs = 0L

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

    private val engineListener = object : NativePlayerListener {
        override fun onState(state: NativePlayerState) {
            if (disposed) return
            when (state) {
                NativePlayerState.IDLE -> _playerState.tryEmit(PlayerState.IDLE)
                NativePlayerState.OPENING,
                NativePlayerState.BUFFERING -> _playerState.tryEmit(PlayerState.BUFFERING)
                NativePlayerState.PLAYING -> _playerState.tryEmit(PlayerState.PLAYING)
                NativePlayerState.PAUSED -> _playerState.tryEmit(PlayerState.PAUSED)
                NativePlayerState.ENDED -> scope.launch { lock.withLock { onMediaEndedLocked() } }
            }
        }

        override fun onTime(positionMs: Long) {
            if (disposed || positionMs < 0) return
            _position.tryEmit(positionMs.milliseconds)
        }

        override fun onDuration(durationMs: Long) {
            if (disposed || durationMs <= 0) return
            _duration.tryEmit(durationMs.milliseconds)
        }

        override fun onError(message: String) {
            if (disposed) return
            _playerState.tryEmit(PlayerState.IDLE)
            _error.tryEmit(IllegalStateException("VLC playback error: $message"))
        }
    }

    init {
        engine.setListener(engineListener)
    }

    /** Loads [currentPlaylist] at [currentIndex] into the engine. Caller holds [lock]. */
    private fun loadCurrentIntoEngineLocked(autoPlay: Boolean) {
        val item = currentPlaylist.getOrNull(currentIndex) ?: return
        val startMs = pendingStartMs
        pendingStartMs = 0L
        engineHasMedia = true
        _position.tryEmit(startMs.milliseconds)
        _bufferingPosition.tryEmit(Duration.ZERO)
        _duration.tryEmit(item.duration)
        engine.load(item.url, autoPlay, startMs)
    }

    /** Switches to [index] and starts it. Caller holds [lock]. */
    private fun playIndexLocked(index: Int) {
        currentIndex = index
        _currentMediaItem.tryEmit(currentPlaylist[index])
        pendingStartMs = 0L
        loadCurrentIntoEngineLocked(autoPlay = true)
    }

    private fun onMediaEndedLocked() {
        if (disposed || currentPlaylist.isEmpty()) return
        _completion.tryEmit(Unit)
        when {
            _loopState.value == LoopState.ONE -> playIndexLocked(currentIndex.coerceAtLeast(0))
            currentIndex < currentPlaylist.lastIndex -> playIndexLocked(currentIndex + 1)
            _loopState.value == LoopState.ALL -> playIndexLocked(0)
            else -> {
                _playerState.tryEmit(PlayerState.COMPLETED)
                engineHasMedia = false
            }
        }
    }

    actual override suspend fun play() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            if (currentIndex !in currentPlaylist.indices) {
                currentIndex = 0
                _currentMediaItem.tryEmit(currentPlaylist[0])
            }
            if (engineHasMedia) engine.play() else loadCurrentIntoEngineLocked(autoPlay = true)
        }
    }

    actual override suspend fun pause() {
        lock.withLock {
            if (disposed) return
            engine.pause()
        }
    }

    actual override suspend fun stop() {
        lock.withLock {
            if (disposed) return
            engine.stop()
            engineHasMedia = false
            pendingStartMs = 0L
            _position.tryEmit(Duration.ZERO)
            _playerState.tryEmit(PlayerState.IDLE)
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
            if (engineHasMedia) engine.seekTo(targetMs) else pendingStartMs = targetMs
            _position.tryEmit(targetMs.milliseconds)
        }
    }

    actual override suspend fun loop(state: LoopState) {
        lock.withLock {
            if (disposed) return
            _loopState.tryEmit(state)
        }
    }

    actual override suspend fun shuffle(enabled: Boolean) {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty() || shuffleEnabled == enabled) return

            val currentItem = _currentMediaItem.value
            val rebuilt = if (enabled) {
                if (currentItem != null) {
                    listOf(currentItem) + originalPlaylist.filterNot { it.url == currentItem.url }.shuffled()
                } else {
                    originalPlaylist.shuffled()
                }
            } else {
                originalPlaylist.toList()
            }

            currentPlaylist.clear()
            currentPlaylist.addAll(rebuilt)
            shuffleEnabled = enabled
            _shuffleMode.tryEmit(enabled)
            _playlist.tryEmit(currentPlaylist.toList())

            currentIndex = if (currentItem == null) 0 else {
                currentPlaylist.indexOfFirst { it.url == currentItem.url }.takeIf { it >= 0 } ?: 0
            }
            _currentMediaItem.tryEmit(currentPlaylist.getOrNull(currentIndex))
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
            _playlist.tryEmit(currentPlaylist.toList())

            engine.stop()
            engineHasMedia = false
            pendingStartMs = 0L
            _position.tryEmit(Duration.ZERO)
            _bufferingPosition.tryEmit(Duration.ZERO)

            if (currentPlaylist.isEmpty()) {
                currentIndex = -1
                _currentMediaItem.tryEmit(null)
                _playerState.tryEmit(PlayerState.IDLE)
                _duration.tryEmit(Duration.ZERO)
                return
            }

            currentIndex = startPosition.coerceIn(0, currentPlaylist.lastIndex)
            _currentMediaItem.tryEmit(currentPlaylist[currentIndex])
            _duration.tryEmit(currentPlaylist[currentIndex].duration)

            if (autoPlay) {
                loadCurrentIntoEngineLocked(autoPlay = true)
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
            originalPlaylist.add(insertIndex.coerceAtMost(originalPlaylist.size), mediaItem)
            currentPlaylist.add(insertIndex, mediaItem)
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
            val currentUrl = _currentMediaItem.value?.url
            val removedCurrent = currentUrl == mediaItem.url
            val removalIndices = currentPlaylist.withIndex()
                .filter { it.value.url == mediaItem.url }
                .map { it.index }
            if (removalIndices.isEmpty()) return

            // Items before the playing one shift it down, so keep the index on the same item.
            val removedBefore = removalIndices.count { it < currentIndex }
            removalIndices.asReversed().forEach { currentPlaylist.removeAt(it) }

            if (currentPlaylist.isEmpty()) {
                currentIndex = -1
                _currentMediaItem.tryEmit(null)
                engine.stop()
                engineHasMedia = false
                _playerState.tryEmit(PlayerState.IDLE)
            } else if (removedCurrent) {
                currentIndex = currentIndex.coerceIn(0, currentPlaylist.lastIndex)
                _currentMediaItem.tryEmit(currentPlaylist[currentIndex])
                engine.stop()
                engineHasMedia = false
            } else {
                currentIndex = (currentIndex - removedBefore).coerceIn(0, currentPlaylist.lastIndex)
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
                originalPlaylist.add(toIndex, originalPlaylist.removeAt(fromIndex))
            }

            val currentUrl = _currentMediaItem.value?.url
            currentIndex = currentUrl?.let { url ->
                currentPlaylist.indexOfFirst { it.url == url }.takeIf { it >= 0 }
            } ?: currentIndex.coerceIn(0, currentPlaylist.lastIndex)

            _playlist.tryEmit(currentPlaylist.toList())
            _currentMediaItem.tryEmit(currentPlaylist.getOrNull(currentIndex))
        }
    }

    actual override suspend fun skipToNext() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            playIndexLocked((currentIndex + 1).coerceAtMost(currentPlaylist.lastIndex))
        }
    }

    actual override suspend fun skipToPrevious() {
        lock.withLock {
            if (disposed || currentPlaylist.isEmpty()) return
            playIndexLocked((currentIndex - 1).coerceAtLeast(0))
        }
    }

    actual override suspend fun jumpTo(index: Int) {
        lock.withLock {
            if (disposed || index !in currentPlaylist.indices) return
            playIndexLocked(index)
        }
    }

    actual override suspend fun setVolume(volume: Float) {
        lock.withLock {
            if (disposed) return
            val clamped = volume.coerceIn(0f, 1f)
            engine.setVolume(clamped)
            _volume.tryEmit(clamped)
        }
    }

    actual override suspend fun setPlaybackSpeed(speed: Float) {
        lock.withLock {
            if (disposed) return
            val clamped = speed.coerceIn(0.25f, 4f)
            engine.setRate(clamped)
            _playbackSpeed.tryEmit(clamped)
        }
    }

    actual override fun isDisposed(): Boolean = disposed

    actual override fun dispose() {
        if (disposed) return
        disposed = true
        engine.setListener(null)
        engine.destroy()
        scope.cancel()

        originalPlaylist.clear()
        currentPlaylist.clear()
        currentIndex = -1
        engineHasMedia = false

        _playerState.tryEmit(PlayerState.IDLE)
        _currentMediaItem.tryEmit(null)
        _playlist.tryEmit(emptyList())
        _duration.tryEmit(Duration.ZERO)
        _position.tryEmit(Duration.ZERO)
        _bufferingPosition.tryEmit(Duration.ZERO)
    }
}
