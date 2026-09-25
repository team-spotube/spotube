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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val navigatorCommands = koinInject<NavigationCommands>()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val platformType = remember { getPlatform().type }
    val isDesktopPlatform = platformType == PlatformType.Windows ||
            platformType == PlatformType.Linux ||
            platformType == PlatformType.MacOS

    val shellBottomInset = LocalAppShellBottomInset.current
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    Scaffold(
        topBar = {
            ApplicationMainBar(
                title = {
                    Text(stringResource(Res.string.settings_screen_title))
                },
                backButton = false
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1280.dp)
                    .align(Alignment.TopCenter),
                contentPadding = contentPadding,
            ) {
                pluginsSection(
                    navigatorCommands = navigatorCommands,
                )
                if (settingsState != null) {
                    languageRegionSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
                }
                if (settingsState != null)
                    appearanceSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
                if (settingsState != null)
                    playbackSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                        navigatorCommands = navigatorCommands,
                        requestLocalNetworkPermission = requestLocalNetworkPermission,
                    )
                if (settingsState != null)
                    jamSection(
                        settings = settingsState!!,
                        navigationCommands = navigatorCommands,
                    )
                if (settingsState != null)
                    cacheSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
                if (settingsState != null)
                    downloadsSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
                if (isDesktopPlatform && settingsState != null) {
                    desktopSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
                }
                if (settingsState != null)
                    updatesSection(
                        settings = settingsState!!,
                        settingsViewModel = settingsViewModel,
                    )
            }

        }
    }
}
