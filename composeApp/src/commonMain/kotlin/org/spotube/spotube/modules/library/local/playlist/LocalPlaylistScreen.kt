/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumAlertDialog
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.library.local.LocalCollectionScreen
import org.spotube.spotube.modules.library.local.decodeLocalCoverBase64
import org.spotube.spotube.modules.library.playlist.PlaylistFormData
import org.spotube.spotube.modules.library.playlist.PlaylistFormSheet
import org.spotube.spotube.modules.library.playlist.PlaylistFormSource
import org.spotube.spotube.modules.library.playlist.PlaylistFormUiState
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import spotube.composeapp.generated.resources.*

@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    viewModel: LocalPlaylistViewModel,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        LocalPlaylistUiState.Loading -> Column {
            ApplicationMainBar(backButton = true)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MillenniumCircularProgress(size = 40.dp) }
        }
        LocalPlaylistUiState.NotFound -> Column {
            ApplicationMainBar(backButton = true)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.local_playlist_not_found))
            }
        }
        is LocalPlaylistUiState.Loaded -> {
            val playlist = current.playlist
            LocalCollectionScreen(
                title = playlist.name,
                description = playlist.description?.takeIf { it.isNotBlank() }
                    ?: pluralStringResource(Res.plurals.library_local_tracks_count, current.tracks.size, current.tracks.size),
                artwork = decodeLocalCoverBase64(playlist.coverBase64)
                    ?: current.tracks.firstOrNull { it.artwork != null }?.artwork,
                placeholderIcon = Iconsax.IconsaxMusic,
                tracks = current.tracks,
                isPlaying = current.isPlaying,
                onPlay = viewModel::play,
                onShuffle = viewModel::shufflePlay,
                onAddToQueue = viewModel::addToQueue,
                onTrackClick = viewModel::playFrom,
                onRemoveTrack = viewModel::removeTrack,
                onEdit = viewModel::showEdit,
                sharedElementKey = "local_playlist_art_${playlist.id}",
                extraActions = {
                    MillenniumIconButton(onClick = viewModel::confirmDelete, style = MillenniumIconButtonStyle.Quiet) {
                        AppIcon(Iconsax.IconsaxTrash, stringResource(Res.string.local_playlist_delete))
                    }
                },
                footer = {
                    MillenniumButton(onClick = viewModel::showAddTracks, style = MillenniumButtonStyle.Secondary) {
                        AppIcon(Iconsax.IconsaxAddSquare, contentDescription = null)
                        Text(stringResource(Res.string.playlist_add_tracks_button))
                    }
                },
                emptyContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.local_playlist_empty), style = AppTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.local_playlist_empty_hint),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                }
            )
            PlaylistFormSheet(
                visible = current.showEditForm,
                formState = current.editFormState,
                onDismiss = viewModel::dismissEdit,
                onSubmit = viewModel::updatePlaylist,
                onImagePicked = viewModel::onEditImagePicked,
                isEditing = true,
                initialData = PlaylistFormData(
                    name = playlist.name,
                    description = playlist.description.orEmpty(),
                    imageBase64 = playlist.coverBase64.orEmpty(),
                    source = PlaylistFormSource.Local
                )
            )
            LocalTrackPickerSheet(
                state = current.addTracksState,
                onQueryChange = viewModel::onAddTracksQueryChange,
                onToggle = viewModel::toggleAddTrack,
                onSubmit = viewModel::addSelectedTracks,
                onDismiss = viewModel::dismissAddTracks
            )
            if (current.showDeleteConfirm) MillenniumAlertDialog(
                visible = true,
                onDismissRequest = viewModel::cancelDelete,
                title = { Text(stringResource(Res.string.local_playlist_delete_title)) },
                description = { Text(stringResource(Res.string.local_playlist_delete_message, playlist.name)) }
            ) {
                MillenniumButton(onClick = viewModel::cancelDelete, style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(onClick = viewModel::deletePlaylist, style = MillenniumButtonStyle.Danger) {
                    Text(stringResource(Res.string.local_playlist_delete))
                }
            }
        }
    }
}
