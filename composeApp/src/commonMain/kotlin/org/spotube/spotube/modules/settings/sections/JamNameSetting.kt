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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.jam.JamNames
import org.spotube.spotube.modules.settings.components.TextInputSettingCard
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxEdit
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_name_description
import spotube.composeapp.generated.resources.jam_name_subtitle
import spotube.composeapp.generated.resources.jam_name_title
import spotube.composeapp.generated.resources.jam_name_too_long

/**
 * "Your name" card for Group Jam. Shared by the Jam screen and Jam settings so both stay
 * identical, like the remote control device name card.
 *
 * [customName] is what the user saved (blank = none), [defaultName] the generated one.
 */
@Composable
internal fun JamNameSettingCard(
    customName: String,
    defaultName: String,
    onNameSaved: (String) -> Unit,
    enabled: Boolean = true
) {
    val tooLong = stringResource(Res.string.jam_name_too_long, JamNames.MAX_LENGTH)
    TextInputSettingCard(
        title = stringResource(Res.string.jam_name_title),
        subtitle = stringResource(Res.string.jam_name_subtitle, customName.ifBlank { defaultName }),
        icon = { SettingsItemIcon(Iconsax.IconsaxEdit, stringResource(Res.string.jam_name_title)) },
        value = customName,
        dialogDescription = stringResource(Res.string.jam_name_description, defaultName),
        placeholder = defaultName,
        normalize = JamNames::normalize,
        validate = { value -> if (value.length > JamNames.MAX_LENGTH) tooLong else null },
        onValueSaved = onNameSaved,
        enabled = enabled
    )
}
