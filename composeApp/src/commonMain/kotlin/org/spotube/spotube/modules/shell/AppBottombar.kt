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

package org.spotube.spotube.modules.shell

import org.spotube.spotube.core.ui.component.styled
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumBottomNavigationBar
import org.spotube.millennium.composables.MillenniumBottomNavigationItem
import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.spotube.spotube.NavigationItem
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.bottombarTabs
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.downloads.DownloadBadgeIndicator

@Composable
fun AppBottombar(
    navigator: Navigator,
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    libraryBadge: @Composable () -> Unit = { DownloadBadgeIndicator() }
) {
    // Modern: the bar floats as a pill above the system navigation area.
    val modern = AppTheme.isModern
    MillenniumBottomNavigationBar(
        modifier = if (modern) {
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)
        } else modifier,
        windowInsets = if (modern) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
        minimumHeight = if (modern) 64.dp else null
    ) {
        bottombarTabs.filterIsInstance<NavigationItem.Tab<*>>()
            .forEach { tab ->
                val selected = navigationState.topLevelRoute == tab.route
                val title = stringResource(tab.title)
                MillenniumBottomNavigationItem(
                    selected = selected,
                    onClick = { navigator.navigate(tab.route) },
                    contentDescription = title,
                    icon = {
                        MillenniumIcon(
                            imageVector = tab.icon.styled(),
                            contentDescription = null,
                            tint = LocalTextStyle.current.color
                        )
                    },
                    label = { Text(title) },
                    badge = if (tab.route == Routes.Library) {
                        libraryBadge
                    } else {
                        null
                    }
                )
            }
    }
}
