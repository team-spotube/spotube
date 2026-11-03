/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaLibraryCoordinator
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack

class LocalFolderViewModel(
    private val folderPath: String,
    coordinator: LocalMediaLibraryCoordinator,
    private val audioPlayerQueue: AudioPlayerQueue
) : ViewModel() {
    val state: StateFlow<LibraryLocalTracksState> = coordinator.cacheState
        .map { cache ->
            localTracksUiState(
                LibraryLocalTracksState(folders = cache.folders, currentFolderPath = folderPath),
                ""
            )
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            localTracksUiState(LibraryLocalTracksState(currentFolderPath = folderPath), "")
        )

    fun playFolder() = play(0)

    fun playTrack(trackPath: String) {
        val index = tracks().indexOfFirst { it.path == trackPath }
        if (index >= 0) play(index)
    }

    private fun tracks(): List<LocalMediaTrack> =
        state.value.currentFolder?.tracks.orEmpty()

    private fun play(startPosition: Int) {
        val tracks = tracks()
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            audioPlayerQueue.load(
                entries = tracks.map {
                    QueueEntry.LocalTrack(
                        name = it.name,
                        artists = it.artists,
                        duration = it.durationMs,
                        album = it.album,
                        coverBytes = it.coverBytes,
                        url = it.path
                    )
                },
                autoPlay = true,
                startPosition = startPosition,
                collectionEntry = null
            )
        }
    }
}
