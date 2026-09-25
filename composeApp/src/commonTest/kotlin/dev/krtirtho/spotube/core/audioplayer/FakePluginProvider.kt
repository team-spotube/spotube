package dev.krtirtho.spotube.core.audioplayer

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import dev.krtirtho.spotube.core.zipline.PluginService
import dev.krtirtho.spotube.core.zipline.PluginServiceScope
import dev.krtirtho.spotube.modules.plugin.PluginProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

class FakePluginService(
    private val metadataTrackAPI: MetadataTrackAPI = FakeMetadataTrackAPI(),
    override val pluginId: String = "fake-plugin",
) : PluginService {
    override val loggedInFlow: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun start() = Unit
    override suspend fun stop() = Unit

    override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
        val scope = PluginServiceScope(
            mapOf(MetadataTrackAPI::class to metadataTrackAPI)
        )
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
