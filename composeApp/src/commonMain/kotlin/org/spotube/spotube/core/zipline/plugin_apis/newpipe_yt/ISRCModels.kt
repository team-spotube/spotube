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

package org.spotube.spotube.core.zipline.plugin_apis.newpipe_yt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IFPISessionResponse(
    val token: String,
    val response: String
)

/**
 * {
 *   "searchFields": {
 *     "recordingArtistName": {
 *       "value": "Justin Bieber"
 *     },
 *     "recordingTitle": {
 *       "value": "Peaches"
 *     },
 *     "releaseName": {
 *       "value": "Justice"
 *     }
 *   },
 *   "start": 0,
 *   "number": 10,
 *   "showReleases": false
 * }
 */
@Serializable
data class IFPIRecordingRequestInput(
    val searchFields: SearchFields,
    val start: Int = 0,
    val number: Int = 10,
    val showReleases: Boolean = false
) {
    @Serializable
    data class SearchFields(
        val recordingArtistName: FieldValue? = null,
        val recordingTitle: FieldValue? = null,
        val releaseName: FieldValue? = null
    ) {
        @Serializable
        data class FieldValue(val value: String)
    }
}

/**
 * {
 *   "numberOfRecordings": 1,
 *   "show_releases": false,
 *   "recordings": [
 *     {
 *       "duration": "3:18",
 *       "recordingVersion": null,
 *       "isValidIsrc": "True",
 *       "recordingYear": "2021",
 *       "recordingArtistName": "Daniel Caesar ♦ Giveon ♦ Justin Bieber",
 *       "isrcFailureCode": null,
 *       "isExplicit": "False",
 *       "isrc": "USUM72102647",
 *       "recordingTitle": "Peaches",
 *       "id": "USUM72102647"
 *     }
 *   ]
 * }
 */
@Serializable
data class IFPIRecordingResponse(
    val numberOfRecordings: Int,
    @SerialName("show_releases")
    val showReleases: Boolean,
    val recordings: List<Recording>
) {
    @Serializable
    data class Recording(
        val duration: String?,
        val recordingVersion: String?,
        val isValidIsrc: String?,
        val recordingYear: String?,
        val recordingArtistName: String?,
        val isrcFailureCode: String?,
        val isExplicit: String?,
        val isrc: String,
        val recordingTitle: String?,
        val id: String
    )
}

@Serializable
data class MusicGatewayResponse(
    val result: Result
) {
    @Serializable
    data class Result(
        val tracks: Tracks
    ) {
        @Serializable
        data class Tracks(
            val items: List<Item>
        ) {
            @Serializable
            data class Item(
                val album: Album,
                val artists: List<Artist>,
                @SerialName("external_ids")
                val externalIds: ExternalIds,
                val id: String,
                val name: String,
                val popularity: Int) {
                @Serializable
                data class Album(
                    @SerialName("album_type")
                    val albumType: String,
                    val artists: List<Artist>,
                    val id: String,
                    val name: String)

                @Serializable
                data class Artist(
                    val id: String,
                    val name: String,
                    val type: String,
                    val uri: String
                )

                @Serializable
                data class ExternalIds(
                    val isrc: String
                )
            }
        }
    }
}

@Serializable
data class SoundplateResponse(
    val name: String,
    val artist: String,
    val album: String,
    @SerialName("album_type")
    val albumType: String,
    @SerialName("artwork_url")
    val artworkUrl: String,
    val isrc: String,
    val year: String,
    @SerialName("spotify_url")
    val spotifyUrl: String
)