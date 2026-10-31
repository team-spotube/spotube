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

package org.spotube.spotube.core.jam

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.MediaItem
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.server.PluginTaggedAudioSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Room payloads. The server only routes and retains them; their shape is owned here.
 *
 * - [JamQueue] (retained, host -> everyone): the shared queue.
 * - [JamAnchor] (retained, host -> everyone): where playback is, in server time.
 * - [JamCommand] (guest -> host): playback control and suggestions.
 */
@Serializable
data class JamQueue(
    val items: List<JamMediaItem>,
    val shuffleEnabled: Boolean = false)

/**
 * A playback anchor: track [trackKey] (at queue [index]) was at [positionMs] at server
 * time [serverTimeMs]. While [advancing], everyone extrapolates the position from it at
 * [rate]; a buffering host sends a non-advancing anchor so guests don't run ahead.
 */
@Serializable
data class JamAnchor(
    val trackKey: String,
    val index: Int,
    val positionMs: Long,
    val serverTimeMs: Long,
    /** Room-wide play/pause intent; guests follow every change of it. */
    val playing: Boolean,
    val advancing: Boolean = playing,
    val rate: Float = 1f,
    /** The audio source the host plays [trackKey] from, so guests stream the same file. */
    val source: PluginTaggedAudioSource? = null) {
    /** The room's position at server time [serverNowMs]. */
    fun positionAt(serverNowMs: Long): Long =
        if (advancing) positionMs + ((serverNowMs - serverTimeMs) * rate).toLong() else positionMs
}

/** Playback control and suggestions. Volume, loop and the local queue view stay local. */
@Serializable
sealed class JamCommand {
    @Serializable
    @SerialName("setPlaying")
    data class SetPlaying(val playing: Boolean) : JamCommand()

    @Serializable
    @SerialName("skipNext")
    data object SkipNext : JamCommand()

    @Serializable
    @SerialName("skipPrevious")
    data object SkipPrevious : JamCommand()

    @Serializable
    @SerialName("jumpTo")
    data class JumpTo(val index: Int) : JamCommand()

    @Serializable
    @SerialName("seekTo")
    data class SeekTo(val positionMs: Long) : JamCommand()

    @Serializable
    @SerialName("suggest")
    data class Suggest(
        val items: List<JamMediaItem>,
        val addedBy: String = "") : JamCommand()
}

@Serializable
data class JamMediaItem(
    val url: String,
    val trackId: String = "",
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String,
    val protocol: String,
    val addedBy: String = "") {
    /** Identifies the track across devices: the metadata id, or the file for local tracks. */
    val key: String get() = trackId.ifBlank { "local:$url" }

    companion object {
        fun fromQueueEntry(entry: QueueEntry): JamMediaItem = when (entry) {
            is QueueEntry.StreamingTrack -> JamMediaItem(
                url = "",
                trackId = entry.track.id,
                title = entry.track.title,
                artist = entry.track.artists.joinToString(", ") { it.name },
                album = entry.track.album?.title.orEmpty(),
                durationMs = entry.track.durationMs,
                coverUrl = entry.track.thumbnails?.maxByOrNull { it.width * it.height }?.url
                    ?: entry.track.album?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                    .orEmpty(),
                protocol = entry.protocol.name,
                addedBy = entry.addedBy)

            is QueueEntry.LocalTrack -> JamMediaItem(
                url = entry.url,
                trackId = "",
                title = entry.name,
                artist = entry.artists.joinToString(", "),
                album = entry.album.orEmpty(),
                durationMs = entry.duration,
                coverUrl = "",
                protocol = "PROGRESSIVE",
                addedBy = entry.addedBy)
        }

        fun fromTrack(track: MetadataTrack): JamMediaItem = JamMediaItem(
            url = "",
            trackId = track.id,
            title = track.title,
            artist = track.artists.joinToString(", ") { it.name },
            album = track.album?.title.orEmpty(),
            durationMs = track.durationMs,
            coverUrl = track.thumbnails?.maxByOrNull { it.width * it.height }?.url
                ?: track.album?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                .orEmpty(),
            protocol = "PROGRESSIVE")

        fun fromMediaItem(item: MediaItem): JamMediaItem = JamMediaItem(
            url = item.url,
            title = item.title,
            artist = item.artist,
            album = item.album,
            durationMs = item.duration.inWholeMilliseconds,
            coverUrl = item.coverURL,
            protocol = item.protocol.name)

        fun toMediaItem(item: JamMediaItem): MediaItem = MediaItem(
            title = item.title,
            artist = item.artist,
            album = item.album,
            duration = kotlin.time.Duration.parse("${item.durationMs}ms"),
            coverURL = item.coverUrl,
            url = item.url,
            protocol = org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
                .valueOf(item.protocol.ifBlank { "PROGRESSIVE" }))
    }
}

@Serializable
data class JamParticipant(
    val id: String,
    val displayName: String,
    val isHost: Boolean)

@Serializable
enum class JamRole {
    Host,
    Guest,
}