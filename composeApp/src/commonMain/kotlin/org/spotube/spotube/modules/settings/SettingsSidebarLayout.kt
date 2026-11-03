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


import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSidebarItem
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
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

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.PlatformType
import org.spotube.spotube.getPlatform
import org.spotube.spotube.core.discovery.rememberLocalNetworkPermissionRequester
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.modules.blacklist.BlacklistContent
import org.spotube.spotube.modules.plugin.PluginScreenContent
import org.spotube.spotube.modules.settings.sections.appearanceSection
import org.spotube.spotube.modules.settings.sections.cacheSection
import org.spotube.spotube.modules.settings.sections.desktopSection
import org.spotube.spotube.modules.settings.sections.downloadsSection
import org.spotube.spotube.modules.settings.sections.languageRegionSection
import org.spotube.spotube.modules.settings.sections.playbackSection
import org.spotube.spotube.modules.settings.sections.supportSection
import org.spotube.spotube.modules.settings.sections.SettingsPageTitle
import org.spotube.spotube.modules.settings.sections.updatesSection
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxBoxAdd
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxColorSwatch
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxDocumentDownload
import org.spotube.spotube.resources.iconsax.IconsaxForbidden
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxImportArrow2
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxLanguageSquare
import org.spotube.spotube.resources.iconsax.IconsaxMusicPlay
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_close
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
import spotube.composeapp.generated.resources.settings_section_support

private val SettingsSidebarWidth = 236.dp

/**
 * The sections reachable from the large-screen settings sidebar, in display order.
 *
 * Each entry maps to either a list-based settings section or a standalone
 * screen (Plugins, Blacklist, Group Jam) whose content is embedded in the tab.
 */
internal enum class SettingsTab(
    val titleRes: StringResource,
    val icon: ImageVector) {
    Plugins(Res.string.settings_section_plugins, Iconsax.IconsaxBoxAdd),
    LanguageRegion(Res.string.settings_section_language_region, Iconsax.IconsaxLanguageSquare),
    Appearance(Res.string.settings_section_appearance, Iconsax.IconsaxColorSwatch),
    Playback(Res.string.settings_section_playback, Iconsax.IconsaxMusicPlay),
    Blacklist(Res.string.settings_blacklist_title, Iconsax.IconsaxForbidden),
    GroupJam(Res.string.settings_section_jam, Iconsax.IconsaxSoundTwotone),
    Caching(Res.string.settings_section_caching, Iconsax.IconsaxDocumentDownload),
    Downloads(Res.string.settings_section_downloads, Iconsax.IconsaxDirectboxReceive),
    Desktop(Res.string.settings_section_desktop, Iconsax.IconsaxImportArrow2),
    Support(Res.string.settings_section_support, Iconsax.IconsaxHeart2),
    About(Res.string.settings_section_about, Iconsax.IconsaxInformation),
}

/**
 * Large-screen settings, presented as a modal overlay (dialog) over the whole app so it
 * never sits next to the main navigation sidebar.
 */
@Composable
internal fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The dialog is its own surface; the player/bottom bars behind it should not
        // reserve any inset inside the dialog content.
        CompositionLocalProvider(LocalAppShellBottomInset provides 0.dp) {
            SettingsPane(
                settingsViewModel = settingsViewModel,
                onClose = onDismiss)
        }
    }
}

@Composable
private fun SettingsPane(
    settingsViewModel: SettingsViewModel,
    onClose: () -> Unit) {
    val navigatorCommands: NavigationCommands = koinInject()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val requestLocalNetworkPermission = rememberLocalNetworkPermissionRequester()
    val contentPadding = remember { PaddingValues(top = 16.dp, bottom = 24.dp) }

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
        contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .heightIn(max = 860.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .millenniumSurface(
                    background = millenniumPanelBrush(MillenniumTheme.colors),
                    shape = AppTheme.shapes.large,
                    borderColor = MillenniumTheme.colors.border,
                    borderWidth = 1.dp,
                    shadowElevation = 16.dp)) {
            SettingsSidebar(
                tabs = tabs,
                selectedTab = selectedTab,
                onSelect = { selectedIndex = tabs.indexOf(it) })
            MillenniumSeparator(
                modifier = Modifier.fillMaxHeight(),
                vertical = true)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsPageTitle(selectedTab.titleRes)
                    }
                    MillenniumIconButton(
                        onClick = onClose,
                        modifier = Modifier.padding(end = 8.dp),
                        style = MillenniumIconButtonStyle.Quiet) {
                        MillenniumIcon(
                            imageVector = Iconsax.IconsaxCloseSquare,
                            contentDescription = stringResource(Res.string.common_close))
                    }
                }
                MillenniumSeparator()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                    SettingsTabContent(
                        tab = selectedTab,
                        settings = settingsState,
                        settingsViewModel = settingsViewModel,
                        navigatorCommands = navigatorCommands,
                        requestLocalNetworkPermission = requestLocalNetworkPermission,
                        contentPadding = contentPadding)
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebar(
    tabs: List<SettingsTab>,
    selectedTab: SettingsTab,
    onSelect: (SettingsTab) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .width(SettingsSidebarWidth)
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Text(
                stringResource(Res.string.settings_screen_title),
                style = AppTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)
            )
        }
        items(tabs, key = { it.name }) { tab ->
            MillenniumSidebarItem(
                label = stringResource(tab.titleRes),
                onClick = { onSelect(tab) },
                selected = tab == selectedTab,
                contentDescription = stringResource(tab.titleRes),
                icon = tab.icon)
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
    contentPadding: PaddingValues) {
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
                    contentAlignment = Alignment.Center) {
                    MillenniumCircularProgress(size = 40.dp)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 1280.dp)
                            .align(Alignment.TopCenter),
                        contentPadding = contentPadding) {
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
                                    includeHeader = false)

                            SettingsTab.Caching ->
                                cacheSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Downloads ->
                                downloadsSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Desktop ->
                                desktopSection(settings, settingsViewModel, includeHeader = false)

                            SettingsTab.Support ->
                                supportSection(
                                    settings = settings,
                                    settingsViewModel = settingsViewModel,
                                    includeHeader = false)

                            SettingsTab.About ->
                                updatesSection(settings, settingsViewModel, includeHeader = false)
                        }
                    }
                }
            }
        }
    }
}
