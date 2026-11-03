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

package org.spotube.plugin_interfaces.plugin_apis.metadata.search

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult

const val MetadataSearchAPI_SERVICE_NAME = "MetadataSearchAPI"

interface MetadataSearchAPI : ZiplineService {
    val supportedSearchTypes: List<MetadataSupportedSearchType>

    suspend fun search(query: String): List<MetadataSearchResult>

    suspend fun searchTracks(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Track>

    suspend fun searchArtists(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Artist>

    suspend fun searchAlbums(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Album>

    suspend fun searchPlaylists(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.Playlist>

    suspend fun searchUsers(
        query: String,
        pagination: PaginationStrategy? = null
    ): PaginationResult<MetadataSearchResult.User>
}