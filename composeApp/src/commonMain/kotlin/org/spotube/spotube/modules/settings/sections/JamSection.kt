/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package org.spotube.spotube.modules.settings.sections


import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.foundation.lazy.LazyListScope

import org.spotube.spotube.core.ui.theming.AppTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.CustomServer
import org.spotube.spotube.resources.iconsax.Iconsax
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_jam_server_title
import spotube.composeapp.generated.resources.settings_section_jam

internal fun LazyListScope.jamSection(
    settings: UserSettings,
    navigationCommands: NavigationCommands) {
    settingsSectionHeader(Res.string.settings_section_jam)
    settingsSectionCard(
        items = listOf(
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_jam_server_title),
                    subtitle = settings.jamServerUrl,
                    icon = {
                        SettingsItemIcon(
                            Iconsax.CustomServer,
                            stringResource(Res.string.settings_jam_server_title))
                    },
                    trailingContent = {
                        MillenniumIcon(
                            imageVector = FeatherIcons.ChevronRight,
                            contentDescription = stringResource(Res.string.settings_jam_server_title),
                            tint = AppTheme.colors.onSurfaceVariant)
                    },
                    onClick = { navigationCommands.navigateTo(Routes.JamSettings) })
            })
    )
}
