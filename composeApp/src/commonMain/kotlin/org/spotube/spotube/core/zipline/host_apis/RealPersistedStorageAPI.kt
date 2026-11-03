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

package org.spotube.spotube.core.zipline.host_apis

import androidx.datastore.preferences.core.stringPreferencesKey
import org.spotube.plugin_interfaces.host_apis.PersistedStorageAPI
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.modules.plugin.PluginEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RealPersistedStorageAPI(
    private val pluginInfo: PluginEntry
) : PersistedStorageAPI, KoinComponent {
    val database: Database by inject()

    override suspend fun putString(key: String, value: String) {
        return withContext(Dispatchers.IO) {
            database.pluginsDataStore.updateData { prefs ->
                prefs.toMutablePreferences().apply {
                    this[stringPreferencesKey("${pluginInfo.id}:$key")] = value
                }
            }
        }
    }

    override suspend fun getString(key: String): String? {
        return withContext(Dispatchers.IO) {
            val prefs = database.pluginsDataStore.data.first()
            prefs[stringPreferencesKey("${pluginInfo.id}:$key")]
        }
    }

    override suspend fun remove(key: String) {
        return withContext(Dispatchers.IO) {
            database.pluginsDataStore.updateData { prefs ->
                prefs.toMutablePreferences().apply {
                    remove(stringPreferencesKey("${pluginInfo.id}:$key"))
                }
            }
        }
    }

    override suspend fun getKeys(): List<String> {
        return withContext(Dispatchers.IO) {
            val prefs = database.pluginsDataStore.data.first()
            prefs.asMap().keys.mapNotNull { prefKey ->
                val keyString = prefKey.name
                if (keyString.startsWith("${pluginInfo.id}:")) {
                    keyString.removePrefix("${pluginInfo.id}:")
                } else null
            }
        }
    }
}