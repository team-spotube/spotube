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

import co.touchlab.kermit.Logger
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.modules.blacklist.BlacklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

enum class PlaybackDestinationAction {
    Play,
    AddToQueue,
    PlayNext,
}

enum class RemoteCollectionType {
    Playlist,
    Album,
    ArtistTopTracks,
    SavedTracks,
}

/**
 * A playback request awaiting a destination choice (local device vs a connected
 * remote device). [title] is the content label shown in the picker dialog.
 */
sealed interface PlaybackDestinationRequest {
    val title: String
    val action: PlaybackDestinationAction

    data class Collection(
        override val title: String,
        override val action: PlaybackDestinationAction,
        val type: RemoteCollectionType,
        val id: String,
        val startTrack: MetadataTrack? = null,
    ) : PlaybackDestinationRequest

    data class Track(
        override val title: String,
        override val action: PlaybackDestinationAction,
        val track: MetadataTrack,
    ) : PlaybackDestinationRequest

    data class Tracks(
        override val title: String,
        override val action: PlaybackDestinationAction,
        val tracks: List<MetadataTrack>,
    ) : PlaybackDestinationRequest
}

/**
 * Routes playback actions (play / add to queue / play next) to either the local
 * device or a connected remote device. When a remote device is connected the
 * user is shown a destination picker; otherwise the action runs locally.
 */
class RemotePlaybackController(
    private val remoteControlClient: RemoteControlClient,
    private val collectionPlaybackHelper: CollectionPlaybackHelper,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val blacklistRepository: BlacklistRepository,
    private val jamRoomService: JamRoomService,
) : KoinComponent {
    private val logger = Logger.withTag("RemotePlaybackController")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pendingRequest = MutableStateFlow<PlaybackDestinationRequest?>(null)
    val pendingRequest: StateFlow<PlaybackDestinationRequest?> = _pendingRequest.asStateFlow()

    /** One-shot user-facing messages (e.g. "added to jam queue") for a snackbar host. */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun isRemoteConnected(): Boolean {
        return remoteControlClient.connectionState.value is ConnectionState.Connected
    }

    // ---------- Collection actions ----------

    fun requestCollectionPlay(
        type: RemoteCollectionType,
        id: String,
        title: String,
        startTrack: MetadataTrack? = null,
    ) {
        request(PlaybackDestinationRequest.Collection(title, PlaybackDestinationAction.Play, type, id, startTrack))
    }

    fun requestCollectionAddToQueue(type: RemoteCollectionType, id: String, title: String) {
        request(PlaybackDestinationRequest.Collection(title, PlaybackDestinationAction.AddToQueue, type, id))
    }

    fun requestCollectionPlayNext(type: RemoteCollectionType, id: String, title: String) {
        request(PlaybackDestinationRequest.Collection(title, PlaybackDestinationAction.PlayNext, type, id))
    }

    // ---------- Single track actions ----------

    fun requestTrackAddToQueue(track: MetadataTrack) {
        request(PlaybackDestinationRequest.Track(track.title, PlaybackDestinationAction.AddToQueue, track))
    }

    fun requestTrackPlayNext(track: MetadataTrack) {
        request(PlaybackDestinationRequest.Track(track.title, PlaybackDestinationAction.PlayNext, track))
    }

    // ---------- Bulk track actions ----------

    fun requestTracksAddToQueue(tracks: List<MetadataTrack>, title: String) {
        if (tracks.isEmpty()) return
        request(PlaybackDestinationRequest.Tracks(title, PlaybackDestinationAction.AddToQueue, tracks))
    }

    fun requestTracksPlayNext(tracks: List<MetadataTrack>, title: String) {
        if (tracks.isEmpty()) return
        request(PlaybackDestinationRequest.Tracks(title, PlaybackDestinationAction.PlayNext, tracks))
    }

    // ---------- Picker resolution ----------

    fun playLocally() {
        val request = _pendingRequest.value ?: return
        _pendingRequest.value = null
        executeLocally(request)
    }

    fun playOnRemote() {
        val request = _pendingRequest.value ?: return
        _pendingRequest.value = null
        executeOnRemote(request)
    }

    fun dismissPicker() {
        _pendingRequest.value = null
    }

    // ---------- Jam actions ----------

    /**
     * Adds a single track to the active jam queue. The host applies it to the
     * local (shared) queue directly; a guest suggests it to the host over MQTT.
     */
    fun addTrackToJam(track: MetadataTrack) {
        if (jamRoomService.role.value == null) return
        scope.launch {
            try {
                when (jamRoomService.role.value) {
                    JamRole.Host -> audioPlayerQueue.addToQueue(
                        QueueEntry.StreamingTrack(track = track, url = "", addedBy = jamRoomService.participantClientId)
                    )

                    JamRole.Guest -> jamRoomService.suggestTrack(track)
                    null -> return@launch
                }
                _events.emit("Added to the jam queue")
            } catch (e: Exception) {
                logger.e(e) { "Failed to add track to jam session" }
            }
        }
    }

    /**
     * Adds multiple tracks to the active jam queue (host applies locally,
     * guest suggests to the host).
     */
    fun addTracksToJam(tracks: List<MetadataTrack>) {
        if (tracks.isEmpty() || jamRoomService.role.value == null) return
        scope.launch {
            try {
                when (jamRoomService.role.value) {
                    JamRole.Host -> audioPlayerQueue.addAllToQueue(
                        tracks.map { track ->
                            QueueEntry.StreamingTrack(
                                track = track,
                                url = "",
                                addedBy = jamRoomService.participantClientId,
                            )
                        }
                    )

                    JamRole.Guest -> jamRoomService.suggestPlaylist(tracks)
                    null -> return@launch
                }
                _events.emit("Added ${tracks.size} to the jam queue")
            } catch (e: Exception) {
                logger.e(e) { "Failed to add tracks to jam session" }
            }
        }
    }

    // ---------- Internals ----------

    private fun request(request: PlaybackDestinationRequest) {
        if (isRemoteConnected()) {
            _pendingRequest.value = request
        } else {
            executeLocally(request)
        }
    }

    private fun executeLocally(request: PlaybackDestinationRequest) {
        scope.launch {
            when (request) {
                is PlaybackDestinationRequest.Collection -> {
                    val startTrack = request.startTrack
                    when (request.type) {
                        RemoteCollectionType.Playlist -> when (request.action) {
                            PlaybackDestinationAction.Play -> {
                                if (startTrack != null) {
                                    collectionPlaybackHelper.playPlaylistFromTrack(request.id, startTrack)
                                } else {
                                    collectionPlaybackHelper.playPlaylist(request.id)
                                }
                            }

                            PlaybackDestinationAction.AddToQueue -> collectionPlaybackHelper.addPlaylistToQueue(request.id)
                            PlaybackDestinationAction.PlayNext -> collectionPlaybackHelper.playPlaylistNext(request.id)
                        }

                        RemoteCollectionType.Album -> when (request.action) {
                            PlaybackDestinationAction.Play -> {
                                if (startTrack != null) {
                                    collectionPlaybackHelper.playAlbumFromTrack(request.id, startTrack)
                                } else {
                                    collectionPlaybackHelper.playAlbum(request.id)
                                }
                            }

                            PlaybackDestinationAction.AddToQueue -> collectionPlaybackHelper.addAlbumToQueue(request.id)
                            PlaybackDestinationAction.PlayNext -> collectionPlaybackHelper.playAlbumNext(request.id)
                        }

                        RemoteCollectionType.ArtistTopTracks -> when (request.action) {
                            PlaybackDestinationAction.Play -> collectionPlaybackHelper.playArtistTopTracks(request.id)
                            PlaybackDestinationAction.AddToQueue -> collectionPlaybackHelper.addArtistTopTracksToQueue(request.id)
                            PlaybackDestinationAction.PlayNext -> collectionPlaybackHelper.playArtistTopTracksNext(request.id)
                        }

                        RemoteCollectionType.SavedTracks -> when (request.action) {
                            PlaybackDestinationAction.Play -> {
                                if (startTrack != null) {
                                    collectionPlaybackHelper.playSavedTracksFromTrack(startTrack)
                                } else {
                                    collectionPlaybackHelper.playSavedTracks()
                                }
                            }

                            PlaybackDestinationAction.AddToQueue -> collectionPlaybackHelper.addSavedTracksToQueue()
                            PlaybackDestinationAction.PlayNext -> collectionPlaybackHelper.addSavedTracksToQueue()
                        }
                    }
                }

                is PlaybackDestinationRequest.Track -> {
                    val entry = QueueEntry.StreamingTrack(track = request.track, url = "")
                    when (request.action) {
                        PlaybackDestinationAction.Play -> {
                            audioPlayerQueue.load(
                                entries = listOf(entry),
                                autoPlay = true,
                                startPosition = 0,
                                collectionEntry = null,
                            )
                        }

                        PlaybackDestinationAction.AddToQueue -> {
                            audioPlayerQueue.addToQueue(entry)
                        }

                        PlaybackDestinationAction.PlayNext -> {
                            val queue = audioPlayerQueue.getQueue()
                            queue.find { candidate ->
                                (candidate as? QueueEntry.StreamingTrack)?.track?.matchesTrack(request.track) == true
                            }?.let { audioPlayerQueue.removeFromQueue(it) }
                            audioPlayerQueue.addAllAfterCurrent(listOf(entry))
                        }
                    }
                }

                is PlaybackDestinationRequest.Tracks -> {
                    val entries = request.tracks
                        .filter { track -> !isTrackBlacklisted(track) }
                        .map { track -> QueueEntry.StreamingTrack(track = track, url = "") }
                    when (request.action) {
                        PlaybackDestinationAction.Play -> {
                            audioPlayerQueue.load(
                                entries = entries,
                                autoPlay = true,
                                startPosition = 0,
                                collectionEntry = null,
                            )
                        }

                        PlaybackDestinationAction.AddToQueue -> {
                            audioPlayerQueue.addAllToQueue(entries)
                        }

                        PlaybackDestinationAction.PlayNext -> {
                            audioPlayerQueue.addAllAfterCurrent(entries)
                        }
                    }
                }
            }
        }
    }

    private fun executeOnRemote(request: PlaybackDestinationRequest) {
        scope.launch {
            try {
                val command = when (request) {
                    is PlaybackDestinationRequest.Collection -> {
                        val source = when (request.type) {
                            RemoteCollectionType.Playlist -> "spotube://playlist/${request.id}"
                            RemoteCollectionType.Album -> "spotube://album/${request.id}"
                            RemoteCollectionType.ArtistTopTracks -> "spotube://artist/${request.id}"
                            RemoteCollectionType.SavedTracks -> "spotube://saved_tracks"
                        }
                        when (request.action) {
                            PlaybackDestinationAction.Play -> RemoteControlCommand.Play(source)
                            PlaybackDestinationAction.AddToQueue -> RemoteControlCommand.AddToQueue(source)
                            PlaybackDestinationAction.PlayNext -> RemoteControlCommand.PlayNext(source)
                        }
                    }

                    is PlaybackDestinationRequest.Track -> when (request.action) {
                        PlaybackDestinationAction.Play -> RemoteControlCommand.PlayTrack(request.track)
                        PlaybackDestinationAction.AddToQueue -> RemoteControlCommand.AddTrackToQueue(request.track)
                        PlaybackDestinationAction.PlayNext -> RemoteControlCommand.PlayTrackNext(request.track)
                    }

                    is PlaybackDestinationRequest.Tracks -> when (request.action) {
                        PlaybackDestinationAction.Play -> RemoteControlCommand.PlayTracksNext(request.tracks)
                        PlaybackDestinationAction.AddToQueue -> RemoteControlCommand.AddTracksToQueue(request.tracks)
                        PlaybackDestinationAction.PlayNext -> RemoteControlCommand.PlayTracksNext(request.tracks)
                    }
                }
                remoteControlClient.sendCommand(command)
                logger.i { "Sent remote ${request.action} for ${request.title}" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to send remote playback command" }
            }
        }
    }

    private suspend fun isTrackBlacklisted(track: MetadataTrack): Boolean {
        val trackIds = blacklistRepository.getTracksSnapshot().map { it.id }.toSet()
        val artistIds = blacklistRepository.getArtistsSnapshot().map { it.id }.toSet()
        return track.id in trackIds || track.artists.any { it.id in artistIds }
    }

    private fun MetadataTrack.matchesTrack(other: MetadataTrack): Boolean {
        if (id.isNotBlank() && other.id.isNotBlank()) return id == other.id
        return title == other.title &&
            durationMs == other.durationMs &&
            album?.id == other.album?.id &&
            artists.map { it.id.ifBlank { it.name } } == other.artists.map { it.id.ifBlank { it.name } }
    }
}