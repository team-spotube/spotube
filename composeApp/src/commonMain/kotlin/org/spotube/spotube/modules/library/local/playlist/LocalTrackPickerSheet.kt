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

package org.spotube.spotube.modules.library.local.playlist

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCheckbox
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.component.cards.MediaArtwork
import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_cancel
import spotube.composeapp.generated.resources.local_playlist_add_tracks_empty
import spotube.composeapp.generated.resources.local_playlist_add_tracks_search_placeholder
import spotube.composeapp.generated.resources.local_playlist_add_tracks_selected_count
import spotube.composeapp.generated.resources.local_playlist_add_tracks_title

/**
 * Multi-select sheet that appends indexed local tracks to a playlist. The filtered list, the
 * query and the selection all live in [LocalPlaylistViewModel]; this composable only renders
 * [LocalTrackPickerUiState] and forwards events.
 */
@Composable
fun LocalTrackPickerSheet(
    state: LocalTrackPickerUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.visible) return

    // The sheet leaves composition while hidden, so the field restarts from the reset query.
    val searchState = rememberTextFieldState(state.query)
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .drop(1)
            .collect { onQueryChange(it) }
    }

    val pickerTitle = stringResource(Res.string.local_playlist_add_tracks_title)
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        dialogTitle = pickerTitle,
        title = { Text(pickerTitle, style = AppTheme.typography.headlineSmall) },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                MillenniumButton(
                    onClick = onDismiss,
                    enabled = !state.isSubmitting,
                    style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(
                    onClick = onSubmit,
                    enabled = state.selectedPaths.isNotEmpty() && !state.isSubmitting,
                    loading = state.isSubmitting) {
                    Text(
                        stringResource(
                            Res.string.local_playlist_add_tracks_selected_count,
                            state.selectedPaths.size
                        )
                    )
                }
            }
        }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIcon(imageVector = Iconsax.IconsaxFilterSearch, contentDescription = null)
                MillenniumTextField(
                    state = searchState,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.local_playlist_add_tracks_search_placeholder))
            }

            // Bounded height like AddToPlaylistPicker: the sheet's own scroll must not meet an
            // unbounded LazyColumn.
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (state.allTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.local_playlist_add_tracks_empty))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsWithUniqueKeys(
                            state.allTracks, id = { it.path }, keyPrefix = "picker:") { track ->
                            LocalTrackPickerRow(
                                track = track,
                                checked = track.path in state.selectedPaths,
                                onToggle = { onToggle(track.path) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTrackPickerRow(
    track: LocalTrackPickerTrackUi,
    checked: Boolean,
    onToggle: () -> Unit
) {
    // The whole row toggles; the checkbox mirrors the selection and consumes its own clicks.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MediaArtwork(
            model = track.artwork,
            title = track.title,
            modifier = Modifier.size(36.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            track.subtitle?.let {
                Text(
                    text = it,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        MillenniumCheckbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
