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

package org.spotube.spotube.core.ui.component


import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import androidx.compose.foundation.layout.RowScope

import androidx.compose.runtime.Composable
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.resources.iconsax.ArrowLeft3
import org.spotube.spotube.resources.iconsax.Iconsax
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

@Composable
fun ApplicationBackButton() {
    val navigationCommands: NavigationCommands = koinInject()

    MillenniumIconButton(
        onClick = {
            navigationCommands.pop()
        },
        style = MillenniumIconButtonStyle.Quiet) {
        MillenniumIcon(
            imageVector = Iconsax.ArrowLeft3,
            contentDescription = stringResource(Res.string.common_back)
        )
    }
}

@Composable
expect fun ApplicationMainBar(
    title: @Composable () -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    backButton: Boolean = true,
    transparent: Boolean = false)
