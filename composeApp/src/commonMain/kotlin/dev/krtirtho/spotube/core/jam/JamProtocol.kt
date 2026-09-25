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

package dev.krtirtho.spotube.core.jam

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.MediaItem
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Jam messages exchanged over MQTT.
 *
 * - `state` topic: [QueueState] (retained, host -> everyone)
 * - `cmd` topic: [PlaybackCommand], [Kick], [SuggestTrack], [SuggestPlaylist]
 *   (anyone -> host, except Kick which is host -> guest)
 * - `presence/{clientId}` topic: [JamPresence] (retained, one per participant)
 */
@Serializable
sealed class JamMessage {
    @Serializable
    @SerialName("queueState")
    data class QueueState(
        val items: List<JamMediaItem>,
        val currentIndex: Int,
        val shuffleEnabled: Boolean = false,
        /**
         * Whether guests should follow the host's current index. True when the
         * host manually skipped/jumped or loaded a queue; false when the host
         * merely auto-advanced because its song ended (guests stay put).
         */
        val follow: Boolean = false,
        /** Host's live play state — late-joining guests start with it. */
        val isPlaying: Boolean = false,
    ) : JamMessage()

    @Serializable
    @SerialName("playbackCommand")
    data class PlaybackCommand(val command: PlaybackCmd) : JamMessage()

    @Serializable
    @SerialName("suggestTrack")
    data class SuggestTrack(
        val mediaItem: JamMediaItem,
        val addedBy: String = "",
    ) : JamMessage()

    @Serializable
    @SerialName("suggestPlaylist")
    data class SuggestPlaylist(
        val tracks: List<JamMediaItem>,
        val addedBy: String = "",
    ) : JamMessage()

    @Serializable
    @SerialName("kick")
    data class Kick(
        val participantId: String,
        val reason: String = "kicked",
    ) : JamMessage()

    @Serializable
    @SerialName("leave")
    data class Leave(val reason: String = "user_left") : JamMessage()
}

/**
 * Playback commands. Only queue navigation is global — play/pause, seek,
 * volume, shuffle and loop are local to each participant.
 */
@Serializable
sealed class PlaybackCmd {
    @Serializable
    @SerialName("skipNext")
    data object SkipNext : PlaybackCmd()

    @Serializable
    @SerialName("skipPrevious")
    data object SkipPrevious : PlaybackCmd()

    @Serializable
    @SerialName("jumpTo")
    data class JumpTo(val index: Int) : PlaybackCmd()
}

/** Retained per-participant presence entry (with an MQTT Last Will for leave). */
@Serializable
data class JamPresence(
    val clientId: String,
    val displayName: String,
    val isHost: Boolean,
    val left: Boolean = false,
)

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
    val addedBy: String = "",
) {
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
                addedBy = entry.addedBy,
            )

            is QueueEntry.LocalTrack -> JamMediaItem(
                url = entry.url,
                trackId = "",
                title = entry.name,
                artist = entry.artists.joinToString(", "),
                album = entry.album.orEmpty(),
                durationMs = entry.duration,
                coverUrl = "",
                protocol = "PROGRESSIVE",
                addedBy = entry.addedBy,
            )
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
            protocol = "PROGRESSIVE",
        )

        fun fromMediaItem(item: MediaItem): JamMediaItem = JamMediaItem(
            url = item.url,
            title = item.title,
            artist = item.artist,
            album = item.album,
            durationMs = item.duration.inWholeMilliseconds,
            coverUrl = item.coverURL,
            protocol = item.protocol.name,
        )

        fun toMediaItem(item: JamMediaItem): MediaItem = MediaItem(
            title = item.title,
            artist = item.artist,
            album = item.album,
            duration = kotlin.time.Duration.parse("${item.durationMs}ms"),
            coverURL = item.coverUrl,
            url = item.url,
            protocol = dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
                .valueOf(item.protocol.ifBlank { "PROGRESSIVE" }),
        )
    }
}

@Serializable
data class JamParticipant(
    val id: String,
    val displayName: String,
    val isHost: Boolean,
)

@Serializable
enum class JamRole {
    Host,
    Guest,
}