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

package org.spotube.js_plugin_example

import app.cash.zipline.Zipline
import org.spotube.js_plugin_example.plugin_apis.audio.RealAudioAPI
import org.spotube.js_plugin_example.plugin_apis.core.RealCoreAPI
import org.spotube.js_plugin_example.plugin_apis.lyrics.RealLyricsAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataAlbumAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataArtistAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataBrowseAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataPlaylistAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataSearchAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataTrackAPI
import org.spotube.js_plugin_example.plugin_apis.metadata.RealMetadataUserAPI
import org.spotube.js_plugin_example.plugin_apis.scrobble.RealScrobbleAPI
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.PersistedStorageAPI
import org.spotube.plugin_interfaces.host_apis.PersistedStorageAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.host_apis.WebViewAPI
import org.spotube.plugin_interfaces.host_apis.WebViewAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleAPI_SERVICE_NAME
import kotlin.js.Date

private val zipline by lazy { Zipline.get() }

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    zipline.take<HttpClientAPI>(HttpClientAPI_SERVICE_NAME)
    zipline.take<WebViewAPI>(WebViewAPI_SERVICE_NAME)
    zipline.take<PersistedStorageAPI>(PersistedStorageAPI_SERVICE_NAME)
    console.log("Epoch Time: ${Date.now()}")

    zipline.bind<CoreAPI>(CoreAPI_SERVICE_NAME, RealCoreAPI())
    zipline.bind<AudioAPI>(AudioAPI_SERVICE_NAME, RealAudioAPI())
    zipline.bind<LyricsAPI>(LyricsAPI_SERVICE_NAME, RealLyricsAPI())
    zipline.bind<ScrobbleAPI>(ScrobbleAPI_SERVICE_NAME, RealScrobbleAPI())

    zipline.bind<MetadataAlbumAPI>(MetadataAlbumAPI_SERVICE_NAME, RealMetadataAlbumAPI())
    zipline.bind<MetadataArtistAPI>(MetadataArtistAPI_SERVICE_NAME, RealMetadataArtistAPI())
    zipline.bind<MetadataBrowseAPI>(MetadataBrowseAPI_SERVICE_NAME, RealMetadataBrowseAPI())
    zipline.bind<MetadataPlaylistAPI>(MetadataPlaylistAPI_SERVICE_NAME, RealMetadataPlaylistAPI())
    zipline.bind<MetadataSearchAPI>(MetadataSearchAPI_SERVICE_NAME, RealMetadataSearchAPI())
    zipline.bind<MetadataTrackAPI>(MetadataTrackAPI_SERVICE_NAME, RealMetadataTrackAPI())
    zipline.bind<MetadataUserAPI>(MetadataUserAPI_SERVICE_NAME, RealMetadataUserAPI())
}