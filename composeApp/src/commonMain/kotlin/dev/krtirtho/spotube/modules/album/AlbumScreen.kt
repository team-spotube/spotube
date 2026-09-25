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

package dev.krtirtho.spotube.modules.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.PlayerState
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import org.koin.compose.koinInject
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.ui.component.CollectionView
import dev.krtirtho.spotube.modules.library.playlist.AddToPlaylistPicker

@Composable
fun AlbumScreen(
    albumId: String,
    viewModel: AlbumViewModel,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val savedAlbumIds by viewModel.savedAlbumIds.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()

    val dataState = state as? AlbumScreenState.Data
    val album = dataState?.album
    val ownerName =
        album?.artists?.joinToString { it.name }.orEmpty().ifBlank { "Unknown artist" }
    val ownerImageURL = album?.artists?.firstOrNull()?.thumbnails?.firstOrNull()?.url
    val firstArtistId = album?.artists?.firstOrNull()?.id
    val artworkUrl = album?.thumbnails?.firstOrNull()?.url.orEmpty()
    val isAlbumPlaying = currentCollectionEntry?.id == albumId &&
        playerState == PlayerState.PLAYING

    val errorMessage = (state as? AlbumScreenState.Error)?.message
    val isLoading = state is AlbumScreenState.Loading && (dataState == null || dataState.tracks.isEmpty())

    CollectionView(
        title = album?.title ?: "Loading album...",
        description = album?.description ?: "${album?.albumType?.name.orEmpty()} • ${album?.releaseDate.orEmpty()}",
        imageURL = artworkUrl,
        ownerName = ownerName,
        ownerImageURL = ownerImageURL,
        onOwnerClick = {
            firstArtistId?.let {
                navigationCommands.navigateTo(Routes.Artist(it))
            }
        },
        onPlay = viewModel::playAlbum,
        onShufflePlay = {},
        onAddToQueue = viewModel::addAlbumToQueue,
        isPlaying = isAlbumPlaying,
        isFollowing = savedAlbumIds.contains(albumId),
        onFollowClick = viewModel::toggleSavedAlbum,
        isLoading = isLoading,
        error = errorMessage,
        onRetry = { viewModel.refresh() },
        tracks = dataState?.tracks ?: emptyList(),
        hasMore = dataState?.nextPagination != null,
        isLoadingNextPage = state is AlbumScreenState.Data.LoadingMore,
        currentTrackId = trackOptionsContext.currentTrackId,
        isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
        onTrackClick = viewModel::playAlbumFromTrack,
        onLoadNextPage = viewModel::loadNextTracksPage,
        onArtistClick = { navigationCommands.navigateTo(Routes.Artist(it.id)) },
        onAlbumClick = { navigationCommands.navigateTo(Routes.Album(it.id)) },
        onTrackOptionsAction = viewModel::handleTrackOptionsAction,
        trackOptionsState = trackOptionsContext::stateFor,
        onBulkDownload = viewModel::downloadTracks,
        onBulkAddToQueue = viewModel::addTracksToQueue,
        onBulkPlayNext = viewModel::playTracksNext,
        onBulkAddToPlaylist = viewModel::showAddToPlaylistPicker,
        onBulkAddToJam = viewModel::addTracksToJam,
        isInJam = jamActive,
        isJamGuest = isJamGuest,
        onAddToJam = viewModel::addTracksToJam,
        trailingContent = {
            AddToPlaylistPicker(
                visible = showAddToPlaylistPicker,
                currentUserId = currentUserId,
                onDismiss = viewModel::dismissAddToPlaylistPicker,
                onPlaylistSelected = viewModel::addTracksToPlaylist,
            )
        },
    )
}
