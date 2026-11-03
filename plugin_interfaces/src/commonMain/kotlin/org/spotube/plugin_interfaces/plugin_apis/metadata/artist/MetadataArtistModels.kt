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

package org.spotube.plugin_interfaces.plugin_apis.metadata.artist

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MetadataArtist {
    abstract val id: String
    abstract val name: String
    abstract val thumbnails: List<Thumbnail>
    abstract val externalUri: String?

    @Serializable
    @SerialName("base")
    data class Basic(
        override val id: String,
        override val name: String,
        override val thumbnails: List<Thumbnail>,
        override val externalUri: String?
    ) : MetadataArtist()

    @Serializable
    @SerialName("detailed")
    data class Detailed(
        val genres: List<String>,
        val biography: String?,
        val followersCount: Int?,
        override val id: String,
        override val name: String,
        override val thumbnails: List<Thumbnail>,
        override val externalUri: String?
    ) : MetadataArtist()
}

@Serializable
data class MetadataArtistOverview(
    val artist: MetadataArtist.Detailed,
    val top10Tracks: List<MetadataTrack>,
    val albums: PaginationResult<MetadataAlbum.Detailed>,
    val relatedArtists: PaginationResult<MetadataArtist.Basic>,
    val featuredPlaylists: PaginationResult<MetadataPlaylist>
)