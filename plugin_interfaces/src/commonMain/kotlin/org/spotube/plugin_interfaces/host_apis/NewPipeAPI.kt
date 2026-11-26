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

package org.spotube.plugin_interfaces.host_apis

import app.cash.zipline.ZiplineService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioStream

const val NewPipeAPI_SERVICE_NAME = "NewPipeAPI"

@Serializable
enum class NewPipeSearchKind {
    /** Search YouTube Music's songs catalogue. */
    @SerialName("songs")
    SONGS,

    /** Search regular YouTube videos. */
    @SerialName("videos")
    VIDEOS
}

@Serializable
data class NewPipeVideoSearchResult(
    val id: String,
    val title: String,
    val url: String,
    val uploader: String,
    val durationMs: Long,
    val thumbnailUrl: String
)

@Serializable
data class NewPipeVideoInfo(
    val id: String,
    val title: String,
    val url: String,
    val uploader: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val audioStreams: List<AudioStream>,
    val videoStreams: List<AudioStream>
)

/** Host-provided NewPipe extraction for plugins that need a fallback source. */
interface NewPipeAPI : ZiplineService {
    /** Returns NewPipe's first page of video search results for [query]. */
    suspend fun searchVideos(
        query: String,
        kind: NewPipeSearchKind = NewPipeSearchKind.SONGS
    ): List<NewPipeVideoSearchResult>

    /** Extracts metadata and available media streams for a YouTube video id. */
    suspend fun getVideoInfo(id: String): NewPipeVideoInfo
}
