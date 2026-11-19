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

package org.spotube.spotube.modules.library.local.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.i18n.getString
import org.spotube.spotube.modules.library.local.LocalMediaAlbum
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.media_unknown_artist

data class LocalAlbumTrackUi(
    val path: String,
    val title: String,
    val subtitle: String?,
    val durationLabel: String?,
    val artwork: ByteArray?
)

sealed interface LocalAlbumUiState {
    data object Loading : LocalAlbumUiState
    data object NotFound : LocalAlbumUiState
    data class Loaded(
        val album: LocalMediaAlbum,
        val tracks: List<LocalAlbumTrackUi>,
        /** Distinct artist names across the album, ready to render (unknown-artist fallback applied). */
        val artistsLabel: String,
        val isPlaying: Boolean
    ) : LocalAlbumUiState
}

/**
 * The detail screen of one synthetic local album. Playback loads the album's tracks as
 * [QueueEntry.LocalTrack]s under [QueueCollectionEntry.Album], so the album's card, the queue
 * history and this screen agree on what "now playing" is.
 */
class LocalAlbumViewModel(
    private val albumName: String,
    private val collectionsRepository: LocalMediaCollectionsRepository,
    private val audioPlayerQueue: AudioPlayerQueue
) : ViewModel() {
    val state: StateFlow<LocalAlbumUiState> = combine(
        collectionsRepository.albumFlow(albumName),
        audioPlayerQueue.currentCollectionEntryFlow
    ) { album, currentCollectionEntry ->
        if (album == null) LocalAlbumUiState.NotFound
        else LocalAlbumUiState.Loaded(
            album = album,
            tracks = album.tracks.map { it.toTrackUi() },
            artistsLabel = album.artistsLabel(),
            isPlaying = currentCollectionEntry?.id == album.key
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        LocalAlbumUiState.Loading
    )

    /** Plays the album from its first track; a no-op while it is already the active collection. */
    fun play() {
        val album = currentAlbum() ?: return
        if (audioPlayerQueue.currentCollectionEntryFlow.value?.id == album.key) return
        playTracks(album.tracks, 0, album.key)
    }

    fun playFromTrack(path: String) {
        val album = currentAlbum() ?: return
        val index = album.tracks.indexOfFirst { it.path == path }
        if (index >= 0) playTracks(album.tracks, index, album.key)
    }

    fun shufflePlay() {
        val album = currentAlbum() ?: return
        val shuffled = album.tracks.shuffled()
        if (shuffled.isEmpty()) return
        playTracks(shuffled, 0, album.key)
    }

    fun addToQueue() {
        val album = currentAlbum() ?: return
        val entries = album.tracks.map { it.toQueueEntry() }
        if (entries.isEmpty()) return
        viewModelScope.launch {
            audioPlayerQueue.addAllToQueue(entries)
        }
    }

    private fun currentAlbum(): LocalMediaAlbum? =
        (state.value as? LocalAlbumUiState.Loaded)?.album

    private fun playTracks(tracks: List<LocalMediaTrack>, startPosition: Int, albumKey: String) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            audioPlayerQueue.load(
                entries = tracks.map { it.toQueueEntry() },
                autoPlay = true,
                startPosition = startPosition,
                collectionEntry = QueueCollectionEntry.Album(albumKey)
            )
        }
    }
}

private fun LocalMediaTrack.toTrackUi() = LocalAlbumTrackUi(
    path = path,
    title = name,
    subtitle = listOfNotNull(
        artists.joinToString(", ").takeIf { it.isNotBlank() },
        album?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").takeIf { it.isNotBlank() },
    durationLabel = durationMs.toDurationLabel(),
    artwork = coverBytes?.takeIf { it.isNotEmpty() }
)

/** Distinct credited artists across the album, joined for the header, with the unknown fallback. */
private suspend fun LocalMediaAlbum.artistsLabel(): String {
    val joined = tracks.asSequence()
        .flatMap { it.artists.asSequence() }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
    return joined.ifBlank { getString(Res.string.media_unknown_artist) }
}

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
