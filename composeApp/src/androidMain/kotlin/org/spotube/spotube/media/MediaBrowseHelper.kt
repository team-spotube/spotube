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

package org.spotube.spotube.media

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.server.LocalServer
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.playback.CollectionPlaybackHelper
import org.spotube.spotube.modules.album.AlbumRepository
import org.spotube.spotube.modules.blacklist.BlacklistRepository
import org.spotube.spotube.modules.home.HomeScreenRepository
import org.spotube.spotube.modules.library.LibraryRepository
import org.spotube.spotube.modules.playlist.PlaylistRepository
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.saved_tracks.SavedTracksRepository
import org.spotube.spotube.modules.search.SearchRepository
import org.spotube.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.library_tab_albums
import spotube.composeapp.generated.resources.library_tab_artists
import spotube.composeapp.generated.resources.library_tab_playlists
import spotube.composeapp.generated.resources.library_toolbar_compact_title
import spotube.composeapp.generated.resources.media_browser_browse
import spotube.composeapp.generated.resources.media_browser_featured
import spotube.composeapp.generated.resources.media_browser_now_playing
import spotube.composeapp.generated.resources.media_browser_saved_tracks
import spotube.composeapp.generated.resources.media_browser_top_tracks

class MediaBrowseHelper(
    private val pluginManager: PluginManager,
    private val homeScreenRepository: HomeScreenRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val savedTracksRepository: SavedTracksRepository,
    private val searchRepository: SearchRepository,
    private val collectionPlaybackHelper: CollectionPlaybackHelper,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsRepository: SettingsRepository,
    private val blacklistRepository: BlacklistRepository) {

    companion object {
        const val MEDIA_ID_ROOT = "root"
        const val MEDIA_ID_BROWSE = "browse"
        const val MEDIA_ID_BROWSE_GENRE = "browse:genre"
        const val MEDIA_ID_BROWSE_FEATURED = "browse:featured"
        const val MEDIA_ID_LIBRARY = "library"
        const val MEDIA_ID_SAVED_PLAYLISTS = "library:playlists"
        const val MEDIA_ID_SAVED_ALBUMS = "library:albums"
        const val MEDIA_ID_SAVED_ARTISTS = "library:artists"
        const val MEDIA_ID_SAVED_TRACKS = "library:tracks"
        const val MEDIA_ID_PLAYLIST = "playlist"
        const val MEDIA_ID_ALBUM = "album"
        const val MEDIA_ID_ARTIST = "artist"
        const val MEDIA_ID_ARTIST_TRACKS = "artist:tracks"
        const val MEDIA_ID_ARTIST_ALBUMS = "artist:albums"
        const val MEDIA_ID_TRACK = "track"
        const val MEDIA_ID_QUEUE = "queue"

        private const val CONTENT_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_GRID = 2
        private const val CONTENT_STYLE_GROUP_TITLE = "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT"
    }

    fun buildRootItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Spotube")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        Log.d("MediaBrowseHelper", "Getting children for parentId: $parentId")
        return when {
            parentId == MEDIA_ID_ROOT -> buildRootChildren()
            parentId == MEDIA_ID_BROWSE -> buildBrowseItems()
            parentId == MEDIA_ID_BROWSE_FEATURED -> buildFeaturedItems()
            parentId.startsWith("$MEDIA_ID_BROWSE_GENRE:") -> {
                val genreId = parentId.removePrefix("$MEDIA_ID_BROWSE_GENRE:")
                buildBrowseGenreItems(genreId)
            }
            parentId == MEDIA_ID_LIBRARY -> buildLibraryRootChildren()
            parentId == MEDIA_ID_SAVED_PLAYLISTS -> buildSavedPlaylistsItems()
            parentId == MEDIA_ID_SAVED_ALBUMS -> buildSavedAlbumsItems()
            parentId == MEDIA_ID_SAVED_ARTISTS -> buildSavedArtistsItems()
            parentId == MEDIA_ID_SAVED_TRACKS -> buildSavedTracksItems()
            parentId.startsWith("$MEDIA_ID_PLAYLIST:") -> {
                val playlistId = parentId.removePrefix("$MEDIA_ID_PLAYLIST:")
                buildPlaylistTracksItems(playlistId)
            }
            parentId.startsWith("$MEDIA_ID_ALBUM:") -> {
                val albumId = parentId.removePrefix("$MEDIA_ID_ALBUM:")
                buildAlbumTracksItems(albumId)
            }
            parentId.startsWith("$MEDIA_ID_ARTIST:") -> {
                val artistId = parentId.removePrefix("$MEDIA_ID_ARTIST:")
                buildArtistOverviewItems(artistId)
            }
            parentId.startsWith("$MEDIA_ID_ARTIST_TRACKS:") -> {
                val artistId = parentId.removePrefix("$MEDIA_ID_ARTIST_TRACKS:")
                buildArtistTracksItems(artistId)
            }
            parentId.startsWith("$MEDIA_ID_ARTIST_ALBUMS:") -> {
                val artistId = parentId.removePrefix("$MEDIA_ID_ARTIST_ALBUMS:")
                buildArtistAlbumsItems(artistId)
            }
            parentId == MEDIA_ID_QUEUE -> buildQueueItems()
            else -> emptyList()
        }
    }

    suspend fun search(query: String): List<MediaItem> {
        val results = try {
            searchRepository.searchAll(query)
        } catch (_: Exception) {
            return emptyList()
        }
        val items = mutableListOf<MediaItem>()
        for (result in results) {
            val item = when (result) {
                is MetadataSearchResult.Track ->
                    buildTrackMediaItem(result.data)

                is MetadataSearchResult.Album ->
                    buildAlbumMediaItem(result.data)

                is MetadataSearchResult.Artist ->
                    buildArtistMediaItem(result.data)

                is MetadataSearchResult.Playlist ->
                    buildPlaylistMediaItem(result.data)

                is MetadataSearchResult.User -> null
            }
            if (item != null) {
                items.add(item)
            }
        }
        return items
    }

    suspend fun resolveTrackById(trackId: String): MetadataTrack? {
        return getTrackById(trackId)
    }

    suspend fun resolveTrackToMediaItem(trackId: String): MediaItem? {
        val track = getTrackById(trackId) ?: return null
        val url = buildStreamingUrl(track.id) ?: return null
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_TRACK:${track.id}")
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artists.joinToString(", ") { it.name })
                    .setAlbumTitle(track.album?.title)
                    .setArtworkUri(track.album?.thumbnails?.firstOrNull()?.url?.let { Uri.parse(it) })
                    .setTrackNumber(track.trackNumber)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    suspend fun resolveAndPlayTrack(track: MetadataTrack) {
        val entry = QueueEntry.StreamingTrack(track = track, url = "")
        audioPlayerQueue.load(listOf(entry), autoPlay = true, startPosition = 0)
    }

    suspend fun resolveAndPlayFromMediaId(mediaId: String): Boolean {
        return when {
            mediaId.startsWith("$MEDIA_ID_TRACK:") -> {
                val trackId = mediaId.removePrefix("$MEDIA_ID_TRACK:")
                val track = getTrackById(trackId) ?: return false
                resolveAndPlayTrack(track)
                true
            }
            mediaId.startsWith("$MEDIA_ID_PLAYLIST:") -> {
                val playlistId = mediaId.removePrefix("$MEDIA_ID_PLAYLIST:")
                collectionPlaybackHelper.playPlaylist(playlistId)
                true
            }
            mediaId.startsWith("$MEDIA_ID_ALBUM:") -> {
                val albumId = mediaId.removePrefix("$MEDIA_ID_ALBUM:")
                collectionPlaybackHelper.playAlbum(albumId)
                true
            }
            mediaId == MEDIA_ID_SAVED_TRACKS -> {
                collectionPlaybackHelper.playSavedTracks()
                true
            }
            mediaId.startsWith("$MEDIA_ID_ARTIST_TRACKS:") -> {
                val artistId = mediaId.removePrefix("$MEDIA_ID_ARTIST_TRACKS:")
                playArtistTracks(artistId)
                true
            }
            else -> false
        }
    }

    private suspend fun buildRootChildren(): List<MediaItem> {
        return listOf(
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_BROWSE)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.media_browser_browse))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_LIBRARY)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.library_toolbar_compact_title))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_QUEUE)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.media_browser_now_playing))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build())
    }

    private suspend fun buildBrowseItems(): List<MediaItem> {
        val gridExtras = Bundle().apply { putInt(CONTENT_STYLE_BROWSABLE, CONTENT_STYLE_GRID) }
        val items = mutableListOf<MediaItem>()

        items.add(
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_BROWSE_FEATURED)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.media_browser_featured))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setExtras(gridExtras)
                        .build()
                )
                .build()
        )

        val genres = try {
            homeScreenRepository.genres() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        for (genre in genres) {
            items.add(
                MediaItem.Builder()
                    .setMediaId("$MEDIA_ID_BROWSE_GENRE:${genre.id}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(genre.name)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setExtras(gridExtras)
                            .build()
                    )
                    .build()
            )
        }

        return items
    }

    private suspend fun buildFeaturedItems(): List<MediaItem> {
        val featured = try {
            homeScreenRepository.featuredItems() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return featured.mapNotNull { item -> browseItemToMediaItem(item) }
    }

    private suspend fun buildBrowseGenreItems(genreId: String): List<MediaItem> {
        val sections = try {
            homeScreenRepository.list(genreId)?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val items = mutableListOf<MediaItem>()

        for (section in sections) {
            for (item in section.items) {
                val mediaItem = browseItemToMediaItem(
                    when (item) {
                        is MetadataBrowseItem.Track -> item
                        is MetadataBrowseItem.Album -> item
                        is MetadataBrowseItem.Artist -> item
                        is MetadataBrowseItem.Playlist -> item
                        is MetadataBrowseItem.User -> null
                    } ?: continue
                ) ?: continue
                items.add(mediaItem.withGroupTitle(section.title))
            }
        }

        return items
    }

    private fun MediaItem.withGroupTitle(groupTitle: String): MediaItem {
        val existingExtras = mediaMetadata.extras ?: Bundle()
        existingExtras.putString(CONTENT_STYLE_GROUP_TITLE, groupTitle)
        return buildUpon()
            .setMediaMetadata(
                mediaMetadata.buildUpon()
                    .setExtras(existingExtras)
                    .build()
            )
            .build()
    }

    private suspend fun buildLibraryRootChildren(): List<MediaItem> {
        return listOf(
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_SAVED_PLAYLISTS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.library_tab_playlists))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_SAVED_ALBUMS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.library_tab_albums))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_SAVED_ARTISTS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.library_tab_artists))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MEDIA_ID_SAVED_TRACKS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.media_browser_saved_tracks))
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .build()
                )
                .build())
    }

    private suspend fun buildSavedPlaylistsItems(): List<MediaItem> {
        val result = try {
            libraryRepository.savedPlaylists()?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return result.map { buildPlaylistMediaItem(it) }
    }

    private suspend fun buildSavedAlbumsItems(): List<MediaItem> {
        val result = try {
            libraryRepository.savedAlbums()?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return result.map { buildAlbumMediaItem(it) }
    }

    private suspend fun buildSavedArtistsItems(): List<MediaItem> {
        val result = try {
            libraryRepository.savedArtists()?.items ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return result.map { buildArtistMediaItem(it) }
    }

    private suspend fun buildSavedTracksItems(): List<MediaItem> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = savedTracksRepository.getSavedTracks()
        pagination?.items?.let { allTracks.addAll(it) }
        while (pagination?.nextPagination != null) {
            pagination = savedTracksRepository.getSavedTracks(pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }
        return allTracks.map { buildTrackMediaItem(it) }
    }

    private suspend fun buildPlaylistTracksItems(playlistId: String): List<MediaItem> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = playlistRepository.getPlaylistTracks(playlistId)
        pagination?.items?.let { allTracks.addAll(it) }
        while (pagination?.nextPagination != null) {
            pagination = playlistRepository.getPlaylistTracks(playlistId, pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }
        return allTracks.map { buildTrackMediaItem(it) }
    }

    private suspend fun buildAlbumTracksItems(albumId: String): List<MediaItem> {
        val allTracks = mutableListOf<MetadataTrack>()
        var pagination = albumRepository.getAlbumTracks(albumId)
        pagination?.items?.let { allTracks.addAll(it) }
        while (pagination?.nextPagination != null) {
            pagination = albumRepository.getAlbumTracks(albumId, pagination.nextPagination)
            pagination?.items?.let { allTracks.addAll(it) }
        }
        return allTracks.map { buildTrackMediaItem(it) }
    }

    private suspend fun buildArtistOverviewItems(artistId: String): List<MediaItem> {
        return listOf(
            MediaItem.Builder()
                .setMediaId("$MEDIA_ID_ARTIST_TRACKS:$artistId")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.media_browser_top_tracks))
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId("$MEDIA_ID_ARTIST_ALBUMS:$artistId")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(Res.string.library_tab_albums))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build())
    }

    private suspend fun buildArtistTracksItems(artistId: String): List<MediaItem> {
        val plugin = pluginManager.selectedMetadataPlugin.value ?: return emptyList()
        val tracks = try {
            pluginManager.withScope {
                plugin.use { metadataArtistAPI.getArtistTop10Tracks(artistId) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        val items = mutableListOf<MediaItem>()
        for (track in tracks) {
            val item = buildTrackMediaItem(track)
            if (item != null) {
                items.add(item)
            }
        }
        return items
    }

    private suspend fun buildArtistAlbumsItems(artistId: String): List<MediaItem> {
        val plugin = pluginManager.selectedMetadataPlugin.value ?: return emptyList()
        val result = try {
            pluginManager.withScope {
                plugin.use { metadataArtistAPI.getArtistAlbums(artistId) }
            }
        } catch (_: Exception) {
            null
        }
        return result?.items?.map { buildAlbumMediaItem(it) } ?: emptyList()
    }

    private suspend fun buildQueueItems(): List<MediaItem> {
        val queue = audioPlayerQueue.queueFlow.value
        val items = mutableListOf<MediaItem>()
        for (entry in queue) {
            val item = when (entry) {
                is QueueEntry.StreamingTrack -> buildTrackMediaItem(entry.track)
                is QueueEntry.LocalTrack -> buildLocalTrackMediaItem(entry)
            }
            if (item != null) {
                items.add(item)
            }
        }
        return items
    }

    private suspend fun playArtistTracks(artistId: String) {
        val plugin = pluginManager.selectedMetadataPlugin.value ?: return
        val tracks = try {
            pluginManager.withScope {
                plugin.use { metadataArtistAPI.getArtistTop10Tracks(artistId) }
            }
        } catch (_: Exception) {
            return
        }
        
        val blacklistedTracks = blacklistRepository.getTracksSnapshot()
        val blacklistedArtists = blacklistRepository.getArtistsSnapshot()
        val blacklistedTrackIds = blacklistedTracks.map { it.id }.toSet()
        val blacklistedArtistIds = blacklistedArtists.map { it.id }.toSet()
        
        val filteredTracks = tracks.filter { track ->
            track.id !in blacklistedTrackIds && 
            track.artists.none { it.id in blacklistedArtistIds }
        }
        
        val entries = filteredTracks.map { QueueEntry.StreamingTrack(track = it, url = "") }
        if (entries.isNotEmpty()) {
            audioPlayerQueue.load(entries, autoPlay = true, startPosition = 0)
        }
    }

    private suspend fun getTrackById(trackId: String): MetadataTrack? {
        val plugin = pluginManager.selectedMetadataPlugin.value ?: return null
        return try {
            pluginManager.withScope {
                plugin.use { metadataTrackAPI.getTrack(trackId) }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildAlbumMediaItem(album: MetadataAlbum): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_ALBUM:${album.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(album.title)
                    .setArtist(album.artists.joinToString(", ") { it.name })
                    .setArtworkUri(album.thumbnails.firstOrNull()?.url?.let { Uri.parse(it) })
                    .setDescription(album.description)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun buildPlaylistMediaItem(playlist: MetadataPlaylist): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PLAYLIST:${playlist.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playlist.title)
                    .setArtist(playlist.owner?.displayName ?: playlist.owner?.username)
                    .setArtworkUri(playlist.thumbnails.firstOrNull()?.url?.let { Uri.parse(it) })
                    .setDescription(playlist.description)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun buildTrackMediaItem(track: MetadataTrack): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_TRACK:${track.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artists.joinToString(", ") { it.name })
                    .setAlbumTitle(track.album?.title)
                    .setArtworkUri(track.album?.thumbnails?.firstOrNull()?.url?.let { Uri.parse(it) })
                    .setTrackNumber(track.trackNumber)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    private fun buildArtistMediaItem(artist: MetadataArtist): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_ARTIST:${artist.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(artist.name)
                    .setArtworkUri(artist.thumbnails.firstOrNull()?.url?.let { Uri.parse(it) })
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun buildLocalTrackMediaItem(localTrack: QueueEntry.LocalTrack): MediaItem {
        return MediaItem.Builder()
            .setMediaId(localTrack.url)
            .setUri(localTrack.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(localTrack.name)
                    .setArtist(localTrack.artists.joinToString(", "))
                    .setAlbumTitle(localTrack.album)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    private suspend fun browseItemToMediaItem(item: MetadataBrowseItem): MediaItem? {
        return when (item) {
            is MetadataBrowseItem.Track -> buildTrackMediaItem(item.data)
            is MetadataBrowseItem.Album -> buildAlbumMediaItem(item.data)
            is MetadataBrowseItem.Artist -> buildArtistMediaItem(item.data)
            is MetadataBrowseItem.Playlist -> buildPlaylistMediaItem(item.data)
            is MetadataBrowseItem.User -> null
        }
    }

    private suspend fun buildStreamingUrl(trackId: String): String? {
        val baseUrl = localServer?.awaitBaseUrl() ?: run {
            val settings = settingsRepository.userSettings.first()
            val port = settings.playbackProxyServerPort
            if (port == 0) return null
            "http://127.0.0.1:$port"
        }
        return "${baseUrl.trimEnd('/')}/stream/$trackId"
    }
}
