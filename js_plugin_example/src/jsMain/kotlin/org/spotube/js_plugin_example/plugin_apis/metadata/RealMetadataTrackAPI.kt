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
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI

class RealMetadataTrackAPI : MetadataTrackAPI {

    override suspend fun getTrack(id: String): MetadataTrack {
        return FakeMetadataStore.getTrack(id)
    }

    override suspend fun savedTracks(
        pagination: PaginationStrategy?
    ): PaginationResult<MetadataTrack> {
        return FakeMetadataStore.paginate(FakeMetadataStore.savedTracks(), pagination)
    }

    override suspend fun isSavedTracks(ids: List<String>): List<Boolean> {
        return FakeMetadataStore.isSavedTracks(ids)
    }

    override suspend fun saveTracks(ids: List<String>) {
        FakeMetadataStore.saveTracks(ids)
    }

    override suspend fun removeSavedTracks(ids: List<String>) {
        FakeMetadataStore.removeSavedTracks(ids)
    }

    override suspend fun recommendationsBasedOnTracks(
        seedTrackIds: List<String>,
        limit: Int
    ): List<MetadataTrack> {
        return FakeMetadataStore.recommendationsBasedOnTracks(seedTrackIds, limit)
    }
}
