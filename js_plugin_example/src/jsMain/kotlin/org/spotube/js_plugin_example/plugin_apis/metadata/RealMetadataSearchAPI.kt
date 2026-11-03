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

package org.spotube.js_plugin_example.plugin_apis.metadata

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType

class RealMetadataSearchAPI : MetadataSearchAPI {

    override val supportedSearchTypes: List<MetadataSupportedSearchType> = listOf(
        MetadataSupportedSearchType.ALL,
        MetadataSupportedSearchType.TRACK,
        MetadataSupportedSearchType.ARTIST,
        MetadataSupportedSearchType.ALBUM,
        MetadataSupportedSearchType.PLAYLIST,
        MetadataSupportedSearchType.USER,
    )

    override suspend fun search(query: String): List<MetadataSearchResult> {
        return FakeMetadataStore.search(query)
    }

    override suspend fun searchTracks(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Track> {
        return FakeMetadataStore.paginate(FakeMetadataStore.searchTracks(query), pagination)
    }

    override suspend fun searchArtists(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Artist> {
        return FakeMetadataStore.paginate(FakeMetadataStore.searchArtists(query), pagination)
    }

    override suspend fun searchAlbums(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Album> {
        return FakeMetadataStore.paginate(FakeMetadataStore.searchAlbums(query), pagination)
    }

    override suspend fun searchPlaylists(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.Playlist> {
        return FakeMetadataStore.paginate(FakeMetadataStore.searchPlaylists(query), pagination)
    }

    override suspend fun searchUsers(
        query: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataSearchResult.User> {
        return FakeMetadataStore.paginate(FakeMetadataStore.searchUsers(query), pagination)
    }
}
