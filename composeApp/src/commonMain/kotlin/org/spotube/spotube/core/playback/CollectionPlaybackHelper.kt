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

package org.spotube.spotube.core.playback

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.album.AlbumRepository
import org.spotube.spotube.modules.artist.ArtistRepository
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import org.spotube.spotube.modules.playlist.PlaylistRepository
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import org.spotube.spotube.core.remote.RemoteCollectionType

class CollectionPlaybackHelper(
    private val albumRepository: AlbumRepository,
    private val playlistRepository: PlaylistRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val artistRepository: ArtistRepository,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository) {
    suspend fun playAlbum(albumId: String, shuffle: Boolean = false) {
        if (!shuffle && audioPlayerQueue.isAlbumPlaying(albumId)) return
        val entries = fetchAllAlbumTracks(albumId).let { if (shuffle) it.shuffled() else it }
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.Album(albumId))
        }
    }

    suspend fun addAlbumToQueue(albumId: String) {
        val entries = fetchAllAlbumTracks(albumId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(
                entries,
                collectionEntry = QueueCollectionEntry.Album(albumId))
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
            collectionEntry = QueueCollectionEntry.Album(albumId))
    }

    suspend fun playPlaylist(playlistId: String, shuffle: Boolean = false) {
        if (!shuffle && audioPlayerQueue.isPlaylistPlaying(playlistId)) return
        val entries = fetchAllPlaylistTracks(playlistId).let { if (shuffle) it.shuffled() else it }
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.Playlist(playlistId))
        }
    }

    suspend fun addPlaylistToQueue(playlistId: String) {
        val entries = fetchAllPlaylistTracks(playlistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.addAllToQueue(
                entries,
                collectionEntry = QueueCollectionEntry.Playlist(playlistId))
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
            collectionEntry = QueueCollectionEntry.Playlist(playlistId))
    }

    suspend fun playSavedTracks(shuffle: Boolean = false) {
        if (!shuffle && audioPlayerQueue.isSavedTracksPlaying()) return
        val entries = fetchAllSavedTracks().let { if (shuffle) it.shuffled() else it }
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = QueueCollectionEntry.SavedTracks)
        }
    }

    suspend fun playArtistTopTracks(artistId: String) {
        val entries = fetchArtistTopTracks(artistId)
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = 0,
                collectionEntry = null)
        }
    }

    suspend fun playArtistTopTracksFromTrack(artistId: String, track: MetadataTrack) {
        val entries = fetchArtistTopTracks(artistId)
        if (entries.isEmpty()) return

        when (val start = planStartFromTrack(audioPlayerQueue.getQueue(), entries, track)) {
            is StartFromTrack.Jump -> audioPlayerQueue.jumpTo(start.index)
            is StartFromTrack.Load -> audioPlayerQueue.load(
                entries = entries,
                autoPlay = true,
                startPosition = start.startPosition,
                collectionEntry = null)
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
                collectionEntry = QueueCollectionEntry.SavedTracks)
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
            collectionEntry = QueueCollectionEntry.SavedTracks)
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
        blacklistedArtistIds: Set<String>): Boolean {
        return track.id in blacklistedTrackIds || track.artists.any { it.id in blacklistedArtistIds }
    }
}

internal sealed interface StartFromTrack {
    /** The queue already holds this list: just jump to the tapped track. */
    data class Jump(val index: Int) : StartFromTrack
    /** Load the list and start at the tapped track. */
    data class Load(val startPosition: Int) : StartFromTrack
}

/**
 * Where to start when [track] is tapped in a list of [entries] that has no collection entry
 * (e.g. an artist's top tracks). The queue only counts as "already this list" when it holds
 * exactly the same tracks in the same order; otherwise a matching track that happens to be in
 * some other playlist would be jumped to inside that playlist. A track not found in [entries]
 * starts from the top.
 */
internal fun planStartFromTrack(
    queue: List<QueueEntry>,
    entries: List<QueueEntry>,
    track: MetadataTrack): StartFromTrack {
    val startPosition = entries.indexOfFirst { it.streamingTrack?.matchesTrack(track) == true }.coerceAtLeast(0)
    val isSameQueue = queue.size == entries.size && queue.zip(entries).all { (queued, entry) ->
        val queuedTrack = queued.streamingTrack
        val entryTrack = entry.streamingTrack
        queuedTrack != null && entryTrack != null && queuedTrack.matchesTrack(entryTrack)
    }
    return if (isSameQueue) StartFromTrack.Jump(startPosition) else StartFromTrack.Load(startPosition)
}

private val QueueEntry.streamingTrack: MetadataTrack?
    get() = (this as? QueueEntry.StreamingTrack)?.track

private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
    if (id.isNotBlank() && other.id.isNotBlank()) {
        return id == other.id
    }

    return title == other.title &&
            durationMs == other.durationMs &&
            album?.id == other.album?.id &&
            artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
}
