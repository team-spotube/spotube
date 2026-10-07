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

package dev.krtirtho.spotube.modules.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.PlatformType
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import spotube.composeapp.generated.resources.*
import dev.krtirtho.spotube.modules.settings.sections.appearanceSection
import dev.krtirtho.spotube.modules.settings.sections.cacheSection
import dev.krtirtho.spotube.modules.settings.sections.desktopSection
import dev.krtirtho.spotube.modules.settings.sections.downloadsSection
import dev.krtirtho.spotube.modules.settings.sections.jamSection
import dev.krtirtho.spotube.modules.settings.sections.languageRegionSection
import dev.krtirtho.spotube.modules.settings.sections.playbackSection
import dev.krtirtho.spotube.modules.settings.sections.pluginsSection
import dev.krtirtho.spotube.modules.settings.sections.updatesSection
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= SettingsDialogMinWidth) {
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                onDismiss = { navigatorCommands.pop(Routes.Settings) },
            )
        } else {
            CompactSettingsScreen(settingsViewModel = settingsViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSettingsScreen(settingsViewModel: SettingsViewModel) {
    val navigatorCommands = koinInject<NavigationCommands>()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val shellBottomInset = LocalAppShellBottomInset.current
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    BaseScaffold(
        topBar = {
            ApplicationMainBar(
                title = {
                    Text(stringResource(Res.string.settings_screen_title))
                },
                backButton = false
            )
        }
    ) { innerPadding ->
        CompactSettingsContent(
            settings = settingsState,
            settingsViewModel = settingsViewModel,
            navigatorCommands = navigatorCommands,
            requestLocalNetworkPermission = requestLocalNetworkPermission,
            contentPadding = contentPadding,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun CompactSettingsContent(
    settings: UserSettings?,
    settingsViewModel: SettingsViewModel,
    navigatorCommands: NavigationCommands,
    requestLocalNetworkPermission: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val platformType = remember { getPlatform().type }
    val isDesktopPlatform = platformType == PlatformType.Windows ||
            platformType == PlatformType.Linux ||
            platformType == PlatformType.MacOS

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .align(Alignment.TopCenter),
            contentPadding = contentPadding,
        ) {
            pluginsSection(
                navigatorCommands = navigatorCommands,
            )
            if (settings != null) {
                languageRegionSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                )
                appearanceSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                )
                playbackSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                    navigatorCommands = navigatorCommands,
                    requestLocalNetworkPermission = requestLocalNetworkPermission,
                )
                jamSection(
                    settings = settings,
                    navigationCommands = navigatorCommands,
                )
                cacheSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                )
                downloadsSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                )
                if (isDesktopPlatform) {
                    desktopSection(
                        settings = settings,
                        settingsViewModel = settingsViewModel,
                    )
                }
                updatesSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
