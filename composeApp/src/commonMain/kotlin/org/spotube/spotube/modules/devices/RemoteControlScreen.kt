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

package org.spotube.spotube.modules.devices


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.composables.MillenniumSpokeSpinner
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import com.composeunstyled.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.shell.player_queue.QueueSheet
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxMirroringScreen
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemove
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxVolumeCross
import org.spotube.spotube.resources.iconsax.IconsaxVolumeHigh
import org.spotube.spotube.resources.iconsax.IconsaxVolumeLow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.devices_connecting
import spotube.composeapp.generated.resources.devices_disconnect
import spotube.composeapp.generated.resources.devices_remote_back_to_devices
import spotube.composeapp.generated.resources.devices_remote_connecting_hint
import spotube.composeapp.generated.resources.devices_remote_connection_error
import spotube.composeapp.generated.resources.devices_remote_disconnected
import spotube.composeapp.generated.resources.devices_remote_disconnected_hint
import spotube.composeapp.generated.resources.devices_remote_more_options
import spotube.composeapp.generated.resources.devices_remote_nothing_playing
import spotube.composeapp.generated.resources.devices_remote_nothing_playing_hint
import spotube.composeapp.generated.resources.devices_remote_playing_on
import spotube.composeapp.generated.resources.devices_remote_remove_from_queue
import spotube.composeapp.generated.resources.devices_remote_title
import spotube.composeapp.generated.resources.devices_remote_unknown_artist
import spotube.composeapp.generated.resources.devices_remote_unknown_track
import spotube.composeapp.generated.resources.devices_remote_volume
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.queue_no_queue_entries
import spotube.composeapp.generated.resources.queue_title

// Below this content width the queue moves into a sheet; above it, it sits beside the player.
private val WideLayoutMinWidth = 760.dp
private val QueuePanelWidth = 380.dp
private val NowPlayingMaxWidth = 520.dp

// Rough height of everything under the artwork (track info, seek bar, controls, volume).
// The artwork gets what's left, so the whole player fits without scrolling.
private val ControlsReservedHeight = 330.dp
private val MinArtworkSize = 140.dp
private val MaxArtworkSize = 460.dp

@Composable
fun RemoteControlScreen(
    onDisconnect: () -> Unit,
    viewModel: RemoteControlViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current
    val isConnected = state.connection is ConnectionState.Connected

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MillenniumTheme.colors.background)
    ) {
        val isWide = maxWidth >= WideLayoutMinWidth

        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationMainBar(
                title = { Text(stringResource(Res.string.devices_remote_title)) },
                subtitle = {
                    if (isConnected && state.deviceName.isNotBlank()) {
                        Text(stringResource(Res.string.devices_remote_playing_on, state.deviceName))
                    }
                },
                backButton = true,
                actions = {
                    if (isConnected && !isWide) {
                        MillenniumIconButton(
                            onClick = viewModel::toggleQueueVisibility,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxMusicFilter,
                                contentDescription = stringResource(Res.string.queue_title),
                                tint = if (state.isQueueVisible) {
                                    AppTheme.colors.primary
                                } else {
                                    AppTheme.colors.onSurfaceVariant
                                }
                            )
                        }
                    }
                    if (isConnected) {
                        MillenniumIconButton(
                            onClick = {
                                viewModel.disconnect()
                                onDisconnect()
                            },
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxCloseSquare,
                                contentDescription = stringResource(Res.string.devices_disconnect)
                            )
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = shellBottomInset)
            ) {
                when (val connection = state.connection) {
                    is ConnectionState.Connected -> {
                        if (isWide) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                NowPlayingPane(
                                    state = state,
                                    viewModel = viewModel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                MillenniumCard(
                                    modifier = Modifier
                                        .width(QueuePanelWidth)
                                        .fillMaxHeight(),
                                    maxWidth = QueuePanelWidth,
                                    contentPadding = PaddingValues(12.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    RemoteQueueList(
                                        state = state,
                                        onPlayQueueItem = viewModel::playQueueItem,
                                        onRemoveQueueItem = viewModel::removeQueueItem
                                    )
                                }
                            }
                        } else {
                            NowPlayingPane(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    is ConnectionState.Connecting -> ConnectionStatus(
                        title = stringResource(Res.string.devices_connecting),
                        message = stringResource(Res.string.devices_remote_connecting_hint),
                        leading = { MillenniumSpokeSpinner(size = 40.dp) }
                    )

                    is ConnectionState.Disconnected -> ConnectionStatus(
                        title = stringResource(Res.string.devices_remote_disconnected),
                        message = stringResource(Res.string.devices_remote_disconnected_hint),
                        leading = { StatusIcon(Iconsax.IconsaxMirroringScreen, isError = false) },
                        actionLabel = stringResource(Res.string.devices_remote_back_to_devices),
                        onAction = onDisconnect
                    )

                    is ConnectionState.Error -> ConnectionStatus(
                        title = stringResource(Res.string.devices_remote_disconnected),
                        message = stringResource(
                            Res.string.devices_remote_connection_error,
                            connection.message ?: stringResource(connection.fallback)
                        ),
                        leading = { StatusIcon(Iconsax.IconsaxMirroringScreen, isError = true) },
                        actionLabel = stringResource(Res.string.devices_remote_back_to_devices),
                        onAction = onDisconnect
                    )
                }
            }
        }

        if (isConnected && !isWide) {
            // Click-outside scrim for the sliding drawer variant of the queue sheet.
            // (The bottom sheet variant has its own built-in scrim.)
            if (state.isQueueVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = viewModel::dismissQueue
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = shellBottomInset)
            ) {
                QueueSheet(
                    isVisible = state.isQueueVisible,
                    onDismiss = viewModel::dismissQueue,
                    modifier = Modifier.fillMaxSize()
                ) {
                    RemoteQueueList(
                        state = state,
                        onPlayQueueItem = viewModel::playQueueItem,
                        onRemoveQueueItem = viewModel::removeQueueItem
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingPane(
    state: RemoteControlUiState,
    viewModel: RemoteControlViewModel,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val artworkSize = minOf(
            maxWidth - 32.dp,
            maxHeight - ControlsReservedHeight,
            MaxArtworkSize
        ).coerceAtLeast(MinArtworkSize)

        // Centered when it fits; scrolls only on very short windows.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = NowPlayingMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Artwork(
                    coverUrl = state.coverUrl,
                    contentDescription = state.title,
                    size = artworkSize
                )
                TrackInfo(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
                SeekBar(
                    state = state,
                    onSeekChange = viewModel::onSeekChange,
                    onSeekFinished = viewModel::onSeekFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                )
                PlaybackControls(
                    state = state,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onSkipPrevious = viewModel::skipPrevious,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onCycleLoopMode = viewModel::cycleLoopMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp)
                )
                VolumeControl(
                    state = state,
                    onVolumeChange = viewModel::onVolumeChange,
                    onVolumeChangeFinished = viewModel::onVolumeChangeFinished,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Artwork(
    coverUrl: String?,
    contentDescription: String?,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(22.dp))
            .background(AppTheme.colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxMusic,
                contentDescription = null,
                tint = AppTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun TrackInfo(
    state: RemoteControlUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!state.hasTrack) {
            Text(
                text = stringResource(Res.string.devices_remote_nothing_playing),
                style = AppTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = stringResource(Res.string.devices_remote_nothing_playing_hint),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            return@Column
        }

        Text(
            text = state.title ?: stringResource(Res.string.devices_remote_unknown_track),
            style = AppTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.artists ?: stringResource(Res.string.devices_remote_unknown_artist),
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        state.album?.let { album ->
            Text(
                text = album,
                style = AppTheme.typography.bodySmall,
                color = MillenniumTheme.colors.muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SeekBar(
    state: RemoteControlUiState,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MillenniumSlider(
            value = state.seekProgress,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            enabled = state.canSeek,
            label = stringResource(Res.string.player_seek_label),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.positionLabel,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant
            )
            Text(
                text = state.durationLabel,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    state: RemoteControlUiState,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleLoopMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillenniumIconButton(
            onClick = onToggleShuffle,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxShuffle,
                contentDescription = stringResource(
                    if (state.isShuffling) {
                        Res.string.player_disable_shuffle_content_description
                    } else {
                        Res.string.player_enable_shuffle_content_description
                    }
                ),
                tint = if (state.isShuffling) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant
            )
        }
        MillenniumIconButton(
            onClick = onSkipPrevious,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxPrevious,
                contentDescription = stringResource(Res.string.player_previous_content_description)
            )
        }
        MillenniumIconButton(
            onClick = onTogglePlayPause,
            style = MillenniumIconButtonStyle.Primary
        ) {
            MillenniumIcon(
                imageVector = if (state.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                contentDescription = stringResource(
                    if (state.isPlaying) {
                        Res.string.player_pause_content_description
                    } else {
                        Res.string.player_play_content_description
                    }
                ),
                modifier = Modifier.size(30.dp),
                tint = LocalTextStyle.current.color
            )
        }
        MillenniumIconButton(
            onClick = onSkipNext,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                imageVector = Iconsax.IconsaxNext,
                contentDescription = stringResource(Res.string.player_next_content_description)
            )
        }
        MillenniumIconButton(
            onClick = onCycleLoopMode,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                imageVector = when (state.loopMode) {
                    RemoteLoopMode.NONE -> Iconsax.IconsaxRepeateMusic
                    RemoteLoopMode.ONE -> Iconsax.IconsaxRepeateOne
                    RemoteLoopMode.ALL -> Iconsax.IconsaxRepeatMusic
                },
                contentDescription = stringResource(
                    when (state.loopMode) {
                        RemoteLoopMode.NONE -> Res.string.player_loop_mode_none
                        RemoteLoopMode.ONE -> Res.string.player_loop_mode_one
                        RemoteLoopMode.ALL -> Res.string.player_loop_mode_all
                    }
                ),
                tint = if (state.loopMode == RemoteLoopMode.NONE) {
                    AppTheme.colors.onSurfaceVariant
                } else {
                    AppTheme.colors.primary
                }
            )
        }
    }
}

@Composable
private fun VolumeControl(
    state: RemoteControlUiState,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MillenniumIcon(
            imageVector = when (state.volumeLevel) {
                RemoteVolumeLevel.MUTED -> Iconsax.IconsaxVolumeCross
                RemoteVolumeLevel.LOW -> Iconsax.IconsaxVolumeLow
                RemoteVolumeLevel.HIGH -> Iconsax.IconsaxVolumeHigh
            },
            contentDescription = null,
            tint = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        MillenniumSlider(
            value = state.volume,
            onValueChange = onVolumeChange,
            onValueChangeFinished = onVolumeChangeFinished,
            label = stringResource(Res.string.devices_remote_volume),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConnectionStatus(
    title: String,
    message: String,
    leading: @Composable () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading()
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                MillenniumButton(
                    onClick = onAction,
                    style = MillenniumButtonStyle.Secondary,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, isError: Boolean) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isError) AppTheme.colors.errorContainer else AppTheme.colors.surfaceContainerHighest
            ),
        contentAlignment = Alignment.Center
    ) {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) AppTheme.colors.onErrorContainer else AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun RemoteQueueList(
    state: RemoteControlUiState,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(Res.string.queue_title),
            style = AppTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (state.queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.queue_no_queue_entries),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        } else {
            // Open with the current track in view; after that the user owns the scroll position.
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = state.currentQueueIndex.coerceAtLeast(0)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = state.queue, key = { it.key }) { item ->
                    RemoteQueueItemRow(
                        item = item,
                        onPlayClick = { onPlayQueueItem(item.index) },
                        onRemoveClick = { onRemoveQueueItem(item.mediaUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteQueueItemRow(
    item: RemoteQueueItemUi,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    MillenniumListTile(
        onClick = onPlayClick,
        selected = item.isCurrent,
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppTheme.shapes.small)
                    .background(AppTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverUrl != null) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "${item.index + 1}",
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            }
        },
        title = {
            Text(
                text = item.title,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isCurrent) {
                    AppTheme.colors.onSecondaryContainer
                } else {
                    AppTheme.colors.onSurface
                }
            )
        },
        subtitle = {
            Text(
                text = item.artists,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailing = {
            Text(
                text = item.durationLabel,
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant
            )

            MillenniumDropdownMenu(
                expanded = showMenu,
                onExpandedChange = { showMenu = it },
                anchor = {
                    MillenniumIconButton(
                        onClick = { showMenu = true },
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            Iconsax.Iconsax3DotsMore,
                            contentDescription = stringResource(Res.string.devices_remote_more_options)
                        )
                    }
                }) {
                MillenniumDropdownMenuItem(
                    label = stringResource(Res.string.devices_remote_remove_from_queue),
                    icon = Iconsax.IconsaxMusicSquareRemove,
                    destructive = true,
                    onClick = {
                        onRemoveClick()
                        showMenu = false
                    })
            }
        }
    )
}
