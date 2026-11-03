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

package org.spotube.spotube.modules.settings.sections


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import androidx.compose.foundation.lazy.LazyListScope

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.LocalMediaFoldersViewModel
import org.spotube.spotube.modules.settings.components.LocalMediaFoldersDialogHost
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.spotube.modules.settings.components.SelectionSettingCard
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxFolder
import org.spotube.spotube.resources.iconsax.IconsaxFolderAdd
import org.spotube.spotube.resources.iconsax.IconsaxMusicPlay
import org.spotube.spotube.resources.iconsax.IconsaxVerticalSetting
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_download_folder_current
import spotube.composeapp.generated.resources.settings_download_folder_default_path
import spotube.composeapp.generated.resources.settings_download_folder_title
import spotube.composeapp.generated.resources.settings_download_format_subtitle_current
import spotube.composeapp.generated.resources.settings_download_format_title
import spotube.composeapp.generated.resources.settings_download_quality_title
import spotube.composeapp.generated.resources.settings_local_media_folders_manage
import spotube.composeapp.generated.resources.settings_local_media_folders_subtitle_current
import spotube.composeapp.generated.resources.settings_local_media_folders_subtitle_empty
import spotube.composeapp.generated.resources.settings_local_media_folders_title
import spotube.composeapp.generated.resources.settings_section_downloads
import spotube.composeapp.generated.resources.settings_subtitle_current

internal fun LazyListScope.downloadsSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true
) {
    val downloadFormats = availableAudioFormats(settings.downloadMusicFormat, downloadFormatPresets)
    val downloadQualities = availableAudioQualities(
        format = settings.downloadMusicFormat,
        current = settings.downloadMusicQuality
    )

    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_downloads)
    }
    settingsSectionCard(
        items = listOf(
            {
                DownloadFolderSettingCard(
                    folder = settings.overloadedDownloadFolder,
                    defaultFolder = settingsViewModel.defaultDownloadFolder,
                    onFolderSelected = { folder ->
                        settingsViewModel.updateSettings {
                            copy(overloadedDownloadFolder = folder)
                        }
                    }
                )
            },
            {
                LocalMediaFoldersSettingCard()
            },
            {
                SelectionSettingCard(
                    title = stringResource(Res.string.settings_download_format_title),
                    subtitle = stringResource(
                        Res.string.settings_download_format_subtitle_current,
                        settings.downloadMusicFormat.displayLabel()
                    ),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxMusicPlay,
                            stringResource(Res.string.settings_download_format_title)
                        )
                    },
                    selectedOption = settings.downloadMusicFormat,
                    options = downloadFormats,
                    optionLabel = { it.displayLabel() },
                    onOptionSelected = { format ->
                        settingsViewModel.updateSettings {
                            copy(
                                downloadMusicFormat = format,
                                downloadMusicQuality = format.resolveQuality(downloadMusicQuality)
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
                        SettingsItemIcon(
                            Iconsax.IconsaxVerticalSetting,
                            stringResource(Res.string.settings_download_quality_title)
                        )
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
            })
    )
}

@Composable
private fun DownloadFolderSettingCard(
    folder: String?,
    defaultFolder: String,
    onFolderSelected: (String?) -> Unit
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
        } ?: stringResource(Res.string.settings_download_folder_default_path, defaultFolder),
        icon = {
            SettingsItemIcon(
                Iconsax.IconsaxDirectboxReceive,
                stringResource(Res.string.settings_download_folder_title)
            )
        },
        trailingContent = {
            MillenniumIconButton(
                icon = Iconsax.IconsaxFolderAdd,
                contentDescription = stringResource(Res.string.settings_download_folder_title),
                onClick = { pickerLauncher.launch() },
                style = MillenniumIconButtonStyle.Quiet
            )
        },
        onClick = {
            pickerLauncher.launch()
        }
    )
}

@Composable
private fun LocalMediaFoldersSettingCard(
    viewModel: LocalMediaFoldersViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subtitlePreview = state.singleConfiguredPath ?: if (state.configuredCount == 0) {
        stringResource(Res.string.settings_local_media_folders_subtitle_empty)
    } else stringResource(
        Res.string.settings_local_media_folders_subtitle_current,
        state.configuredCount
    )

    SettingCardItem(
        title = stringResource(Res.string.settings_local_media_folders_title),
        subtitle = subtitlePreview,
        icon = {
            SettingsItemIcon(
                Iconsax.IconsaxFolder,
                stringResource(Res.string.settings_local_media_folders_title)
            )
        },
        trailingContent = {
            MillenniumButton(
                onClick = viewModel::open,
                style = MillenniumButtonStyle.Secondary
            ) {
                Text(stringResource(Res.string.settings_local_media_folders_manage))
            }
        },
        onClick = viewModel::open
    )

    LocalMediaFoldersDialogHost(viewModel)
}
