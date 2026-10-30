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

import androidx.compose.foundation.lazy.LazyListScope
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDocumentText
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_section_logs
import spotube.composeapp.generated.resources.settings_logs_title
import spotube.composeapp.generated.resources.settings_logs_subtitle

internal fun LazyListScope.logsSection(
    navigatorCommands: NavigationCommands,
    includeHeader: Boolean = true
) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_logs)
    }
    settingsSectionCard(
        items = listOf {
            SettingCardItem(
                title = stringResource(Res.string.settings_logs_title),
                subtitle = stringResource(Res.string.settings_logs_subtitle),
                icon = {
                    SettingsItemIcon(
                        imageVector = Iconsax.IconsaxDocumentText,
                        contentDescription = stringResource(Res.string.settings_logs_title)
                    )
                },
                onClick = {
                    navigatorCommands.navigateTo(Routes.Logs)
                }
            )
        }
    )
}
