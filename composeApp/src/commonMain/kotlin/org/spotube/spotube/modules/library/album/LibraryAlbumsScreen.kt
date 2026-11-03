/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.spotube.core.ui.component.AlbumCard
import org.spotube.spotube.core.ui.component.ErrorDisplay
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.LibraryMediaCollection
import org.spotube.spotube.modules.library.LibraryViewMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.*

@Composable
fun LibraryAlbumsScreen(
    modifier: Modifier = Modifier,
    viewMode: LibraryViewMode = LibraryViewMode.Grid,
    viewModel: LibraryAlbumsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        is LibraryAlbumsState.Error -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ErrorDisplay(errorMessage = current.message, onRetry = viewModel::refresh)
        }

        else -> {
            val data = current as? LibraryAlbumsState.Data
            if (data != null && data.items.isEmpty() && current !is LibraryAlbumsState.Data.LoadingMore) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (data.query.isNotBlank()) stringResource(
                            Res.string.library_albums_no_results,
                            data.query
                        )
                        else stringResource(Res.string.library_albums_empty)
                    )
                }
            } else LibraryMediaCollection<MetadataAlbum.Detailed>(
                items = data?.items.orEmpty(), itemKey = { it.id }, viewMode = viewMode,
                loading = current is LibraryAlbumsState.Loading,
                loadingMore = current is LibraryAlbumsState.Data.LoadingMore,
                onViewportChanged = viewModel::onItemsVisible,
                itemContent = { item, list ->
                    AlbumCard(
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
                },
                modifier = modifier
            )
        }
    }
}
