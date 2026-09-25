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

package dev.krtirtho.spotube.modules.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.krtirtho.spotube.core.remote.ConnectionState
import dev.krtirtho.spotube.core.remote.RemoteControlClient
import dev.krtirtho.spotube.core.remote.RemoteControlCommand
import dev.krtirtho.spotube.core.remote.RemoteControlEvent
import dev.krtirtho.spotube.core.remote.RemoteQueueEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class RemotePlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val volume: Float = 1.0f,
    val shuffleEnabled: Boolean = false,
    val loopMode: String = "none",
    val currentTrackId: String? = null,
    val currentTrackTitle: String? = null,
    val currentTrackArtists: String? = null,
    val currentTrackAlbum: String? = null,
    val currentTrackCoverUrl: String? = null,
)

data class RemoteQueueState(
    val entries: List<RemoteQueueEntry> = emptyList(),
    val currentIndex: Int = -1,
)

class RemoteControlViewModel : ViewModel(), KoinComponent {
    private val logger = Logger.withTag("RemoteControlViewModel")
    private val remoteControlClient: RemoteControlClient by inject()

    private val _playerState = MutableStateFlow(RemotePlayerState())
    val playerState: StateFlow<RemotePlayerState> = _playerState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _queueState = MutableStateFlow(RemoteQueueState())
    val queueState: StateFlow<RemoteQueueState> = _queueState.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    init {
        viewModelScope.launch {
            remoteControlClient.connectionState.collect { state ->
                _connectionState.value = state
            }
        }

        viewModelScope.launch {
            remoteControlClient.latestPlayerState.collect { event ->
                if (event != null) {
                    handlePlayerState(event)
                }
            }
        }

        viewModelScope.launch {
            remoteControlClient.latestQueue.collect { event ->
                if (event != null) {
                    handleQueueUpdated(event)
                }
            }
        }
    }

    private fun handlePlayerState(event: RemoteControlEvent.PlayerState) {
        _playerState.update {
            it.copy(
                isPlaying = event.isPlaying,
                positionMs = event.positionMs,
                durationMs = event.durationMs,
                volume = event.volume,
                shuffleEnabled = event.shuffleEnabled,
                loopMode = event.loopMode,
                currentTrackId = event.currentTrackId,
                currentTrackTitle = event.currentTrackTitle,
                currentTrackArtists = event.currentTrackArtists,
                currentTrackAlbum = event.currentTrackAlbum,
                currentTrackCoverUrl = event.currentTrackCoverUrl,
            )
        }
    }

    private fun handleQueueUpdated(event: RemoteControlEvent.QueueUpdated) {
        _queueState.value = RemoteQueueState(
            entries = event.entries,
            currentIndex = event.currentIndex,
        )
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.TogglePlayPause)
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.SkipNext)
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.SkipPrevious)
        }
    }

    fun seek(positionMs: Long) {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.Seek(positionMs))
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.SetVolume(volume))
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            val newState = !_playerState.value.shuffleEnabled
            remoteControlClient.sendCommand(RemoteControlCommand.SetShuffle(newState))
        }
    }

    fun cycleLoopMode() {
        viewModelScope.launch {
            val currentMode = _playerState.value.loopMode
            val newMode = when (currentMode) {
                "none" -> "one"
                "one" -> "all"
                else -> "none"
            }
            remoteControlClient.sendCommand(RemoteControlCommand.SetLoopMode(newMode))
        }
    }

    fun playQueueItem(index: Int) {
        if (index < 0) return
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.PlayIndex(index))
        }
    }

    fun removeQueueItem(mediaUrl: String) {
        viewModelScope.launch {
            remoteControlClient.sendCommand(RemoteControlCommand.RemoveFromQueue(mediaUrl))
        }
    }

    fun toggleQueueVisibility() {
        _isQueueVisible.update { !it }
    }

    fun disconnect() {
        viewModelScope.launch {
            remoteControlClient.disconnect()
        }
    }
}
