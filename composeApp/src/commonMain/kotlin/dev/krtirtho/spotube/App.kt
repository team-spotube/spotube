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

package dev.krtirtho.spotube

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.mikepenz.markdown.m3.Markdown
import dev.krtirtho.spotube.core.navigation.Navigator
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.navigation.TOP_LEVEL_ROUTES
import dev.krtirtho.spotube.core.navigation.rememberNavigationState
import dev.krtirtho.spotube.core.navigation.toEntries
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.PrimaryButton
import dev.krtirtho.spotube.core.ui.base.ThemedDialog
import dev.krtirtho.spotube.core.ui.base.rememberBaseUITheme
import dev.krtirtho.spotube.core.ui.component.LocalSharedTransitionScope
import dev.krtirtho.spotube.core.ui.theming.SpotubeTheme
import dev.krtirtho.spotube.modules.library.LibraryTab
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.shell.AppShell
import dev.krtirtho.spotube.modules.update.UpdateCheckerViewModel
import dev.krtirtho.spotube.modules.webview.WebViewScreen
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxCd
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxHome
import dev.krtirtho.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicDashboard
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicLibrary
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxSetting2
import dev.krtirtho.spotube.resources.iconsax.IconsaxSoundTwotone
import dev.krtirtho.spotube.resources.iconsax.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

interface NavigationItem {
    data class Tab<T : Any>(
        val title: String, val icon: ImageVector, val route: Routes,
        val data: T? = null
    ) : NavigationItem

    data class Group<T : Any>(
        val title: String, val items: List<Tab<T>>
    ) : NavigationItem
}

private val tabs = listOf<NavigationItem>(
    NavigationItem.Tab<Any>("Home", Iconsax.IconsaxHome, Routes.Home),
    NavigationItem.Tab<Any>("Search", Iconsax.IconsaxSearch, Routes.Search),
)

val sidebarTabs = tabs + listOf(
    NavigationItem.Group(
        "Library",
        items = listOf(
            NavigationItem.Tab(
                "Playlists",
                Iconsax.IconsaxMusicDashboard,
                Routes.Library,
                LibraryTab.Playlists
            ),
            NavigationItem.Tab("Albums", Iconsax.IconsaxCd, Routes.Library, LibraryTab.Albums),
            NavigationItem.Tab("Artists", Iconsax.User, Routes.Library, LibraryTab.Artists),
        )
    ),
    NavigationItem.Group(
        "On Device",
        items = listOf(
            NavigationItem.Tab(
                "Local Tracks",
                Iconsax.IconsaxMusicLibrary,
                Routes.Library,
                LibraryTab.LocalTracks
            ),
            NavigationItem.Tab(
                "Downloads",
                Iconsax.IconsaxDirectboxReceive,
                Routes.Library,
                LibraryTab.Downloads
            ),
        )
    ),
    NavigationItem.Group(
        "Connect",
        items = listOf(
            NavigationItem.Tab("Devices", Iconsax.IconsaxMirrorScreenRegular, Routes.Devices),
            NavigationItem.Tab("Group Jam", Iconsax.IconsaxSoundTwotone, Routes.Jam),
        ),
    ),
)
val bottombarTabs = tabs + listOf(
    NavigationItem.Tab<Any>("Library", Iconsax.IconsaxMusicLibrary, Routes.Library),
    NavigationItem.Tab("Settings", Iconsax.IconsaxSetting2, Routes.Settings)
)

@OptIn(
    KoinExperimentalAPI::class,
    ExperimentalCoroutinesApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun App(
    content: @Composable () -> Unit = {}
) {
    val settingsRepository: SettingsRepository = koinInject<SettingsRepository>()
    val userSettings by settingsRepository.userSettings.collectAsStateWithLifecycle(initialValue = UserSettings())
    val updateCheckerViewModel: UpdateCheckerViewModel = koinViewModel()
    val availableUpdate by updateCheckerViewModel.availableUpdate.collectAsStateWithLifecycle()

    val navigationState = rememberNavigationState(
        startRoute = Routes.Home, topLevelRoutes = TOP_LEVEL_ROUTES
    )
    val navigator = remember {
        Navigator(navigationState)
    }

    SpotubeTheme(settings = userSettings) {
        val baseUITheme = rememberBaseUITheme()
        CompositionLocalProvider(LocalBaseUITheme provides baseUITheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.last()

                Box(Modifier.fillMaxSize()) {
                    AppShell(navigator, navigationState) {
                        SharedTransitionLayout {
                            CompositionLocalProvider(
                                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                            ) {
                                Column {
                                    NavDisplay(
                                        modifier = Modifier.fillMaxSize(),
                                        onBack = navigator::pop,
                                        entries = navigationState.toEntries(koinEntryProvider())
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
            if (availableUpdate != null) {
                val update = availableUpdate!!
                ThemedDialog(
                    onDismissRequest = updateCheckerViewModel::dismissUpdate,
                    title = {
                        Text(
                            text = "Spotube ${update.tag} is available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        OutlineButton(onClick = updateCheckerViewModel::ignoreUpdate) {
                            Text("Ignore")
                        }
                        PrimaryButton(
                            onClick = {
                                runCatching { openUrlInBrowser("https://spotube.cc/downloads/") }
                                updateCheckerViewModel.dismissUpdate()
                            },
                        ) {
                            Text("Update")
                        }
                    },
                ) {
                    if (update.releaseNotesMarkdown.isNotBlank()) {
                        SelectionContainer {
                            Markdown(
                                content = update.releaseNotesMarkdown,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    } else {
                        Text("See what's new in this release on the Spotube downloads page.")
                    }
                }
            }
        }
    }
}
