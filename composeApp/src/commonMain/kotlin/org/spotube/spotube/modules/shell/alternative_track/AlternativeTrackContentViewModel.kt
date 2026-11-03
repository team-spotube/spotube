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

package org.spotube.spotube.modules.shell.alternative_track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.server.AlternativeTracksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlternativeTrackUiState(
    val currentTrack: MetadataTrack? = null,
    val alternatives: List<AudioSource> = emptyList(),
    val activeSourceId: String? = null,
    val isLoading: Boolean = false)

class AlternativeTrackContentViewModel(
    private val audioPlayerQueue: AudioPlayerQueue,
    private val alternativeTracksRepository: AlternativeTracksRepository) : ViewModel() {
    private val alternativeVisibilityFlow = MutableStateFlow(false)
    private val alternativesFlow = MutableStateFlow<List<AudioSource>>(emptyList())
    private val activeSourceIdFlow = MutableStateFlow<String?>(null)
    private val isLoadingFlow = MutableStateFlow(false)

    val isAlternativeVisible: StateFlow<Boolean> = alternativeVisibilityFlow.asStateFlow()

    val alternativeTrackUiState: StateFlow<AlternativeTrackUiState> = combine(
        audioPlayerQueue.currentQueueEntryFlow,
        alternativesFlow,
        activeSourceIdFlow,
        isLoadingFlow) { currentEntry, alternatives, activeSourceId, isLoading ->
        val currentTrack = (currentEntry as? QueueEntry.StreamingTrack)?.track
        AlternativeTrackUiState(
            currentTrack = currentTrack,
            alternatives = alternatives,
            activeSourceId = activeSourceId,
            isLoading = isLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlternativeTrackUiState())

    fun toggleAlternativeVisibility() {
        alternativeVisibilityFlow.update { !it }
    }

    fun setAlternativeVisibility(isVisible: Boolean) {
        alternativeVisibilityFlow.value = isVisible
    }

    fun loadAlternatives() {
        val currentTrack = (audioPlayerQueue.currentQueueEntryFlow.value as? QueueEntry.StreamingTrack)?.track
        if (currentTrack == null) {
            alternativesFlow.value = emptyList()
            activeSourceIdFlow.value = null
            return
        }
        viewModelScope.launch {
            isLoadingFlow.value = true
            val sources = alternativeTracksRepository.resolveAlternatives(currentTrack)
            alternativesFlow.value = sources
            activeSourceIdFlow.value = alternativeTracksRepository.getActiveSourceId(currentTrack)
            isLoadingFlow.value = false
        }
    }

    fun selectAlternative(source: AudioSource) {
        val currentTrack =
            (audioPlayerQueue.currentQueueEntryFlow.value as? QueueEntry.StreamingTrack)?.track
                ?: return
        viewModelScope.launch {
            alternativeTracksRepository.selectAlternative(currentTrack, source)
            activeSourceIdFlow.value = source.id
            setAlternativeVisibility(false)
        }
    }
}
