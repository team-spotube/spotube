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

package dev.krtirtho.spotube.modules.shell.player_queue

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.krtirtho.spotube.core.jam.JamParticipant
import dev.krtirtho.spotube.core.ui.base.Card
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.IconButton
import dev.krtirtho.spotube.core.ui.base.ListRowTile
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.TextField
import dev.krtirtho.spotube.core.ui.base.copyShape
import dev.krtirtho.spotube.core.ui.component.AdaptiveDialogBottomSheet
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxDragHandle
import dev.krtirtho.spotube.resources.iconsax.IconsaxFilterSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxCloseSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicSquareRemove
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash
import dev.krtirtho.spotube.resources.iconsax.IconsaxUserRemove
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlayerQueueContent(
    viewModel: PlayerQueueContentViewModel = koinViewModel<PlayerQueueContentViewModel>(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.queueContentUiState.collectAsState()
    val displayItems = state.displayItems
    val filterQuery = state.filterQuery
    val isFiltered = state.isFiltered
    val isReadOnly = state.isReadOnly
    var selectedParticipant by remember { mutableStateOf<JamParticipant?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState,
        onMove = { from, to ->
            if (isFiltered || isReadOnly) return@rememberReorderableLazyListState
            viewModel.onMove(from.index, to.index)
        },
    )

    Surface(modifier = modifier) {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = filterQuery,
                    onValueChange = viewModel::setQueueFilter,
                    placeholder = { Text("Filter queue...") },
                    leadingIcon = {
                        Icon(
                            Iconsax.IconsaxFilterSearch,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (!isReadOnly) {
                    IconButton(
                        onClick = viewModel::clearQueue,
                        theme = LocalBaseUITheme.current.iconButtons.outline.copyShape(MaterialTheme.shapes.small),
                    ) {
                        Icon(Iconsax.IconsaxTrash, contentDescription = "Clear Queue")
                    }
                }
            }

            if (displayItems.isEmpty()) {
                Text(
                    text = "No queue entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(displayItems, key = { it.id }) { item ->
                            ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                QueueItemRow(
                                    item = item,
                                    reorderScope = if (isFiltered || isReadOnly) null else this,
                                    onPlayClick = { viewModel.playQueueItem(item.originalIndex) },
                                    onRemoveClick = { viewModel.removeQueueItem(item.originalIndex) },
                                    onDragStarted = { viewModel.onDragStart() },
                                    onDragStopped = { viewModel.onDragStop() },
                                    showOptions = !isReadOnly,
                                    enabled = !isReadOnly,
                                    onParticipantClick = { selectedParticipant = it },
                                )
                            }
                        }
                    }
                }
            }

            selectedParticipant?.let { participant ->
                ParticipantDialog(
                    participant = participant,
                    isJamHost = state.isJamHost,
                    onDismiss = { selectedParticipant = null },
                    onKick = { viewModel.kickParticipant(participant.id) },
                    onBan = { viewModel.banParticipant(participant.id) },
                    onRemoveSuggestions = { viewModel.removeParticipantTracks(participant.id) },
                )
            }
        }
    }
}

@Composable
private fun ParticipantDialog(
    participant: JamParticipant,
    isJamHost: Boolean,
    onDismiss: () -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit,
    onRemoveSuggestions: () -> Unit,
) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { Text(participant.displayName, style = MaterialTheme.typography.titleLarge) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                ParticipantAvatar(participant, size = 40)
                Text(
                    text = participant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (participant.isHost) {
                    Text(
                        text = "Host",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (isJamHost && !participant.isHost) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                ListRowTile(
                    onClick = {
                        onKick()
                        onDismiss()
                    },
                    leading = {
                        Icon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    title = { Text("Kick") },
                    subtitle = { Text("Remove them from the session") },
                )
                ListRowTile(
                    onClick = {
                        onBan()
                        onDismiss()
                    },
                    leading = {
                        Icon(
                            imageVector = Iconsax.IconsaxUserRemove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    title = { Text("Ban") },
                    subtitle = { Text("Kick and prevent them from rejoining") },
                )
                ListRowTile(
                    onClick = {
                        onRemoveSuggestions()
                        onDismiss()
                    },
                    leading = {
                        Icon(
                            imageVector = Iconsax.IconsaxMusicSquareRemove,
                            contentDescription = null,
                        )
                    },
                    title = { Text("Remove suggestions") },
                    subtitle = { Text("Remove every track they added to the queue") },
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(participant: JamParticipant, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItemUi,
    reorderScope: sh.calvin.reorderable.ReorderableCollectionItemScope?,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    showOptions: Boolean = true,
    enabled: Boolean = true,
    onParticipantClick: (JamParticipant) -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }

    ListRowTile(
        onClick = onPlayClick,
        enabled = enabled,
        selected = item.isCurrent,
        modifier = Modifier,
        leading = {
            Row(
                modifier = Modifier
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Iconsax.IconsaxDragHandle,
                    contentDescription = if (reorderScope != null) "Reorder" else null,
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (reorderScope != null) {
                                with(reorderScope) {
                                    Modifier.draggableHandle(
                                        onDragStarted = { onDragStarted() },
                                        onDragStopped = onDragStopped,
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = "${item.originalIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        title = {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isCurrent) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        subtitle = {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Text(
                text = item.durationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(4.dp))

            item.addedByParticipant?.let { participant ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onParticipantClick(participant) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (showOptions) {
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
        }
    )
}
