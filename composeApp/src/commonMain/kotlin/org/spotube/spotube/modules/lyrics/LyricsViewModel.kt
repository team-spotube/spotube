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

package org.spotube.spotube.modules.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.plugin.PluginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
enum class LyricType {
    STATIC,
    SYNCED
}

data class LyricsUiState(
    val mode: LyricType = LyricType.SYNCED,
    val syncedLyrics: List<LyricsLine>? = null,
    val plainLyrics: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = -1,
    val trackTitle: String = "",
    val trackArtists: String = "",
    val coverUrl: String = "")

class LyricsViewModel(
    private val pluginManager: PluginManager,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())

    // Only re-emits when the active line changes, not on every position tick.
    private val currentIndexFlow = combine(
        _uiState.map { it.syncedLyrics }.distinctUntilChanged(),
        audioPlayer.positionFlow
    ) { lyrics, position -> activeLineIndex(lyrics, position.inWholeMilliseconds) }
        .distinctUntilChanged()

    val uiState: StateFlow<LyricsUiState> = combine(_uiState, currentIndexFlow) { state, index ->
        state.copy(currentIndex = index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsUiState())

    private var currentTrackId: String? = null

    fun setMode(mode: LyricType) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun loadLyrics(forceReload: Boolean = false) {
        viewModelScope.launch {
            val entry = audioPlayerQueue.currentQueueEntryFlow.value ?: return@launch
            val track = when (entry) {
                is QueueEntry.StreamingTrack -> entry.track
                is QueueEntry.LocalTrack -> return@launch
            }

            if (!forceReload && currentTrackId == track.id && _uiState.value.syncedLyrics != null) {
                return@launch
            }

            currentTrackId = track.id
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                pluginManager.selectedLyricsPlugin.value?.let { pluginService ->
                    pluginService.use {
                       val lyrics = lyricsAPI.getLyrics(track)

                        _uiState.update {
                            it.copy(
                                syncedLyrics = lyrics?.syncedLyrics,
                                plainLyrics = lyrics?.plainLyrics,
                                isLoading = false)
                        }
                    }
                } ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    init {
        viewModelScope.launch {
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                val trackId = when (entry) {
                    is QueueEntry.StreamingTrack -> entry.track.id
                    else -> null
                }
                if (entry is QueueEntry.StreamingTrack) {
                    val track = entry.track
                    _uiState.update {
                        it.copy(
                            trackTitle = track.title,
                            trackArtists = track.artists.joinToString(", ") { artist -> artist.name },
                            coverUrl = (track.album?.thumbnails ?: track.thumbnails)
                                ?.firstOrNull()?.url.orEmpty()
                        )
                    }
                }
                if (trackId != null && trackId != currentTrackId) {
                    loadLyrics()
                }
            }
        }
    }

    fun seekToLine(line: LyricsLine) {
        viewModelScope.launch {
            audioPlayer.seekTo(line.time.milliseconds)
        }
    }

    private fun activeLineIndex(lyrics: List<LyricsLine>?, positionMillis: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        val index = lyrics.indexOfLast { it.time <= positionMillis }
        return index.coerceAtLeast(0)
    }
}
