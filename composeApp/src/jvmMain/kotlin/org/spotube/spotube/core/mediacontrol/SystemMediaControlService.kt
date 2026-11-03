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

package org.spotube.spotube.core.mediacontrol

import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.MediaItem
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.di.injectLogger
import dev.nucleusframework.media.control.MediaControlEvent
import dev.nucleusframework.media.control.MediaControlService
import dev.nucleusframework.media.control.MediaMetadata
import dev.nucleusframework.media.control.MediaPlaybackState
import dev.nucleusframework.media.control.MediaPlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.milliseconds

/**
 * Publishes now-playing metadata and playback state to the OS media center
 * (MPRIS on Linux, Now Playing on macOS, SMTC on Windows) and forwards media
 * key commands back into the [AudioPlayerInterface].
 *
 * Events from the OS are delivered on the Swing EDT; playback commands are
 * dispatched onto [scope] so they reach the player's suspend API.
 */
class SystemMediaControlService(
    private val audioPlayer: AudioPlayerInterface) : KoinComponent, AutoCloseable {

    private val logger by injectLogger<SystemMediaControlService>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val enabled: Boolean = MediaControlService.isAvailable()

    init {
        if (enabled) {
            MediaControlService.configure()
            MediaControlService.attach(::onMediaControlEvent)
            observePlayer()
        } else {
            logger.i { "System media control backend unavailable; skipping integration" }
        }
    }

    private fun onMediaControlEvent(event: MediaControlEvent) {
        when (event) {
            MediaControlEvent.Play -> scope.launch { audioPlayer.play() }
            MediaControlEvent.Pause -> scope.launch { audioPlayer.pause() }
            MediaControlEvent.Toggle -> scope.launch {
                if (audioPlayer.playerStateFlow.value == PlayerState.PLAYING) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.play()
                }
            }
            MediaControlEvent.Next -> scope.launch { audioPlayer.skipToNext() }
            MediaControlEvent.Previous -> scope.launch { audioPlayer.skipToPrevious() }
            MediaControlEvent.Stop -> scope.launch { audioPlayer.stop() }
            is MediaControlEvent.SeekBy -> scope.launch {
                audioPlayer.seekTo(audioPlayer.positionFlow.value + event.offsetMs.milliseconds)
            }
            is MediaControlEvent.SetPosition -> scope.launch {
                audioPlayer.seekTo(event.positionMs.milliseconds)
            }
            is MediaControlEvent.SetVolume -> scope.launch {
                audioPlayer.setVolume(event.volume.toFloat())
            }
            is MediaControlEvent.OpenUri,
            MediaControlEvent.Raise,
            MediaControlEvent.Quit,
            -> Unit
        }
    }

    private fun observePlayer() {
        scope.launch {
            audioPlayer.currentMediaItemFlow.collect { item ->
                if (item != null) {
                    MediaControlService.setMetadata(item.toMediaMetadata())
                }
            }
        }

        scope.launch {
            combine(
                audioPlayer.playerStateFlow,
                audioPlayer.positionFlow) { state, position ->
                MediaPlaybackState(
                    status = state.toMediaPlaybackStatus(),
                    positionMs = position.inWholeMilliseconds)
            }.collect { MediaControlService.setPlaybackState(it) }
        }

        scope.launch {
            audioPlayer.volumeFlow.collect { volume ->
                MediaControlService.setVolume(volume.toDouble())
            }
        }
    }

    private fun MediaItem.toMediaMetadata(): MediaMetadata {
        return MediaMetadata(
            title = title,
            artist = artist,
            album = album,
            coverUrl = coverURL.ifBlank { null },
            duration = duration.inWholeMilliseconds)
    }

    private fun PlayerState.toMediaPlaybackStatus(): MediaPlaybackStatus {
        return when (this) {
            PlayerState.PLAYING -> MediaPlaybackStatus.PLAYING
            PlayerState.PAUSED -> MediaPlaybackStatus.PAUSED
            else -> MediaPlaybackStatus.STOPPED
        }
    }

    override fun close() {
        if (enabled) {
            MediaControlService.detach()
        }
        scope.cancel()
    }
}
