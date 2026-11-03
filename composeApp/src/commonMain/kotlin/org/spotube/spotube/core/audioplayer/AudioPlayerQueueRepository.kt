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

package org.spotube.spotube.core.audioplayer

import androidx.datastore.preferences.core.edit
import org.spotube.spotube.core.db.Database
import org.spotube.spotube.core.db.DatabaseKeys
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface QueueStateRepository {
    suspend fun getPersistedState(): PersistedQueueState?
    suspend fun saveState(state: PersistedQueueState)
    suspend fun clearState()
}

class AudioPlayerQueueRepository(private val database: Database) : QueueStateRepository {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getPersistedState(): PersistedQueueState? {
        return database.audioPlayerQueueDataStore.data.map { preferences ->
            val payload = preferences[DatabaseKeys.AUDIO_PLAYER_QUEUE_STATE_KEY]
            payload?.let {
                runCatching { json.decodeFromString<PersistedQueueState>(it) }
                    .getOrNull()
            }
        }.first()
    }

    override suspend fun saveState(state: PersistedQueueState) {
        database.audioPlayerQueueDataStore.edit { preferences ->
            preferences[DatabaseKeys.AUDIO_PLAYER_QUEUE_STATE_KEY] = json.encodeToString(state)
        }
    }

    override suspend fun clearState() {
        database.audioPlayerQueueDataStore.edit { preferences ->
            preferences.remove(DatabaseKeys.AUDIO_PLAYER_QUEUE_STATE_KEY)
        }
    }
}