/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.modules.settings.LocalMediaFoldersViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import org.spotube.millennium.composables.MillenniumDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.component.VerticalScrollbar
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.settings.LocalMediaFolderRowUi
import org.spotube.spotube.modules.settings.LocalMediaFoldersUiState
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxFolderOpen
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTooltip
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import spotube.composeapp.generated.resources.*

@Composable
internal fun LocalMediaFoldersEditorContent(
    state: LocalMediaFoldersUiState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.settings_local_media_folders_description),
            style = AppTheme.typography.bodySmall, color = AppTheme.colors.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                pluralStringResource(
                    Res.plurals.settings_local_media_folders_count,
                    state.folders.size,
                    state.folders.size
                ),
                style = AppTheme.typography.labelMedium, modifier = Modifier.weight(1f)
            )
            MillenniumButton(
                onClick = onAddFolder,
                enabled = !state.isSaving,
                style = MillenniumButtonStyle.Secondary,
                padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                minSize = DpSize(0.dp, 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MillenniumIcon(Iconsax.IconsaxAddSquare, null, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(Res.string.settings_local_media_folders_add_action),
                        style = AppTheme.typography.labelMedium
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxWidth()
                .heightIn(min = if (state.folders.isEmpty()) 104.dp else 76.dp, max = 240.dp)
                .millenniumSurface(
                    background = SolidColor(MillenniumTheme.colors.background),
                    shape = RoundedCornerShape(8.dp),
                    borderColor = MillenniumTheme.colors.border,
                    borderWidth = 1.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.folders.isEmpty()) {
                Column(
                    Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxFolderOpen,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = AppTheme.colors.onSurfaceVariant
                    )
                    Text(
                        stringResource(Res.string.settings_local_media_folders_none_added),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        state.folders,
                        key = { _, folder -> folder.path }) { index, folder ->
                        if (index > 0) MillenniumSeparator(Modifier.padding(horizontal = 12.dp))
                        LocalMediaFolderEditorRow(
                            folder,
                            !state.isSaving
                        ) { onRemoveFolder(folder.path) }
                    }
                }
                if (listState.canScrollForward || listState.canScrollBackward) {
                    VerticalScrollbar(
                        listState = listState,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
        state.automaticNote?.let { note ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MillenniumIcon(
                    Iconsax.IconsaxDirectboxReceive,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = AppTheme.colors.onSurfaceVariant
                )
                Text(
                    stringResource(note),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
        if (state.saveFailed) Text(
            stringResource(Res.string.settings_local_media_folders_save_error),
            style = AppTheme.typography.bodySmall, color = AppTheme.colors.error
        )
        MillenniumSeparator()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            MillenniumButton(
                onClick = onCancel,
                enabled = !state.isSaving,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(stringResource(Res.string.common_cancel))
            }
            MillenniumButton(onClick = onSave, enabled = state.canSave, loading = state.isSaving) {
                Text(stringResource(Res.string.common_save))
            }
        }
    }
}

@Composable
private fun LocalMediaFolderEditorRow(
    folder: LocalMediaFolderRowUi,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).millenniumSurface(
                background = millenniumBevelBrush(MillenniumTheme.colors),
                shape = RoundedCornerShape(6.dp),
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                Iconsax.IconsaxFolderOpen,
                null,
                modifier = Modifier.size(20.dp),
                tint = AppTheme.colors.primary
            )
        }
        Box(Modifier.weight(1f)) {
            MillenniumTooltip(folder.path) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            folder.name,
                            style = AppTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            folder.path,
                            style = AppTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = AppTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        val removeLabel =
            stringResource(Res.string.settings_local_media_folders_remove_named, folder.name)
        MillenniumTooltip(removeLabel) {
            MillenniumIconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(32.dp),
                style = MillenniumIconButtonStyle.Secondary
            ) {
                MillenniumIcon(Iconsax.IconsaxTrash, removeLabel, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Renders the "manage local folders" dialog (folder picker included) while [viewModel] has it open.
 * Shared by Settings and the Local Tracks screen; callers trigger it via [LocalMediaFoldersViewModel.open]. */
@Composable
fun LocalMediaFoldersDialogHost(viewModel: LocalMediaFoldersViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerLauncher =
        rememberDirectoryPickerLauncher { directory -> viewModel.addFolder(directory?.path) }
    if (state.isOpen) {
        MillenniumDialog(
            visible = true,
            onDismissRequest = viewModel::dismiss,
            title = stringResource(Res.string.settings_local_media_folders_title)
        ) {
            LocalMediaFoldersEditorContent(
                state, { pickerLauncher.launch() }, viewModel::removeFolder,
                viewModel::save, viewModel::dismiss
            )
        }
    }
}
