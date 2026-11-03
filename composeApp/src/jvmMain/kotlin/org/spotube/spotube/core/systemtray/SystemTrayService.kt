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

package org.spotube.spotube.core.systemtray

import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.MediaItem
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the system tray. Owns the audio state projected into the
 * tray menu and exposes the actions the menu items trigger. It no longer
 * touches AWT/Swing — the tray itself is rendered by the [SystemTray]
 * composable via Compose Native Tray, so it stays compatible with the
 * no-AWT Tao backend.
 */
class SystemTrayService(
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val savedTracksRepository: SavedTracksRepository) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val volumeStep = 0.05f

    private var onToggleWindowVisibility: () -> Unit = {}

    val state: StateFlow<TrayState> =
        combine(
            audioPlayer.playerStateFlow,
            audioPlayer.currentMediaItemFlow,
            audioPlayer.loopStateFlow,
            audioPlayer.shuffleModeFlow,
            audioPlayer.volumeFlow,
            audioPlayerQueue.currentQueueEntryFlow,
            savedTracksRepository.savedTracksIdsFlow) { values ->
            TrayState(
                playerState = values[0] as PlayerState,
                mediaItem = values[1] as MediaItem?,
                loopState = values[2] as LoopState,
                shuffleEnabled = values[3] as Boolean,
                volume = values[4] as Float,
                currentEntry = values[5] as QueueEntry?,
                savedTrackIds = @Suppress("UNCHECKED_CAST") (values[6] as Set<String>))
        }.stateIn(scope, SharingStarted.Eagerly, TrayState.Initial)

    fun setCallbacks(onToggleWindowVisibility: () -> Unit) {
        this.onToggleWindowVisibility = onToggleWindowVisibility
    }

    fun hideWindow() = onToggleWindowVisibility()

    fun togglePlayPause() {
        scope.launch {
            if (state.value.playerState == PlayerState.PLAYING) audioPlayer.pause() else audioPlayer.play()
        }
    }

    fun skipToNext() {
        scope.launch { audioPlayer.skipToNext() }
    }

    fun skipToPrevious() {
        scope.launch { audioPlayer.skipToPrevious() }
    }

    fun setShuffle(enabled: Boolean) {
        scope.launch { audioPlayer.shuffle(enabled) }
    }

    fun setLoop(loopState: LoopState) {
        scope.launch { audioPlayer.loop(loopState) }
    }

    fun increaseVolume() {
        scope.launch { audioPlayer.setVolume((state.value.volume + volumeStep).coerceAtMost(1f)) }
    }

    fun decreaseVolume() {
        scope.launch { audioPlayer.setVolume((state.value.volume - volumeStep).coerceAtLeast(0f)) }
    }

    fun toggleMute() {
        scope.launch { audioPlayer.setVolume(if (state.value.volume <= 0f) 0.5f else 0f) }
    }

    fun toggleLike() {
        scope.launch {
            val trackId = (state.value.currentEntry as? QueueEntry.StreamingTrack)?.track?.id ?: return@launch
            val isLiked = state.value.savedTrackIds.contains(trackId)
            if (isLiked) {
                savedTracksRepository.removeSavedTracks(listOf(trackId))
            } else {
                savedTracksRepository.saveTracks(listOf(trackId))
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    data class TrayState(
        val playerState: PlayerState,
        val mediaItem: MediaItem?,
        val loopState: LoopState,
        val shuffleEnabled: Boolean,
        val volume: Float,
        val currentEntry: QueueEntry?,
        val savedTrackIds: Set<String>) {
        val isPlaying: Boolean get() = playerState == PlayerState.PLAYING

        val currentTrackId: String? get() = (currentEntry as? QueueEntry.StreamingTrack)?.track?.id

        val isCurrentTrackLiked: Boolean
            get() = currentTrackId != null && savedTrackIds.contains(currentTrackId)

        companion object {
            val Initial =
                TrayState(
                    playerState = PlayerState.IDLE,
                    mediaItem = null,
                    loopState = LoopState.NONE,
                    shuffleEnabled = false,
                    volume = 1f,
                    currentEntry = null,
                    savedTrackIds = emptySet())
        }
    }
}
