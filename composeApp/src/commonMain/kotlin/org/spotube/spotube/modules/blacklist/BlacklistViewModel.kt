/*
 * Copyright (C) 2026 Kingmor Roy Tirtho and Spotube Contributors
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

package org.spotube.spotube.modules.blacklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlacklistViewModel(
    private val repository: BlacklistRepository) : ViewModel() {
    
    val blacklistedTracks: StateFlow<List<MetadataTrack>> = repository.blacklistedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val blacklistedArtists: StateFlow<List<MetadataArtist>> = repository.blacklistedArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTrack(track: MetadataTrack) {
        viewModelScope.launch {
            repository.toggleTrack(track)
        }
    }

    fun toggleArtist(artist: MetadataArtist) {
        viewModelScope.launch {
            repository.toggleArtist(artist)
        }
    }
}
