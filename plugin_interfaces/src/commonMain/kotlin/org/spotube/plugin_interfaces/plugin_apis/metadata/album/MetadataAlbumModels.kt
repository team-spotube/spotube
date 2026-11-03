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

package org.spotube.plugin_interfaces.plugin_apis.metadata.album

import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MetadataAlbumType {
    Single,
    Album,
    Collection
}

@Serializable
sealed class MetadataAlbum {
    abstract val id: String
    abstract val title: String
    abstract val description: String?
    abstract val thumbnails: List<Thumbnail>
    abstract val albumType: MetadataAlbumType
    abstract val artists: List<MetadataArtist.Basic>
    abstract val externalUri: String?

    @Serializable
    @SerialName("base")
    data class Basic(
        override val id: String,
        override val title: String,
        override val description: String?,
        override val thumbnails: List<Thumbnail>,
        override val albumType: MetadataAlbumType,
        override val artists: List<MetadataArtist.Basic>,
        override val externalUri: String?

    ) : MetadataAlbum()

    @Serializable
    @SerialName("detailed")
    data class Detailed(
        val releaseDate: String?,
        val genres: List<String>,
        val trackCount: Int,
        override val id: String,
        override val title: String,
        override val description: String?,
        override val thumbnails: List<Thumbnail>,
        override val albumType: MetadataAlbumType,
        override val artists: List<MetadataArtist.Basic>,
        override val externalUri: String?
    ) : MetadataAlbum()
}