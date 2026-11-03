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


import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.foundation.lazy.LazyListScope

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Package
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import spotube.composeapp.generated.resources.*
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxBoxAdd
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.pluginsSection(
    navigatorCommands: NavigationCommands
) {
    settingsSectionHeader(Res.string.settings_section_plugins)
    settingsSectionCard(
        items = listOf {
            SettingCardItem(
                title = stringResource(Res.string.settings_plugins_manage_title),
                subtitle = stringResource(Res.string.settings_plugins_manage_subtitle),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxBoxAdd,
                        stringResource(Res.string.settings_section_plugins),
                        Color(0xFFFF9800)
                    )
                },
                trailingContent = {
                    MillenniumIcon(
                        imageVector = FeatherIcons.ChevronRight,
                        contentDescription = stringResource(Res.string.settings_plugins_manage_title),
                        tint = AppTheme.colors.onSurfaceVariant
                    )
                },
                onClick = {
                    navigatorCommands.navigateTo(Routes.Plugins)
                }
            )
        }
    )
}
