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

package org.spotube.spotube.core.scrobble

import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.scrobble.ScrobbleTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.plugin.ScrobblePluginSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.time.Clock

/**
 * Watches local playback and submits scrobbles through the currently selected scrobble plugin.
 *
 * Scrobbling follows the standard Last.fm rules, which plugins are expected to honour as well:
 * - Tracks shorter than [MIN_TRACK_DURATION_MS] are never scrobbled.
 * - The track must be listened to for at least half of its duration, or [MAX_SCROBBLE_THRESHOLD_MS],
 *   whichever comes first.
 *
 * Only time during which the player is actively [PlayerState.PLAYING] is counted, so pauses are
 * ignored, and forward seeks don't credit unheard audio. The timestamp sent with the scrobble is
 * the moment playback of the track started. Each play is scrobbled at most once; replaying or
 * repeating the track starts a new scrobble session.
 */
class ScrobbleManager(
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val scrobblePluginSource: ScrobblePluginSource,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) : KoinComponent {

    private val logger by injectLogger<ScrobbleManager>()

    private var activeTrackKey: String? = null
    private var playbackStartedAtEpochSeconds: Long? = null
    private var listenedMs: Long = 0L
    private var lastPositionMs: Long? = null
    private var scrobbled = false

    init {
        scope.launch {
            combine(
                audioPlayerQueue.currentQueueEntryFlow,
                audioPlayer.playerStateFlow,
                audioPlayer.positionFlow,
                audioPlayer.durationFlow) { entry, playerState, position, duration ->
                PlaybackSnapshot(
                    entry = entry,
                    playerState = playerState,
                    positionMs = position.inWholeMilliseconds,
                    playerDurationMs = duration.inWholeMilliseconds)
            }.collect(::onPlaybackSnapshot)
        }
    }

    private fun onPlaybackSnapshot(snapshot: PlaybackSnapshot) {
        val entry = snapshot.entry
        if (entry == null) {
            clearSession()
            return
        }

        val trackKey = entry.trackKey()
        if (trackKey != activeTrackKey) {
            beginSession(trackKey, snapshot.positionMs)
            return
        }

        val previousPositionMs = lastPositionMs
        lastPositionMs = snapshot.positionMs

        if (snapshot.playerState != PlayerState.PLAYING) return
        if (previousPositionMs == null) {
            playbackStartedAtEpochSeconds = playbackStartedAtEpochSeconds ?: nowEpochSeconds()
            return
        }

        if (playbackStartedAtEpochSeconds == null) {
            playbackStartedAtEpochSeconds = nowEpochSeconds()
        }

        val deltaMs = snapshot.positionMs - previousPositionMs
        if (deltaMs < 0) {
            // A backward seek to the very beginning means the play started over
            // (e.g. repeat one), so allow it to be scrobbled again.
            if (snapshot.positionMs <= RESTART_POSITION_THRESHOLD_MS) {
                restartSession(snapshot.positionMs)
            }
            return
        }

        // A jump larger than a couple of position ticks is a forward seek or a
        // buffering/app-suspension stall. Never credit unplayed audio for it.
        if (deltaMs > MAX_COUNTED_POSITION_DELTA_MS) return

        listenedMs += deltaMs
        if (scrobbled) return

        val durationMs = entry.durationMs().takeIf { it > 0 } ?: snapshot.playerDurationMs
        val thresholdMs = scrobbleThresholdMs(durationMs) ?: return
        if (listenedMs < thresholdMs) return

        if (scrobblePluginSource.selectedScrobblePlugin.value == null) {
            // No scrobble plugin selected yet: keep the session hot so it can be
            // submitted if the user selects one while the track is still playing.
            return
        }

        scrobbled = true
        val track = entry.toScrobbleTrack(
            timestamp = playbackStartedAtEpochSeconds ?: nowEpochSeconds(),
            durationMs = durationMs)
        scope.launch { submitScrobble(track) }
    }

    private suspend fun submitScrobble(track: ScrobbleTrack) {
        val plugin = scrobblePluginSource.selectedScrobblePlugin.value ?: return
        try {
            plugin.use { scrobbleAPI.scrobble(track) }
            logger.d { "Scrobbled \"${track.trackName}\" by ${track.artistName} via ${plugin.pluginId}" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to scrobble \"${track.trackName}\" by ${track.artistName}" }
        }
    }

    private fun beginSession(trackKey: String, positionMs: Long) {
        activeTrackKey = trackKey
        restartSession(positionMs)
    }

    private fun restartSession(positionMs: Long) {
        listenedMs = 0L
        playbackStartedAtEpochSeconds = null
        lastPositionMs = positionMs
        scrobbled = false
    }

    private fun clearSession() {
        activeTrackKey = null
        listenedMs = 0L
        playbackStartedAtEpochSeconds = null
        lastPositionMs = null
        scrobbled = false
    }

    private fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    private fun QueueEntry.trackKey(): String = when (this) {
        is QueueEntry.StreamingTrack -> track.id
        is QueueEntry.LocalTrack -> url
    }

    private fun QueueEntry.durationMs(): Long = when (this) {
        is QueueEntry.StreamingTrack -> track.durationMs
        is QueueEntry.LocalTrack -> duration
    }

    private fun QueueEntry.toScrobbleTrack(timestamp: Long, durationMs: Long): ScrobbleTrack {
        return when (this) {
            is QueueEntry.StreamingTrack -> ScrobbleTrack(
                timestamp = timestamp,
                trackId = track.id,
                artistId = track.artists.firstOrNull()?.id.orEmpty(),
                albumId = track.album?.id,
                trackName = track.title,
                artistName = track.artists.joinToString(", ") { it.name },
                albumName = track.album?.title,
                streamingProvider = resolveStreamingProvider(track),
                durationMs = durationMs)

            is QueueEntry.LocalTrack -> ScrobbleTrack(
                timestamp = timestamp,
                trackId = url,
                artistId = "",
                albumId = null,
                trackName = name,
                artistName = artists.joinToString(", "),
                albumName = album,
                streamingProvider = LOCAL_PROVIDER,
                durationMs = durationMs)
        }
    }

    /** Maps a track's external URI to the streaming provider key scrobble plugins expect. */
    private fun resolveStreamingProvider(track: MetadataTrack): String {
        val host = track.externalUri
            ?.substringAfter("://", missingDelimiterValue = "")
            ?.substringBefore('/')
            ?.substringAfter('@')
            ?.substringBefore(':')
            ?.lowercase()
            ?.removePrefix("www.")
            .orEmpty()

        if (host.isBlank()) return UNKNOWN_PROVIDER

        return when {
            "spotify" in host -> "spotify"
            "apple" in host -> "apple_music"
            "music.youtube" in host -> "youtube_music"
            "youtube" in host || "youtu.be" in host -> "youtube"
            "deezer" in host -> "deezer"
            "tidal" in host -> "tidal"
            "soundcloud" in host -> "soundcloud"
            "amazon" in host -> "amazon_music"
            "pandora" in host -> "pandora"
            else -> host.substringBefore('.')
        }
    }

    private data class PlaybackSnapshot(
        val entry: QueueEntry?,
        val playerState: PlayerState,
        val positionMs: Long,
        val playerDurationMs: Long)

    companion object {
        /** Tracks shorter than this are never scrobbled (Last.fm rule). */
        private const val MIN_TRACK_DURATION_MS = 30_000L

        /** Listening threshold is capped at four minutes (Last.fm rule). */
        private const val MAX_SCROBBLE_THRESHOLD_MS = 4 * 60 * 1000L

        /** Largest position delta credited as listening, covers position polling jitter. */
        private const val MAX_COUNTED_POSITION_DELTA_MS = 2_000L

        /** Positions at or below this are treated as the start of a fresh play. */
        private const val RESTART_POSITION_THRESHOLD_MS = 5_000L

        private const val UNKNOWN_PROVIDER = "unknown"
        private const val LOCAL_PROVIDER = "local"

        private fun scrobbleThresholdMs(durationMs: Long): Long? {
            if (durationMs < MIN_TRACK_DURATION_MS) return null
            return minOf(durationMs / 2, MAX_SCROBBLE_THRESHOLD_MS)
        }
    }
}
