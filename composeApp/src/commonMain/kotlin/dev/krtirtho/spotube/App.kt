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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.rememberBaseUITheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import dev.krtirtho.spotube.core.navigation.Navigator
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.navigation.TOP_LEVEL_ROUTES
import dev.krtirtho.spotube.core.navigation.rememberNavigationState
import dev.krtirtho.spotube.core.navigation.toEntries
import dev.krtirtho.spotube.core.ui.component.LocalSharedTransitionScope
import dev.krtirtho.spotube.core.ui.theming.SpotubeTheme
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import dev.krtirtho.spotube.modules.settings.UserSettings
import dev.krtirtho.spotube.modules.shell.AppShell
import dev.krtirtho.spotube.modules.webview.WebViewScreen
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxHome
import dev.krtirtho.spotube.resources.iconsax.IconsaxHomeBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicLibrary
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicLibraryOutline
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearch
import dev.krtirtho.spotube.resources.iconsax.IconsaxSearchBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxSetting2
import dev.krtirtho.spotube.resources.iconsax.IconsaxSettingTwotone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

data class TabItem(
    val title: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val route: Routes
)

val tabs = listOf(
    TabItem(
        "Home",
        Iconsax.IconsaxHomeBroken,
        Iconsax.IconsaxHome,
        Routes.Home
    ),
    TabItem(
        "Search",
        Iconsax.IconsaxSearchBroken,
        Iconsax.IconsaxSearch,
        Routes.Search
    ),
    TabItem(
        "Library",
        Iconsax.IconsaxMusicLibraryOutline,
        Iconsax.IconsaxMusicLibrary,
        Routes.Library
    ),
    TabItem(
        "Settings",
        Iconsax.IconsaxSettingTwotone,
        Iconsax.IconsaxSetting2,
        Routes.Settings
    )
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

    val navigationState = rememberNavigationState(
        startRoute = Routes.Home,
        topLevelRoutes = TOP_LEVEL_ROUTES
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
        }
    }
}
