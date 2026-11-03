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

import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult

class RealMetadataBrowseAPI : MetadataBrowseAPI {

    override suspend fun featured(): List<MetadataBrowseItem> {
        return FakeMetadataStore.getFeaturedItems()
    }

    override suspend fun genres(): List<MetadataBrowseGenre> {
        return listOf(
            MetadataBrowseGenre(id = "1", name = "Pop"),
            MetadataBrowseGenre(id = "2", name = "Rock"),
            MetadataBrowseGenre(id = "3", name = "Hip-Hop"),
            MetadataBrowseGenre(id = "4", name = "Jazz"),
            MetadataBrowseGenre(id = "5", name = "Classical")
        )
    }

    override suspend fun list(genreId: String, pagination: PaginationStrategy?): PaginationResult<MetadataBrowseSection> {
        return FakeMetadataStore.paginate(FakeMetadataStore.getBrowseSections(), pagination)
    }

    override suspend fun sublist(
        genreId: String,
        sectionId: String,
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataBrowseItem> {
        return FakeMetadataStore.paginate(FakeMetadataStore.getBrowseSublist(sectionId), pagination)
    }
}
