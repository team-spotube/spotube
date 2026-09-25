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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.core.remote.RemoteCollectionType
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.core.ui.component.cards.PlayableCard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PlaylistCard(
    playlist: MetadataPlaylist,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    playbackHelper: CollectionPlaybackHelper = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()

    PlayableCard(
        title = playlist.title,
        subtitle = playlist.description,
        imageURL = playlist.thumbnails.firstOrNull()?.url,
        isPlaying = currentCollectionEntry?.id == playlist.id,
        onClick = {
            navigationCommands.navigateTo(Routes.Playlist(playlist.id))
        },
        onPlay = {
            if (audioPlayerQueue.isPlaylistPlaying(playlist.id)) return@PlayableCard
            remotePlaybackController.requestCollectionPlay(
                RemoteCollectionType.Playlist,
                playlist.id,
                playlist.title,
            )
        },
        onAddToQueue = {
            remotePlaybackController.requestCollectionAddToQueue(
                RemoteCollectionType.Playlist,
                playlist.id,
                playlist.title,
            )
        },
        modifier = modifier,
        sharedElementKey = "playlist_art_${playlist.id}",
    )
}
