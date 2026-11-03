/*
 * Copyright (C) 2026 Kingmor Roy Tirtho and Spotube Contributors
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


import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumModalBottomSheet
import org.spotube.millennium.composables.MillenniumTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.SheetDetent
import com.composeunstyled.Text
import com.composeunstyled.rememberModalBottomSheetState
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.playlist_add_tracks_add
import spotube.composeapp.generated.resources.playlist_add_tracks_added_content_description
import spotube.composeapp.generated.resources.playlist_add_tracks_done
import spotube.composeapp.generated.resources.playlist_add_tracks_hint
import spotube.composeapp.generated.resources.playlist_add_tracks_no_results
import spotube.composeapp.generated.resources.playlist_add_tracks_search_content_description
import spotube.composeapp.generated.resources.playlist_add_tracks_search_placeholder
import spotube.composeapp.generated.resources.playlist_add_tracks_title

@Composable
fun AddTracksToPlaylistDialog(
    visible: Boolean,
    playlistId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier) {
    if (!visible) return

    val viewModel: AddTracksToPlaylistViewModel = koinViewModel(
        key = "add-tracks-$playlistId",
        parameters = { parametersOf(playlistId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dismiss = {
        viewModel.reset()
        onDismiss()
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MillenniumTextField(
                    state = state.queryState,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.playlist_add_tracks_search_placeholder))
                MillenniumIconButton(
                    icon = Iconsax.IconsaxSearch,
                    contentDescription = stringResource(Res.string.playlist_add_tracks_search_content_description),
                    onClick = viewModel::search,
                    loading = state.isSearching,
                    style = MillenniumIconButtonStyle.Secondary)
            }

            when {
                state.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center) {
                        MillenniumCircularProgress(size = 40.dp)
                    }
                }
                state.hasSearched && state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.playlist_add_tracks_no_results))
                    }
                }
                state.results.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsWithUniqueKeys(state.results, id = { it.track.id }) { result ->
                            TrackSearchResultRow(
                                track = result.track,
                                isAdding = result.isAdding,
                                isAdded = result.isAdded,
                                onAdd = { viewModel.addTrack(result.track.id) })
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.playlist_add_tracks_hint))
                    }
                }
            }
        }
    }

    val dialogTitle = stringResource(Res.string.playlist_add_tracks_title)
    AdaptiveDialogBottomSheet(
        onDismiss = dismiss,
        modifier = modifier,
        dialogTitle = dialogTitle,
        title = { Text(dialogTitle, style = AppTheme.typography.headlineSmall) },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {
                MillenniumButton(onClick = dismiss) {
                    Text(stringResource(Res.string.playlist_add_tracks_done))
                }
            }
        }) {
        Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
            content()
        }
    }
}

@Composable
private fun TrackSearchResultRow(
    track: MetadataTrack,
    isAdding: Boolean,
    isAdded: Boolean,
    onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val imageUrl = track.album?.thumbnails?.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop)
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center) {
                Text(
                    text = track.title.take(1).uppercase(),
                    style = AppTheme.typography.titleMedium)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }

        when {
            isAdded -> {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxCheckCircle,
                    contentDescription = stringResource(Res.string.playlist_add_tracks_added_content_description),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(24.dp))
            }
            else -> {
                MillenniumButton(
                    onClick = onAdd,
                    loading = isAdding,
                    style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.playlist_add_tracks_add))
                }
            }
        }
    }
}
