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

package org.spotube.spotube.core.newpipe

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioStream
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import co.touchlab.kermit.Logger
import org.spotube.spotube.core.server.describeForLog
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** NewPipe reports durations in seconds (-1 when unknown). */
private fun secondsToMs(seconds: Long): Long = if (seconds > 0) seconds * 1000 else 0

actual class NewPipeService {
    private val logger = Logger.withTag("NewPipeService")

    actual suspend fun searchVideos(query: String, kind: SearchKind): List<VideoSearchResult> {
        return withContext(Dispatchers.IO) {
            val extractor = YouTube.getSearchExtractor(
                query,
                mutableListOf(
                    when (kind) {
                        SearchKind.Songs -> YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
                        SearchKind.Videos -> YoutubeSearchQueryHandlerFactory.VIDEOS
                    }),
                ""
            )
            extractor.fetchPage()
            val results = extractor.initialPage

            results.items.mapNotNull { item ->
                if (item is StreamInfoItem) {
                    VideoSearchResult(
                        id = Url(item.url).parameters["v"]
                            ?: throw IllegalArgumentException("Invalid YouTube URL: ${item.url}"),
                        title = item.name,
                        url = item.url,
                        uploader = item.uploaderName,
                        durationMs = secondsToMs(item.duration),
                        thumbnailUrl = item.thumbnails.first().url)
                } else {
                    null
                }
            }
        }
    }

    actual suspend fun getVideoInfo(id: String): VideoInfo {
        return withContext(Dispatchers.IO) {
            val extractor = YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$id")
            extractor.fetchPage()

            // Raw extractor output, before any mapping/filtering, so stream-selection issues can be
            // traced back to what YouTube actually offered for this video.
            logger.i {
                "[Streams $id] '${extractor.name}' raw audioStreams=${extractor.audioStreams.size}:\n" +
                    extractor.audioStreams.joinToString("\n") { stream ->
                        "        itag=${stream.itag} format=${stream.format?.name} mime=${stream.format?.mimeType} " +
                            "suffix=${stream.format?.suffix} codec=${stream.codec} bitrate=${stream.bitrate} " +
                            "avgBitrate=${stream.averageBitrate} delivery=${stream.deliveryMethod} isUrl=${stream.isUrl} " +
                            "trackType=${stream.audioTrackType}"
                    }
            }
            logger.i {
                "[Streams $id] dashMpdUrl=${extractor.dashMpdUrl.isNotBlank()} hlsUrl=${extractor.hlsUrl.isNotBlank()} " +
                    "raw videoStreams=${extractor.videoStreams.size} videoOnlyStreams=${extractor.videoOnlyStreams.size}"
            }

            val audioStreams = extractor.audioStreams
                .mapNotNull { stream ->
                    val format = stream.format ?: return@mapNotNull null
                    // Non-URL content is an inline manifest, not something a player or downloader can fetch.
                    if (!stream.isUrl) return@mapNotNull null
                    AudioStream.Lossy(
                        url = stream.content,
                        codec = normalizeCodec(stream.codec),
                        container = normalizeContainer(format.suffix),
                        bitrate = stream.bitrate,
                        protocol = stream.deliveryMethod.toProtocol())
                } + buildList {
                if (extractor.dashMpdUrl.isNotBlank()) add(
                    AudioStream.Lossy(
                        url = extractor.dashMpdUrl,
                        codec = "aac",
                        container = "mp4",
                        bitrate = 128_000,
                        protocol = StreamProtocol.DASH
                    )
                )
                if (extractor.hlsUrl.isNotBlank()) add(
                    AudioStream.Lossy(
                        url = extractor.hlsUrl,
                        codec = "aac",
                        container = "mp4",
                        bitrate = 128_000,
                        protocol = StreamProtocol.HLS
                    )
                )
            }

            val videoStreams = extractor.videoStreams
                .filter { stream -> stream.isUrl && stream.format != null }
                .map { stream ->
                    AudioStream.Lossy(
                        url = stream.content,
                        codec = stream.codec,
                        bitrate = stream.bitrate,
                        container = stream.format!!.suffix)
                }
            logger.i {
                "[Streams $id] mapped audio candidates (${audioStreams.size}):\n" +
                    audioStreams.joinToString("\n") { "        ${it.describeForLog()}" }
            }
            VideoInfo(
                id = id,
                title = extractor.name,
                url = extractor.url,
                uploader = extractor.uploaderName,
                durationMs = secondsToMs(extractor.length),
                thumbnailUrl = extractor.thumbnails.first().url,
                // fallback to video streams if no audio streams are available
                audioStreams = audioStreams.ifEmpty {
                    logger.w {
                        "[Streams $id] no audio streams; FALLING BACK TO ${videoStreams.size} VIDEO streams " +
                            "(muxed video+audio, downloads will be video files):\n" +
                            extractor.videoStreams.joinToString("\n") {
                                "        itag=${it.itag} format=${it.format?.name} res=${it.resolution} " +
                                    "codec=${it.codec} videoOnly=${it.isVideoOnly} isUrl=${it.isUrl}"
                            }
                    }
                    videoStreams
                },
                videoStreams = videoStreams
            )
        }
    }
}

/**
 * NewPipe names AAC "mp4a.40.x" in an "m4a" container, but the plugin advertises (and users pick)
 * "aac"/"mp4". Without this a real AAC stream never matched the preference, and the hard-coded
 * aac/mp4 DASH/HLS manifests always won instead.
 */
private fun DeliveryMethod.toProtocol(): StreamProtocol = when (this) {
    DeliveryMethod.DASH -> StreamProtocol.DASH
    DeliveryMethod.HLS -> StreamProtocol.HLS
    else -> StreamProtocol.PROGRESSIVE
}
