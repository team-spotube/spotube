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

package org.spotube.spotube

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import org.spotube.spotube.core.DebugConfig
import org.spotube.spotube.core.i18n.AppLocaleProvider
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.navigation.TOP_LEVEL_ROUTES
import org.spotube.spotube.core.navigation.navPopTransitionSpec
import org.spotube.spotube.core.navigation.navPredictivePopTransitionSpec
import org.spotube.spotube.core.navigation.navPushTransitionSpec
import org.spotube.spotube.core.navigation.rememberNavigationState
import org.spotube.spotube.core.navigation.toEntries
import org.spotube.spotube.core.ui.component.LocalSharedTransitionScope
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.settings.SettingsRepository
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.shell.AppShell
import org.spotube.spotube.modules.support.LocalSupportPrompt
import org.spotube.spotube.modules.support.SupportDialog
import org.spotube.spotube.modules.support.SupportReminderIntervalMillis
import org.spotube.spotube.modules.update.AppUpdateDialog
import org.spotube.spotube.modules.update.AppUpdateService
import org.spotube.spotube.modules.update.PluginUpdateDialog
import org.spotube.spotube.modules.update.PluginUpdateService
import org.spotube.spotube.modules.webview.WebViewScreen
import org.spotube.spotube.modules.welcome.WelcomeRepository
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxHome
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.spotube.spotube.resources.iconsax.IconsaxMusicDashboard
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.resources.iconsax.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.Clock
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_screen_title
import spotube.composeapp.generated.resources.library_tab_albums
import spotube.composeapp.generated.resources.library_tab_artists
import spotube.composeapp.generated.resources.library_tab_downloads
import spotube.composeapp.generated.resources.library_tab_local_tracks
import spotube.composeapp.generated.resources.library_tab_playlists
import spotube.composeapp.generated.resources.library_toolbar_compact_title
import spotube.composeapp.generated.resources.nav_devices
import spotube.composeapp.generated.resources.nav_group_connect
import spotube.composeapp.generated.resources.nav_group_on_device
import spotube.composeapp.generated.resources.nav_home
import spotube.composeapp.generated.resources.nav_search
import spotube.composeapp.generated.resources.settings_screen_title

interface NavigationItem {
    data class Tab<T : Any>(
        val title: StringResource, val icon: ImageVector, val route: Routes,
        val data: T? = null
    ) : NavigationItem

    data class Group<T : Any>(
        val title: StringResource, val items: List<Tab<T>>
    ) : NavigationItem
}

private val tabs = listOf<NavigationItem>(
    NavigationItem.Tab<Any>(Res.string.nav_home, Iconsax.IconsaxHome, Routes.Home),
    NavigationItem.Tab<Any>(Res.string.nav_search, Iconsax.IconsaxSearch, Routes.Search)
)

val sidebarTabs = tabs + listOf(
    NavigationItem.Group(
        Res.string.library_toolbar_compact_title,
        items = listOf(
            NavigationItem.Tab(
                Res.string.library_tab_playlists,
                Iconsax.IconsaxMusicDashboard,
                Routes.Library,
                LibraryTab.Playlists
            ),
            NavigationItem.Tab(Res.string.library_tab_albums, Iconsax.IconsaxCd, Routes.Library, LibraryTab.Albums),
            NavigationItem.Tab(Res.string.library_tab_artists, Iconsax.User, Routes.Library, LibraryTab.Artists)
        )
    ),
    NavigationItem.Group(
        Res.string.nav_group_on_device,
        items = listOf(
            NavigationItem.Tab(
                Res.string.library_tab_local_tracks,
                Iconsax.IconsaxMusicLibrary,
                Routes.Library,
                LibraryTab.LocalTracks
            ),
            NavigationItem.Tab(
                Res.string.library_tab_downloads,
                Iconsax.IconsaxDirectboxReceive,
                Routes.Library,
                LibraryTab.Downloads
            )
        )
    ),
    NavigationItem.Group(
        Res.string.nav_group_connect,
        items = listOf(
            NavigationItem.Tab(Res.string.nav_devices, Iconsax.IconsaxMirrorScreenRegular, Routes.Devices),
            NavigationItem.Tab(Res.string.jam_screen_title, Iconsax.IconsaxSoundTwotone, Routes.Jam)
        )
    )
)
val bottombarTabs = tabs + listOf(
    NavigationItem.Tab<Any>(Res.string.library_toolbar_compact_title, Iconsax.IconsaxMusicLibrary, Routes.Library),
    NavigationItem.Tab(Res.string.settings_screen_title, Iconsax.IconsaxSetting2, Routes.Settings)
)

@OptIn(KoinExperimentalAPI::class)
@Composable
fun App(
    content: @Composable () -> Unit = {}
) {
    val settingsRepository: SettingsRepository = koinInject<SettingsRepository>()
    val userSettings by settingsRepository.userSettings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val appUpdateService: AppUpdateService = koinInject()
    val pluginUpdateService: PluginUpdateService = koinInject()
    val availableAppUpdate by appUpdateService.availableUpdate.collectAsStateWithLifecycle()
    val availablePluginUpdate by pluginUpdateService.availableUpdate.collectAsStateWithLifecycle()

    val welcomeRepository: WelcomeRepository = koinInject()
    val showOnboarding by welcomeRepository.shouldShowOnboarding.collectAsStateWithLifecycle()

    var showSupportDialog by remember { mutableStateOf(false) }
    var supportDialogIsReminder by remember { mutableStateOf(false) }

    // Nudge the user to support the project at most once every seven days, unless they opted
    // out in Settings. The first eligible launch after onboarding only records the baseline, so
    // the prompt never appears right after the onboarding donation page.
    LaunchedEffect(showOnboarding) {
        if (showOnboarding) return@LaunchedEffect
        // Read the persisted value rather than the collected state, whose initial value is the
        // default (0) until DataStore has emitted once.
        val current = settingsRepository.getCurrentSettings()
        val now = Clock.System.now().toEpochMilliseconds()
        // Development override: show the reminder on every launch, regardless of timing or the
        // "Support reminders" toggle.
        if (DebugConfig.FORCE_SUPPORT_REMINDER) {
            supportDialogIsReminder = true
            showSupportDialog = true
            settingsRepository.updateSettings(
                current.copy(lastSupportReminderEpochMs = now))
            return@LaunchedEffect
        }
        if (!current.supportRemindersEnabled) return@LaunchedEffect
        val last = current.lastSupportReminderEpochMs
        if (last == 0L) {
            settingsRepository.updateSettings(
                current.copy(lastSupportReminderEpochMs = now))
        } else if (now - last >= SupportReminderIntervalMillis) {
            supportDialogIsReminder = true
            showSupportDialog = true
            settingsRepository.updateSettings(
                current.copy(lastSupportReminderEpochMs = now))
        }
    }

    // The app always renders the shell + NavDisplay; onboarding is just the start destination.
    // When it is completed the destination flips to Home and the navigation state is rebuilt.
    val startRoute = if (showOnboarding) Routes.Welcome else Routes.Home

    val navigationState = rememberNavigationState(
        startRoute = startRoute, topLevelRoutes = TOP_LEVEL_ROUTES
    )
    val navigator = remember(navigationState) {
        Navigator(navigationState)
    }

    // Below the navigation state, so switching languages keeps the current screen.
    AppLocaleProvider(userSettings.language) {
        SpotubeTheme(settings = userSettings) {
            CompositionLocalProvider(LocalSupportPrompt provides {
                supportDialogIsReminder = false
                showSupportDialog = true
            }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentRoute =
                        navigationState.backStacks[navigationState.topLevelRoute]?.last()

                    Box(Modifier.fillMaxSize()) {
                        AppShell(navigator, navigationState) {
                            SharedTransitionLayout {
                                CompositionLocalProvider(
                                    LocalSharedTransitionScope provides this@SharedTransitionLayout
                                ) {
                                    Column {
                                        NavDisplay(
                                            modifier = Modifier.fillMaxSize(),
                                            onBack = navigator::pop,
                                            entries = navigationState.toEntries(koinEntryProvider()),
                                            transitionSpec = navPushTransitionSpec,
                                            popTransitionSpec = navPopTransitionSpec,
                                            predictivePopTransitionSpec = navPredictivePopTransitionSpec
                                        )
                                    }
                                }
                            }
                        }
                        when (currentRoute) {
                            Routes.WebView -> WebViewScreen(koinInject())
                            else -> {}
                        }
                    }
                }
                content()
                availableAppUpdate?.let { update ->
                    AppUpdateDialog(
                        update = update,
                        onDismiss = appUpdateService::dismissUpdate,
                        onIgnore = appUpdateService::ignoreUpdate,
                        onUpdate = {
                            runCatching { openUrlInBrowser("https://spotube.cc/downloads/") }
                            appUpdateService.dismissUpdate()
                        })
                }
                availablePluginUpdate?.let { update ->
                    PluginUpdateDialog(
                        update = update,
                        onDismiss = pluginUpdateService::dismissUpdate,
                        onIgnore = pluginUpdateService::ignoreUpdate,
                        onUpdate = pluginUpdateService::installUpdate)
                }
                SupportDialog(
                    visible = showSupportDialog,
                    onDismissRequest = { showSupportDialog = false },
                    isReminder = supportDialogIsReminder)
            }
        }
    }
}
