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

package org.spotube.spotube.modules.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.remote.RemoteControlClient
import org.spotube.spotube.core.remote.RemoteControlCommand
import org.spotube.spotube.core.remote.RemoteControlEvent
import org.spotube.spotube.modules.shell.formatPlayerTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

enum class RemoteLoopMode(val wireValue: String) {
    NONE("none"),
    ONE("one"),
    ALL("all");

    fun next(): RemoteLoopMode = when (this) {
        NONE -> ONE
        ONE -> ALL
        ALL -> NONE
    }

    companion object {
        fun fromWire(value: String): RemoteLoopMode = entries.firstOrNull { it.wireValue == value } ?: NONE
    }
}

enum class RemoteVolumeLevel { MUTED, LOW, HIGH }

data class RemoteQueueItemUi(
    val key: String,
    val index: Int,
    val mediaUrl: String,
    val title: String,
    val artists: String,
    val coverUrl: String?,
    val durationLabel: String,
    val isCurrent: Boolean)

data class RemoteControlUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val deviceName: String = "",
    val hasTrack: Boolean = false,
    val title: String? = null,
    val artists: String? = null,
    val album: String? = null,
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val isShuffling: Boolean = false,
    val loopMode: RemoteLoopMode = RemoteLoopMode.NONE,
    val canSeek: Boolean = false,
    val seekProgress: Float = 0f,
    val positionLabel: String = formatPlayerTime(0.milliseconds),
    val durationLabel: String = formatPlayerTime(0.milliseconds),
    val volume: Float = 1f,
    val volumeLevel: RemoteVolumeLevel = RemoteVolumeLevel.HIGH,
    val queue: List<RemoteQueueItemUi> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isQueueVisible: Boolean = false)

private data class LocalOverrides(
    val seekProgress: Float?,
    val volume: Float?,
    val isQueueVisible: Boolean)

class RemoteControlViewModel : ViewModel(), KoinComponent {
    private val remoteControlClient: RemoteControlClient by inject()

    // While the user drags a slider these hold the local value, so the host's
    // periodic state pushes don't yank the thumb back mid-gesture.
    private val seekOverride = MutableStateFlow<Float?>(null)
    private val volumeOverride = MutableStateFlow<Float?>(null)
    private val isQueueVisible = MutableStateFlow(false)

    private val overrides = combine(seekOverride, volumeOverride, isQueueVisible) { seek, volume, queueVisible ->
        LocalOverrides(seek, volume, queueVisible)
    }

    val uiState: StateFlow<RemoteControlUiState> = combine(
        remoteControlClient.connectionState,
        remoteControlClient.latestPlayerState,
        remoteControlClient.latestQueue,
        overrides
    ) { connection, player, queue, local ->
        buildUiState(connection, player, queue, local)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteControlUiState())

    init {
        // Stream volume changes while dragging, throttled so a fast drag doesn't
        // flood the socket with one command per frame.
        viewModelScope.launch {
            volumeOverride.filterNotNull().collect { volume ->
                remoteControlClient.sendCommand(RemoteControlCommand.SetVolume(volume))
                delay(VOLUME_THROTTLE_MS)
            }
        }
    }

    private fun buildUiState(
        connection: ConnectionState,
        player: RemoteControlEvent.PlayerState?,
        queue: RemoteControlEvent.QueueUpdated?,
        local: LocalOverrides
    ): RemoteControlUiState {
        val durationMs = player?.durationMs ?: 0L
        val canSeek = durationMs > 0L
        val liveProgress = if (canSeek) {
            ((player?.positionMs ?: 0L).toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
        val seekProgress = local.seekProgress ?: liveProgress
        val currentIndex = queue?.currentIndex ?: -1
        val volume = local.volume ?: player?.volume ?: 1f

        return RemoteControlUiState(
            connection = connection,
            deviceName = (connection as? ConnectionState.Connected)?.deviceName.orEmpty(),
            hasTrack = player?.currentTrackId != null || player?.currentTrackTitle != null,
            title = player?.currentTrackTitle,
            artists = player?.currentTrackArtists,
            album = player?.currentTrackAlbum,
            coverUrl = player?.currentTrackCoverUrl,
            isPlaying = player?.isPlaying ?: false,
            isShuffling = player?.shuffleEnabled ?: false,
            loopMode = RemoteLoopMode.fromWire(player?.loopMode ?: RemoteLoopMode.NONE.wireValue),
            canSeek = canSeek,
            seekProgress = seekProgress,
            positionLabel = formatPlayerTime((durationMs * seekProgress).toLong().milliseconds),
            durationLabel = formatPlayerTime(durationMs.milliseconds),
            volume = volume,
            volumeLevel = when {
                volume <= 0f -> RemoteVolumeLevel.MUTED
                volume < 0.5f -> RemoteVolumeLevel.LOW
                else -> RemoteVolumeLevel.HIGH
            },
            queue = queue?.entries.orEmpty().mapIndexed { index, entry ->
                RemoteQueueItemUi(
                    key = "${entry.mediaUrl}@$index",
                    index = index,
                    mediaUrl = entry.mediaUrl,
                    title = entry.title,
                    artists = entry.artists,
                    coverUrl = entry.coverUrl,
                    durationLabel = formatPlayerTime(entry.durationMs.milliseconds),
                    isCurrent = index == currentIndex)
            },
            currentQueueIndex = currentIndex,
            isQueueVisible = local.isQueueVisible)
    }

    private fun send(command: RemoteControlCommand) {
        viewModelScope.launch {
            remoteControlClient.sendCommand(command)
        }
    }

    /**
     * Keeps [override] at [value] until the host reports back (or a timeout), so
     * the slider doesn't flash the stale value in between. Skips the reset if the
     * user already started a new drag.
     */
    private suspend fun releaseOverrideAfterEcho(override: MutableStateFlow<Float?>, value: Float) {
        withTimeoutOrNull(OVERRIDE_RELEASE_TIMEOUT_MS) {
            remoteControlClient.latestPlayerState.drop(1).first()
        }
        override.compareAndSet(value, null)
    }

    fun togglePlayPause() = send(RemoteControlCommand.TogglePlayPause)

    fun skipNext() = send(RemoteControlCommand.SkipNext)

    fun skipPrevious() = send(RemoteControlCommand.SkipPrevious)

    fun onSeekChange(progress: Float) {
        seekOverride.value = progress.coerceIn(0f, 1f)
    }

    fun onSeekFinished() {
        val progress = seekOverride.value ?: return
        val durationMs = remoteControlClient.latestPlayerState.value?.durationMs ?: 0L
        if (durationMs <= 0L) {
            seekOverride.value = null
            return
        }
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.Seek((durationMs * progress).toLong()))
            releaseOverrideAfterEcho(seekOverride, progress)
        }
    }

    fun onVolumeChange(volume: Float) {
        volumeOverride.value = volume.coerceIn(0f, 1f)
    }

    fun onVolumeChangeFinished() {
        val volume = volumeOverride.value ?: return
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.SetVolume(volume))
            releaseOverrideAfterEcho(volumeOverride, volume)
        }
    }

    fun toggleShuffle() {
        val shuffling = remoteControlClient.latestPlayerState.value?.shuffleEnabled ?: false
        send(RemoteControlCommand.SetShuffle(!shuffling))
    }

    fun cycleLoopMode() {
        val current = RemoteLoopMode.fromWire(
            remoteControlClient.latestPlayerState.value?.loopMode ?: RemoteLoopMode.NONE.wireValue)
        send(RemoteControlCommand.SetLoopMode(current.next().wireValue))
    }

    fun playQueueItem(index: Int) {
        if (index < 0) return
        send(RemoteControlCommand.PlayIndex(index))
    }

    fun removeQueueItem(mediaUrl: String) = send(RemoteControlCommand.RemoveFromQueue(mediaUrl))

    fun toggleQueueVisibility() {
        isQueueVisible.update { !it }
    }

    fun dismissQueue() {
        isQueueVisible.value = false
    }

    fun disconnect() {
        viewModelScope.launch {
            remoteControlClient.disconnect()
        }
    }

    private companion object {
        const val VOLUME_THROTTLE_MS = 100L
        const val OVERRIDE_RELEASE_TIMEOUT_MS = 1_500L
    }
}
