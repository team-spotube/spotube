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

package org.spotube.js_plugin_example.plugin_apis.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser

private data class ArtistEntity(
    val id: String,
    val name: String,
    val genres: List<String>,
    val biography: String,
    val followersCount: Int,
)

private data class AlbumEntity(
    val id: String,
    val title: String,
    val description: String,
    val albumType: MetadataAlbumType,
    val artistIds: List<String>,
    val releaseDate: String,
    val genres: List<String>,
    val trackIds: MutableList<String>,
)

private data class TrackEntity(
    val id: String,
    val title: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val artistIds: List<String>,
    val albumId: String,
    val explicit: Boolean,
    val popularity: Int,
    val isrcCode: String,
)

private data class UserEntity(
    val id: String,
    val username: String,
    var displayName: String,
)

private data class PlaylistEntity(
    val id: String,
    var title: String,
    var description: String?,
    var ownerId: String,
    var thumbnail: String,
    var isPublic: Boolean,
    var isCollaborating: Boolean,
    val trackIds: MutableList<String>,
)

object FakeMetadataStore {
    const val SECTION_NEW_RELEASES = "new-releases"
    const val SECTION_TOP_ARTISTS = "top-artists"
    const val SECTION_FEATURED_PLAYLISTS = "featured-playlists"

    private const val CURRENT_USER_ID = "user-1"

    private val artists = linkedMapOf<String, ArtistEntity>()
    private val albums = linkedMapOf<String, AlbumEntity>()
    private val tracks = linkedMapOf<String, TrackEntity>()
    private val users = linkedMapOf<String, UserEntity>()
    private val playlists = linkedMapOf<String, PlaylistEntity>()

    private val savedTrackIds = linkedSetOf<String>()
    private val savedAlbumIds = linkedSetOf<String>()
    private val savedArtistIds = linkedSetOf<String>()
    private val savedPlaylistIds = linkedSetOf<String>()

    private var playlistCounter = 1000

    init {
        seed()
    }

    fun getUser(id: String): MetadataUser? = users[id]?.toModel()

    fun getCurrentUser(): MetadataUser? = users[CURRENT_USER_ID]?.toModel()

    fun getTrack(id: String): MetadataTrack = tracks[id]?.toModel()
        ?: error("Track not found: $id")

    fun getAlbum(id: String): MetadataAlbum.Detailed = albums[id]?.toDetailedModel()
        ?: error("Album not found: $id")

    fun getArtist(id: String): MetadataArtist.Detailed = artists[id]?.toDetailedModel()
        ?: error("Artist not found: $id")

    fun getArtistTopTracks(id: String): List<MetadataTrack> {
        return tracks.values
            .filter { id in it.artistIds }
            .sortedByDescending { it.popularity }
            .take(10)
            .map { it.toModel() }
    }

    fun getArtistAlbums(id: String): List<MetadataAlbum.Detailed> {
        return albums.values
            .filter { id in it.artistIds }
            .sortedByDescending { it.releaseDate }
            .map { it.toDetailedModel() }
    }

    fun getAlbumTracks(id: String): List<MetadataTrack> {
        val album = albums[id] ?: error("Album not found: $id")
        return album.trackIds.mapNotNull { trackId -> tracks[trackId]?.toModel() }
    }

    fun getPlaylist(id: String): MetadataPlaylist = playlists[id]?.toModel()
        ?: error("Playlist not found: $id")

    fun getPlaylistTracks(id: String): List<MetadataTrack> {
        val playlist = playlists[id] ?: error("Playlist not found: $id")
        return playlist.trackIds.mapNotNull { trackId -> tracks[trackId]?.toModel() }
    }

    fun getFeaturedItems(): List<MetadataBrowseItem> {
        val featuredTrack = tracks.values.maxByOrNull { it.popularity }?.toModel()
        val featuredAlbum = albums.values.maxByOrNull { it.trackIds.size }?.toBasicModel()
        val featuredArtist = artists.values.maxByOrNull { it.followersCount }?.toBasicModel()
        val featuredPlaylist = playlists.values.firstOrNull()?.toModel()

        return listOfNotNull(
            featuredTrack?.let { MetadataBrowseItem.Track(it) },
            featuredAlbum?.let { MetadataBrowseItem.Album(it) },
            featuredArtist?.let { MetadataBrowseItem.Artist(it) },
            featuredPlaylist?.let { MetadataBrowseItem.Playlist(it) },
        )
    }

    fun getBrowseSections(): List<MetadataBrowseSection> {
        val newReleases = albums.values
            .sortedByDescending { it.releaseDate }
            .take(8)
            .map { MetadataBrowseItem.Album(it.toBasicModel()) }

        val topArtists = artists.values
            .sortedByDescending { it.followersCount }
            .take(8)
            .map { MetadataBrowseItem.Artist(it.toBasicModel()) }

        val featuredPlaylists = playlists.values
            .take(8)
            .map { MetadataBrowseItem.Playlist(it.toModel()) }

        return listOf(
            MetadataBrowseSection(
                title = "New Releases",
                description = "Recently released albums",
                items = newReleases,
                moreLink = "https://example.com/new-releases",
            ),
            MetadataBrowseSection(
                title = "Top Artists",
                description = "Trending artists in this simulation",
                items = topArtists,
                moreLink = "https://example.com/top-artists",
            ),
            MetadataBrowseSection(
                title = "Featured Playlists",
                description = "Curated playlists from linked track data",
                items = featuredPlaylists,
                moreLink = "https://example.com/featured-playlists",
            ),
        )
    }

    fun getBrowseSublist(sectionId: String): List<MetadataBrowseItem> {
        return when (sectionId) {
            SECTION_NEW_RELEASES -> albums.values
                .sortedByDescending { it.releaseDate }
                .map { MetadataBrowseItem.Album(it.toBasicModel()) }

            SECTION_TOP_ARTISTS -> artists.values
                .sortedByDescending { it.followersCount }
                .map { MetadataBrowseItem.Artist(it.toBasicModel()) }

            SECTION_FEATURED_PLAYLISTS -> playlists.values
                .map { MetadataBrowseItem.Playlist(it.toModel()) }

            else -> tracks.values
                .sortedByDescending { it.popularity }
                .map { MetadataBrowseItem.Track(it.toModel()) }
        }
    }

    fun search(query: String): List<MetadataSearchResult> {
        val q = query.trim().lowercase()
        val trackHit = tracks.values.firstOrNull { it.title.lowercase().contains(q) }
        val artistHit = artists.values.firstOrNull { it.name.lowercase().contains(q) }
        val albumHit = albums.values.firstOrNull { it.title.lowercase().contains(q) }
        val playlistHit = playlists.values.firstOrNull { it.title.lowercase().contains(q) }
        val userHit = users.values.firstOrNull {
            it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }

        return listOfNotNull(
            trackHit?.let { MetadataSearchResult.Track(it.toModel()) },
            artistHit?.let { MetadataSearchResult.Artist(it.toBasicModel()) },
            albumHit?.let { MetadataSearchResult.Album(it.toBasicModel()) },
            playlistHit?.let { MetadataSearchResult.Playlist(it.toModel()) },
            userHit?.let { MetadataSearchResult.User(it.toModel()) },
        )
    }

    fun searchTracks(query: String): List<MetadataSearchResult.Track> {
        val q = query.trim().lowercase()
        return tracks.values
            .filter { it.title.lowercase().contains(q) }
            .map { MetadataSearchResult.Track(it.toModel()) }
    }

    fun searchArtists(query: String): List<MetadataSearchResult.Artist> {
        val q = query.trim().lowercase()
        return artists.values
            .filter { it.name.lowercase().contains(q) }
            .map { MetadataSearchResult.Artist(it.toBasicModel()) }
    }

    fun searchAlbums(query: String): List<MetadataSearchResult.Album> {
        val q = query.trim().lowercase()
        return albums.values
            .filter { it.title.lowercase().contains(q) }
            .map { MetadataSearchResult.Album(it.toBasicModel()) }
    }

    fun searchPlaylists(query: String): List<MetadataSearchResult.Playlist> {
        val q = query.trim().lowercase()
        return playlists.values
            .filter { it.title.lowercase().contains(q) }
            .map { MetadataSearchResult.Playlist(it.toModel()) }
    }

    fun searchUsers(query: String): List<MetadataSearchResult.User> {
        val q = query.trim().lowercase()
        return users.values
            .filter {
                it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
            }
            .map { MetadataSearchResult.User(it.toModel()) }
    }

    fun savedTracks(): List<MetadataTrack> {
        return savedTrackIds
            .mapNotNull { trackId -> tracks[trackId]?.toModel() }
    }

    fun savedAlbums(): List<MetadataAlbum.Detailed> {
        return savedAlbumIds
            .mapNotNull { albumId -> albums[albumId]?.toDetailedModel() }
    }

    fun savedArtists(): List<MetadataArtist.Detailed> {
        return savedArtistIds
            .mapNotNull { artistId -> artists[artistId]?.toDetailedModel() }
    }

    fun savedPlaylists(): List<MetadataPlaylist> {
        return savedPlaylistIds
            .mapNotNull { playlistId -> playlists[playlistId]?.toModel() }
    }

    fun isSavedTracks(ids: List<String>): List<Boolean> = ids.map { it in savedTrackIds }
    fun isSavedAlbums(ids: List<String>): List<Boolean> = ids.map { it in savedAlbumIds }
    fun isSavedArtists(ids: List<String>): List<Boolean> = ids.map { it in savedArtistIds }
    fun isSavedPlaylists(ids: List<String>): List<Boolean> = ids.map { it in savedPlaylistIds }

    fun saveTracks(ids: List<String>) {
        ids.filterTo(savedTrackIds) { it in tracks }
    }

    fun removeSavedTracks(ids: List<String>) {
        savedTrackIds.removeAll(ids.toSet())
    }

    fun saveAlbums(ids: List<String>) {
        ids.filterTo(savedAlbumIds) { it in albums }
    }

    fun removeSavedAlbums(ids: List<String>) {
        savedAlbumIds.removeAll(ids.toSet())
    }

    fun saveArtists(ids: List<String>) {
        ids.filterTo(savedArtistIds) { it in artists }
    }

    fun removeSavedArtists(ids: List<String>) {
        savedArtistIds.removeAll(ids.toSet())
    }

    fun savePlaylists(ids: List<String>) {
        ids.filterTo(savedPlaylistIds) { it in playlists }
    }

    fun removeSavedPlaylists(ids: List<String>) {
        savedPlaylistIds.removeAll(ids.toSet())
    }

    fun createPlaylist(
        name: String,
        description: String?,
        isPublic: Boolean,
        isCollaborating: Boolean,
        imageBase64: String,
        trackIds: List<String>,
    ): MetadataPlaylist {
        val id = "playlist-${playlistCounter++}"
        val resolvedTracks = trackIds.filter { it in tracks }
        playlists[id] = PlaylistEntity(
            id = id,
            title = name,
            description = description,
            ownerId = CURRENT_USER_ID,
            thumbnail = imageBase64.takeIf { it.isNotBlank() }
                ?: "https://picsum.photos/seed/$id/300/300",
            isPublic = isPublic,
            isCollaborating = isCollaborating,
            trackIds = resolvedTracks.toMutableList(),
        )
        return playlists.getValue(id).toModel()
    }

    fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        isCollaborating: Boolean?,
        imageBase64: String?,
        trackIds: List<String>?,
    ): MetadataPlaylist {
        val playlist = playlists[id] ?: error("Playlist not found: $id")
        if (name != null) playlist.title = name
        if (description != null) playlist.description = description
        if (isPublic != null) playlist.isPublic = isPublic
        if (isCollaborating != null) playlist.isCollaborating = isCollaborating
        if (imageBase64 != null) playlist.thumbnail = imageBase64
        if (trackIds != null) {
            playlist.trackIds.clear()
            playlist.trackIds.addAll(trackIds.filter { it in tracks })
        }
        return playlist.toModel()
    }

    fun deletePlaylist(id: String) {
        playlists.remove(id)
        savedPlaylistIds.remove(id)
    }

    fun recommendationsBasedOnTracks(seedTrackIds: List<String>, limit: Int): List<MetadataTrack> {
        if (tracks.isEmpty()) return emptyList()
        val seedTracks = seedTrackIds.mapNotNull { tracks[it] }
        val seedArtistIds = seedTracks.flatMap { it.artistIds }.toSet()
        val seedAlbumIds = seedTracks.map { it.albumId }.toSet()

        val ranked = tracks.values
            .asSequence()
            .filter { it.id !in seedTrackIds.toSet() }
            .sortedWith(
                compareByDescending<TrackEntity> { track ->
                    track.artistIds.count { it in seedArtistIds } * 10 +
                            (if (track.albumId in seedAlbumIds) 3 else 0) +
                            track.popularity
                }
            )
            .take(limit)
            .map { it.toModel() }
            .toList()

        return if (ranked.isEmpty()) {
            tracks.values.take(limit).map { it.toModel() }
        } else ranked
    }

    fun <T> paginate(
        items: List<T>,
        pagination: PaginationStrategy? = PaginationStrategy.Offset(0, 20)
    ): PaginationResult<T> {
        if (pagination is PaginationStrategy.Offset) {
            val safeOffset = pagination.offset.coerceAtLeast(0)
            val safePageSize = pagination.limit.coerceAtLeast(1)
            val paged = items.drop(safeOffset).take(safePageSize)
            val nextOffset =
                if (safeOffset + paged.size < items.size) safeOffset + paged.size else null
            return PaginationResult(
                items = paged,
                totalCount = items.size,
                nextPagination = PaginationStrategy.Offset(nextOffset ?: 0, safePageSize)
            )
        }
        return PaginationResult(
            items = items,
            totalCount = items.size,
            nextPagination = null
        )
    }

    private fun seed() {
        users["user-1"] = UserEntity("user-1", "demo_user", "Demo User")
        users["user-2"] = UserEntity("user-2", "mixmaster", "Mix Master")

        repeat(6) { index ->
            val id = "artist-${index + 1}"
            artists[id] = ArtistEntity(
                id = id,
                name = "Sim Artist ${index + 1}",
                genres = listOf("Pop", "Electronic", "Rock").shuffled().take(2),
                biography = "Simulated biography for ${index + 1}.",
                followersCount = 50_000 * (index + 1),
            )
        }

        var trackCounter = 1
        var albumCounter = 1
        artists.values.forEachIndexed { idx, artist ->
            repeat(2) { releaseIndex ->
                val albumId = "album-$albumCounter"
                val albumTrackIds = mutableListOf<String>()
                val album = AlbumEntity(
                    id = albumId,
                    title = "${artist.name} Album ${releaseIndex + 1}",
                    description = "Simulated album ${releaseIndex + 1} by ${artist.name}",
                    albumType = if (releaseIndex == 0) MetadataAlbumType.Album else MetadataAlbumType.Single,
                    artistIds = listOf(artist.id),
                    releaseDate = "202${idx % 4 + 1}-0${releaseIndex + 1}-15",
                    genres = artist.genres,
                    trackIds = albumTrackIds,
                )
                albums[albumId] = album

                repeat(5) { trackIndex ->
                    val trackId = "track-$trackCounter"
                    tracks[trackId] = TrackEntity(
                        id = trackId,
                        title = "${artist.name} Track ${trackIndex + 1}",
                        durationMs = 180_000L + (trackIndex * 12_000L),
                        trackNumber = trackIndex + 1,
                        discNumber = 1,
                        artistIds = listOf(artist.id),
                        albumId = albumId,
                        explicit = trackIndex % 4 == 0,
                        popularity = (95 - trackCounter).coerceAtLeast(35),
                        isrcCode = "USSIM${trackCounter.toString().padStart(6, '0')}",
                    )
                    albumTrackIds += trackId
                    trackCounter++
                }
                albumCounter++
            }
        }

        playlists["playlist-1"] = PlaylistEntity(
            id = "playlist-1",
            title = "Morning Flow",
            description = "A smooth mix to start the day.",
            ownerId = "user-2",
            thumbnail = "https://picsum.photos/seed/playlist-1/300/300",
            isPublic = true,
            isCollaborating = false,
            trackIds = tracks.keys.take(10).toMutableList(),
        )

        playlists["playlist-2"] = PlaylistEntity(
            id = "playlist-2",
            title = "Workout Pulse",
            description = "Higher energy tracks for workouts.",
            ownerId = "user-1",
            thumbnail = "https://picsum.photos/seed/playlist-2/300/300",
            isPublic = true,
            isCollaborating = true,
            trackIds = tracks.keys.drop(8).take(12).toMutableList(),
        )

        savedTrackIds += tracks.keys.take(5)
        savedAlbumIds += albums.keys.take(2)
        savedArtistIds += artists.keys.take(2)
        savedPlaylistIds += playlists.keys.take(1)
    }

    private fun ArtistEntity.toBasicModel(): MetadataArtist.Basic {
        return MetadataArtist.Basic(
            id = id,
            name = name,
            thumbnails = listOf(Thumbnail("https://picsum.photos/seed/$id/200/200", 200, 200)),
            externalUri = "https://example.com/artist/$id",
        )
    }

    private fun ArtistEntity.toDetailedModel(): MetadataArtist.Detailed {
        return MetadataArtist.Detailed(
            id = id,
            name = name,
            thumbnails = listOf(Thumbnail("https://picsum.photos/seed/$id/200/200", 200, 200)),
            externalUri = "https://example.com/artist/$id",
            genres = genres,
            biography = biography,
            followersCount = followersCount,
        )
    }

    private fun AlbumEntity.toBasicModel(): MetadataAlbum.Basic {
        return MetadataAlbum.Basic(
            id = id,
            title = title,
            description = description,
            thumbnails = listOf(Thumbnail("https://picsum.photos/seed/$id/300/300", 300, 300)),
            albumType = albumType,
            artists = artistIds.mapNotNull { artists[it]?.toBasicModel() },
            externalUri = "https://example.com/album/$id",
        )
    }

    private fun AlbumEntity.toDetailedModel(): MetadataAlbum.Detailed {
        return MetadataAlbum.Detailed(
            id = id,
            title = title,
            description = description,
            thumbnails = listOf(Thumbnail("https://picsum.photos/seed/$id/300/300", 300, 300)),
            albumType = albumType,
            artists = artistIds.mapNotNull { artists[it]?.toBasicModel() },
            externalUri = "https://example.com/album/$id",
            releaseDate = releaseDate,
            genres = genres,
            trackCount = trackIds.size,
        )
    }

    private fun TrackEntity.toModel(): MetadataTrack {
        return MetadataTrack(
            id = id,
            title = title,
            durationMs = durationMs,
            trackNumber = trackNumber,
            discNumber = discNumber,
            artists = artistIds.mapNotNull { artists[it]?.toBasicModel() },
            album = albums.getValue(albumId).toDetailedModel(),
            explicit = explicit,
            popularity = popularity,
            isrcCode = isrcCode,
            externalUri = "https://example.com/track/$id",
            thumbnails = null
        )
    }

    private fun UserEntity.toModel(): MetadataUser {
        return MetadataUser(
            id = id,
            username = username,
            displayName = displayName,
            thumbnails = listOf(Thumbnail("https://picsum.photos/seed/$id/100/100", 100, 100)),
            externalUri = "https://example.com/user/$id",
        )
    }

    private fun PlaylistEntity.toModel(): MetadataPlaylist {
        return MetadataPlaylist(
            id = id,
            title = title,
            description = description,
            thumbnails = listOf(Thumbnail(thumbnail, 300, 300)),
            trackCount = trackIds.count { it in tracks },
            externalUri = "https://example.com/playlist/$id",
            owner = users[ownerId]?.toModel(),
        )
    }
}

