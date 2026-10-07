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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.PlatformType
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.component.SidebarItem
import dev.krtirtho.spotube.modules.blacklist.BlacklistContent
import dev.krtirtho.spotube.modules.plugin.PluginScreenContent
import dev.krtirtho.spotube.modules.settings.sections.appearanceSection
import dev.krtirtho.spotube.modules.settings.sections.cacheSection
import dev.krtirtho.spotube.modules.settings.sections.desktopSection
import dev.krtirtho.spotube.modules.settings.sections.downloadsSection
import dev.krtirtho.spotube.modules.settings.sections.languageRegionSection
import dev.krtirtho.spotube.modules.settings.sections.playbackSection
import dev.krtirtho.spotube.modules.settings.sections.SettingsPageTitle
import dev.krtirtho.spotube.modules.settings.sections.updatesSection
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxBoxAdd
import dev.krtirtho.spotube.resources.iconsax.IconsaxCloseSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxColorSwatch
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxDocumentDownload
import dev.krtirtho.spotube.resources.iconsax.IconsaxForbidden
import dev.krtirtho.spotube.resources.iconsax.IconsaxImportArrow2
import dev.krtirtho.spotube.resources.iconsax.IconsaxInformation
import dev.krtirtho.spotube.resources.iconsax.IconsaxLanguageSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxSoundTwotone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_blacklist_title
import spotube.composeapp.generated.resources.settings_screen_title
import spotube.composeapp.generated.resources.settings_section_about
import spotube.composeapp.generated.resources.settings_section_appearance
import spotube.composeapp.generated.resources.settings_section_caching
import spotube.composeapp.generated.resources.settings_section_desktop
import spotube.composeapp.generated.resources.settings_section_downloads
import spotube.composeapp.generated.resources.settings_section_jam
import spotube.composeapp.generated.resources.settings_section_language_region
import spotube.composeapp.generated.resources.settings_section_playback
import spotube.composeapp.generated.resources.settings_section_plugins

private val SettingsSidebarWidth = 236.dp

/**
 * The sections reachable from the large-screen settings sidebar, in display order.
 *
 * Each entry maps to either a list-based settings section or a standalone
 * screen (Plugins, Blacklist, Group Jam) whose content is embedded in the tab.
 */
internal enum class SettingsTab(
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    Plugins(Res.string.settings_section_plugins, Iconsax.IconsaxBoxAdd),
    LanguageRegion(Res.string.settings_section_language_region, Iconsax.IconsaxLanguageSquare),
    Appearance(Res.string.settings_section_appearance, Iconsax.IconsaxColorSwatch),
    Playback(Res.string.settings_section_playback, Iconsax.IconsaxMusicPlay),
    Blacklist(Res.string.settings_blacklist_title, Iconsax.IconsaxForbidden),
    GroupJam(Res.string.settings_section_jam, Iconsax.IconsaxSoundTwotone),
    Caching(Res.string.settings_section_caching, Iconsax.IconsaxDocumentDownload),
    Downloads(Res.string.settings_section_downloads, Iconsax.IconsaxDirectboxReceive),
    Desktop(Res.string.settings_section_desktop, Iconsax.IconsaxImportArrow2),
    About(Res.string.settings_section_about, Iconsax.IconsaxInformation),
}

/**
 * Large-screen settings, presented as a modal overlay (dialog) over the whole app so it
 * never sits next to the main navigation sidebar.
 */
@Composable
internal fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // The dialog is its own surface; the player/bottom bars behind it should not
        // reserve any inset inside the dialog content.
        CompositionLocalProvider(LocalAppShellBottomInset provides 0.dp) {
            SettingsPane(
                settingsViewModel = settingsViewModel,
                onClose = onDismiss,
            )
        }
    }
}

@Composable
private fun SettingsPane(
    settingsViewModel: SettingsViewModel,
    onClose: () -> Unit,
) {
    val navigatorCommands: NavigationCommands = koinInject()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val contentPadding = remember { PaddingValues(top = 16.dp, bottom = 24.dp) }
    val dialogTheme = LocalBaseUITheme.current.dialog

    val platformType = remember { getPlatform().type }
    val isDesktopPlatform = platformType == PlatformType.Windows ||
            platformType == PlatformType.Linux ||
            platformType == PlatformType.MacOS

    val tabs = remember(isDesktopPlatform) {
        SettingsTab.entries.filter { it != SettingsTab.Desktop || isDesktopPlatform }
    }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = tabs.getOrElse(selectedIndex) { tabs.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .heightIn(max = 860.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .shadow(
                    elevation = dialogTheme.shadow.elevation,
                    shape = dialogTheme.shape,
                    ambientColor = dialogTheme.shadow.ambientColor,
                    spotColor = dialogTheme.shadow.spotColor,
                )
                .clip(dialogTheme.shape)
                .background(dialogTheme.background, dialogTheme.shape)
                .border(
                    BorderStroke(dialogTheme.border.width, dialogTheme.border.color),
                    dialogTheme.shape,
                ),
        ) {
            SettingsSidebar(
                tabs = tabs,
                selectedTab = selectedTab,
                onSelect = { selectedIndex = tabs.indexOf(it) },
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsPageTitle(selectedTab.titleRes)
                    }
                    GhostIconButton(
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = "Close",
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    SettingsTabContent(
                        tab = selectedTab,
                        settings = settingsState,
                        settingsViewModel = settingsViewModel,
                        navigatorCommands = navigatorCommands,
                        requestLocalNetworkPermission = requestLocalNetworkPermission,
                        contentPadding = contentPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebar(
    tabs: List<SettingsTab>,
    selectedTab: SettingsTab,
    onSelect: (SettingsTab) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(SettingsSidebarWidth)
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.settings_screen_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)
            )
        }
        items(tabs, key = { it.name }) { tab ->
            SidebarItem(
                label = stringResource(tab.titleRes),
                icon = tab.icon,
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun SettingsTabContent(
    tab: SettingsTab,
    settings: UserSettings?,
    settingsViewModel: SettingsViewModel,
    navigatorCommands: NavigationCommands,
    requestLocalNetworkPermission: () -> Unit,
    contentPadding: PaddingValues,
) {
    when (tab) {
        SettingsTab.Plugins -> {
            PluginScreenContent(viewModel = koinViewModel())
        }

        SettingsTab.Blacklist -> {
            BlacklistContent(viewModel = koinViewModel())
        }

        SettingsTab.GroupJam -> {
            JamSettingsContent(viewModel = koinViewModel())
        }

        else -> {
            if (settings == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 1280.dp)
                            .align(Alignment.TopCenter),
                        contentPadding = contentPadding,
                    ) {
                        when (tab) {
                            SettingsTab.LanguageRegion ->
                                languageRegionSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Appearance ->
                                appearanceSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Playback ->
                                playbackSection(
                                    settings = settings,
                                    settingsViewModel = settingsViewModel,
                                    navigatorCommands = navigatorCommands,
                                    requestLocalNetworkPermission = requestLocalNetworkPermission,
                                    includeBlacklist = false,
                                    includeHeader = false,
                                )

                            SettingsTab.Caching ->
                                cacheSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Downloads ->
                                downloadsSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Desktop ->
                                desktopSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.About ->
                                updatesSection(settings, settingsViewModel, includeHeader = false)
                        }
                    }
                }
            }
        }
    }
}
