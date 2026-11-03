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

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

const val MetadataAlbumAPI_SERVICE_NAME = "MetadataAlbumAPI"

interface MetadataAlbumAPI : ZiplineService {
    suspend fun getAlbum(id: String): MetadataAlbum.Detailed
    suspend fun getTrackAlbum(track: MetadataTrack): MetadataAlbum.Detailed
    suspend fun getAlbumTracks(
        id: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataTrack>

    suspend fun savedAlbums(
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataAlbum.Detailed>

    suspend fun isSavedAlbums(ids: List<String>): List<Boolean>
    suspend fun saveAlbums(ids: List<String>)
    suspend fun removeSavedAlbums(ids: List<String>)
}