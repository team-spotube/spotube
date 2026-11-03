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

package org.spotube.spotube.modules.settings


import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import org.spotube.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import spotube.composeapp.generated.resources.*
import org.spotube.spotube.modules.settings.sections.appearanceSection
import org.spotube.spotube.modules.settings.sections.cacheSection
import org.spotube.spotube.modules.settings.sections.desktopSection
import org.spotube.spotube.modules.settings.sections.downloadsSection
import org.spotube.spotube.modules.settings.sections.jamSection
import org.spotube.spotube.modules.settings.sections.languageRegionSection
import org.spotube.spotube.modules.settings.sections.playbackSection
import org.spotube.spotube.modules.settings.sections.pluginsSection
import org.spotube.spotube.modules.settings.sections.supportSection
import org.spotube.spotube.modules.settings.sections.updatesSection
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Minimum width of the settings content area required to show the settings as an
 * overlay dialog instead of a full-screen destination.
 */
private val SettingsDialogMinWidth = 720.dp

/**
 * On large screens settings is presented as a modal overlay so it does not sit next to
 * the main navigation sidebar. On smaller screens it stays a full-screen destination.
 */
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val navigatorCommands: NavigationCommands = koinInject()

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded) {
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                onDismiss = { navigatorCommands.pop(Routes.Settings) })
        } else {
            CompactSettingsScreen(settingsViewModel = settingsViewModel)
        }
    }
}

@Composable
private fun CompactSettingsScreen(settingsViewModel: SettingsViewModel) {
    val navigatorCommands = koinInject<NavigationCommands>()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationMainBar(
            title = {
                Text(stringResource(Res.string.settings_screen_title))
            },
            backButton = false
        )
        CompactSettingsContent(
            settings = settingsState,
            settingsViewModel = settingsViewModel,
            navigatorCommands = navigatorCommands,
            requestLocalNetworkPermission = requestLocalNetworkPermission,
            contentPadding = contentPadding,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CompactSettingsContent(
    settings: UserSettings?,
    settingsViewModel: SettingsViewModel,
    navigatorCommands: NavigationCommands,
    requestLocalNetworkPermission: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier) {
    val platformType = remember { getPlatform().type }
    val isDesktopPlatform = platformType == PlatformType.Windows ||
            platformType == PlatformType.Linux ||
            platformType == PlatformType.MacOS

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .align(Alignment.TopCenter),
            contentPadding = contentPadding) {
            pluginsSection(
                navigatorCommands = navigatorCommands)
            if (settings != null) {
                languageRegionSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
                appearanceSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
                playbackSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    navigatorCommands = navigatorCommands,
                    requestLocalNetworkPermission = requestLocalNetworkPermission)
                jamSection(
                    settings = settings,
                    navigationCommands = navigatorCommands)
                cacheSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
                downloadsSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
                if (isDesktopPlatform) {
                    desktopSection(
                        settings = settings,
                        settingsViewModel = settingsViewModel)
                }
                supportSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
                updatesSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel)
            }
        }
    }
}
