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
                                        MillenniumIconButton(
                                            icon = Iconsax.IconsaxAddSquare,
                                            contentDescription = stringResource(Res.string.library_action_create_playlist),
                                            onClick = { localPlaylistViewModel.showCreatePlaylistDialog() },
                                            style = MillenniumIconButtonStyle.Quiet
                                        )
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