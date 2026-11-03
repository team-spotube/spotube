package org.spotube.spotube.core.audioplayer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

class FakeAudioPlayer : AudioPlayerInterface {
    private val _playlistFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    override val playlistFlow: StateFlow<List<MediaItem>> = _playlistFlow.asStateFlow()

    private val _durationFlow = MutableStateFlow(Duration.ZERO)
    override val durationFlow: StateFlow<Duration> = _durationFlow.asStateFlow()

    private val _positionFlow = MutableStateFlow(Duration.ZERO)
    override val positionFlow: StateFlow<Duration> = _positionFlow.asStateFlow()

    override val bufferingPositionFlow: StateFlow<Duration>
        get() = TODO("Not yet implemented")

    private val _currentMediaItemFlow = MutableStateFlow<MediaItem?>(null)
    override val currentMediaItemFlow: StateFlow<MediaItem?> = _currentMediaItemFlow.asStateFlow()

    private val _loopStateFlow = MutableStateFlow(LoopState.NONE)
    override val loopStateFlow: StateFlow<LoopState> = _loopStateFlow.asStateFlow()
    override val shuffleModeFlow: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    override val playbackSpeedFlow: StateFlow<Float>
        get() = TODO("Not yet implemented")
    override val volumeFlow: StateFlow<Float>
        get() = TODO("Not yet implemented")
    override val completionFlow: Flow<Unit>
        get() = TODO("Not yet implemented")
    override val errorFlow: Flow<Throwable>
        get() = TODO("Not yet implemented")

    override suspend fun setVolume(volume: Float) {
        TODO("Not yet implemented")
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        TODO("Not yet implemented")
    }

    override fun isDisposed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    var loadCallCount = 0
    var lastLoadPlaylist: List<MediaItem>? = null
    var lastLoadAutoPlay: Boolean = true
    var lastLoadStartPosition: Int = 0

    var playCallCount = 0

    var addMediaItemCallCount = 0
    var lastAddedMediaItem: MediaItem? = null

    var insertAtNextCallCount = 0
    var lastInsertedMediaItem: MediaItem? = null

    var removeMediaItemCallCount = 0
    var lastRemovedMediaItem: MediaItem? = null

    var moveCallCount = 0
    var lastMoveFromIndex: Int = 0
    var lastMoveToIndex: Int = 0

    var jumpToCallCount = 0
    var lastJumpToIndex: Int = 0

    override suspend fun load(playlist: List<MediaItem>, autoPlay: Boolean, startPosition: Int) {
        loadCallCount++
        lastLoadPlaylist = playlist
        lastLoadAutoPlay = autoPlay
        lastLoadStartPosition = startPosition
        _playlistFlow.value = playlist.toList()
        _currentMediaItemFlow.value = playlist.getOrNull(startPosition)
    }

    override suspend fun play() {
        playCallCount++
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(position: Duration) {
        TODO("Not yet implemented")
    }

    override suspend fun loop(state: LoopState) {
        TODO("Not yet implemented")
    }

    override suspend fun shuffle(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun addMediaItem(mediaItem: MediaItem) {
        addMediaItemCallCount++
        lastAddedMediaItem = mediaItem
        val updated = _playlistFlow.value.toMutableList()
        updated.add(mediaItem)
        _playlistFlow.value = updated
    }

    override suspend fun insertMediaItemAtNextIndex(mediaItem: MediaItem) {
        insertAtNextCallCount++
        lastInsertedMediaItem = mediaItem
        val updated = _playlistFlow.value.toMutableList()
        updated.add(mediaItem)
        _playlistFlow.value = updated
    }

    override suspend fun removeMediaItem(mediaItem: MediaItem) {
        removeMediaItemCallCount++
        lastRemovedMediaItem = mediaItem
        _playlistFlow.value = _playlistFlow.value.filter { it.url != mediaItem.url }
    }

    override suspend fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        moveCallCount++
        lastMoveFromIndex = fromIndex
        lastMoveToIndex = toIndex
        val updated = _playlistFlow.value.toMutableList()
        if (fromIndex in updated.indices && toIndex in updated.indices) {
            val item = updated.removeAt(fromIndex)
            updated.add(toIndex, item)
            _playlistFlow.value = updated
        }
    }

    override suspend fun skipToNext() {
        TODO("Not yet implemented")
    }

    override suspend fun skipToPrevious() {
        TODO("Not yet implemented")
    }

    override suspend fun jumpTo(index: Int) {
        jumpToCallCount++
        lastJumpToIndex = index
        _currentMediaItemFlow.value = _playlistFlow.value.getOrNull(index)
    }

    private val _playerStateFlow = MutableStateFlow(PlayerState.IDLE)
    override val playerStateFlow: StateFlow<PlayerState> = _playerStateFlow.asStateFlow()

    fun setPlaylist(items: List<MediaItem>) {
        _playlistFlow.value = items
    }

    fun setCurrentItem(item: MediaItem?) {
        _currentMediaItemFlow.value = item
    }

    fun setLoopState(state: LoopState) {
        _loopStateFlow.value = state
    }

    fun setPlayerState(state: PlayerState) {
        _playerStateFlow.value = state
    }

    fun setPosition(position: Duration) {
        _positionFlow.value = position
    }

    fun setDuration(duration: Duration) {
        _durationFlow.value = duration
    }

    fun resetCallCounts() {
        loadCallCount = 0
        playCallCount = 0
        addMediaItemCallCount = 0
        insertAtNextCallCount = 0
        removeMediaItemCallCount = 0
        moveCallCount = 0
        jumpToCallCount = 0
    }
}
