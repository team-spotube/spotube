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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.core.remote.RemoteCollectionType
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.ui.component.cards.PlayableCard
import org.koin.compose.koinInject

@Composable
fun AlbumCard(
    album: MetadataAlbum,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    playbackHelper: CollectionPlaybackHelper = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject(),
) {
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()

    PlayableCard(
        title = album.title,
        subtitle = album.description ?: "${album.albumType} • ${album.artists.joinToString { it.name }}",
        imageURL = album.thumbnails.firstOrNull()?.url,
        isPlaying = currentCollectionEntry?.id == album.id,
        onClick = {
            navigationCommands.navigateTo(Routes.Album(album.id))
        },
        onPlay = {
            if (currentCollectionEntry?.id == album.id) return@PlayableCard
            remotePlaybackController.requestCollectionPlay(
                RemoteCollectionType.Album,
                album.id,
                album.title,
            )
        },
        onAddToQueue = {
            remotePlaybackController.requestCollectionAddToQueue(
                RemoteCollectionType.Album,
                album.id,
                album.title,
            )
        },
        modifier = modifier.width(160.dp),
        sharedElementKey = "album_art_${album.id}",
    )
}
