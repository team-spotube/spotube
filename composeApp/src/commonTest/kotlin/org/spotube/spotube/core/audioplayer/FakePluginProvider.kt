package org.spotube.spotube.core.audioplayer

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistOverview
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.spotube.core.zipline.PluginService
import org.spotube.spotube.core.zipline.PluginServiceScope
import org.spotube.spotube.modules.plugin.PluginProvider
import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

class FakeMetadataTrackAPI(
    private val recommendations: List<MetadataTrack> = emptyList()
) : MetadataTrackAPI {
    override suspend fun getTrack(id: String): MetadataTrack =
        error("not mocked")

    override suspend fun savedTracks(pagination: PaginationStrategy?): PaginationResult<MetadataTrack> =
        error("not mocked")

    override suspend fun isSavedTracks(ids: List<String>): List<Boolean> =
        error("not mocked")

    override suspend fun saveTracks(ids: List<String>) = error("not mocked")

    override suspend fun removeSavedTracks(ids: List<String>) = error("not mocked")

    override suspend fun recommendationsBasedOnTracks(
        seedTrackIds: List<String>,
        limit: Int
    ): List<MetadataTrack> = recommendations
}

class FakeMetadataAlbumAPI(
    private val albumTracks: Map<String, List<MetadataTrack>> = emptyMap()
) : MetadataAlbumAPI {
    override suspend fun getAlbum(id: String): MetadataAlbum.Detailed = error("not mocked")
    override suspend fun getTrackAlbum(track: MetadataTrack): MetadataAlbum.Detailed = error("not mocked")
    override suspend fun getAlbumTracks(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataTrack> {
        val tracks = albumTracks[id] ?: emptyList()
        return PaginationResult(
            items = tracks,
            totalCount = tracks.size,
            nextPagination = null
        )
    }
    override suspend fun savedAlbums(pagination: PaginationStrategy?): PaginationResult<MetadataAlbum.Detailed> = error("not mocked")
    override suspend fun isSavedAlbums(ids: List<String>): List<Boolean> = error("not mocked")
    override suspend fun saveAlbums(ids: List<String>) = error("not mocked")
    override suspend fun removeSavedAlbums(ids: List<String>) = error("not mocked")
}

class FakeMetadataArtistAPI(
    private val topTracks: Map<String, List<MetadataTrack>> = emptyMap()
) : MetadataArtistAPI {
    override suspend fun getArtist(id: String): MetadataArtist.Detailed = error("not mocked")
    override suspend fun artistOverview(id: String): MetadataArtistOverview = error("not mocked")
    override suspend fun getArtistTop10Tracks(id: String): List<MetadataTrack> = topTracks[id] ?: emptyList()
    override suspend fun relatedArtists(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataArtist.Basic> = error("not mocked")
    override suspend fun featuredPlaylists(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataPlaylist> = error("not mocked")
    override suspend fun getArtistAlbums(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataAlbum.Detailed> = error("not mocked")
    override suspend fun savedArtists(pagination: PaginationStrategy?): PaginationResult<MetadataArtist.Detailed> = error("not mocked")
    override suspend fun isSavedArtists(ids: List<String>): List<Boolean> = error("not mocked")
    override suspend fun saveArtists(ids: List<String>) = error("not mocked")
    override suspend fun removeSavedArtists(ids: List<String>) = error("not mocked")
}

class FakePluginService(
    private val metadataTrackAPI: MetadataTrackAPI = FakeMetadataTrackAPI(),
    private val metadataAlbumAPI: MetadataAlbumAPI? = null,
    private val metadataArtistAPI: MetadataArtistAPI? = null,
    override val pluginId: String = "fake-plugin") : PluginService {
    override val loggedInFlow: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun start() = Unit
    override suspend fun stop() = Unit

    override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
        val registry = buildMap<KClass<*>, ZiplineService> {
            put(MetadataTrackAPI::class, metadataTrackAPI)
            metadataAlbumAPI?.let { put(MetadataAlbumAPI::class, it) }
            metadataArtistAPI?.let { put(MetadataArtistAPI::class, it) }
        }
        val scope = PluginServiceScope(registry)
        return scope.block()
    }
}

class FakePluginProvider(
    private val pluginService: PluginService? = null
) : PluginProvider {
    private val _selectedMetadataPlugin = MutableStateFlow(pluginService)
    override val selectedMetadataPlugin: StateFlow<PluginService?> = _selectedMetadataPlugin

    fun setPlugin(service: PluginService?) {
        _selectedMetadataPlugin.value = service
    }
}
