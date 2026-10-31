/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.spotube.modules.library.playlist.LocalPlaylistViewModel
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.LikedTracksCard
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.LibraryMediaCollection
import org.spotube.spotube.modules.library.LibraryViewMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.*

import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSpokeSpinner
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.modules.library.playlist.LocalPlaylistRepository
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.Typography
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_local_playlists_section

@Composable
fun LibraryPlaylistsScreen(
    modifier: Modifier = Modifier,
    showCreatePlaylist: Boolean = false,
    viewMode: LibraryViewMode = LibraryViewMode.Grid,
    viewModel: LibraryPlaylistsViewModel = koinViewModel(),
    localPlaylistViewModel: LocalPlaylistViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val localState by localPlaylistViewModel.uiState.collectAsStateWithLifecycle()
    val playlistFormState by viewModel.playlistFormState.collectAsStateWithLifecycle()
    Column(modifier) {
        when (val current = state) {
            is LibraryPlaylistsState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ErrorDisplay(errorMessage = current.message, onRetry = viewModel::refresh)
            }

            else -> {
                val data = current as? LibraryPlaylistsState.Data
                val localData = localState as? LocalPlaylistScreenState.Data
                val spotifyPlaylists = data?.items.orEmpty()
                val localPlaylists = localData?.playlists.orEmpty()
                val allPlaylists = spotifyPlaylists + localPlaylists
                val hasPlaylists = allPlaylists.isNotEmpty()
                val isLoading = current is LibraryPlaylistsState.Loading || localState is LocalPlaylistScreenState.Loading
                val syncState = localData?.syncState ?: LocalPlaylistRepository.SyncState.Idle
                
                if (!hasPlaylists && !isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (data?.query.isNotBlank() == true) stringResource(
                                Res.string.library_playlists_no_results,
                                data.query
                            )
                            else stringResource(Res.string.library_playlists_empty)
                        )
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        // Spotify Playlists Section
                        if (spotifyPlaylists.isNotEmpty() || data?.query.isBlank() == true) {
                            LibraryMediaCollection<MetadataPlaylist>(
                                items = spotifyPlaylists, itemKey = { it.id }, viewMode = viewMode,
                                loading = current is LibraryPlaylistsState.Loading,
                                loadingMore = current is LibraryPlaylistsState.Data.LoadingMore,
                                onViewportChanged = viewModel::onItemsVisible,
                                header = { list ->
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                stringResource(Res.string.library_playlists_spotify_section),
                                                style = com.composeunstyled.Typography.h6
                                            )
                                            if (listMode == LibraryViewMode.Grid) {
                                                // Show create playlist button in header for Spotify playlists
                                            }
                                        }
                                    }
                                },
                                itemContent = { item, list ->
                                    PlaylistCard(
                                        item,
                                        Modifier.fillMaxWidth(),
                                        listMode = list
                                    )
                                },
                                skeleton = { list ->
                                    PlayableCard(
                                        stringResource(Res.string.media_preview_title),
                                        stringResource(Res.string.media_preview_subtitle),
                                        modifier = Modifier.fillMaxWidth(),
                                        listMode = list
                                    )
                                }
                            )
                        }
                    
                    // Local Playlists Section
                    if (localPlaylists.isNotEmpty()) {
                        LibraryMediaCollection<LocalPlaylist>(
                            items = localPlaylists, itemKey = { it.id }, viewMode = viewMode,
                            loading = localState is LocalPlaylistScreenState.Loading,
                            loadingMore = false,
                            onViewportChanged = { },
                            header = { list ->
                                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            stringResource(Res.string.library_local_playlists_section),
                                            style = com.composeunstyled.Typography.h6
                                        )
                                        // Sync button and status
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Sync status indicator
                                                when (syncState) {
                                                    is LocalPlaylistRepository.SyncState.Syncing -> {
                                                        MillenniumSpokeSpinner(size = 16.dp)
                                                        Text(
                                                            stringResource(Res.string.library_playlist_syncing),
                                                            style = Typography.bodySmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    is LocalPlaylistRepository.SyncState.Success -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            MillenniumIcon(
                                                                Iconsax.IconsaxTickSquare,
                                                                contentDescription = null,
                                                                tint = Color.Green,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                stringResource(Res.string.library_playlist_synced),
                                                                style = Typography.bodySmall,
                                                                color = Color.Green
                                                            )
                                                        }
                                                    }
                                                    is LocalPlaylistRepository.SyncState.Error -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            MillenniumIcon(
                                                                Iconsax.IconsaxWarning,
                                                                contentDescription = null,
                                                                tint = Color.Red,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                stringResource(Res.string.library_playlist_sync_error),
                                                                style = Typography.bodySmall,
                                                                color = Color.Red
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        // Idle - show sync button
                                                        MillenniumIconButton(
                                                            onClick = { localPlaylistViewModel.syncNow() },
                                                            enabled = true,
                                                            style = MillenniumIconButtonStyle.Quiet
                                                        ) {
                                                            MillenniumIcon(
                                                                Iconsax.IconsaxRefreshRight,
                                                                contentDescription = stringResource(Res.string.library_playlist_sync_now),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                // Create playlist button
                                                MillenniumIconButton(
                                                    icon = Iconsax.IconsaxAddSquare,
                                                    contentDescription = stringResource(Res.string.library_action_create_playlist),
                                                    onClick = { localPlaylistViewModel.showCreatePlaylistDialog() },
                                                    style = MillenniumIconButtonStyle.Quiet
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            itemContent = { item, list ->
                                LocalPlaylistCard(
                                    item,
                                    Modifier.fillMaxWidth(),
                                    listMode = list,
                                    onClick = { },
                                    onMoreClick = { localPlaylistViewModel.showEditPlaylistDialog(item) },
                                    onPlayClick = { }
                                )
                            },
                            skeleton = { list ->
                                PlayableCard(
                                    stringResource(Res.string.media_preview_title),
                                    stringResource(Res.string.media_preview_subtitle),
                                    modifier = Modifier.fillMaxWidth(),
                                    listMode = list
                                )
                            }
                        )
                    }
                }
            }
        }
        PlaylistFormSheet(
            visible = showCreatePlaylist,
            formState = playlistFormState,
            onDismiss = viewModel::dismissCreatePlaylist,
            onSubmit = viewModel::createPlaylist,
            onImagePicked = viewModel::onPlaylistFormImagePicked
        )
    }
}