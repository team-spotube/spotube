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

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.krtirtho.spotube.core.db.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

/** Narrow read-only view over the current [UserSettings], for consumers (like
 * [dev.krtirtho.spotube.core.server.StreamingUrlRepository]) that only need to read the
 * user's streaming preferences without depending on the full [SettingsRepository]. */
interface UserSettingsSource {
    val userSettings: StateFlow<UserSettings>
}

class SettingsRepository(private val database: Database) : UserSettingsSource {
    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("user_settings")
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val userSettings: StateFlow<UserSettings> = database.settingsDataStore.data.map { prefs ->
        val json = prefs[SETTINGS_KEY]
        if (json != null) {
            Json.decodeFromString<UserSettings>(json as String)
        } else {
            UserSettings() // Default value
        }
    }.stateIn(
        scope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings() // Default value
    )

    suspend fun updateSettings(newSettings: UserSettings) {
        database.settingsDataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = Json.encodeToString(newSettings)
        }
    }
}