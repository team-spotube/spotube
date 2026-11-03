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

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult

const val MetadataBrowseAPI_SERVICE_NAME = "MetadataBrowseAPI"

interface MetadataBrowseAPI: ZiplineService {
    suspend fun featured(): List<MetadataBrowseItem>
    suspend fun genres(): List<MetadataBrowseGenre>
    suspend fun list(genreId: String, pagination: PaginationStrategy? = null): PaginationResult<MetadataBrowseSection>
    suspend fun sublist(genreId: String, sectionId: String, pagination: PaginationStrategy? = null): PaginationResult<MetadataBrowseItem>
}