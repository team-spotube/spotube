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

package org.spotube.spotube.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
import org.spotube.spotube.modules.library.local.LocalPlaylist
import org.spotube.spotube.modules.library.local.decodeLocalCoverBase64
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_local_tracks_count

/**
 * A user-created playlist of on-device tracks in the library grid/list. Unlike [PlaylistCard]
 * there is no remote playback round-trip: the tracks resolve straight from the local media
 * index, so play/add-to-queue never show a loading state.
 */
@Composable
fun LocalPlaylistCard(
    playlist: LocalPlaylist,
    modifier: Modifier = Modifier,
    artworkOverride: ByteArray? = null,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    listMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val collectionsRepository: LocalMediaCollectionsRepository = koinInject()

    // Resolved on demand from the current index snapshot; missing paths are skipped.
    fun resolveEntries(): List<QueueEntry> {
        val tracksByPath = collectionsRepository.tracks.value.associateBy { it.path }
        return playlist.trackPaths.mapNotNull { path ->
            tracksByPath[path]?.let { track ->
                QueueEntry.LocalTrack(
                    name = track.name,
                    artists = track.artists,
                    duration = track.durationMs,
                    album = track.album,
                    coverBytes = track.coverBytes,
                    url = track.path
                )
            }
        }
    }

    PlayableCard(
        title = playlist.name,
        subtitle = playlist.description?.takeIf { it.isNotBlank() }
            ?: pluralStringResource(
                Res.plurals.library_local_tracks_count,
                playlist.trackPaths.size,
                playlist.trackPaths.size
            ),
        imageModel = artworkOverride ?: decodeLocalCoverBase64(playlist.coverBase64),
        isPlaying = currentCollectionEntry?.id == playlist.id,
        onClick = {
            navigationCommands.navigateTo(Routes.LocalPlaylist(playlist.id))
        },
        onPlay = {
            if (audioPlayerQueue.isPlaylistPlaying(playlist.id)) return@PlayableCard
            val entries = resolveEntries()
            if (entries.isEmpty()) return@PlayableCard
            scope.launch {
                audioPlayerQueue.load(
                    entries = entries,
                    autoPlay = true,
                    startPosition = 0,
                    collectionEntry = QueueCollectionEntry.Playlist(playlist.id)
                )
            }
        },
        onAddToQueue = {
            val entries = resolveEntries()
            if (entries.isEmpty()) return@PlayableCard
            scope.launch { audioPlayerQueue.addAllToQueue(entries) }
        },
        modifier = modifier,
        sharedElementKey = "local_playlist_art_${playlist.id}",
        listMode = listMode
    )
}
