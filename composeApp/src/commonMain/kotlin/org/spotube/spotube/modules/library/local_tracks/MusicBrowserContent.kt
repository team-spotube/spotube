/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.composeunstyled.Text
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.spotube.millennium.composables.*
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.spotube.core.ui.component.AdaptiveDialogBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveDropdownBottomSheet
import org.spotube.spotube.core.ui.component.AdaptiveMenuItem
import org.spotube.spotube.core.ui.component.cards.PlayableListTile
import org.spotube.spotube.core.ui.component.cards.MediaArtwork
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.modules.library.local_tracks.media.MusicFileDetails
import org.spotube.spotube.resources.iconsax.*
import spotube.composeapp.generated.resources.*
import kotlin.math.roundToInt

@Composable
internal fun MusicBrowserContent(
    state: MusicBrowserState,
    viewModel: LibraryLocalTracksViewModel,
    requestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    fileActionsInAppBar: Boolean = false
) {
    val picker = rememberDirectoryPickerLauncher { folder -> folder?.let(viewModel::addFolder) }
    val coverPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file -> file?.let(viewModel::pickCover) }
    val inset = LocalAppShellBottomInset.current
    val showFloatingFileActions = state.hasFileActions && !fileActionsInAppBar
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (state.breadcrumb.isNotEmpty()) MillenniumIconButton(
                    onClick = viewModel::up, style = MillenniumIconButtonStyle.Quiet, enabled = !state.busy
                ) {
                    AppIcon(FeatherIcons.ArrowUp, stringResource(Res.string.library_browser_up))
                }
                Text(
                    state.breadcrumb.lastOrNull()?.let { it.label?.let { label -> stringResource(label) } ?: it.name }
                        ?: stringResource(Res.string.library_browser_locations),
                    modifier = Modifier.weight(1f), style = AppTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (state.loading || state.busy) MillenniumSpokeSpinner(size = 16.dp)
                if (state.canPlay && !state.isSelecting && state.transfer == null) {
                    BrowserIconButton(Iconsax.IconsaxPlay, stringResource(Res.string.library_local_play_folder), !state.busy) { viewModel.play() }
                    BrowserIconButton(FeatherIcons.CheckSquare, stringResource(Res.string.library_browser_select_tracks), !state.busy, viewModel::startSelection)
                }
                if (state.breadcrumb.isEmpty()) {
                    BrowserIconButton(FeatherIcons.FolderPlus, stringResource(Res.string.library_browser_add_location), !state.busy) { picker.launch() }
                } else {
                    BrowserIconButton(FeatherIcons.FolderPlus, stringResource(Res.string.library_browser_new_folder), !state.busy, viewModel::showNewFolder)
                }
            }
            if (!state.canAccessMedia) Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(Res.string.library_browser_permission), Modifier.weight(1f), style = AppTheme.typography.bodySmall)
                MillenniumButton(onClick = requestPermission, style = MillenniumButtonStyle.Secondary) {
                    Text(stringResource(Res.string.library_local_grant_access))
                }
            }
            if (!state.hasDialog) BrowserMessage(state)
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = inset + if (showFloatingFileActions) 116.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.folders, key = { it.id }) { folder ->
                    MillenniumListTile(
                        onClick = { viewModel.openFolder(folder.id) },
                        leading = { AppIcon(Iconsax.IconsaxFolderOpen, null, tint = AppTheme.colors.primary) },
                        title = { Text(folder.label?.let { stringResource(it) } ?: folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailing = { AppIcon(FeatherIcons.ChevronRight, null) }
                    )
                }
                items(state.tracks, key = { it.id }) { track ->
                    BrowserTrackRow(track, selected = track.id in state.selected, selecting = state.isSelecting,
                        actionsEnabled = state.transfer == null && !state.busy,
                        onClick = { viewModel.clickTrack(track.id) }, onPlay = { viewModel.play(track.id) },
                        onToggleSelection = { viewModel.toggleSelection(track.id) }, onAction = { viewModel.action(track.id, it) })
                }
                if (!state.loading && state.folders.isEmpty() && state.tracks.isEmpty()) item {
                    Text(stringResource(Res.string.library_browser_empty), Modifier.padding(12.dp), style = AppTheme.typography.bodyMedium)
                }
            }
        }
        if (showFloatingFileActions) Box(Modifier.widthIn(max = 960.dp).fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            BrowserFileActions(state, viewModel, Modifier.padding(end = 12.dp, bottom = inset + 12.dp))
        }
    }
    state.deleteRequest?.let { request ->
        val title = stringResource(if (request.count == 1) Res.string.library_browser_delete else Res.string.library_browser_delete_selected)
        AdaptiveDialogBottomSheet(
            onDismiss = viewModel::dismissAction, title = { Text(title) }, dialogTitle = title,
            actions = {
                MillenniumButton(onClick = viewModel::delete, enabled = !state.busy) { Text(title) }
            }
        ) {
            Text(if (request.title != null) stringResource(Res.string.library_browser_delete_confirm, request.title) else
                pluralStringResource(Res.plurals.library_browser_delete_many_confirm, request.count, request.count))
            BrowserMessage(state)
        }
    }
    state.newFolder?.let { name ->
        val title = stringResource(Res.string.library_browser_new_folder)
        AdaptiveDialogBottomSheet(onDismiss = viewModel::dismissAction, title = { Text(title) }, dialogTitle = title,
            actions = {
                MillenniumButton(onClick = viewModel::createFolder, enabled = !state.busy) {
                    Text(stringResource(Res.string.library_browser_create_folder))
                }
            }
        ) {
            MillenniumTextField(name, label = stringResource(Res.string.library_browser_folder_name), modifier = Modifier.fillMaxWidth())
            BrowserMessage(state)
        }
    }
    state.details?.let { BrowserDetails(it, viewModel::dismissAction) }
    state.editor?.let { editor ->
        val title = stringResource(Res.string.library_browser_edit)
        AdaptiveDialogBottomSheet(
            onDismiss = viewModel::dismissAction, title = { Text(title) }, dialogTitle = title,
            actions = {
                MillenniumButton(onClick = viewModel::saveTags,
                    enabled = !state.busy && !state.isLoadingCover && !state.awaitingWriteAccess) {
                    Text(stringResource(Res.string.common_save))
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MediaArtwork(state.editorCover, stringResource(Res.string.library_browser_cover), Modifier.size(64.dp))
                    MillenniumButton(
                        onClick = { coverPicker.launch() }, enabled = !state.busy && !state.isLoadingCover,
                        style = MillenniumButtonStyle.Secondary
                    ) {
                        Text(stringResource(if (state.editorCover == null) Res.string.library_browser_cover_add else Res.string.library_browser_cover_change))
                    }
                    if (state.isLoadingCover) MillenniumSpokeSpinner(size = 16.dp)
                }
                MillenniumTextField(editor.title, label = stringResource(Res.string.player_track_details_title), modifier = Modifier.fillMaxWidth())
                MillenniumTextField(editor.artists, label = stringResource(Res.string.library_browser_artists), modifier = Modifier.fillMaxWidth())
                MillenniumTextField(editor.album, label = stringResource(Res.string.player_track_details_album), modifier = Modifier.fillMaxWidth())
                MillenniumTextField(editor.track, label = stringResource(Res.string.player_track_details_track_number), modifier = Modifier.fillMaxWidth())
                MillenniumTextField(editor.disc, label = stringResource(Res.string.player_track_details_disc_number), modifier = Modifier.fillMaxWidth())
                BrowserMessage(state)
            }
        }
    }
}

@Composable
private fun BrowserMessage(state: MusicBrowserState) {
    val message = state.report?.let { stringResource(Res.string.library_browser_batch_report, it.done, it.conflicts, it.errors) }
        ?: state.message?.let { stringResource(it) } ?: return
    Text(message, Modifier.padding(vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }, style = AppTheme.typography.bodySmall)
}

@Composable
internal fun BrowserTrackRow(
    track: BrowserTrack,
    selected: Boolean,
    selecting: Boolean,
    actionsEnabled: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleSelection: () -> Unit,
    onAction: (TrackAction) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf<Offset?>(null) }
    val actions = listOf(
        AdaptiveMenuItem(FeatherIcons.CheckSquare, stringResource(if (selected) Res.string.library_browser_deselect else Res.string.library_browser_select),
            onClick = onToggleSelection),
        AdaptiveMenuItem(FeatherIcons.Info, stringResource(Res.string.library_browser_details), onClick = { onAction(TrackAction.Details) }),
        AdaptiveMenuItem(FeatherIcons.Edit2, stringResource(Res.string.library_browser_edit), onClick = { onAction(TrackAction.Edit) }),
        AdaptiveMenuItem(FeatherIcons.Copy, stringResource(Res.string.library_browser_copy), onClick = { onAction(TrackAction.Copy) }),
        AdaptiveMenuItem(FeatherIcons.Scissors, stringResource(Res.string.library_browser_move), onClick = { onAction(TrackAction.Move) }),
        AdaptiveMenuItem(FeatherIcons.Trash2, stringResource(Res.string.library_browser_delete), onClick = { onAction(TrackAction.Delete) })
    )

    Box(Modifier.fillMaxWidth()) {
        PlayableListTile(
            track.name, track.subtitle ?: stringResource(Res.string.media_unknown_artist),
            imageModel = track.cover, compact = true, isSelected = selected,
            onClick = onClick.takeIf { actionsEnabled },
            onPlay = onPlay.takeIf { !selecting && actionsEnabled },
            onLongClick = if (actionsEnabled) ({ cursor = null; menuOpen = true }) else null,
            onRightClick = if (actionsEnabled) ({ point -> cursor = point; menuOpen = true }) else null,
            trailingContent = {
                if (selecting) BrowserIconButton(
                    if (selected) FeatherIcons.CheckSquare else FeatherIcons.Square,
                    stringResource(if (selected) Res.string.library_browser_deselect else Res.string.library_browser_select),
                    actionsEnabled, onToggleSelection
                )
                if (cursor == null) AdaptiveDropdownBottomSheet(
                    expanded = menuOpen, onExpandedChange = { menuOpen = it }, items = actions,
                    trigger = { open -> BrowserIconButton(Iconsax.Iconsax3DotsMore, stringResource(Res.string.library_browser_actions, track.name), actionsEnabled, open) }
                ) else BrowserIconButton(Iconsax.Iconsax3DotsMore, stringResource(Res.string.library_browser_actions, track.name), actionsEnabled) {
                    cursor = null
                    menuOpen = true
                }
            }
        )
        cursor?.let { point ->
            AdaptiveDropdownBottomSheet(
                expanded = menuOpen, onExpandedChange = { menuOpen = it; if (!it) cursor = null }, items = actions,
                modifier = Modifier.offset { IntOffset(point.x.roundToInt(), point.y.roundToInt()) },
                trigger = { Spacer(Modifier.size(1.dp)) }
            )
        }
    }
}

@Composable
private fun BrowserIconButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    MillenniumIconButton(onClick = onClick, enabled = enabled, style = MillenniumIconButtonStyle.Quiet) {
        AppIcon(icon, label)
    }
}

@Composable
private fun BrowserFileActions(state: MusicBrowserState, viewModel: LibraryLocalTracksViewModel, modifier: Modifier) {
    val colors = MillenniumTheme.colors
    Column(modifier.millenniumSurface(background = millenniumPanelBrush(colors), shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp, borderWidth = 1.dp, borderColor = colors.border).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End) {
        BrowserFileActionLabel(state, Modifier.padding(horizontal = 6.dp))
        BrowserFileActionButtons(state, viewModel)
    }
}

@Composable
internal fun BrowserFileActionLabel(state: MusicBrowserState, modifier: Modifier = Modifier) {
    val transfer = state.transfer
    val label = if (transfer == null) pluralStringResource(Res.plurals.library_browser_selected_count, state.selected.size, state.selected.size)
    else pluralStringResource(if (transfer.move) Res.plurals.library_browser_moving_count else Res.plurals.library_browser_copying_count,
        transfer.count, transfer.count)
    Text(label, style = AppTheme.typography.labelMedium, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun BrowserFileActionButtons(state: MusicBrowserState, viewModel: LibraryLocalTracksViewModel, inAppBar: Boolean = false) =
    BrowserFileActionButtons(state, inAppBar, onSelectAll = viewModel::selectAll, onSelectionAction = viewModel::selectionAction,
        onClearSelection = viewModel::clearSelection, onCancelTransfer = viewModel::cancelTransfer, onPaste = viewModel::paste)

@Composable
internal fun BrowserFileActionButtons(
    state: MusicBrowserState,
    inAppBar: Boolean,
    onSelectAll: () -> Unit,
    onSelectionAction: (TrackAction) -> Unit,
    onClearSelection: () -> Unit,
    onCancelTransfer: () -> Unit,
    onPaste: () -> Unit
) {
    val enabled = !state.busy
    val buttonModifier = if (inAppBar) Modifier.size(36.dp) else Modifier
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.transfer != null) {
            BrowserFileActionIcon(FeatherIcons.X, stringResource(Res.string.common_cancel), enabled, buttonModifier, onCancelTransfer)
            MillenniumIconButton(onClick = onPaste, enabled = enabled && state.canPaste, modifier = buttonModifier,
                style = MillenniumIconButtonStyle.Primary) {
                AppIcon(FeatherIcons.Clipboard, stringResource(Res.string.library_browser_paste))
            }
        } else {
            val hasSelection = enabled && state.selected.isNotEmpty()
            BrowserFileActionIcon(FeatherIcons.CheckSquare, stringResource(Res.string.library_browser_select_all), enabled, buttonModifier, onSelectAll)
            BrowserFileActionIcon(FeatherIcons.Copy, stringResource(Res.string.library_browser_copy_selected), hasSelection, buttonModifier) { onSelectionAction(TrackAction.Copy) }
            BrowserFileActionIcon(FeatherIcons.Scissors, stringResource(Res.string.library_browser_move_selected), hasSelection, buttonModifier) { onSelectionAction(TrackAction.Move) }
            BrowserFileActionIcon(FeatherIcons.Trash2, stringResource(Res.string.library_browser_delete_selected), hasSelection, buttonModifier) { onSelectionAction(TrackAction.Delete) }
            BrowserFileActionIcon(FeatherIcons.X, stringResource(Res.string.library_browser_clear_selection), enabled, buttonModifier, onClearSelection)
        }
    }
}

@Composable
private fun BrowserFileActionIcon(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    MillenniumIconButton(onClick = onClick, enabled = enabled, modifier = modifier, style = MillenniumIconButtonStyle.Quiet) {
        AppIcon(icon, label)
    }
}

@Composable
private fun BrowserDetails(details: MusicFileDetails, dismiss: () -> Unit) {
    val title = stringResource(Res.string.library_browser_details)
    val unknown = stringResource(Res.string.common_unknown)
    AdaptiveDialogBottomSheet(onDismiss = dismiss, title = { Text(title) }, dialogTitle = title,
        actions = { MillenniumButton(onClick = dismiss, style = MillenniumButtonStyle.Secondary) { Text(stringResource(Res.string.common_close)) } }
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(details.title, style = AppTheme.typography.titleMedium)
                BrowserDetailRow(stringResource(Res.string.library_browser_file_name), details.fileName)
                BrowserDetailRow(stringResource(Res.string.library_browser_path), details.path)
                BrowserDetailRow(stringResource(Res.string.library_browser_format), details.format.ifBlank { unknown })
                BrowserDetailRow(stringResource(Res.string.library_browser_size), details.bytes?.let {
                    stringResource(Res.string.library_browser_size_bytes, it)
                } ?: unknown)
                BrowserDetailRow(stringResource(Res.string.library_browser_modified), details.modified ?: unknown)
                BrowserDetailRow(stringResource(Res.string.library_browser_access), stringResource(Res.string.library_browser_access_summary,
                    permissionLabel(details.access.read), permissionLabel(details.access.write), permissionLabel(details.access.delete)))
            }
        }
    }
}

@Composable
private fun permissionLabel(allowed: Boolean?): String = stringResource(when (allowed) {
    true -> Res.string.library_browser_access_allowed
    false -> Res.string.library_browser_access_denied
    null -> Res.string.common_unknown
})

@Composable
private fun BrowserDetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = AppTheme.typography.labelMedium, color = AppTheme.colors.onSurfaceVariant)
        Text(value, style = AppTheme.typography.bodyMedium)
    }
}
