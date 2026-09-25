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

import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.LoopState
import dev.krtirtho.spotube.core.audioplayer.PlayerState as AudioPlayerState
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.core.playback.CollectionPlaybackHelper
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlin.coroutines.resume
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

class RemoteControlHandler(
    private val settingsRepository: SettingsRepository,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val collectionPlaybackHelper: CollectionPlaybackHelper,
) : KoinComponent {
    val logger by injectLogger<RemoteControlHandler>()

    val incomingConnectionRequests = MutableSharedFlow<ConnectionRequest>(
        extraBufferCapacity = 16,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private val pendingRequestResolutions = mutableMapOf<String, (ConnectionRequestResponse) -> Unit>()

    suspend fun handleConnection(session: WebSocketServerSession) {
        val settings = settingsRepository.userSettings.first()
        if (!settings.allowRemoteControl) {
            logger.w { "Rejecting remote control connection: remote control is disabled" }
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Remote control is disabled"))
            return
        }

        val deviceId = session.call.request.headers["X-Device-Id"]
        val deviceName = session.call.request.headers["X-Device-Name"] ?: "Unknown"

        val isAllowed = deviceId != null && deviceId in settings.allowedRemoteDevices

        if (!isAllowed) {
            // Send waiting for permission message
            val waitingMessage = RemoteControlEvent.WaitingForPermission(
                "Waiting for permission from $deviceName..."
            )
            session.send(Frame.Text(json.encodeToString(RemoteControlEvent.serializer(), waitingMessage)))

            val request = ConnectionRequest(
                deviceId = deviceId ?: "unknown",
                deviceName = deviceName,
                sessionId = session.call.request.headers["X-Request-Id"]
                    ?: "req-${kotlin.time.Clock.System.now().toEpochMilliseconds()}",
            )
            incomingConnectionRequests.emit(request)
            val response = waitForRequestResolution(request.sessionId)
            if (response != ConnectionRequestResponse.Allow && response != ConnectionRequestResponse.AllowAlways) {
                session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Connection denied"))
                return
            }
            if (response == ConnectionRequestResponse.AllowAlways && deviceId != null) {
                settingsRepository.updateSettings(
                    settings.copy(
                        allowedRemoteDevices = (settings.allowedRemoteDevices + deviceId).distinct()
                    )
                )
                logger.i { "Device $deviceId added to always-allowed devices" }
            }
        }

        // Send connected message
        session.send(Frame.Text(json.encodeToString(RemoteControlEvent.serializer(), RemoteControlEvent.Connected)))
        logger.i { "Remote control connection established from $deviceName ($deviceId)" }

        // Broadcast initial player state so the controller shows current track info
        broadcastState(session)
        broadcastQueue(session)

        try {
            // Periodically push player state so the controller's progress bar
            // stays in sync even when no commands are being sent.
            coroutineScope {
                launch {
                    while (isActive) {
                        delay(1_000)
                        broadcastState(session)
                    }
                }
                handleControlLoop(session)
            }
        } catch (e: Exception) {
            logger.w(e) { "Error in remote control session" }
        } finally {
            logger.d { "Closing session in finally block" }
            session.close()
        }
    }

    fun resolveConnectionRequest(sessionId: String, response: ConnectionRequestResponse) {
        pendingRequestResolutions.remove(sessionId)?.invoke(response)
    }

    private suspend fun waitForRequestResolution(sessionId: String): ConnectionRequestResponse {
        return suspendCancellableCoroutine { continuation ->
            pendingRequestResolutions[sessionId] = { response ->
                if (continuation.isActive) {
                    continuation.resume(response)
                }
            }
            continuation.invokeOnCancellation {
                pendingRequestResolutions.remove(sessionId)
            }
        }
    }

    private suspend fun handleControlLoop(session: WebSocketServerSession) {
        logger.d { "Starting control loop for session" }
        for (frame in session.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                logger.d { "Received command: $text" }
                try {
                    val envelope = json.decodeFromString(CommandEnvelope.serializer(), text)
                    handleCommand(session, envelope)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to parse remote control command" }
                    sendError(session, "Invalid command: ${e.message}")
                }
            }
        }
        logger.d { "Control loop exited normally" }
    }

    private suspend fun handleCommand(session: WebSocketServerSession, envelope: CommandEnvelope) {
        when (val command = envelope.command) {
            is RemoteControlCommand.Play -> {
                handleCollectionSource(command.source, RemoteCollectionAction.Play)
            }
            is RemoteControlCommand.Pause -> {
                audioPlayer.pause()
            }
            is RemoteControlCommand.TogglePlayPause -> {
                val isPlaying = audioPlayer.playerStateFlow.value == AudioPlayerState.PLAYING
                if (isPlaying) {
                    audioPlayer.pause()
                    waitForPlaybackState(expectPlaying = false)
                } else {
                    audioPlayer.play()
                    waitForPlaybackState(expectPlaying = true)
                }
            }
            is RemoteControlCommand.Seek -> {
                audioPlayer.seekTo(kotlin.time.Duration.parse("${command.positionMs}ms"))
            }
            is RemoteControlCommand.SetVolume -> {
                audioPlayer.setVolume(command.volume)
            }
            is RemoteControlCommand.SkipNext -> {
                audioPlayer.skipToNext()
            }
            is RemoteControlCommand.SkipPrevious -> {
                audioPlayer.skipToPrevious()
            }
            is RemoteControlCommand.SetShuffle -> {
                audioPlayer.shuffle(command.enabled)
            }
            is RemoteControlCommand.SetLoopMode -> {
                val loopState = when (command.mode) {
                    "none" -> LoopState.NONE
                    "one" -> LoopState.ONE
                    "all" -> LoopState.ALL
                    else -> {
                        sendError(session, "Invalid loop mode: ${command.mode}")
                        return
                    }
                }
                audioPlayer.loop(loopState)
            }
            is RemoteControlCommand.AddToQueue -> {
                handleCollectionSource(command.source, RemoteCollectionAction.AddToQueue)
            }
            is RemoteControlCommand.PlayNext -> {
                handleCollectionSource(command.source, RemoteCollectionAction.PlayNext)
            }
            is RemoteControlCommand.PlayTrack -> {
                audioPlayerQueue.load(
                    entries = listOf(QueueEntry.StreamingTrack(track = command.track, url = "")),
                    autoPlay = true,
                    startPosition = 0,
                    collectionEntry = null,
                )
            }
            is RemoteControlCommand.AddTrackToQueue -> {
                audioPlayerQueue.addToQueue(QueueEntry.StreamingTrack(track = command.track, url = ""))
            }
            is RemoteControlCommand.PlayTrackNext -> {
                audioPlayerQueue.addAllAfterCurrent(listOf(QueueEntry.StreamingTrack(track = command.track, url = "")))
            }
            is RemoteControlCommand.AddTracksToQueue -> {
                val entries = command.tracks.map { track ->
                    QueueEntry.StreamingTrack(track = track, url = "")
                }
                audioPlayerQueue.addAllToQueue(entries)
            }
            is RemoteControlCommand.PlayTracksNext -> {
                val entries = command.tracks.map { track ->
                    QueueEntry.StreamingTrack(track = track, url = "")
                }
                audioPlayerQueue.addAllAfterCurrent(entries)
            }
            is RemoteControlCommand.PlayIndex -> {
                audioPlayerQueue.jumpTo(command.index)
            }
            is RemoteControlCommand.RemoveFromQueue -> {
                val queue = audioPlayerQueue.queueFlow.value
                val entry = queue.firstOrNull { candidate ->
                    when (candidate) {
                        is QueueEntry.StreamingTrack -> candidate.track.id == command.mediaUrl
                        is QueueEntry.LocalTrack -> candidate.url == command.mediaUrl
                    }
                }
                if (entry != null) {
                    audioPlayerQueue.removeFromQueue(entry)
                } else {
                    logger.w { "Remote remove: no matching queue entry for ${command.mediaUrl}" }
                }
            }
        }
        sendAck(session, envelope.commandId)
        broadcastState(session)
        broadcastQueue(session)
    }

    private suspend fun sendAck(session: WebSocketServerSession, commandId: String) {
        val text = json.encodeToString(RemoteControlEvent.serializer(), RemoteControlEvent.Ack(commandId))
        session.send(Frame.Text(text))
    }

    private suspend fun sendError(session: WebSocketServerSession, message: String) {
        val text = json.encodeToString(RemoteControlEvent.serializer(), RemoteControlEvent.Error(message))
        session.send(Frame.Text(text))
    }

    /**
     * Player state changes are applied asynchronously (e.g. ExoPlayer listener
     * callbacks posted to the main looper), so after play/pause we poll until
     * [playerStateFlow] reflects the expected state before broadcasting it back
     * to the controller. Otherwise the client would see a stale (inverted) icon.
     */
    private suspend fun waitForPlaybackState(expectPlaying: Boolean, timeoutMs: Long = 1_000) {
        val timeoutAt = TimeSource.Monotonic.markNow() + timeoutMs.milliseconds
        while (timeoutAt.hasNotPassedNow()) {
            if ((audioPlayer.playerStateFlow.value == AudioPlayerState.PLAYING) == expectPlaying) return
            delay(25)
        }
    }

    /**
     * Resolves a `spotube://` collection source URI (playlist/album/artist top
     * tracks/saved tracks) and applies the requested action on the remote queue.
     */
    private suspend fun handleCollectionSource(source: String, action: RemoteCollectionAction) {
        logger.d { "Remote collection $action for source: $source" }
        when {
            source.startsWith(COLLECTION_PLAYLIST_PREFIX) -> {
                val id = source.removePrefix(COLLECTION_PLAYLIST_PREFIX)
                when (action) {
                    RemoteCollectionAction.Play -> collectionPlaybackHelper.playPlaylist(id)
                    RemoteCollectionAction.AddToQueue -> collectionPlaybackHelper.addPlaylistToQueue(id)
                    RemoteCollectionAction.PlayNext -> collectionPlaybackHelper.playPlaylistNext(id)
                }
            }

            source.startsWith(COLLECTION_ALBUM_PREFIX) -> {
                val id = source.removePrefix(COLLECTION_ALBUM_PREFIX)
                when (action) {
                    RemoteCollectionAction.Play -> collectionPlaybackHelper.playAlbum(id)
                    RemoteCollectionAction.AddToQueue -> collectionPlaybackHelper.addAlbumToQueue(id)
                    RemoteCollectionAction.PlayNext -> collectionPlaybackHelper.playAlbumNext(id)
                }
            }

            source.startsWith(COLLECTION_ARTIST_TOP_PREFIX) -> {
                val id = source.removePrefix(COLLECTION_ARTIST_TOP_PREFIX)
                when (action) {
                    RemoteCollectionAction.Play -> collectionPlaybackHelper.playArtistTopTracks(id)
                    RemoteCollectionAction.AddToQueue -> collectionPlaybackHelper.addArtistTopTracksToQueue(id)
                    RemoteCollectionAction.PlayNext -> collectionPlaybackHelper.playArtistTopTracksNext(id)
                }
            }

            source == COLLECTION_SAVED_TRACKS -> {
                when (action) {
                    RemoteCollectionAction.Play -> collectionPlaybackHelper.playSavedTracks()
                    RemoteCollectionAction.AddToQueue -> collectionPlaybackHelper.addSavedTracksToQueue()
                    RemoteCollectionAction.PlayNext -> {
                        // Saved tracks "play next" is not supported; add to queue instead
                        collectionPlaybackHelper.addSavedTracksToQueue()
                    }
                }
            }

            else -> logger.w { "Unknown remote collection source: $source" }
        }
    }

    private suspend fun broadcastState(session: WebSocketServerSession) {
        val current = audioPlayerQueue.currentQueueEntryFlow.value
        val state = RemoteControlEvent.PlayerState(
            isPlaying = audioPlayer.playerStateFlow.value == AudioPlayerState.PLAYING,
            positionMs = audioPlayer.positionFlow.value.inWholeMilliseconds,
            durationMs = audioPlayer.durationFlow.value.inWholeMilliseconds,
            volume = audioPlayer.volumeFlow.value,
            shuffleEnabled = audioPlayer.shuffleModeFlow.value,
            loopMode = audioPlayer.loopStateFlow.value.name.lowercase(),
            currentTrackId = current?.mediaKey(),
            currentTrackTitle = current?.titleOrNull(),
            currentTrackArtists = current?.artistsOrNull(),
            currentTrackAlbum = current?.albumOrNull(),
            currentTrackCoverUrl = current?.coverUrlOrNull(),
        )
        val text = json.encodeToString(RemoteControlEvent.serializer(), state)
        session.send(Frame.Text(text))
    }

    private suspend fun broadcastQueue(session: WebSocketServerSession) {
        val queue = audioPlayerQueue.queueFlow.value
        val current = audioPlayerQueue.currentQueueEntryFlow.value
        val currentIndex = if (current != null) {
            queue.indexOfFirst { it.matchesCurrent(current) }
        } else {
            -1
        }
        val event = RemoteControlEvent.QueueUpdated(
            entries = queue.map { it.toRemoteQueueEntry() },
            currentIndex = currentIndex,
        )
        val text = json.encodeToString(RemoteControlEvent.serializer(), event)
        session.send(Frame.Text(text))
    }

    private fun QueueEntry.matchesCurrent(current: QueueEntry): Boolean {
        return when {
            this is QueueEntry.StreamingTrack && current is QueueEntry.StreamingTrack -> {
                this.track.id == current.track.id
            }

            this is QueueEntry.LocalTrack && current is QueueEntry.LocalTrack -> {
                this.url == current.url && this.name == current.name
            }

            else -> false
        }
    }

    private fun QueueEntry.toRemoteQueueEntry(): RemoteQueueEntry = when (this) {
        is QueueEntry.StreamingTrack -> RemoteQueueEntry(
            mediaUrl = track.id,
            trackId = track.id,
            title = track.title,
            artists = track.artists.joinToString(", ") { artist -> artist.name },
            album = track.album?.title,
            coverUrl = coverUrlOrNull(),
            durationMs = track.durationMs,
        )

        is QueueEntry.LocalTrack -> RemoteQueueEntry(
            mediaUrl = url,
            trackId = url,
            title = name,
            artists = artists.joinToString(", "),
            album = album,
            coverUrl = null,
            durationMs = duration,
        )
    }

    private fun QueueEntry.mediaKey(): String = when (this) {
        is QueueEntry.StreamingTrack -> track.id
        is QueueEntry.LocalTrack -> url
    }

    private fun QueueEntry.titleOrNull(): String = when (this) {
        is QueueEntry.StreamingTrack -> track.title
        is QueueEntry.LocalTrack -> name
    }

    private fun QueueEntry.artistsOrNull(): String = when (this) {
        is QueueEntry.StreamingTrack -> track.artists.joinToString(", ") { artist -> artist.name }
        is QueueEntry.LocalTrack -> artists.joinToString(", ")
    }

    private fun QueueEntry.albumOrNull(): String? = when (this) {
        is QueueEntry.StreamingTrack -> track.album?.title
        is QueueEntry.LocalTrack -> album
    }

    private fun QueueEntry.coverUrlOrNull(): String? = when (this) {
        is QueueEntry.StreamingTrack -> track.thumbnails?.maxByOrNull { it.width * it.height }?.url
            ?: track.album?.thumbnails?.maxByOrNull { it.width * it.height }?.url
        is QueueEntry.LocalTrack -> null
    }
}

@Serializable
data class CommandEnvelope(
    val commandId: String,
    val command: RemoteControlCommand,
)

private const val COLLECTION_PLAYLIST_PREFIX = "spotube://playlist/"
private const val COLLECTION_ALBUM_PREFIX = "spotube://album/"
private const val COLLECTION_ARTIST_TOP_PREFIX = "spotube://artist/"
private const val COLLECTION_SAVED_TRACKS = "spotube://saved_tracks"

enum class RemoteCollectionAction {
    Play,
    AddToQueue,
    PlayNext,
}

data class ConnectionRequest(
    val deviceId: String,
    val deviceName: String,
    val sessionId: String,
)

enum class ConnectionRequestResponse {
    Allow,
    AllowAlways,
    Deny,
}