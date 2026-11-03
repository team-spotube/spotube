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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.spotube.spotube.core.paths.Paths

interface SettingsProvider {
    val settingsState: StateFlow<UserSettings?>
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val paths: Paths
) : ViewModel(), SettingsProvider {
    override val settingsState = repository.userSettings

    /** Where downloads go when no override folder is set. */
    val defaultDownloadFolder: String = paths.getUserDownloadsDirPath()

    fun updateSettings(transform: UserSettings.() -> UserSettings) {
        viewModelScope.launch {
            // Get current state, apply transform, and save
            val currentSettings = settingsState.value ?: return@launch
            val newSettings = currentSettings.transform()
            repository.updateSettings(newSettings)
        }
    }
}