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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import org.spotube.spotube.core.ui.component.LocalAnimatedVisibilityScope
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.component.cards.MediaArtwork
import org.spotube.spotube.core.ui.component.cards.PlayableListTile
import org.spotube.spotube.core.ui.component.sharedElementOrNone
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.library.local.decodeLocalCoverBase64
import org.spotube.spotube.modules.library.playlist.PlaylistFormData
import org.spotube.spotube.modules.library.playlist.PlaylistFormSheet
import org.spotube.spotube.modules.library.playlist.PlaylistFormSource
import org.spotube.spotube.modules.library.playlist.PlaylistFormUiState
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import spotube.composeapp.generated.resources.*

/**
 * A single user-created playlist of on-device tracks
 * ([org.spotube.spotube.core.navigation.Routes.LocalPlaylist]): header with cover/name/play
 * controls, the track rows, and the edit/add-tracks/delete overlays.
 */
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    viewModel: LocalPlaylistViewModel,
    navigationCommands: NavigationCommands
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = state as? LocalPlaylistUiState.Loaded

    Column(Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = true,
            title = {
                Text(
                    loaded?.playlist?.name ?: stringResource(Res.string.playlist_loading_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                if (loaded != null) {
                    MillenniumIconButton(
                        onClick = viewModel::showEdit,
                        style = MillenniumIconButtonStyle.Quiet) {
                        AppIcon(
                            imageVector = Iconsax.IconsaxEdit,
                            contentDescription = stringResource(Res.string.collection_details_edit_playlist)
                        )
                    }
                    MillenniumIconButton(
                        onClick = viewModel::confirmDelete,
                        style = MillenniumIconButtonStyle.Quiet) {
                        AppIcon(
                            imageVector = Iconsax.IconsaxTrash,
                            contentDescription = stringResource(Res.string.local_playlist_delete)
                        )
                    }
                }
            }
        )

        when (val current = state) {
            is LocalPlaylistUiState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MillenniumCircularProgress(size = 40.dp)
            }

            is LocalPlaylistUiState.NotFound -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.local_playlist_not_found))
            }

            is LocalPlaylistUiState.Loaded -> LocalPlaylistContent(
                state = current,
                playlistId = playlistId,
                onPlay = viewModel::play,
                onShuffle = viewModel::shufflePlay,
                onAddToQueue = viewModel::addToQueue,
                onPlayTrack = viewModel::playFrom,
                onRemoveTrack = viewModel::removeTrack,
                onAddTracks = viewModel::showAddTracks
            )
        }

        PlaylistFormSheet(
            visible = loaded?.showEditForm == true,
            formState = loaded?.editFormState ?: PlaylistFormUiState(),
            onDismiss = viewModel::dismissEdit,
            onSubmit = viewModel::updatePlaylist,
            onImagePicked = viewModel::onEditImagePicked,
            isEditing = true,
            initialData = loaded?.playlist?.let { playlist ->
                PlaylistFormData(
                    name = playlist.name,
                    description = playlist.description.orEmpty(),
                    imageBase64 = playlist.coverBase64.orEmpty(),
                    source = PlaylistFormSource.Local)
            }
        )

        LocalTrackPickerSheet(
            state = loaded?.addTracksState ?: LocalTrackPickerUiState(),
            onQueryChange = viewModel::onAddTracksQueryChange,
            onToggle = viewModel::toggleAddTrack,
            onSubmit = viewModel::addSelectedTracks,
            onDismiss = viewModel::dismissAddTracks
        )

        if (loaded?.showDeleteConfirm == true) {
            MillenniumAlertDialog(
                visible = true,
                onDismissRequest = viewModel::cancelDelete,
                title = { Text(stringResource(Res.string.local_playlist_delete_title)) },
                description = {
                    Text(
                        stringResource(
                            Res.string.local_playlist_delete_message,
                            loaded.playlist.name
                        )
                    )
                }) {
                MillenniumButton(
                    onClick = viewModel::cancelDelete,
                    style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.common_cancel))
                }
                MillenniumButton(
                    onClick = viewModel::deletePlaylist,
                    style = MillenniumButtonStyle.Danger) {
                    Text(stringResource(Res.string.local_playlist_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LocalPlaylistContent(
    state: LocalPlaylistUiState.Loaded,
    playlistId: String,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayTrack: (String) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onAddTracks: () -> Unit
) {
    val playlist = state.playlist
    val inset = LocalAppShellBottomInset.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = inset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "local_playlist_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top) {
                MediaArtwork(
                    model = decodeLocalCoverBase64(playlist.coverBase64)
                        ?: state.tracks.firstOrNull { it.artwork != null }?.artwork,
                    title = playlist.name,
                    modifier = Modifier
                        .size(200.dp)
                        .sharedElementOrNone(
                            key = "local_playlist_art_$playlistId",
                            sharedTransitionScope = LocalSharedTransitionScope.current,
                            animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                        ),
                    shape = RoundedCornerShape(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = AppTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    playlist.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            Res.plurals.library_local_tracks_count,
                            state.tracks.size,
                            state.tracks.size
                        ),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MillenniumButton(
                            onClick = onPlay,
                            modifier = Modifier.weight(1f)) {
                            AppIcon(
                                imageVector = if (state.isPlaying) Iconsax.IconsaxPauseCircle
                                else Iconsax.IconsaxPlayCircle2,
                                contentDescription = stringResource(
                                    if (state.isPlaying) Res.string.player_pause_content_description
                                    else Res.string.player_play_content_description
                                )
                            )
                            TextWithShimmer(
                                text = stringResource(
                                    if (state.isPlaying) Res.string.player_pause_content_description
                                    else Res.string.player_play_content_description
                                )
                            )
                        }
                        MillenniumButton(
                            onClick = onShuffle,
                            modifier = Modifier.weight(1f),
                            style = MillenniumButtonStyle.Secondary) {
                            AppIcon(
                                imageVector = Iconsax.IconsaxShuffle,
                                contentDescription = stringResource(Res.string.collection_details_shuffle)
                            )
                            TextWithShimmer(text = stringResource(Res.string.collection_details_shuffle))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MillenniumIconButton(
                            onClick = onAddToQueue,
                            style = MillenniumIconButtonStyle.Quiet) {
                            AppIcon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = stringResource(
                                    Res.string.player_add_to_queue_content_description
                                )
                            )
                        }
                        MillenniumButton(
                            onClick = onAddTracks,
                            style = MillenniumButtonStyle.Secondary) {
                            AppIcon(
                                imageVector = Iconsax.IconsaxAddSquare,
                                contentDescription = stringResource(Res.string.playlist_add_tracks_button)
                            )
                            TextWithShimmer(text = stringResource(Res.string.playlist_add_tracks_button))
                        }
                    }
                }
            }
        }

        if (state.tracks.isEmpty()) {
            item(key = "local_playlist_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(Res.string.local_playlist_empty))
                        Text(
                            stringResource(Res.string.local_playlist_empty_hint),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsWithUniqueKeys(
                state.tracks, id = { it.path }, keyPrefix = "local_playlist_track:") { track ->
                PlayableListTile(
                    title = track.title,
                    subtitle = track.subtitle,
                    imageModel = track.artwork,
                    onClick = { onPlayTrack(track.path) },
                    onPlay = { onPlayTrack(track.path) },
                    compact = true,
                    trailingContent = {
                        track.durationLabel?.let {
                            Text(
                                it,
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                        MillenniumIconButton(
                            onClick = { onRemoveTrack(track.path) },
                            modifier = Modifier.size(32.dp),
                            style = MillenniumIconButtonStyle.Quiet) {
                            AppIcon(
                                imageVector = Iconsax.IconsaxTrash,
                                contentDescription = stringResource(Res.string.local_playlist_remove_track),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    })
            }
        }
    }
}
