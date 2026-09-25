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

package dev.krtirtho.spotube.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.krtirtho.spotube.core.di.injectLogger
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.component.KoinComponent

val TOP_LEVEL_ROUTES = setOf(
    Routes.Home,
    Routes.Search,
    Routes.Library,
    Routes.Settings
)

val serializersConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Routes.Home::class, Routes.Home.serializer())
            subclass(Routes.Search::class, Routes.Search.serializer())
            subclass(Routes.Library::class, Routes.Library.serializer())
            subclass(Routes.Settings::class, Routes.Settings.serializer())
            subclass(Routes.Plugins::class, Routes.Plugins.serializer())
            subclass(Routes.WebView::class, Routes.WebView.serializer())
            subclass(Routes.Playlist::class, Routes.Playlist.serializer())
            subclass(Routes.Artist::class, Routes.Artist.serializer())
            subclass(Routes.Album::class, Routes.Album.serializer())
            subclass(Routes.Blacklist::class, Routes.Blacklist.serializer())
            subclass(Routes.Devices::class, Routes.Devices.serializer())
            subclass(Routes.Jam::class, Routes.Jam.serializer())
        }
    }
}

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) : KoinComponent {
    var topLevelRoute by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    internal val logger by injectLogger<NavigationState>()

}

@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        configuration = serializersConfig,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(
            configuration = serializersConfig,
            key
        )
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator()
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    val state = stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
    logger.d { "Current entries: ${state.joinToString("->") { it.contentKey.toString() }}" }
    return state
}

class Navigator(val navigationState: NavigationState) : KoinComponent {
    val logger by injectLogger<Navigator>()
    fun navigate(route: NavKey) {
        if (route in TOP_LEVEL_ROUTES) {
            val targetStack = navigationState.backStacks[navigationState.topLevelRoute]
            if (targetStack != null) {
                // Clear everything except the very first entry (the root)
                while (targetStack.size > 1) {
                    targetStack.removeLastOrNull()
                }
            }
            navigationState.topLevelRoute = route
            logger.d { "Navigating to top-level route: $route, cleared back stack: ${targetStack != null}" }
        } else {
            logger.d { "Navigating to route: $route" }
            logger.d {
                "Back stack before navigation: ${
                    navigationState.backStacks[navigationState.topLevelRoute]?.joinToString(
                        "->"
                    )
                }"
            }
            val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            if (currentStack?.lastOrNull() != route) {
                currentStack?.add(route)
            }
        }
    }

    fun pop() {
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            ?: error("Current back stack not found for route: ${navigationState.topLevelRoute}")
        val currentRoute = currentStack.last()
        if (currentRoute == navigationState.topLevelRoute) {
            navigationState.topLevelRoute = navigationState.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}