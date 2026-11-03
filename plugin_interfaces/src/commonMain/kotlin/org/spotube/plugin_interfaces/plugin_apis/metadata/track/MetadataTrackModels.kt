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

package org.spotube.plugin_interfaces.plugin_apis.metadata.track

import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class MetadataTrack(
    val id: String,
    val title: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val artists: List<MetadataArtist.Basic>,
    val album: MetadataAlbum.Detailed?,
    val thumbnails: List<Thumbnail>?,
    val explicit: Boolean?,
    val popularity: Int?,
    val isrcCode: String?,
    val externalUri: String?
)