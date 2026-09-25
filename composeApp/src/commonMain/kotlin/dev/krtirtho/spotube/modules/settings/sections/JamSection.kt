/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.krtirtho.spotube.modules.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.settings.components.SettingCardItem
import dev.krtirtho.spotube.resources.iconsax.CustomServer
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_jam_broker_host_subtitle
import spotube.composeapp.generated.resources.settings_jam_broker_title
import spotube.composeapp.generated.resources.settings_section_jam

internal fun LazyListScope.jamSection(
    settings: UserSettings,
    navigationCommands: NavigationCommands,
) {
    settingsSectionHeader(Res.string.settings_section_jam)
    settingsSectionCard(
        items = listOf(
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_jam_broker_title),
                    subtitle = stringResource(
                        Res.string.settings_jam_broker_host_subtitle,
                        settings.jamBroker.host.ifBlank { "—" },
                        settings.jamBroker.port,
                    ),
                    icon = {
                        SettingsItemIcon(
                            Iconsax.CustomServer,
                            stringResource(Res.string.settings_jam_broker_title),
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = FeatherIcons.ChevronRight,
                            contentDescription = stringResource(Res.string.settings_jam_broker_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { navigationCommands.navigateTo(Routes.JamSettings) },
                )
            },
        )
    )
}
