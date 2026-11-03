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

package org.spotube.plugin_interfaces.plugin_apis.metadata.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PaginationStrategy {
    @Serializable
    @SerialName("Offset")
    data class Offset(val offset: Int, val limit: Int) : PaginationStrategy

    @Serializable
    @SerialName("Cursor")
    data class Cursor(val cursor: String?, val limit: Int) : PaginationStrategy

    @Serializable
    @SerialName("Page")
    data class Page(val page: Int, val pageSize: Int) : PaginationStrategy

    @Serializable
    @SerialName("Continuation")
    data class Continuation(val continuationToken: String?) : PaginationStrategy
}

@Serializable
data class PaginationResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val nextPagination: PaginationStrategy?,
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int
)