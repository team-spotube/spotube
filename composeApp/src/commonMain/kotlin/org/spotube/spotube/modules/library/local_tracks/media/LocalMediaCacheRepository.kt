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

package org.spotube.spotube.modules.library.local_tracks.media

import androidx.datastore.preferences.core.edit
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.db.DatabaseKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class LocalMediaCacheRepository(private val database: Database) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val cacheFlow: Flow<LocalMediaCache> = database.localMediaDataStore.data.map { prefs ->
        prefs[DatabaseKeys.LOCAL_MEDIA_CACHE_STATE_KEY]?.let { raw ->
            runCatching {
                json.decodeFromString<LocalMediaCache>(raw)
            }.getOrDefault(LocalMediaCache())
        } ?: LocalMediaCache()
    }

    suspend fun save(cache: LocalMediaCache) {
        database.localMediaDataStore.edit { prefs ->
            prefs[DatabaseKeys.LOCAL_MEDIA_CACHE_STATE_KEY] = json.encodeToString(cache)
            prefs[DatabaseKeys.LOCAL_MEDIA_LAST_SCAN_AT_KEY] = cache.indexedAtEpochMs
        }
    }
}
