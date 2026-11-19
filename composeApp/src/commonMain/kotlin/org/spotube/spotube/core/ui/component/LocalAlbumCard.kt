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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.koinInject
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.local.LocalMediaAlbum
import org.spotube.spotube.modules.library.local.LocalMediaCollectionsRepository
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_local_tracks_count

/**
 * A synthetic album built from the local media index. Unlike [AlbumCard] there is no
 * RemotePlaybackController (local tracks can't stream to remote devices) and no busy-loading
 * state: the tracks are already on the album object, so playing is a single queue load.
 */
@Composable
fun LocalAlbumCard(
    album: LocalMediaAlbum,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    collectionsRepository: LocalMediaCollectionsRepository = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    listMode: Boolean = false
) {
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    PlayableCard(
        title = album.name,
        subtitle = pluralStringResource(
            Res.plurals.library_local_tracks_count,
            album.trackCount,
            album.trackCount
        ),
        imageModel = album.coverBytes,
        isPlaying = currentCollectionEntry?.id == album.key,
        onClick = {
            navigationCommands.navigateTo(Routes.LocalAlbum(album.key))
        },
        onPlay = {
            if (currentCollectionEntry?.id == album.key) return@PlayableCard
            scope.launch {
                audioPlayerQueue.load(
                    entries = album.tracks.map { it.toQueueEntry() },
                    autoPlay = true,
                    startPosition = 0,
                    collectionEntry = QueueCollectionEntry.Album(album.key)
                )
            }
        },
        onAddToQueue = {
            scope.launch {
                audioPlayerQueue.addAllToQueue(album.tracks.map { it.toQueueEntry() })
            }
        },
        modifier = modifier,
        sharedElementKey = "local_album_art_${album.key}",
        listMode = listMode
    )
}

private fun LocalMediaTrack.toQueueEntry() = QueueEntry.LocalTrack(
    name = name,
    artists = artists,
    duration = durationMs,
    album = album,
    coverBytes = coverBytes,
    url = path
)
