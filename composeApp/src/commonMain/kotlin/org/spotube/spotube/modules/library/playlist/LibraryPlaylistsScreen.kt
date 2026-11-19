/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.LikedTracksCard
import org.spotube.spotube.core.ui.component.LocalPlaylistCard
import org.spotube.spotube.core.ui.component.PlaylistCard
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.LibraryMediaCollection
import org.spotube.spotube.modules.library.LibraryOnlineHint
import org.spotube.spotube.modules.library.LibrarySourceFilter
import org.spotube.spotube.modules.library.LibraryViewMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.*

@Composable
fun LibraryPlaylistsScreen(
    modifier: Modifier = Modifier,
    showCreatePlaylist: Boolean = false,
    viewMode: LibraryViewMode = LibraryViewMode.Grid,
    viewModel: LibraryPlaylistsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistFormState by viewModel.playlistFormState.collectAsStateWithLifecycle()
    val data = state as? LibraryPlaylistsState.Data
    Column(modifier) {
        when (val current = state) {
            is LibraryPlaylistsState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ErrorDisplay(errorMessage = current.message, onRetry = viewModel::refresh)
            }

            else -> {
                Column(Modifier.weight(1f)) {
                    // Local playlists still work without an account; only the online half needs it.
                    if (data?.onlineAvailable == false) {
                        LibraryOnlineHint(
                            stringResource(Res.string.library_online_hint_playlists),
                            Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (data != null && data.items.isEmpty() &&
                        current !is LibraryPlaylistsState.Data.LoadingMore
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (data.query.isNotBlank()) stringResource(
                                    Res.string.library_playlists_no_results,
                                    data.query
                                )
                                else stringResource(
                                    if (data.sourceFilter == LibrarySourceFilter.Local)
                                        Res.string.library_playlists_empty_local
                                    else Res.string.library_playlists_empty
                                )
                            )
                        }
                    } else {
                        LibraryMediaCollection<LibraryPlaylistEntry>(
                            items = data?.items.orEmpty(), itemKey = { it.id }, viewMode = viewMode,
                            loading = current is LibraryPlaylistsState.Loading,
                            loadingMore = current is LibraryPlaylistsState.Data.LoadingMore,
                            onViewportChanged = viewModel::onItemsVisible,
                            header = { list ->
                                LikedTracksCard(
                                    Modifier.fillMaxWidth(),
                                    listMode = list
                                )
                            },
                            itemContent = { item, list ->
                                when (item) {
                                    is LibraryPlaylistEntry.Online -> PlaylistCard(
                                        item.playlist,
                                        Modifier.fillMaxWidth(),
                                        listMode = list
                                    )

                                    is LibraryPlaylistEntry.Local -> LocalPlaylistCard(
                                        item.playlist,
                                        Modifier.fillMaxWidth(),
                                        listMode = list
                                    )
                                }
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
            onImagePicked = viewModel::onPlaylistFormImagePicked,
            showSourceChoice = true,
            defaultSource = if (data?.onlineAvailable == false) PlaylistFormSource.Local
            else PlaylistFormSource.Online
        )
    }
}
