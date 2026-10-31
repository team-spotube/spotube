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

package org.spotube.spotube.modules.library.playlist

import org.spotube.spotube.core.jam.JamRoomClient
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import org.koin.core.component.KoinComponent

/**
 * Repository for managing local playlists (stored as M3U8 files).
 *
 * Local playlists are stored as M3U8 files in the configured playlists directory.
 * They contain local tracks (from LocalMediaTrack) referenced by their file path.
 */
package org.spotube.spotube.modules.library.playlist

import org.spotube.spotube.core.jam.JamRoomClient
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import org.koin.core.component.KoinComponent

/**
 * Repository for managing local playlists (stored as M3U8 files).
 *
 * Local playlists are stored as M3U8 files in the configured playlists directory.
 * They contain local tracks (from LocalMediaTrack) referenced by their file path.
 * Supports cross-device sync via Jam (MQTT) mechanism.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalPlaylistRepository(
    private val settingsRepository: SettingsRepository,
    private val jamRoomClient: JamRoomClient,
    private val jamRoomService: JamRoomService
) : KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists: StateFlow<List<LocalPlaylist>> = _playlists.asStateFlow()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var isInitialized = false
    private var currentSyncJob: kotlinx.coroutines.Job? = null
    
    init {
        loadPlaylists()
        observeJamSync()
    }
    
    sealed interface SyncState {
        data object Idle : SyncState
        data object Syncing : SyncState
        data class Success(val timestamp: Long) : SyncState
        data class Error(val message: String) : SyncState
    }
    
    private val playlistsDir: String
        get() = settingsRepository.userSettings.value.overloadedDownloadFolder
            ?.let { File(it, "Playlists").absolutePath }
            ?: File(settingsRepository.userSettings.value.localMediaFolders.firstOrNull() ?: File(System.getProperty("user.home"), "Music").absolutePath).parentFile.absolutePath
    
    private val playlistsDirFile: File
        get() = File(playlistsDir).apply { mkdirs() }
    
    private fun getPlaylistFile(name: String): File {
        val safeName = name.replace(Regex(r"[<>:\"/\\|?*]"), "_")
        return File(playlistsDirFile, "$safeName.m3u8")
    }
    
    fun initialize() {
        if (!isInitialized) {
            loadPlaylists()
            isInitialized = true
        }
    }
    
    fun getPlaylists(): List<LocalPlaylist> = _playlists.value
    
    fun getPlaylist(name: String): LocalPlaylist? = _playlists.value.find { it.name == name }
    
    suspend fun createPlaylist(name: String, description: String? = null): Result<LocalPlaylist> {
        return runCatching {
            if (name.isBlank()) {
                throw IllegalArgumentException(getString(Res.string.library_playlist_name_empty))
            }
            
            if (_playlists.value.any { it.name == name }) {
                throw IllegalArgumentException(getString(Res.string.library_playlist_name_exists))
            }
            
            val playlist = LocalPlaylist(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                trackPaths = emptyList()
            )
            
            _playlists.update { it + playlist }
            savePlaylist(playlist)
            broadcastPlaylistChange(PlaylistChange.Created(playlist))
            playlist
        }
    }
    
    suspend fun updatePlaylist(playlist: LocalPlaylist): Result<Unit> {
        return runCatching {
            val index = _playlists.value.indexOfFirst { it.id == playlist.id }
            if (index < 0) {
                throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            }
            
            val updated = playlist.copy(
                updatedAt = System.currentTimeMillis()
            )
            
            _playlists.update { list ->
                list.set(index, updated)
            }
            savePlaylist(updated)
            broadcastPlaylistChange(PlaylistChange.Updated(updated))
        }
    }
    
    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return runCatching {
            val playlist = _playlists.value.find { it.id == playlistId }
            if (playlist == null) {
                throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            }
            
            val file = getPlaylistFile(playlist.name)
            file.delete()
            
            _playlists.update { it.filter { it.id != playlistId } }
            broadcastPlaylistChange(PlaylistChange.Deleted(playlistId))
        }
    }
    
    suspend fun addTrack(playlistId: String, track: LocalMediaTrack): Result<Unit> {
        return runCatching {
            val playlist = _playlists.value.find { it.id == playlistId }
                ?: throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            
            val trackPath = track.path
            if (playlist.trackPaths.contains(trackPath)) {
                return // Already in playlist
            }
            
            val updated = playlist.copy(
                trackPaths = playlist.trackPaths + trackPath,
                updatedAt = System.currentTimeMillis()
            )
            
            _playlists.update { list ->
                list.map { if (it.id == playlistId) updated else it }
            }
            savePlaylist(updated)
            broadcastPlaylistChange(PlaylistChange.TrackAdded(playlistId, track.path))
        }
    }
    
    suspend fun addTracks(playlistId: String, tracks: List<LocalMediaTrack>): Result<Unit> {
        return runCatching {
            val playlist = _playlists.value.find { it.id == playlistId }
                ?: throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            
            val newTrackPaths = tracks.map { it.path }.filterNot { it in playlist.trackPaths }
            if (newTrackPaths.isEmpty()) return
            
            val updated = playlist.copy(
                trackPaths = playlist.trackPaths + newTrackPaths,
                updatedAt = System.currentTimeMillis()
            )
            
            _playlists.update { list ->
                list.map { if (it.id == playlistId) updated else it }
            }
            savePlaylist(updated)
            broadcastPlaylistChange(PlaylistChange.TracksAdded(playlistId, newTrackPaths))
        }
    }
    
    suspend fun removeTrack(playlistId: String, trackPath: String): Result<Unit> {
        return runCatching {
            val playlist = _playlists.value.find { it.id == playlistId }
                ?: throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            
            if (!playlist.trackPaths.contains(trackPath)) return
            
            val updated = playlist.copy(
                trackPaths = playlist.trackPaths.filter { it != trackPath },
                updatedAt = System.currentTimeMillis()
            )
            
            _playlists.update { list ->
                list.map { if (it.id == playlistId) updated else it }
            }
            savePlaylist(updated)
            broadcastPlaylistChange(PlaylistChange.TrackRemoved(playlistId, trackPath))
        }
    }
    
    suspend fun reorderTracks(playlistId: String, fromIndex: Int, toIndex: Int): Result<Unit> {
        return runCatching {
            val playlist = _playlists.value.find { it.id == playlistId }
                ?: throw IllegalArgumentException(getString(Res.string.library_playlist_not_found))
            
            if (fromIndex < 0 || fromIndex >= playlist.trackPaths.size ||
                toIndex < 0 || toIndex >= playlist.trackPaths.size) {
                throw IndexOutOfBoundsException()
            }
            
            val newTrackPaths = playlist.trackPaths.toMutableList()
            val trackPath = newTrackPaths.removeAt(fromIndex)
            newTrackPaths.add(toIndex, trackPath)
            
            val updated = playlist.copy(
                trackPaths = newTrackPaths,
                updatedAt = System.currentTimeMillis()
            )
            
            _playlists.update { list ->
                list.map { if (it.id == playlistId) updated else it }
            }
            savePlaylist(updated)
            broadcastPlaylistChange(PlaylistChange.Reordered(playlistId, newTrackPaths))
        }
    }
    
    private fun savePlaylist(playlist: LocalPlaylist) {
        scope.launch {
            val file = getPlaylistFile(playlist.name)
            val m3u8Content = buildM3u8Content(playlist)
            file.writeText(m3u8Content, StandardCharsets.UTF_8)
        }
    }
    
    private fun buildM3u8Content(playlist: LocalPlaylist): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        sb.append("#PLAYLIST:${playlist.name}\n")
        if (playlist.description != null) {
            sb.append("#DESCRIPTION:${playlist.description}\n")
        }
        sb.append("#EXTENC:UTF-8\n")
        
        for (trackPath in playlist.trackPaths) {
            val trackName = File(trackPath).nameWithoutExtension
            sb.append("#EXTINF:-1,$trackName\n")
            sb.append("$trackPath\n")
        }
        
        return sb.toString()
    }
    
    private fun loadPlaylists() {
        scope.launch {
            val files = playlistsDirFile.listFiles { it.extension == "m3u8" } ?: emptyArray()
            val playlists = files.mapNotNull { file ->
                parseM3u8(file).copy(filePath = file.absolutePath)
            }
            _playlists.value = playlists
        }
    }
    
    private fun parseM3u8(file: File): LocalPlaylist {
        val lines = file.readText(StandardCharsets.UTF_8).splitLines()
        var name = file.nameWithoutExtension
        var description: String? = null
        var trackPaths = mutableListOf<String>()
        
        for (line in lines) {
            when {
                line.startsWith("#PLAYLIST:") -> name = line.substringAfter("#PLAYLIST:").trim()
                line.startsWith("#DESCRIPTION:") -> description = line.substringAfter("#DESCRIPTION:").trim()
                line.startsWith("#EXTINF:") -> {
                    // Format: #EXTINF:-1,trackName
                    // Next line is the track path
                }
                !line.startsWith("#") && line.isNotBlank() -> {
                    // This is a track path line
                    val trackPath = line.trim()
                    if (trackPath.isNotBlank()) trackPaths.add(trackPath)
                }
            }
        }
        
        return LocalPlaylist(
            id = file.nameWithoutExtension,
            name = name,
            description = description,
            createdAt = file.lastModified(),
            updatedAt = file.lastModified(),
            trackPaths = trackPaths,
            filePath = file.absolutePath
        )
    }
    
    // ============ Sync via Jam (MQTT) ============
    
    sealed interface PlaylistChange {
        @Serializable
        data class Created(val playlist: LocalPlaylist) : PlaylistChange
        @Serializable
        data class Updated(val playlist: LocalPlaylist) : PlaylistChange
        @Serializable
        data class Deleted(val playlistId: String) : PlaylistChange
        @Serializable
        data class TrackAdded(val playlistId: String, val trackPath: String) : PlaylistChange
        @Serializable
        data class TracksAdded(val playlistId: String, val trackPaths: List<String>) : PlaylistChange
        @Serializable
        data class TrackRemoved(val playlistId: String, val trackPath: String) : PlaylistChange
        @Serializable
        data class Reordered(val playlistId: String, val trackPaths: List<String>) : PlaylistChange
    }
    
    private fun broadcastPlaylistChange(change: PlaylistChange) {
        if (!jamRoomService.isConnected.value) return
        if (jamRoomService.role.value != org.spotube.spotube.core.jam.JamRole.Host) return
        
        scope.launch {
            jamRoomClient.publishCommand(
                org.spotube.spotube.core.jam.JamMessage.PlaybackCommand(
                    org.spotube.spotube.core.jam.PlaybackCmd.Custom(
                        type = "playlist_sync",
                        payload = json.encodeToString(PlaylistChange.serializer(), change)
                    )
                )
            )
        }
    }
    
    private fun observeJamSync() {
        scope.launch {
            jamRoomClient.commands
                .filter { it is org.spotube.spotube.core.jam.JamMessage.PlaybackCommand }
                .map { it as org.spotube.spotube.core.jam.JamMessage.PlaybackCommand }
                .filter { it.command is org.spotube.spotube.core.jam.PlaybackCmd.Custom }
                .map { (it.command as org.spotube.spotube.core.jam.PlaybackCmd.Custom) }
                .filter { it.type == "playlist_sync" }
                .onEach { cmd ->
                    try {
                        val change = json.decodeFromString(PlaylistChange.serializer(), cmd.payload)
                        applyRemoteChange(change)
                    } catch (e: Exception) {
                        // Ignore parse errors
                    }
                }
                .launchIn(scope)
        }
    }
    
    private fun applyRemoteChange(change: PlaylistChange) {
        when (change) {
            is PlaylistChange.Created -> {
                _playlists.update { it + change.playlist }
            }
            is PlaylistChange.Updated -> {
                _playlists.update { list ->
                    val index = list.indexOfFirst { it.id == change.playlist.id }
                    if (index >= 0) list.set(index, change.playlist) else list
                }
            }
            is PlaylistChange.Deleted -> {
                _playlists.update { it.filter { it.id != change.playlistId } }
                getPlaylistFile(change.playlistId).delete()
            }
            is PlaylistChange.TrackAdded -> {
                _playlists.update { list ->
                    list.map { pl ->
                        if (pl.id == change.playlistId && !pl.trackPaths.contains(change.trackPath)) {
                            pl.copy(trackPaths = pl.trackPaths + change.trackPath, updatedAt = System.currentTimeMillis())
                        } else pl
                    }
                }
            }
            is PlaylistChange.TracksAdded -> {
                _playlists.update { list ->
                    list.map { pl ->
                        if (pl.id == change.playlistId) {
                            val newPaths = change.trackPaths.filterNot { it in pl.trackPaths }
                            if (newPaths.isEmpty()) pl else pl.copy(trackPaths = pl.trackPaths + newPaths, updatedAt = System.currentTimeMillis())
                        } else pl
                    }
                }
            }
            is PlaylistChange.TrackRemoved -> {
                _playlists.update { list ->
                    list.map { pl ->
                        if (pl.id == change.playlistId) {
                            pl.copy(trackPaths = pl.trackPaths.filter { it != change.trackPath }, updatedAt = System.currentTimeMillis())
                        } else pl
                    }
                }
            }
            is PlaylistChange.Reordered -> {
                _playlists.update { list ->
                    list.map { pl ->
                        if (pl.id == change.playlistId) {
                            pl.copy(trackPaths = change.trackPaths, updatedAt = System.currentTimeMillis())
                        } else pl
                    }
                }
            }
        }
    }
    
    // ============ Manual Sync Trigger ============
    
    suspend fun syncNow(): Result<Unit> {
        return runCatching {
            _syncState.value = SyncState.Syncing
            try {
                // Broadcast full playlist state to other devices
                for (playlist in _playlists.value) {
                    broadcastPlaylistChange(PlaylistChange.Created(playlist))
                }
                _syncState.value = SyncState.Success(System.currentTimeMillis())
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                throw e
            }
        }
    }
    
    fun getSyncState(): SyncState = _syncState.value
}

@Serializable
data class LocalPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val trackPaths: List<String> = emptyList(),
    val filePath: String? = null
)