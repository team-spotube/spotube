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

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioQuality
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.newpipe.NewPipeService
import org.spotube.spotube.core.newpipe.SearchKind
import org.spotube.spotube.core.newpipe.VideoSearchResult
import org.koin.core.component.KoinComponent

class RealNewPipeAudioAPI : AudioAPI, KoinComponent {
    private val logger by injectLogger<RealNewPipeAudioAPI>()
    private val newPipeService = NewPipeService()

    companion object {
        private val youtubeIDRegex = Regex("^[a-zA-Z0-9_-]{11}$")
        private val isrcRegex = Regex("^[A-Z]{2}[A-Z0-9]{3}\\d{7}$")
        private const val minConfidence = 0.3f
        private const val confidentMatch = 0.6f
    }

    private val isrcProvider = ISRCProviders()

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    /** Drops "(feat. X)", "[Remastered]" style suffixes so the core title can be matched. */
    private fun coreTitle(title: String): String =
        normalize(title.replace(Regex("[(\\[].*?[)\\]]"), " "))

    private fun transformVideo(track: MetadataTrack, video: VideoSearchResult): AudioSource.Basic {
        var confidence = 0f

        val videoTitle = normalize(video.title)
        val isTopicChannel = video.uploader.trim().endsWith("- Topic", ignoreCase = true)
        val uploader = normalize(video.uploader.replace(Regex("(?i)\\s*-\\s*topic$"), ""))
        val artistNames = track.artists.map { normalize(it.name) }.filter { it.isNotEmpty() }
        val title = coreTitle(track.title)

        val videoTokens = videoTitle.split(' ').toSet()
        val isTitleMatch = title.isNotEmpty() &&
            (title in videoTitle || title.split(' ').all { it in videoTokens })
        val isArtistInTitle = artistNames.any { it in videoTitle }
        val isAlbumInTitle = track.album?.title?.let { normalize(it) }
            ?.takeIf { it.isNotEmpty() }?.let { it in videoTitle } ?: false
        val isUploaderMatch = uploader.isNotEmpty() && artistNames.any { it in uploader || uploader in it }
        // Unknown duration (0) must neither reward nor punish
        val durationDiff = if (video.durationMs > 0) kotlin.math.abs(video.durationMs - track.durationMs) else Long.MAX_VALUE

        if (isTitleMatch) confidence += 0.3f
        if (isUploaderMatch) confidence += 0.3f
        if (durationDiff <= 10_000) confidence += 0.2f
        else if (durationDiff <= 30_000) confidence += 0.1f
        if (isArtistInTitle) confidence += 0.1f
        if (isAlbumInTitle) confidence += 0.1f
        // Auto-generated "Artist - Topic" uploads are the studio audio, preferred over videos
        if (isTopicChannel && isUploaderMatch) confidence += 0.1f
        confidence = confidence.coerceAtMost(1f)

        logger.i {
            "[Match] video id=${video.id} title='${video.title}' uploader='${video.uploader}' " +
                "durationMs=${video.durationMs} url=${video.url}\n" +
                "        vs track title='${track.title}' artists=${track.artists.map { it.name }} " +
                "album='${track.album?.title}' durationMs=${track.durationMs}\n" +
                "        normalized: videoTitle='$videoTitle' uploader='$uploader' coreTitle='$title' artists=$artistNames\n" +
                "        titleMatch=$isTitleMatch uploaderMatch=$isUploaderMatch topic=$isTopicChannel " +
                "artistInTitle=$isArtistInTitle albumInTitle=$isAlbumInTitle durationDiffMs=$durationDiff " +
                "=> confidence=$confidence"
        }

        return AudioSource.Basic(
            id = video.id,
            title = video.title,
            artist = video.uploader,
            album = null,
            thumbnails = listOf(
                Thumbnail(
                    url = video.thumbnailUrl,
                    width = 300,
                    height = 300)
            ),
            externalUri = "https://www.youtube.com/watch?v=${video.id}",
            confidence = confidence)
    }

    override val supportedQualities: List<AudioFormat> = listOf(
        AudioFormat(
            codec = "opus", container = "webm", qualities = listOf(
                AudioQuality.Lossy(bitrate = 44_000),
                AudioQuality.Lossy(bitrate = 96_000),
                AudioQuality.Lossy(bitrate = 128_000),
                AudioQuality.Lossy(bitrate = 256_000))
        ), AudioFormat(
            codec = "aac", container = "mp4", qualities = listOf(
                AudioQuality.Lossy(bitrate = 44_000),
                AudioQuality.Lossy(bitrate = 96_000),
                AudioQuality.Lossy(bitrate = 128_000),
                AudioQuality.Lossy(bitrate = 256_000))
        )
    )

    override suspend fun getStreamsByTrack(track: MetadataTrack): List<AudioSource> {
        val isYouTubeUrl =
            track.externalUri != null && (track.externalUri!!.contains("youtube.com") || track.externalUri!!.contains(
                "youtu.be"
            ))
        val isYouTubeID = track.id.matches(youtubeIDRegex)

        logger.i {
            "Searching streams for track(${track.id}): ${track.title} by ${
                track.artists.joinToString(
                    ", "
                ) { it.name }
            }"
        }
        if (isYouTubeID || isYouTubeUrl) {
            logger.i { "Track has YouTube ID or URL, fetching video info directly" }
            return newPipeService.getVideoInfo(track.id).let { videoInfo ->
                listOf(
                    AudioSource.Streamed(
                        id = videoInfo.id,
                        title = videoInfo.title,
                        artist = videoInfo.uploader,
                        album = null,
                        thumbnails = listOf(
                            Thumbnail(
                                url = videoInfo.thumbnailUrl,
                                width = 300,
                                height = 300)
                        ),
                        externalUri = "https://www.youtube.com/watch?v=${videoInfo.id}",
                        confidence = 1.0f,
                        streams = videoInfo.audioStreams)
                )
            }
        }

        logger.i { "No YouTube ID or URL found, performing search with track metadata" }

        logger.i {
            "[Search] input track id=${track.id} title='${track.title}' artists=${track.artists.map { it.name }} " +
                "album='${track.album?.title}' durationMs=${track.durationMs} trackIsrc=${track.isrcCode} " +
                "externalUri=${track.externalUri}"
        }
        val rawIsrc = track.isrcCode ?: isrcProvider.auto(track)
        val isrcCode = rawIsrc
            ?.trim()?.uppercase()?.replace("-", "")
            ?.takeIf { isrcRegex.matches(it) }
        logger.i {
            "[Search] isrc raw=$rawIsrc (from ${if (track.isrcCode != null) "track" else "providers"}) " +
                "validated=$isrcCode"
        }

        val artistText = track.artists.joinToString(" ") { it.name }.trim()
        val firstArtist = track.artists.firstOrNull()?.name?.trim().orEmpty()
        val plainTitle = track.title.replace(Regex("[(\\[].*?[)\\]]"), " ").replace(Regex("\\s+"), " ").trim()

        // Ordered from most to least trusted. The ISRC is only a hint: a text search for it on
        // YouTube Music often returns unrelated songs, so every step is scored against the track
        // and the next step runs until one result clears [confidentMatch].
        val plan = buildList {
            if (isrcCode != null) add("isrc" to (isrcCode to SearchKind.Songs))
            add("songs: title+artist" to ("${track.title} $artistText".trim() to SearchKind.Songs))
            if (plainTitle.isNotEmpty() && firstArtist.isNotEmpty() && plainTitle != track.title) {
                add("songs: plain title+first artist" to ("$plainTitle $firstArtist" to SearchKind.Songs))
            }
            add("videos: title+artist" to ("${track.title} $artistText".trim() to SearchKind.Videos))
            add("songs: title only" to (plainTitle.ifEmpty { track.title } to SearchKind.Songs))
        }

        val sources = linkedMapOf<String, AudioSource.Basic>()
        val searched = mutableSetOf<Pair<String, SearchKind>>()
        // Results of the text searches in YouTube's relevance order, used if nothing scores well
        val textSearchResults = linkedMapOf<String, AudioSource.Basic>()
        for ((stage, request) in plan) {
            if (sources.values.any { it.confidence >= confidentMatch }) break
            if (!searched.add(request)) continue
            val (query, kind) = request
            val raw = runCatching { newPipeService.searchVideos(query, kind) }
                .onFailure {
                    if (it is kotlinx.coroutines.CancellationException) throw it
                    logger.w(it) { "[Search] stage '$stage' failed for query '$query' ($kind)" }
                }.getOrDefault(emptyList())
            logger.i { "[Search] stage '$stage' query='$query' kind=$kind -> ${raw.size} results: $raw" }
            val scored = raw.map { transformVideo(track, it) }
            logger.i {
                "[Search] stage '$stage' best confidence=${scored.maxOfOrNull { it.confidence }} " +
                    "(need >= $confidentMatch to stop)"
            }
            if (stage != "isrc") scored.forEach { if (it.id !in textSearchResults) textSearchResults[it.id] = it }
            scored.forEach { s ->
                val existing = sources[s.id]
                if (existing == null || existing.confidence < s.confidence) sources[s.id] = s
            }
        }

        val ranked = sources.values.sortedByDescending { it.confidence }
        val matched = ranked.filter { it.confidence >= minConfidence }
        logger.i { "[Search] ${matched.size} of ${ranked.size} candidates have confidence >= $minConfidence" }
        logger.i {
            "[Search] final ranking:\n" + matched.take(5).mapIndexed { i, s ->
                "        #$i ${s.id} conf=${s.confidence} '${s.title}' by '${s.artist}'"
            }.joinToString("\n")
        }

        if (matched.isNotEmpty()) return matched.take(5)

        // Nothing scored well. Playing YouTube's best text-search guess beats failing playback,
        // and ISRC results are skipped because an ISRC text search tends to return unrelated songs.
        val fallback = textSearchResults.values.toList()
            .sortedByDescending { it.confidence } // stable: ties keep YouTube's relevance order
            .take(5)
            .ifEmpty { ranked.take(5) }
        logger.w {
            "[Search] no confident match for '${track.title}'; falling back to best guesses: " +
                fallback.joinToString { "${it.id} conf=${it.confidence} '${it.title}'" }
        }
        return fallback
    }

    override suspend fun getStreamsOfAudioSource(source: AudioSource.Basic): List<AudioSource.Streamed> {
        return newPipeService.getVideoInfo(source.id).let { videoInfo ->
            listOf(
                AudioSource.Streamed(
                    id = videoInfo.id,
                    title = videoInfo.title,
                    artist = videoInfo.uploader,
                    album = null,
                    thumbnails = listOf(
                        Thumbnail(
                            url = videoInfo.thumbnailUrl,
                            width = 300,
                            height = 300)
                    ),
                    externalUri = "https://www.youtube.com/watch?v=${videoInfo.id}",
                    confidence = source.confidence,
                    streams = videoInfo.audioStreams
                )
            )
        }
    }
}