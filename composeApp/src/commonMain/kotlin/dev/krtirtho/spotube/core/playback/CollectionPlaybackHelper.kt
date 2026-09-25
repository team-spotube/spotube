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

package dev.krtirtho.spotube.core.playback

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueCollectionEntry
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.modules.album.AlbumRepository
import dev.krtirtho.spotube.modules.artist.ArtistRepository
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import dev.krtirtho.spotube.modules.playlist.PlaylistRepository
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksRepository
import dev.krtirtho.spotube.core.remote.RemoteCollectionType

class CollectionPlaybackHelper(
    private val albumRepository: AlbumRepository,
    private val playlistRepository: PlaylistRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val artistRepository: ArtistRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
) {
    suspend fun playAlbum(albumId: String) {
        if (audioPlayerQueue.isAlbumPlaying(albumId)) return
        val entries = fetchAllAlbumTracks(albumId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.Album(albumId),
            )
        }
    }

    suspend fun addAlbumToQueue(albumId: String) {
        val entries = fetchAllAlbumTracks(albumId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(
                entries,
                collectionEntry = QueueCollectionEntry.Album(albumId),
            )
        }
    }

    suspend fun playAlbumNext(albumId: String) {
        val entries = fetchAllAlbumTracks(albumId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllAfterCurrent(entries)
        }
    }

    suspend fun playAlbumFromTrack(albumId: String, track: MetadataTrack) {
        val entries = fetchAllAlbumTracks(albumId)
        if (entries.isEmpty()) return

        val queue = audioPlayerQueue.getQueue()
        val queueIndex = queue.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }
        if (queueIndex >= 0) {
            audioPlayerQueue.jumpTo(queueIndex)
            return
        }

        val startPosition = entries.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }.coerceAtLeast(0)

        audioPlayerQueue.load(
            entries = entries,
            autoPlay = true,
            startPosition = startPosition,
            collectionEntry = QueueCollectionEntry.Album(albumId),
        )
    }

    suspend fun playPlaylist(playlistId: String) {
        if (audioPlayerQueue.isPlaylistPlaying(playlistId)) return
        val entries = fetchAllPlaylistTracks(playlistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.Playlist(playlistId),
            )
        }
    }

    suspend fun addPlaylistToQueue(playlistId: String) {
        val entries = fetchAllPlaylistTracks(playlistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(
                entries,
                collectionEntry = QueueCollectionEntry.Playlist(playlistId),
            )
        }
    }

    suspend fun playPlaylistNext(playlistId: String) {
        val entries = fetchAllPlaylistTracks(playlistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllAfterCurrent(entries)
        }
    }

    suspend fun playPlaylistFromTrack(playlistId: String, track: MetadataTrack) {
        val entries = fetchAllPlaylistTracks(playlistId)
        if (entries.isEmpty()) return

        val queue = audioPlayerQueue.getQueue()
        val queueIndex = queue.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }
        if (queueIndex >= 0) {
            audioPlayerQueue.jumpTo(queueIndex)
            return
        }

        val startPosition = entries.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }.coerceAtLeast(0)

        audioPlayerQueue.load(
            entries = entries,
            autoPlay = true,
            startPosition = startPosition,
            collectionEntry = QueueCollectionEntry.Playlist(playlistId),
        )
    }

    suspend fun playSavedTracks() {
        if (audioPlayerQueue.isSavedTracksPlaying()) return
        val entries = fetchAllSavedTracks()
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.SavedTracks,
            )
        }
    }

    suspend fun playArtistTopTracks(artistId: String) {
        val entries = fetchArtistTopTracks(artistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = null,
            )
        }
    }

    suspend fun addArtistTopTracksToQueue(artistId: String) {
        val entries = fetchArtistTopTracks(artistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(entries)
        }
    }

    suspend fun playArtistTopTracksNext(artistId: String) {
        val entries = fetchArtistTopTracks(artistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllAfterCurrent(entries)
        }
    }

    suspend fun addSavedTracksToQueue() {
        val entries = fetchAllSavedTracks()
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(
                entries,
                collectionEntry = QueueCollectionEntry.SavedTracks,
            )
        }
    }

    suspend fun playSavedTracksFromTrack(track: MetadataTrack) {
        val entries = fetchAllSavedTracks()
        if (entries.isEmpty()) return

        val queue = audioPlayerQueue.getQueue()
        val queueIndex = queue.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }
        if (queueIndex >= 0) {
            audioPlayerQueue.jumpTo(queueIndex)
            return
        }

        val startPosition = entries.indexOfFirst { entry ->
            (entry as? QueueEntry.StreamingTrack)?.track?.matchesTrack(track) == true
        }.coerceAtLeast(0)

        audioPlayerQueue.load(
            entries = entries,
            autoPlay = true,
            startPosition = startPosition,
            collectionEntry = QueueCollectionEntry.SavedTracks,
        )
    }

    private suspend fun fetchAllAlbumTracks(albumId: String): List<QueueEntry> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = albumRepository.getAlbumTracks(albumId)
        pagination?.items?.let { allTracks.addAll(it) }

        while (pagination?.nextPagination != null) {
            pagination = albumRepository.getAlbumTracks(albumId, pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }

        val blacklistedTrackIds = blacklistRepository.getTracksSnapshot().map { it.id }.toSet()
        val blacklistedArtistIds = blacklistRepository.getArtistsSnapshot().map { it.id }.toSet()

        val filteredTracks = allTracks.filter { track ->
            !isTrackBlacklisted(track, blacklistedTrackIds, blacklistedArtistIds)
        }
        return filteredTracks.map { track ->
            QueueEntry.StreamingTrack(track = track, url = "")
        }
    }

    private suspend fun fetchAllPlaylistTracks(playlistId: String): List<QueueEntry> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = playlistRepository.getPlaylistTracks(playlistId)
        pagination?.items?.let { allTracks.addAll(it) }

        while (pagination?.nextPagination != null) {
            pagination = playlistRepository.getPlaylistTracks(playlistId, pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }

        val blacklistedTracks = blacklistRepository.getTracksSnapshot()
        val blacklistedArtists = blacklistRepository.getArtistsSnapshot()
        val blacklistedTrackIds = blacklistedTracks.map { it.id }.toSet()
        val blacklistedArtistIds = blacklistedArtists.map { it.id }.toSet()

        val filteredTracks = allTracks.filter { track ->
            !isTrackBlacklisted(track, blacklistedTrackIds, blacklistedArtistIds)
        }
        
        return filteredTracks.map { track ->
            QueueEntry.StreamingTrack(track = track, url = "")
        }
    }

    /**
     * Resolves the tracks of a collection without loading them into the local
     * queue — used to suggest a collection into a jam session from a guest.
     */
    suspend fun resolveCollectionTracks(type: RemoteCollectionType, id: String): List<MetadataTrack> =
        when (type) {
            RemoteCollectionType.Playlist -> fetchAllPlaylistTracks(id).asTracks()
            RemoteCollectionType.Album -> fetchAllAlbumTracks(id).asTracks()
            RemoteCollectionType.ArtistTopTracks -> fetchArtistTopTracks(id).asTracks()
            RemoteCollectionType.SavedTracks -> fetchAllSavedTracks().asTracks()
        }

    private fun List<QueueEntry>.asTracks(): List<MetadataTrack> =
        mapNotNull { (it as? QueueEntry.StreamingTrack)?.track }

    private suspend fun fetchAllSavedTracks(): List<QueueEntry> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = savedTracksRepository.getSavedTracks()
        pagination?.items?.let { allTracks.addAll(it) }

        while (pagination?.nextPagination != null) {
            pagination = savedTracksRepository.getSavedTracks(pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }

        val blacklistedTrackIds = blacklistRepository.getTracksSnapshot().map { it.id }.toSet()
        val blacklistedArtistIds = blacklistRepository.getArtistsSnapshot().map { it.id }.toSet()

        val filteredTracks = allTracks.filter { track ->
            !isTrackBlacklisted(track, blacklistedTrackIds, blacklistedArtistIds)
        }
        return filteredTracks.map { track ->
            QueueEntry.StreamingTrack(track = track, url = "")
        }
    }

    private suspend fun fetchArtistTopTracks(artistId: String): List<QueueEntry> {
        val tracks = artistRepository.topTracks(artistId).orEmpty()
        val blacklistedTrackIds = blacklistRepository.getTracksSnapshot().map { it.id }.toSet()
        val blacklistedArtistIds = blacklistRepository.getArtistsSnapshot().map { it.id }.toSet()
        return tracks
            .filter { track -> !isTrackBlacklisted(track, blacklistedTrackIds, blacklistedArtistIds) }
            .map { track -> QueueEntry.StreamingTrack(track = track, url = "") }
    }

    private fun isTrackBlacklisted(
        track: MetadataTrack,
        blacklistedTrackIds: Set<String>,
        blacklistedArtistIds: Set<String>,
    ): Boolean {
        return track.id in blacklistedTrackIds || track.artists.any { it.id in blacklistedArtistIds }
    }

    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) {
            return id == other.id
        }

        return title == other.title &&
                durationMs == other.durationMs &&
                album?.id == other.album?.id &&
                artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}
