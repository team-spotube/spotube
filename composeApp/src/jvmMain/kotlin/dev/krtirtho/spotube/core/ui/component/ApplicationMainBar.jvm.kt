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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import dev.krtirtho.spotube.core.systemtray.SystemTrayService
import dev.krtirtho.spotube.core.ui.base.LocalBaseUIColors
import dev.krtirtho.spotube.modules.settings.SettingsProvider
import dev.krtirtho.spotube.resources.iconsax.FluentDismiss
import dev.krtirtho.spotube.resources.iconsax.FluentMaximize
import dev.krtirtho.spotube.resources.iconsax.FluentMinus
import dev.krtirtho.spotube.resources.iconsax.FluentSquareMultiple
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.nucleusframework.window.WindowControls
import dev.nucleusframework.window.WindowControlsRenderer
import dev.nucleusframework.window.WindowDoubleClickAction
import dev.nucleusframework.window.windowDragArea
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun ApplicationMainBar(
    title: @Composable (() -> Unit),
    subtitle: @Composable () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    backButton: Boolean,
    transparent: Boolean,
) {
    val colors = LocalBaseUIColors.current
    Box(
        modifier = Modifier.fillMaxWidth()
            .windowDragArea(doubleClickAction = WindowDoubleClickAction.ToggleMaximize)
    ) {
        TopAppBar(
            title = title,
            actions = {
                actions()
            },
            navigationIcon = {
                if (backButton) {
                    ApplicationBackButton()
                }
            },
            subtitle = subtitle,
            colors = if (transparent) {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = colors.onSurface,
                    actionIconContentColor = colors.onSurfaceVariant,
                    navigationIconContentColor = colors.onSurfaceVariant,
                )
            } else {
                // The app bar follows the selected surface theme rather than Material's.
                TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surfaceContainer,
                    titleContentColor = colors.onSurface,
                    actionIconContentColor = colors.onSurfaceVariant,
                    navigationIconContentColor = colors.onSurfaceVariant,
                )
            },
            modifier = Modifier.padding(end = 125.dp) // To avoid overlap with window buttons
        )
        LocalWindowScope.current.WindowControls(
            modifier = Modifier.align(Alignment.TopEnd).heightIn(max = 48.dp),
            renderer = WindowControlsRenderer.Platform, // default
        )
    }
}

@Composable
private fun WindowButtons(modifier: Modifier = Modifier) {
    val windowState = LocalWindowState.current
    val applicationScope = LocalApplicationScope.current
    val settingsProvider = koinInject<SettingsProvider>()
    val trayService = koinInject<SystemTrayService>()
    val settings by settingsProvider.settingsState.collectAsState(initial = null)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { windowState.isMinimized = true }) {
            Icon(Iconsax.FluentMinus, "Minimize", modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = {
            windowState.placement = if (windowState.placement == WindowPlacement.Maximized)
                WindowPlacement.Floating else WindowPlacement.Maximized
        }) {
            if (windowState.placement == WindowPlacement.Floating) {
                Icon(Iconsax.FluentMaximize, "Maximize", modifier = Modifier.size(14.dp))
            } else {
                Icon(Iconsax.FluentSquareMultiple, "Restore", modifier = Modifier.size(14.dp))
            }
        }
        IconButton(onClick = {
            if (settings?.minimizeToTray == true) {
                trayService.hideWindow()
            } else {
                applicationScope.exitApplication()
            }
        }) {
            Icon(Iconsax.FluentDismiss, "Close", modifier = Modifier.size(14.dp))
        }
    }
}
