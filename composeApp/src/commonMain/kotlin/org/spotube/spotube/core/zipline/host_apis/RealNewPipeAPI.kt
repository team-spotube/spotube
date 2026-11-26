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

package org.spotube.spotube.core.zipline.host_apis

import org.spotube.plugin_interfaces.host_apis.NewPipeAPI
import org.spotube.plugin_interfaces.host_apis.NewPipeSearchKind
import org.spotube.plugin_interfaces.host_apis.NewPipeVideoInfo
import org.spotube.plugin_interfaces.host_apis.NewPipeVideoSearchResult
import org.spotube.spotube.core.newpipe.NewPipeService

class RealNewPipeAPI(
    private val newPipeService: NewPipeService = NewPipeService()
) : NewPipeAPI {
    override suspend fun searchVideos(
        query: String,
        kind: NewPipeSearchKind
    ): List<NewPipeVideoSearchResult> = newPipeService.searchVideos(query, kind)

    override suspend fun getVideoInfo(id: String): NewPipeVideoInfo =
        newPipeService.getVideoInfo(id)
}
