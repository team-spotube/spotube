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

package org.spotube.spotube.modules.plugin.guard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.modules.plugin.PluginAbility
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.plugin.PluginManagerStates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MetadataPluginGuardState {
    data object Loading : MetadataPluginGuardState
    data object Available : MetadataPluginGuardState

    /** No metadata plugin is selected. [hasInstalledPlugins] tells whether the user only needs
     * to pick one (true) or has to install one first (false). */
    data class Missing(val hasInstalledPlugins: Boolean) : MetadataPluginGuardState
}

class MetadataPluginGuardViewModel(
    pluginManager: PluginManager,
    private val navigationCommands: NavigationCommands,
    private val libraryState: LibraryState
) : ViewModel() {
    val uiState: StateFlow<MetadataPluginGuardState> = pluginManager.state
        .map { state ->
            when (state) {
                is PluginManagerStates.Loading -> MetadataPluginGuardState.Loading
                is PluginManagerStates.Data ->
                    if (PluginAbility.METADATA in state.selectedPlugins) {
                        MetadataPluginGuardState.Available
                    } else {
                        MetadataPluginGuardState.Missing(
                            hasInstalledPlugins = state.plugins.any { PluginAbility.METADATA in it.abilities })
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MetadataPluginGuardState.Loading)

    fun openPlugins() {
        navigationCommands.navigateTo(Routes.Plugins)
    }

    /** Local tracks are a plugin-free player, so offer them as the way forward. */
    fun openLocalTracks() {
        libraryState.onTabSelected(LibraryTab.LocalTracks)
        navigationCommands.navigateTo(Routes.Library)
    }
}
