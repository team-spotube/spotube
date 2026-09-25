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

package dev.krtirtho.spotube.core.server

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioSource
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.db.Database
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A matched [AudioSource.Basic] tagged with the id of the audio plugin that produced it.
 * The [pluginId] tag lets [MatchedTracksRepository] detect and discard matches that were
 * made by a plugin the user is no longer using, since a source id from one plugin (e.g. a
 * YouTube video id) is meaningless - or worse, silently wrong - when handed to a different
 * plugin's `getStreamsOfAudioSource`.
 */
@Serializable
data class PluginTaggedAudioSource(
    val pluginId: String,
    val source: AudioSource.Basic,
)

/** Persists (and looks up) the audio source matched to a [MetadataTrack] by a given plugin. */
interface TrackSourceRepository {
    suspend fun getTrackSource(track: MetadataTrack, pluginId: String): AudioSource.Basic?
    suspend fun saveTrackSource(track: MetadataTrack, source: AudioSource.Basic, pluginId: String)
}

class MatchedTracksRepository(private val database: Database) : TrackSourceRepository {
    /**
     * Returns the previously matched source for [track], but only if it was produced by
     * [pluginId]. A match saved by a different (or since-removed) plugin is treated as a
     * cache miss rather than being handed to the wrong plugin.
     */
    override suspend fun getTrackSource(track: MetadataTrack, pluginId: String): AudioSource.Basic? {
        return database.matchedTracksDataStore.data.map { prefs ->
            val json = prefs[stringPreferencesKey(track.id)] ?: return@map null
            val tagged = try {
                Json.decodeFromString<PluginTaggedAudioSource>(json)
            } catch (_: Exception) {
                // Legacy entries stored before plugin-tagging was introduced; treat as
                // unverifiable and force a fresh match instead of risking a wrong plugin.
                return@map null
            }
            if (tagged.pluginId != pluginId) return@map null
            tagged.source
        }.first()
    }

    override suspend fun saveTrackSource(track: MetadataTrack, source: AudioSource.Basic, pluginId: String) {
        database.matchedTracksDataStore.edit { prefs ->
            prefs[stringPreferencesKey(track.id)] =
                Json.encodeToString(PluginTaggedAudioSource(pluginId, source))
        }
    }
}