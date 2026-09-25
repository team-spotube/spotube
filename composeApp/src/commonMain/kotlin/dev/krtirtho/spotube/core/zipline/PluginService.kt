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

package dev.krtirtho.spotube.core.zipline

import app.cash.zipline.ZiplineService
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.core.CoreAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import dev.krtirtho.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

class PluginServiceScope(private val registry: Map<KClass<*>, ZiplineService>) {
    val coreAPI: CoreAPI get() = getService()
    val metadataUserAPI: MetadataUserAPI get() = getService()
    val metadataTrackAPI: MetadataTrackAPI get() = getService()
    val metadataAlbumAPI: MetadataAlbumAPI get() = getService()
    val metadataArtistAPI: MetadataArtistAPI get() = getService()
    val metadataPlaylistAPI: MetadataPlaylistAPI get() = getService()
    val metadataBrowseAPI: MetadataBrowseAPI get() = getService()
    val metadataSearchAPI: MetadataSearchAPI get() = getService()
    val audioAPI: AudioAPI get() = getService()
    val lyricsAPI: LyricsAPI get() = getService()
    val scrobbleAPI: ScrobbleAPI get() = getService()

    private inline fun <reified T : ZiplineService> getService(): T {
        return registry[T::class] as? T
            ?: error("${T::class.simpleName} is not initialized or loaded.")
    }
}

interface PluginService {
    val pluginId: String
    val loggedInFlow: StateFlow<Boolean>

    suspend fun start()
    suspend fun stop()
    suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T
}