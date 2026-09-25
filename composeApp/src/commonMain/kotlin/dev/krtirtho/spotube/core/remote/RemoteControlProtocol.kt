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

package dev.krtirtho.spotube.core.remote

import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class RemoteControlCommand {
    @Serializable
    @SerialName("play")
    data class Play(val source: String) : RemoteControlCommand()

    @Serializable
    @SerialName("pause")
    data object Pause : RemoteControlCommand()

    @Serializable
    @SerialName("togglePlayPause")
    data object TogglePlayPause : RemoteControlCommand()

    @Serializable
    @SerialName("seek")
    data class Seek(val positionMs: Long) : RemoteControlCommand()

    @Serializable
    @SerialName("setVolume")
    data class SetVolume(val volume: Float) : RemoteControlCommand()

    @Serializable
    @SerialName("skipNext")
    data object SkipNext : RemoteControlCommand()

    @Serializable
    @SerialName("skipPrevious")
    data object SkipPrevious : RemoteControlCommand()

    @Serializable
    @SerialName("setShuffle")
    data class SetShuffle(val enabled: Boolean) : RemoteControlCommand()

    @Serializable
    @SerialName("setLoopMode")
    data class SetLoopMode(val mode: String) : RemoteControlCommand()

    @Serializable
    @SerialName("addToQueue")
    data class AddToQueue(val source: String) : RemoteControlCommand()

    @Serializable
    @SerialName("playNext")
    data class PlayNext(val source: String) : RemoteControlCommand()

    @Serializable
    @SerialName("playTrack")
    data class PlayTrack(val track: MetadataTrack) : RemoteControlCommand()

    @Serializable
    @SerialName("addTrackToQueue")
    data class AddTrackToQueue(val track: MetadataTrack) : RemoteControlCommand()

    @Serializable
    @SerialName("playTrackNext")
    data class PlayTrackNext(val track: MetadataTrack) : RemoteControlCommand()

    @Serializable
    @SerialName("addTracksToQueue")
    data class AddTracksToQueue(val tracks: List<MetadataTrack>) : RemoteControlCommand()

    @Serializable
    @SerialName("playTracksNext")
    data class PlayTracksNext(val tracks: List<MetadataTrack>) : RemoteControlCommand()

    @Serializable
    @SerialName("playIndex")
    data class PlayIndex(val index: Int) : RemoteControlCommand()

    @Serializable
    @SerialName("removeFromQueue")
    data class RemoveFromQueue(val mediaUrl: String) : RemoteControlCommand()
}

@Serializable
sealed class RemoteControlEvent {
    @Serializable
    @SerialName("connected")
    data object Connected : RemoteControlEvent()

    @Serializable
    @SerialName("waitingForPermission")
    data class WaitingForPermission(val message: String) : RemoteControlEvent()

    @Serializable
    @SerialName("playerState")
    data class PlayerState(
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val volume: Float,
        val shuffleEnabled: Boolean,
        val loopMode: String,
        val currentTrackId: String?,
        val currentTrackTitle: String?,
        val currentTrackArtists: String?,
        val currentTrackAlbum: String?,
        val currentTrackCoverUrl: String?,
    ) : RemoteControlEvent()

    @Serializable
    @SerialName("queueUpdated")
    data class QueueUpdated(
        val entries: List<RemoteQueueEntry>,
        val currentIndex: Int,
    ) : RemoteControlEvent()

    @Serializable
    @SerialName("ack")
    data class Ack(val commandId: String) : RemoteControlEvent()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : RemoteControlEvent()
}

@Serializable
data class RemoteQueueEntry(
    val mediaUrl: String,
    val trackId: String,
    val title: String,
    val artists: String,
    val album: String?,
    val coverUrl: String?,
    val durationMs: Long,
)