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

package org.spotube.spotube.modules.saved_tracks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.koin.compose.koinInject
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.CollectionView
import org.spotube.spotube.modules.library.playlist.AddToPlaylistPicker
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.liked_tracks
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun SavedTracksScreen(
    viewModel: SavedTracksViewModel,
    audioPlayerQueue: AudioPlayerQueue,
    audioPlayer: AudioPlayerInterface,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackUiState by viewModel.playbackUiState.collectAsStateWithLifecycle()
    val jamRoomService: JamRoomService = koinInject()
    val jamActive by jamRoomService.role.map { it != null }
        .collectAsStateWithLifecycle(initialValue = false)
    val isJamGuest by jamRoomService.role.map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val currentCollectionEntry by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()

    val dataState = state as? SavedTracksScreenState.Data
    val isPlaying = currentCollectionEntry is QueueCollectionEntry.SavedTracks &&
        playerState == PlayerState.PLAYING

    val errorMessage = (state as? SavedTracksScreenState.Error)?.message
    val isLoading = state is SavedTracksScreenState.Loading && (dataState == null || dataState.tracks.isEmpty())

    CollectionView(
        isPlayLoading = playbackUiState.isPlayLoading,
        isShuffleLoading = playbackUiState.isShuffleLoading,
        isAddToQueueLoading = playbackUiState.isAddToQueueLoading,
        title = stringResource(Res.string.saved_tracks_title),
        description = dataState?.let { pluralStringResource(Res.plurals.library_local_tracks_count, it.totalCount, it.totalCount) } ?: "",
        imageURL = "",
        imageResource = Res.drawable.liked_tracks,
        ownerName = stringResource(Res.string.saved_tracks_owner_you),
        ownerImageURL = null,
        onOwnerClick = {},
        onPlay = viewModel::playSavedTracks,
        onShufflePlay = viewModel::shufflePlaySavedTracks,
        onAddToQueue = viewModel::addSavedTracksToQueue,
        isPlaying = isPlaying,
        isFollowing = false,
        onFollowClick = {},
        showFollowButton = false,
        isLoading = isLoading,
        error = errorMessage,
        onRetry = { viewModel.refresh() },
        tracks = dataState?.tracks ?: emptyList(),
        hasMore = dataState?.nextPagination != null,
        isLoadingNextPage = state is SavedTracksScreenState.Data.LoadingMore,
        currentTrackId = trackOptionsContext.currentTrackId,
        isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
        onTrackClick = viewModel::playSavedTracksFromTrack,
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
                onPlaylistSelected = viewModel::addTracksToPlaylist)
        })
}
