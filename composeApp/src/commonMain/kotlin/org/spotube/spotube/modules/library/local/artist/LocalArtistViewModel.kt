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

package org.spotube.spotube.modules.library.local.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.library.local.LocalMediaArtist
import org.spotube.spotube.modules.library.local.LocalCollectionTrack
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack

data class LocalArtistTrackUi(
    override val path: String,
    override val title: String,
    override val subtitle: String?,
    override val durationLabel: String?,
    override val artwork: ByteArray?
) : LocalCollectionTrack

sealed interface LocalArtistUiState {
    data object Loading : LocalArtistUiState
    data object NotFound : LocalArtistUiState
    data class Loaded(
        val artist: LocalMediaArtist,
        val tracks: List<LocalArtistTrackUi>,
        /** Always false: a synthetic artist is not a queue collection, so nothing can mark it playing. */
        val isPlaying: Boolean = false
    ) : LocalArtistUiState
}

/**
 * The detail screen of one synthetic local artist. Unlike an album there is no
 * [org.spotube.spotube.core.audioplayer.QueueCollectionEntry] for an artist, so playback loads
 * the tracks without a collection context and the screen never reports itself as playing.
 */
class LocalArtistViewModel(
    private val artistName: String,
    private val collectionsRepository: LocalMediaCollectionsRepository,
    private val audioPlayerQueue: AudioPlayerQueue
) : ViewModel() {
    val state: StateFlow<LocalArtistUiState> = collectionsRepository.artistFlow(artistName)
        .map { artist ->
            if (artist == null) LocalArtistUiState.NotFound
            else LocalArtistUiState.Loaded(
                artist = artist,
                tracks = artist.tracks.map { it.toTrackUi() }
            )
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            LocalArtistUiState.Loading
        )

    fun play() {
        val artist = currentArtist() ?: return
        playTracks(artist.tracks, 0)
    }

    fun playFromTrack(path: String) {
        val artist = currentArtist() ?: return
        val index = artist.tracks.indexOfFirst { it.path == path }
        if (index >= 0) playTracks(artist.tracks, index)
    }

    fun shufflePlay() {
        val artist = currentArtist() ?: return
        val shuffled = artist.tracks.shuffled()
        if (shuffled.isEmpty()) return
        playTracks(shuffled, 0)
    }

    fun addToQueue() {
        val artist = currentArtist() ?: return
        val entries = artist.tracks.map { it.toQueueEntry() }
        if (entries.isEmpty()) return
        viewModelScope.launch {
            audioPlayerQueue.addAllToQueue(entries)
        }
    }

    private fun currentArtist(): LocalMediaArtist? =
        (state.value as? LocalArtistUiState.Loaded)?.artist

    private fun playTracks(tracks: List<LocalMediaTrack>, startPosition: Int) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            audioPlayerQueue.load(
                entries = tracks.map { it.toQueueEntry() },
                autoPlay = true,
                startPosition = startPosition,
                collectionEntry = null
            )
        }
    }
}

private fun LocalMediaTrack.toTrackUi() = LocalArtistTrackUi(
    path = path,
    title = name,
    subtitle = listOfNotNull(
        artists.joinToString(", ").takeIf { it.isNotBlank() },
        album?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").takeIf { it.isNotBlank() },
    durationLabel = durationMs.toDurationLabel(),
    artwork = coverBytes?.takeIf { it.isNotEmpty() }
)

private fun Long.toDurationLabel(): String? {
    val seconds = this / 1000
    if (seconds <= 0) return null
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private fun LocalMediaTrack.toQueueEntry() = QueueEntry.LocalTrack(
    name = name,
    artists = artists,
    duration = durationMs,
    album = album,
    coverBytes = coverBytes,
    url = path
)
