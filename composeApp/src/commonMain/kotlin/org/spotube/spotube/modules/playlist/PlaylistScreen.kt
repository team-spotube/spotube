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

package org.spotube.spotube.modules.playlist


import androidx.compose.ui.Alignment
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.koin.compose.koinInject
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.CollectionView
import org.spotube.spotube.modules.library.playlist.AddToPlaylistPicker
import org.spotube.spotube.modules.library.playlist.PlaylistFormData
import org.spotube.spotube.modules.library.playlist.PlaylistFormSheet
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.playlist_add_tracks_button
import spotube.composeapp.generated.resources.playlist_loading_title
import spotube.composeapp.generated.resources.playlist_owner_unknown

@Composable
fun PlaylistScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
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
    val savedPlaylistIds by viewModel.savedPlaylistIds.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val trackOptionsContext by viewModel.trackOptionsContext.collectAsStateWithLifecycle()
    val showAddToPlaylistPicker by viewModel.showAddToPlaylistPicker.collectAsStateWithLifecycle()
    val editPlaylistFormState by viewModel.editPlaylistFormState.collectAsStateWithLifecycle()
    var showAddTracksDialog by remember { mutableStateOf(false) }

    val dataState = state as? PlaylistScreenState.Data
    val playlist = dataState?.playlist
    val owner = playlist?.owner
    val ownerName = owner?.displayName ?: owner?.username ?: stringResource(Res.string.playlist_owner_unknown)
    val ownerImageURL = owner?.thumbnails?.firstOrNull()?.url
    val isOwner = currentUserId != null && playlist?.owner?.id == currentUserId
    val artworkUrl = playlist?.thumbnails?.firstOrNull()?.url.orEmpty()
    val isPlaying = currentCollectionEntry?.id == playlistId &&
        playerState == PlayerState.PLAYING

    val errorMessage = (state as? PlaylistScreenState.Error)?.message
    val isLoading = state is PlaylistScreenState.Loading && (dataState == null || dataState.tracks.isEmpty())

    val footerContent: (@Composable () -> Unit)? = if (isOwner && dataState != null) {
        {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center) {
                MillenniumButton(
                    onClick = { showAddTracksDialog = true },
                    style = MillenniumButtonStyle.Secondary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxAddSquare,
                            contentDescription = null)
                        Text(stringResource(Res.string.playlist_add_tracks_button))
                    }
                }
            }
        }
    } else null

    CollectionView(
        isPlayLoading = playbackUiState.isPlayLoading,
        isShuffleLoading = playbackUiState.isShuffleLoading,
        isAddToQueueLoading = playbackUiState.isAddToQueueLoading,
        title = playlist?.title ?: stringResource(Res.string.playlist_loading_title),
        description = playlist?.description.orEmpty(),
        imageURL = artworkUrl,
        ownerName = ownerName,
        ownerImageURL = ownerImageURL,
        onOwnerClick = {},
        onPlay = viewModel::playPlaylist,
        onShufflePlay = viewModel::shufflePlayPlaylist,
        onAddToQueue = viewModel::addPlaylistToQueue,
        isPlaying = isPlaying,
        isFollowing = savedPlaylistIds.contains(playlistId),
        isFollowLoading = dataState?.isSaving == true,
        onFollowClick = viewModel::toggleSavedPlaylist,
        showFollowButton = playlistId != "saved_tracks",
        onEdit = if (isOwner) viewModel::showEditPlaylist else null,
        isLoading = isLoading,
        error = errorMessage,
        onRetry = { viewModel.refresh() },
        tracks = dataState?.tracks ?: emptyList(),
        hasMore = dataState?.nextPagination != null,
        isLoadingNextPage = state is PlaylistScreenState.Data.LoadingMore,
        currentTrackId = trackOptionsContext.currentTrackId,
        isCurrentTrackPlaying = playerState == PlayerState.PLAYING,
        onTrackClick = viewModel::playPlaylistFromTrack,
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
        footerContent = footerContent,
        trailingContent = {
            val loadedPlaylist = (dataState as? PlaylistScreenState.Data.Loaded)?.playlist
            PlaylistFormSheet(
                visible = editPlaylistFormState.isVisible,
                formState = editPlaylistFormState,
                onDismiss = viewModel::dismissEditPlaylist,
                onImagePicked = viewModel::onEditPlaylistImagePicked,
                isEditing = true,
                initialData = loadedPlaylist?.let {
                    PlaylistFormData(
                        name = it.title,
                        description = it.description.orEmpty(),
                        isPublic = true,
                        isCollaborating = false,
                        imagePreviewUrl = it.thumbnails.firstOrNull()?.url)
                },
                onSubmit = viewModel::updatePlaylist)

            AddToPlaylistPicker(
                visible = showAddToPlaylistPicker,
                currentUserId = currentUserId,
                onDismiss = viewModel::dismissAddToPlaylistPicker,
                onPlaylistSelected = viewModel::addTracksToPlaylist)

            AddTracksToPlaylistDialog(
                visible = showAddTracksDialog,
                playlistId = playlistId,
                onDismiss = {
                    showAddTracksDialog = false
                    viewModel.refresh()
                })
        })
}
