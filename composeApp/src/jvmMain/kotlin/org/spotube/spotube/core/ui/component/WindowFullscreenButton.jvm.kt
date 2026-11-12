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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowPlacement
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxFullscreen
import org.spotube.spotube.resources.iconsax.IconsaxFullscreenExit
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.app_window_enter_fullscreen
import spotube.composeapp.generated.resources.app_window_exit_fullscreen

@Composable
actual fun WindowFullscreenButton() {
    val windowState = LocalWindowState.current
    // Where to go back to on exit; only set while this button put the window in full screen
    var placementBeforeFullscreen by remember { mutableStateOf<WindowPlacement?>(null) }
    val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

    // Full screen belongs to the screen that turned it on, so leaving that screen leaves it too
    DisposableEffect(windowState) {
        onDispose {
            val previous = placementBeforeFullscreen
            if (previous != null && windowState.placement == WindowPlacement.Fullscreen) {
                windowState.placement = previous
            }
        }
    }

    MillenniumIconButton(
        onClick = {
            if (isFullscreen) {
                windowState.placement = placementBeforeFullscreen ?: WindowPlacement.Floating
                placementBeforeFullscreen = null
            } else {
                placementBeforeFullscreen = windowState.placement
                windowState.placement = WindowPlacement.Fullscreen
            }
        },
        style = MillenniumIconButtonStyle.Quiet
    ) {
        MillenniumIcon(
            imageVector = if (isFullscreen) Iconsax.IconsaxFullscreenExit else Iconsax.IconsaxFullscreen,
            contentDescription = stringResource(
                if (isFullscreen) Res.string.app_window_exit_fullscreen else Res.string.app_window_enter_fullscreen
            )
        )
    }
}
