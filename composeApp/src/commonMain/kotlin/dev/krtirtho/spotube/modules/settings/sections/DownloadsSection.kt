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

package dev.krtirtho.spotube.modules.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SelectionSettingCard
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxFolder
import dev.krtirtho.spotube.resources.iconsax.IconsaxFolderAdd
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxTrash
import dev.krtirtho.spotube.resources.iconsax.IconsaxVerticalSetting
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_action_cancel
import spotube.composeapp.generated.resources.settings_action_save
import spotube.composeapp.generated.resources.settings_download_folder_current
import spotube.composeapp.generated.resources.settings_download_folder_default
import spotube.composeapp.generated.resources.settings_download_folder_title
import spotube.composeapp.generated.resources.settings_download_format_subtitle_current
import spotube.composeapp.generated.resources.settings_download_format_title
import spotube.composeapp.generated.resources.settings_download_quality_title
import spotube.composeapp.generated.resources.settings_local_media_folders_add_action
import spotube.composeapp.generated.resources.settings_local_media_folders_description
import spotube.composeapp.generated.resources.settings_local_media_folders_manage
import spotube.composeapp.generated.resources.settings_local_media_folders_none_added
import spotube.composeapp.generated.resources.settings_local_media_folders_remove_action
import spotube.composeapp.generated.resources.settings_local_media_folders_subtitle_current
import spotube.composeapp.generated.resources.settings_local_media_folders_subtitle_empty
import spotube.composeapp.generated.resources.settings_local_media_folders_title
import spotube.composeapp.generated.resources.settings_section_downloads
import spotube.composeapp.generated.resources.settings_subtitle_current

internal fun LazyListScope.downloadsSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
) {
    val downloadFormats = availableAudioFormats(settings.downloadMusicFormat, downloadFormatPresets)
    val downloadQualities = availableAudioQualities(
        format = settings.downloadMusicFormat,
        current = settings.downloadMusicQuality,
    )

    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_downloads)
    }
    settingsSectionCard(
        items = listOf(
            {
                DownloadFolderSettingCard(
                    folder = settings.overloadedDownloadFolder,
                    onFolderSelected = { folder ->
                        settingsViewModel.updateSettings {
                            copy(overloadedDownloadFolder = folder)
                        }
                    }
                )
            },
            {
                LocalMediaFoldersSettingCard(
                    folders = settings.localMediaFolders,
                    onFoldersSaved = { folders ->
                        settingsViewModel.updateSettings {
                            copy(localMediaFolders = folders)
                        }
                    }
                )
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_download_format_title),
                    subtitle = stringResource(
                        Res.string.settings_download_format_subtitle_current,
                        settings.downloadMusicFormat.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxMusicPlay, stringResource(Res.string.settings_download_format_title))
                    },
                    selectedOption = settings.downloadMusicFormat,
                    options = downloadFormats,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { format ->
                        settingsViewModel.updateSettings {
                            copy(
                                downloadMusicFormat = format,
                                downloadMusicQuality = format.resolveQuality(downloadMusicQuality),
                            )
                        }
                    }
                )
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_download_quality_title),
                    subtitle = stringResource(
                        Res.string.settings_subtitle_current,
                        settings.downloadMusicQuality.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(Iconsax.IconsaxVerticalSetting, stringResource(Res.string.settings_download_quality_title))
                    },
                    selectedOption = settings.downloadMusicQuality,
                    options = downloadQualities,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { quality ->
                        settingsViewModel.updateSettings {
                            copy(downloadMusicQuality = quality)
                        }
                    }
                )
            },
        )
    )
}

@Composable
private fun DownloadFolderSettingCard(
    folder: String?,
    onFolderSelected: (String?) -> Unit,
) {
    val pickerLauncher = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            val normalized = normalizePath(directory.path)
            if (normalized.isNotEmpty()) {
                onFolderSelected(normalized)
            }
        }
    }

    SettingCardItem(
        title = stringResource(Res.string.settings_download_folder_title),
        subtitle = folder?.let {
            stringResource(Res.string.settings_download_folder_current, it)
        } ?: stringResource(Res.string.settings_download_folder_default),
        icon = {
            SettingsItemIcon(Iconsax.IconsaxDirectboxReceive, stringResource(Res.string.settings_download_folder_title))
        },
        trailingContent = {
            GhostIconButton(onClick = { pickerLauncher.launch() }) {
                Icon(
                    imageVector = Iconsax.IconsaxFolderAdd,
                    contentDescription = stringResource(Res.string.settings_download_folder_title)
                )
            }
        },
        onClick = {
            pickerLauncher.launch()
        }
    )
}

@Composable
private fun LocalMediaFoldersSettingCard(
    folders: List<String>,
    onFoldersSaved: (List<String>) -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    val subtitlePreview = when {
        folders.isEmpty() -> stringResource(Res.string.settings_local_media_folders_subtitle_empty)
        folders.size == 1 -> folders.first()
        else -> stringResource(
            Res.string.settings_local_media_folders_subtitle_current,
            folders.size,
        )
    }

    SettingCardItem(
        title = stringResource(Res.string.settings_local_media_folders_title),
        subtitle = subtitlePreview,
        icon = {
            SettingsItemIcon(
                Iconsax.IconsaxFolder,
                stringResource(Res.string.settings_local_media_folders_title),
            )
        },
        trailingContent = {
            OutlineButton(onClick = { isDialogOpen = true }) {
                Text(stringResource(Res.string.settings_local_media_folders_manage))
            }
        },
        onClick = {
            isDialogOpen = true
        }
    )

    if (isDialogOpen) {
        LocalMediaFoldersDialog(
            folders = folders,
            onDismiss = { isDialogOpen = false },
            onFoldersSaved = onFoldersSaved,
        )
    }
}

@Composable
private fun LocalMediaFoldersDialog(
    folders: List<String>,
    onDismiss: () -> Unit,
    onFoldersSaved: (List<String>) -> Unit,
) {
    val draftFolders = remember(folders) {
        mutableStateListOf<String>().apply { addAll(folders) }
    }
    val pickerLauncher = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            val normalized = normalizePath(directory.path)
            if (normalized.isNotEmpty() && draftFolders.none { normalizePath(it) == normalized }) {
                draftFolders.add(normalized)
            }
        }
    }

    ThemedDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_local_media_folders_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            PrimaryButton(
                onClick = {
                    onFoldersSaved(draftFolders.map(::normalizePath).filter { it.isNotBlank() }.distinct())
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.settings_action_save))
            }
            OutlineButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_action_cancel))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.settings_local_media_folders_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlineButton(onClick = { pickerLauncher.launch() }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Iconsax.IconsaxAddSquare, contentDescription = null)
                    Text(stringResource(Res.string.settings_local_media_folders_add_action))
                }
            }

            if (draftFolders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_local_media_folders_none_added),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    draftFolders.forEachIndexed { index, folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = folder,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GhostIconButton(
                                onClick = {
                                    if (index in draftFolders.indices) {
                                        draftFolders.removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Iconsax.IconsaxTrash,
                                    contentDescription = stringResource(
                                        Res.string.settings_local_media_folders_remove_action,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

