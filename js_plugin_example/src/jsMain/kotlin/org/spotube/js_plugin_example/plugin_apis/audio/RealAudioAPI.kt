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

package org.spotube.js_plugin_example.plugin_apis.audio

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioQuality
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioStream
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

val urls = listOf(
    "https://cdn.pixabay.com/audio/2024/02/28/audio_d1a9995fc3.mp3",
    "https://cdn.pixabay.com/audio/2024/05/29/audio_f3a1d24f19.mp3",
    "https://cdn.pixabay.com/audio/2025/03/18/audio_9c95eb2557.mp3",
    "https://cdn.pixabay.com/audio/2023/06/02/audio_320a2e0f57.mp3",
    "https://cdn.pixabay.com/audio/2024/06/30/audio_0eb1f1f4ec.mp3",
    "https://cdn.pixabay.com/audio/2023/08/12/audio_5cdd274d4b.mp3",
    "https://cdn.pixabay.com/audio/2025/03/17/audio_a71887c1b8.mp3",
    "https://cdn.pixabay.com/audio/2026/03/25/audio_19c3c36ce2.mp3",
    "https://cdn.pixabay.com/audio/2023/06/25/audio_ded864e440.mp3"
)

class RealAudioAPI : AudioAPI {
    override val supportedQualities: List<AudioFormat> = listOf(
        AudioFormat(
            codec = "mp3", container = "mp3", qualities = listOf(
                AudioQuality.Lossy(bitrate = 128),
                AudioQuality.Lossy(bitrate = 320),
            )
        ),
        AudioFormat(
            codec = "mp3", container = "mp3", qualities = listOf(
                AudioQuality.Lossy(bitrate = 128),
                AudioQuality.Lossy(bitrate = 320),
            )
        ),
        AudioFormat(
            codec = "aac", container = "aac", qualities = listOf(
                AudioQuality.Lossy(bitrate = 160),
            )
        ),
    )

    override suspend fun getStreamsByTrack(track: MetadataTrack): List<AudioSource> {
        val title = track.title.ifBlank { "Unknown Track" }
        val artist = track.artists.firstOrNull()?.name ?: "Unknown Artist"
        val album = track.album?.title?.ifBlank { null }

        return listOf(
            AudioSource.Basic(
                id = "${track.id}-source-1",
                title = title,
                artist = artist,
                album = album,
                thumbnails = track.album?.thumbnails.orEmpty(),
                externalUri = track.externalUri,
                confidence = 0.98f,
            ),
            AudioSource.Basic(
                id = "${track.id}-source-2",
                title = "$title (Alternative)",
                artist = artist,
                album = album,
                thumbnails = track.album?.thumbnails.orEmpty(),
                externalUri = track.externalUri,
                confidence = 0.92f,
            )
        )
    }

    override suspend fun getStreamsOfAudioSource(source: AudioSource.Basic): List<AudioSource.Streamed> {
        val defaultDuration = 180_000L
        val primary = AudioSource.Streamed(
            id = source.id,
            title = source.title,
            artist = source.artist,
            album = source.album,
            thumbnails = source.thumbnails,
            externalUri = source.externalUri,
            confidence = source.confidence,
            streams = listOf(
                AudioStream.Lossy(
                    url = urls.random(),
                    codec = "mp3",
                    bitrate = 256000,
                    container = "mp3",
                ),
            ).shuffled()
        )

        return listOf(primary)
    }
}