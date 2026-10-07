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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import compose.icons.FeatherIcons
import compose.icons.feathericons.Folder
import compose.icons.feathericons.HardDrive
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.modules.settings.SettingsViewModel
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.modules.settings.components.SwitchSettingCard
import dev.krtirtho.spotube.modules.settings.components.TextInputSettingCard
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxDocumentDownload
import dev.krtirtho.spotube.resources.iconsax.IconsaxFolder
import dev.krtirtho.spotube.resources.iconsax.IconsaxFolderAdd
import dev.krtirtho.spotube.resources.iconsax.IconsaxStopCircle
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.cacheSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel,
    includeHeader: Boolean = true,
) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_caching)
    }
    settingsSectionCard(
        items = listOf(
            {
                SwitchSettingCard(
                    title = stringResource(Res.string.settings_enable_music_caching_title),
                    subtitle = stringResource(Res.string.settings_enable_music_caching_subtitle),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxDocumentDownload,
                            stringResource(Res.string.settings_enable_music_caching_title)
                        )
                    },
                    checked = settings.enableMusicCaching,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateSettings {
                            copy(enableMusicCaching = enabled)
                        }
                    }
                )
            },
            {
                CacheFolderSettingCard(
                    enabled = settings.enableMusicCaching,
                    folder = settings.cacheFolder,
                    onFolderSelected = { folder ->
                        settingsViewModel.updateSettings {
                            copy(cacheFolder = folder)
                        }
                    }
                )
            },
            {
                val error_whole_number = stringResource(Res.string.settings_error_whole_number)
                val error_cache_range = stringResource(Res.string.settings_error_cache_size_range)

                TextInputSettingCard(
                    enabled = settings.enableMusicCaching,
                    title = stringResource(Res.string.settings_cache_size_limit_title),
                    subtitle = when {
                        settings.cacheSizeLimitMB <= 0L -> stringResource(Res.string.settings_cache_size_limit_unlimited)
                        else -> stringResource(
                            Res.string.settings_cache_size_limit_current,
                            settings.cacheSizeLimitMB
                        )
                    },
                    icon = {
                        SettingsItemIcon(
                            Iconsax.IconsaxStopCircle,
                            stringResource(Res.string.settings_cache_size_limit_title)
                        )
                    },
                    value = when {
                        settings.cacheSizeLimitMB <= 0L -> ""
                        else -> settings.cacheSizeLimitMB.toString()
                    },
                    dialogDescription = stringResource(Res.string.settings_cache_size_limit_description),
                    placeholder = stringResource(Res.string.settings_cache_size_limit_placeholder),
                    normalize = { it.trim() },
                    validate = { value ->
                        if (value.isBlank()) null
                        else {
                            val mb = value.toLongOrNull()
                            when {
                                mb == null -> error_whole_number
                                mb < 0L -> error_cache_range
                                else -> null
                            }
                        }
                    },
                    onValueSaved = { value ->
                        settingsViewModel.updateSettings {
                            copy(cacheSizeLimitMB = value.toLongOrNull()?.coerceAtLeast(0L) ?: 0L)
                        }
                    }
                )
            },
        )
    )
}

@Composable
private fun CacheFolderSettingCard(
    enabled: Boolean,
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
        title = stringResource(Res.string.settings_cache_folder_title),
        subtitle = folder?.let {
            stringResource(Res.string.settings_cache_folder_current, it)
        } ?: stringResource(Res.string.settings_cache_folder_default),
        icon = {
            SettingsItemIcon(
                Iconsax.IconsaxFolder,
                stringResource(Res.string.settings_cache_folder_title)
            )
        },
        trailingContent = {
            GhostIconButton(
                onClick = { pickerLauncher.launch() },
                enabled = enabled,
            ) {
                Icon(
                    Iconsax.IconsaxFolderAdd,
                    contentDescription = stringResource(Res.string.settings_cache_folder_title)
                )
            }
        },
        onClick = { pickerLauncher.launch() },
        enabled = enabled,
    )
}
