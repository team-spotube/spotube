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

package org.spotube.spotube.modules.shell.player_queue


import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.spotube.spotube.core.jam.JamParticipant
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxDragHandle
import org.spotube.spotube.resources.iconsax.IconsaxMusicSquareRemove
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.spotube.spotube.resources.iconsax.IconsaxUserRemove
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.composables.MillenniumTooltip
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.queue_clear_queue
import spotube.composeapp.generated.resources.queue_filter_queue
import spotube.composeapp.generated.resources.queue_no_queue_entries
import spotube.composeapp.generated.resources.queue_title
import spotube.composeapp.generated.resources.jam_participants_ban
import spotube.composeapp.generated.resources.jam_participants_host
import spotube.composeapp.generated.resources.player_more_options_content_description
import spotube.composeapp.generated.resources.queue_item_subtitle_added_by
import spotube.composeapp.generated.resources.queue_participant_ban_description
import spotube.composeapp.generated.resources.queue_participant_kick
import spotube.composeapp.generated.resources.queue_participant_kick_description
import spotube.composeapp.generated.resources.queue_participant_remove_suggestions
import spotube.composeapp.generated.resources.queue_participant_remove_suggestions_description
import spotube.composeapp.generated.resources.queue_remove_from_queue
import spotube.composeapp.generated.resources.queue_reorder_content_description

@Composable
fun PlayerQueueContent(
    viewModel: PlayerQueueContentViewModel = koinViewModel<PlayerQueueContentViewModel>(),
    modifier: Modifier = Modifier
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
        })

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(Res.string.queue_title),
                style = AppTheme.typography.titleLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterState = rememberTextFieldState(filterQuery)
                LaunchedEffect(filterState.text) {
                    viewModel.setQueueFilter(filterState.text.toString())
                }
                MillenniumTextField(
                    state = filterState,
                    label = "",
                    placeholder = stringResource(Res.string.queue_filter_queue),
                    modifier = Modifier.weight(1f)
                )
                if (!isReadOnly) {
                    MillenniumTooltip(stringResource(Res.string.queue_clear_queue)) {
                        MillenniumIconButton(
                            onClick = viewModel::clearQueue,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                Iconsax.IconsaxTrash,
                                contentDescription = stringResource(Res.string.queue_clear_queue)
                            )
                        }
                    }
                }
            }

            if (displayItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.queue_no_queue_entries),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(displayItems, key = { it.id }) { item ->
                        ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                            QueueItemRow(
                                item = item,
                                reorderScope = if (isFiltered || isReadOnly) null else this,
                                onPlayClick = { viewModel.playQueueItem(item.originalIndex) },
                                onRemoveClick = { viewModel.removeQueueItem(item.originalIndex) },
                                onDragStarted = { viewModel.onDragStart() },
                                onDragStopped = { viewModel.onDragStop() },
                                showOptions = !isReadOnly,
                                enabled = !isReadOnly,
                                onParticipantClick = { selectedParticipant = it })
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
                    onRemoveSuggestions = { viewModel.removeParticipantTracks(participant.id) })
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
    onRemoveSuggestions: () -> Unit
) {
    AdaptiveDialogBottomSheet(
        onDismiss = onDismiss,
        title = { Text(participant.displayName, style = AppTheme.typography.titleLarge) }) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ParticipantAvatar(participant, size = 40)
                Text(
                    text = participant.displayName,
                    style = AppTheme.typography.bodyLarge
                )
                if (participant.isHost) {
                    Text(
                        text = stringResource(Res.string.jam_participants_host),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.primary
                    )
                }
            }

            if (isJamHost && !participant.isHost) {
                MillenniumSeparator(
                    modifier = Modifier.fillMaxWidth()
                )
                MillenniumListTile(
                    onClick = {
                        onKick()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = null,
                            tint = AppTheme.colors.error
                        )
                    },
                    title = { Text(stringResource(Res.string.queue_participant_kick)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_kick_description)) })
                MillenniumListTile(
                    onClick = {
                        onBan()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxUserRemove,
                            contentDescription = null,
                            tint = AppTheme.colors.error
                        )
                    },
                    title = { Text(stringResource(Res.string.jam_participants_ban)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_ban_description)) })
                MillenniumListTile(
                    onClick = {
                        onRemoveSuggestions()
                        onDismiss()
                    },
                    leading = {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxMusicSquareRemove,
                            contentDescription = null
                        )
                    },
                    title = { Text(stringResource(Res.string.queue_participant_remove_suggestions)) },
                    subtitle = { Text(stringResource(Res.string.queue_participant_remove_suggestions_description)) })
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
            .background(AppTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.onPrimaryContainer
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
    onParticipantClick: (JamParticipant) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    MillenniumListTile(
        onClick = onPlayClick,
        enabled = enabled,
        selected = item.isCurrent,
        modifier = Modifier,
        leading = {
            Row(
                modifier = Modifier
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MillenniumIcon(
                    Iconsax.IconsaxDragHandle,
                    contentDescription = if (reorderScope != null) stringResource(Res.string.queue_reorder_content_description) else null,
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (reorderScope != null) {
                                with(reorderScope) {
                                    Modifier.draggableHandle(
                                        onDragStarted = { onDragStarted() },
                                        onDragStopped = onDragStopped
                                    )
                                }
                            } else {
                                Modifier
                            }),
                    tint = AppTheme.colors.onSurfaceVariant)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppTheme.shapes.small)
                        .background(AppTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.artwork != null) {
                        AsyncImage(
                            model = item.artwork,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "${item.originalIndex + 1}",
                            style = AppTheme.typography.labelMedium,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
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
                text = item.addedByParticipant?.let { stringResource(Res.string.queue_item_subtitle_added_by, item.subtitle, it.displayName) } ?: item.subtitle,
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

            Spacer(modifier = Modifier.width(4.dp))

            item.addedByParticipant?.let { participant ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceVariant)
                        .clickable { onParticipantClick(participant) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.displayName.firstOrNull()?.uppercase()?.take(1) ?: "?",
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (showOptions) {
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
                                contentDescription = stringResource(Res.string.player_more_options_content_description)
                            )
                        }
                    }) {
                    MillenniumDropdownMenuItem(
                        label = stringResource(Res.string.queue_remove_from_queue),
                        icon = Iconsax.IconsaxMusicSquareRemove,
                        destructive = true,
                        onClick = {
                            onRemoveClick()
                            showMenu = false
                        })
                }
            }
        }
    )
}
