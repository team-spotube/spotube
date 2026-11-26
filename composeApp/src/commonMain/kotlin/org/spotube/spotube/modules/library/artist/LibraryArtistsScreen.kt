/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.artist

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
import org.spotube.spotube.core.ui.component.ArtistCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.LocalArtistCard
import org.spotube.spotube.core.ui.component.cards.AvatarCard
import org.spotube.spotube.modules.library.LibraryMediaCollection
import org.spotube.spotube.modules.library.LibraryOnlineHint
import org.spotube.spotube.modules.library.LibrarySourceFilter
import org.spotube.spotube.modules.library.LibraryViewMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.*

@Composable
fun LibraryArtistsScreen(
    modifier: Modifier = Modifier,
    viewMode: LibraryViewMode = LibraryViewMode.Grid,
    viewModel: LibraryArtistsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        is LibraryArtistsState.Error -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ErrorDisplay(errorMessage = current.message, onRetry = viewModel::refresh)
        }

        else -> {
            val data = current as? LibraryArtistsState.Data
            Column(modifier.fillMaxSize()) {
                if (data != null && !data.onlineAvailable) {
                    LibraryOnlineHint(
                        stringResource(Res.string.library_online_hint_artists),
                        Modifier.padding(bottom = 8.dp)
                    )
                }
                if (data != null && data.items.isEmpty() && current !is LibraryArtistsState.Data.LoadingMore) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (data.query.isNotBlank()) stringResource(
                                Res.string.library_artists_no_results,
                                data.query
                            )
                            else stringResource(
                                if (data.sourceFilter == LibrarySourceFilter.Local)
                                    Res.string.library_artists_empty_local
                                else Res.string.library_artists_empty
                            )
                        )
                    }
                } else LibraryMediaCollection<LibraryArtistEntry>(
                    items = data?.items.orEmpty(), itemKey = { it.id }, viewMode = viewMode,
                    loading = current is LibraryArtistsState.Loading,
                    loadingMore = current is LibraryArtistsState.Data.LoadingMore,
                    onViewportChanged = viewModel::onItemsVisible,
                    itemContent = { item, list ->
                        when (item) {
                            is LibraryArtistEntry.Online -> ArtistCard(
                                item.artist,
                                Modifier.fillMaxWidth(),
                                listMode = list
                            )

                            is LibraryArtistEntry.Local -> LocalArtistCard(
                                item.artist,
                                Modifier.fillMaxWidth(),
                                listMode = list
                            )
                        }
                    },
                    skeleton = { list ->
                        AvatarCard(
                            stringResource(Res.string.media_preview_subtitle),
                            stringResource(Res.string.media_artist),
                            modifier = Modifier.fillMaxWidth(),
                            listMode = list
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
