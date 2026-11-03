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

package org.spotube.plugin_interfaces.plugin_apis.metadata.browse

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MetadataBrowseItem {
    @Serializable
    @SerialName("track")
    data class Track(val data: MetadataTrack) : MetadataBrowseItem

    @Serializable
    @SerialName("album")
    data class Album(val data: MetadataAlbum.Basic) : MetadataBrowseItem

    @Serializable
    @SerialName("artist")
    data class Artist(val data: MetadataArtist.Basic) : MetadataBrowseItem

    @Serializable
    @SerialName("playlist")
    data class Playlist(val data: MetadataPlaylist) : MetadataBrowseItem

    @Serializable
    @SerialName("user")
    data class User(val data: MetadataUser) : MetadataBrowseItem
}

@Serializable
data class MetadataBrowseSection(
    val title: String,
    val description: String? = null,
    val items: List<MetadataBrowseItem>,
    val moreLink: String? = null
)

@Serializable
data class MetadataBrowseGenre(
    val id: String,
    val name: String,
)