/*
 * Copyright (C) 2026 Kingmor Roy Tirtho and Spotube Contributors
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

package org.spotube.spotube.modules.blacklist

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.db.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

open class BlacklistRepository(
    private val database: Database) {
    companion object {
        private val BLACKLISTED_TRACKS_KEY = stringPreferencesKey("blacklisted_tracks")
        private val BLACKLISTED_ARTISTS_KEY = stringPreferencesKey("blacklisted_artists")
    }

    private val json = Json { ignoreUnknownKeys = true }

    open val blacklistedTracks: Flow<List<MetadataTrack>> = database.blacklistDataStore.data
        .map { prefs ->
            val tracksJson = prefs[BLACKLISTED_TRACKS_KEY]
            if (tracksJson != null) {
                try {
                    json.decodeFromString<List<MetadataTrack>>(tracksJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    open val blacklistedArtists: Flow<List<MetadataArtist>> = database.blacklistDataStore.data
        .map { prefs ->
            val artistsJson = prefs[BLACKLISTED_ARTISTS_KEY]
            if (artistsJson != null) {
                try {
                    json.decodeFromString<List<MetadataArtist>>(artistsJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    open suspend fun toggleTrack(track: MetadataTrack) {
        val currentTracks = getTracksSnapshot().toMutableList()
        val existingIndex = currentTracks.indexOfFirst { it.id == track.id }
        
        if (existingIndex >= 0) {
            currentTracks.removeAt(existingIndex)
        } else {
            currentTracks.add(track)
        }
        
        database.blacklistDataStore.edit { prefs ->
            prefs[BLACKLISTED_TRACKS_KEY] = json.encodeToString(currentTracks)
        }
    }

    open suspend fun toggleArtist(artist: MetadataArtist) {
        val currentArtists = getArtistsSnapshot().toMutableList()
        val existingIndex = currentArtists.indexOfFirst { it.id == artist.id }
        
        if (existingIndex >= 0) {
            currentArtists.removeAt(existingIndex)
        } else {
            currentArtists.add(artist)
        }
        
        database.blacklistDataStore.edit { prefs ->
            prefs[BLACKLISTED_ARTISTS_KEY] = json.encodeToString(currentArtists)
        }
    }

    suspend fun getTracksSnapshot(): List<MetadataTrack> {
        return database.blacklistDataStore.data.map { prefs ->
            val tracksJson = prefs[BLACKLISTED_TRACKS_KEY]
            if (tracksJson != null) {
                try {
                    json.decodeFromString<List<MetadataTrack>>(tracksJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.first()
    }

    suspend fun getArtistsSnapshot(): List<MetadataArtist> {
        return database.blacklistDataStore.data.map { prefs ->
            val artistsJson = prefs[BLACKLISTED_ARTISTS_KEY]
            if (artistsJson != null) {
                try {
                    json.decodeFromString<List<MetadataArtist>>(artistsJson)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.first()
    }
}
