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

package dev.krtirtho.spotube.core.audioplayer

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class MediaItem(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Duration,
    val coverURL: String,
    val url: String,
    val protocol: StreamProtocol = StreamProtocol.PROGRESSIVE,
)

enum class LoopState {
    NONE, ONE, ALL;

    fun next(): LoopState {
        return when (this) {
            NONE -> ONE
            ONE -> ALL
            ALL -> NONE
        }
    }
}

enum class PlayerState {
    IDLE, BUFFERING, READY, PLAYING, PAUSED, COMPLETED
}

@Serializable
sealed interface QueueEntry {
    val url: String
    val addedBy: String

    @Serializable
    @SerialName("streaming")
    data class StreamingTrack(
        val track: MetadataTrack,
        override val url: String,
        val protocol: StreamProtocol = StreamProtocol.PROGRESSIVE,
        override val addedBy: String = "",
    ) : QueueEntry

    @Serializable
    @SerialName("local")
    data class LocalTrack(
        val name: String,
        val artists: List<String>,
        val duration: Long,
        val album: String?,
        val coverBytes: ByteArray?,
        override val url: String,
        override val addedBy: String = "",
    ) : QueueEntry {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as LocalTrack

            if (duration != other.duration) return false
            if (name != other.name) return false
            if (artists != other.artists) return false
            if (album != other.album) return false
            if (!coverBytes.contentEquals(other.coverBytes)) return false
            if (url != other.url) return false
            if (addedBy != other.addedBy) return false

            return true
        }

        override fun hashCode(): Int {
            var result = duration.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + artists.hashCode()
            result = 31 * result + (album?.hashCode() ?: 0)
            result = 31 * result + (coverBytes?.contentHashCode() ?: 0)
            result = 31 * result + url.hashCode()
            result = 31 * result + addedBy.hashCode()
            return result
        }
    }
}

@Serializable
sealed interface QueueCollectionEntry {
    val id: String

    @Serializable
    @SerialName("playlist")
    data class Playlist(
        override val id: String,
    ) : QueueCollectionEntry

    @Serializable
    @SerialName("album")
    data class Album(
        override val id: String,
    ) : QueueCollectionEntry

    @Serializable
    @SerialName("saved_tracks")
    data object SavedTracks : QueueCollectionEntry {
        override val id: String = "saved_tracks"
    }
}

@Serializable
data class PersistedQueueState(
    val entries: List<QueueEntry>,
    val currentIndex: Int,
    val currentCollectionEntry: QueueCollectionEntry? = null,
    val collectionHistory: List<QueueCollectionEntry> = emptyList(),
)
