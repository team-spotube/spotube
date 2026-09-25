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

package dev.krtirtho.spotube.modules.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.krtirtho.spotube.core.remote.ConnectionState
import dev.krtirtho.spotube.core.remote.RemoteQueueEntry
import dev.krtirtho.spotube.core.ui.base.BaseUITheme
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.IconButton
import dev.krtirtho.spotube.core.ui.base.ListRowTile
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.PrimaryIconButton
import dev.krtirtho.spotube.core.ui.base.Slider
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.modules.shell.player_queue.QueueSheet
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxCloseSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicFilter
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicSquareRemove
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxPrevious
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeateMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxShuffle
import dev.krtirtho.spotube.resources.iconsax.IconsaxVolumeHigh
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    onDisconnect: () -> Unit,
) {
    val viewModel = koinViewModel<RemoteControlViewModel>()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val queueState by viewModel.queueState.collectAsStateWithLifecycle()
    val isQueueVisible by viewModel.isQueueVisible.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current

    // Center the mobile-inspired layout and limit its width so it doesn't
    // stretch awkwardly on large screens.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxHeight(),
            topBar = {
                ApplicationMainBar(
                    title = { Text("Remote Control") },
                    backButton = true,
                    actions = {
                        GhostIconButton(
                            onClick = viewModel::toggleQueueVisibility,
                        ) {
                            Icon(
                                imageVector = Iconsax.IconsaxMusicFilter,
                                contentDescription = "Queue",
                                tint = if (isQueueVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        GhostIconButton(
                            onClick = {
                                viewModel.disconnect()
                                onDisconnect()
                            }
                        ) {
                            Icon(
                                imageVector = Iconsax.IconsaxCloseSquare,
                                contentDescription = "Disconnect",
                            )
                        }
                    }
                )
            }
        ) { padding ->
            when (connectionState) {
                is ConnectionState.Connected -> {
                    RemoteControlContent(
                        playerState = playerState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipNext = viewModel::skipNext,
                        onSkipPrevious = viewModel::skipPrevious,
                        onSeek = viewModel::seek,
                        onSetVolume = viewModel::setVolume,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onCycleLoopMode = viewModel::cycleLoopMode,
                        modifier = Modifier.padding(padding).padding(bottom = shellBottomInset)
                    )
                }
                is ConnectionState.Connecting -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(bottom = shellBottomInset),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Connecting...")
                    }
                }
                is ConnectionState.Disconnected -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(bottom = shellBottomInset),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Disconnected")
                    }
                }
                is ConnectionState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(bottom = shellBottomInset),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Connection error: ${(connectionState as ConnectionState.Error).message}")
                    }
                }
            }
        }
    }

    // Click-outside scrim for the sliding queue sheet on large screens.
    // (The ModalBottomSheet variant has its own built-in scrim.)
    if (isQueueVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.toggleQueueVisibility() },
                )
        )
    }

    // Keep the sliding sheet above the AppLargePlayer on large screens.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = shellBottomInset),
    ) {
        QueueSheet(
            isVisible = isQueueVisible,
            onDismiss = { viewModel.toggleQueueVisibility() },
            modifier = Modifier.fillMaxSize(),
        ) {
            RemoteQueueSection(
                queueState = queueState,
                onPlayQueueItem = viewModel::playQueueItem,
                onRemoveQueueItem = viewModel::removeQueueItem,
            )
        }
    }
}

@Composable
private fun RemoteControlContent(
    playerState: RemotePlayerState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleLoopMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box (modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Album art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (playerState.currentTrackCoverUrl != null) {
                    AsyncImage(
                        model = playerState.currentTrackCoverUrl,
                        contentDescription = "Album cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Iconsax.IconsaxMusicFilter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = playerState.currentTrackTitle ?: "Unknown Track",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = playerState.currentTrackArtists ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (playerState.currentTrackAlbum != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playerState.currentTrackAlbum!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Seek bar
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Slider(
                    value = playerState.positionMs.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..playerState.durationMs.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(playerState.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(playerState.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxShuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                // Skip previous
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Play/Pause
                val baseTheme = LocalBaseUITheme.current
                val circlePrimaryIconTheme = remember(baseTheme) {
                    baseTheme.iconButtons.primary.copy(
                        shape = BaseUITheme.InteractionState.fromSingleValue(CircleShape)
                    )
                }
                PrimaryIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                    theme = circlePrimaryIconTheme,
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Iconsax.IconsaxPause
                        } else {
                            Iconsax.IconsaxPlay
                        },
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }

                // Skip next
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Loop mode
                IconButton(
                    onClick = onCycleLoopMode,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxRepeateMusic,
                        contentDescription = "Loop mode",
                        tint = if (playerState.loopMode != "none") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Volume control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Iconsax.IconsaxVolumeHigh,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )

                Slider(
                    value = playerState.volume,
                    onValueChange = onSetVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RemoteQueueSection(
    queueState: RemoteQueueState,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
        )

        if (queueState.entries.isEmpty()) {
            Text(
                text = "No queue entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = queueState.entries,
                    key = { index, entry -> "${entry.mediaUrl}@$index" },
                ) { index, entry ->
                    RemoteQueueItemRow(
                        entry = entry,
                        index = index,
                        isCurrent = index == queueState.currentIndex,
                        onPlayClick = { onPlayQueueItem(index) },
                        onRemoveClick = { onRemoveQueueItem(entry.mediaUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteQueueItemRow(
    entry: RemoteQueueEntry,
    index: Int,
    isCurrent: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListRowTile(
        onClick = onPlayClick,
        selected = isCurrent,
        modifier = Modifier,
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.coverUrl != null) {
                    AsyncImage(
                        model = entry.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        title = {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        subtitle = {
            Text(
                text = entry.artists,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Text(
                text = formatDuration(entry.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box {
                GhostIconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Iconsax.Iconsax3DotsMore,
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from queue") },
                        onClick = {
                            onRemoveClick()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Iconsax.IconsaxMusicSquareRemove, contentDescription = null)
                        },
                    )
                }
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val duration = ms.milliseconds
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}