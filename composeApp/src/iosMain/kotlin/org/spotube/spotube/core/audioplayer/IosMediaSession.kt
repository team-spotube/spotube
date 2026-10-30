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

@file:OptIn(ExperimentalForeignApi::class)

package org.spotube.spotube.core.audioplayer

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

internal class IosMediaSession {
    private val audioSession = AVAudioSession.sharedInstance()
    private val nowPlayingInfoCenter = MPNowPlayingInfoCenter.defaultCenter()
    private val remoteCommandCenter = MPRemoteCommandCenter.sharedCommandCenter()

    private var configured = false
    private val notificationCenter = NSNotificationCenter.defaultCenter()
    private val notificationTokens = mutableListOf<Any>()
    private val remoteCommandTokens = mutableListOf<Pair<MPRemoteCommand, Any>>()
    private var onPlay: (() -> Unit)? = null
    private var onPause: (() -> Unit)? = null
    private var onSeek: ((Double) -> Unit)? = null
    private var onNext: (() -> Unit)? = null
    private var onPrevious: (() -> Unit)? = null

    private var lastIsPlaying = false

    fun configure(
        onPlay: () -> Unit,
        onPause: () -> Unit,
        onSeek: (Double) -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit
    ) {
        this.onPlay = onPlay
        this.onPause = onPause
        this.onSeek = onSeek
        this.onNext = onNext
        this.onPrevious = onPrevious

        if (configured) return
        configured = true

        runCatching {
            audioSession.setCategory(
                category = AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = 0u,
                error = null
            )
        }
        registerAudioSessionNotifications()
        registerRemoteCommands()
    }

    fun activate() {
        // AVPlayer automatically activates the shared audio session on play.
    }

    fun deactivate() {
        // AVPlayer automatically manages session deactivation when paused or stopped.
    }

    fun update(
        item: MediaItem?,
        positionSeconds: Double,
        durationSeconds: Double,
        isPlaying: Boolean
    ) {
        if (item == null) {
            nowPlayingInfoCenter.nowPlayingInfo = null
            return
        }

        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.title
        info[MPMediaItemPropertyArtist] = item.artist
        item.album.takeIf { it.isNotBlank() }?.let {
            info[MPMediaItemPropertyAlbumTitle] = it
        }
        if (durationSeconds.isFinite() && durationSeconds > 0) {
            info[MPMediaItemPropertyPlaybackDuration] = durationSeconds
        }
        if (positionSeconds.isFinite() && positionSeconds >= 0) {
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionSeconds
        }
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0
        lastIsPlaying = isPlaying
        nowPlayingInfoCenter.nowPlayingInfo = info
    }

    fun clear() {
        lastIsPlaying = false
        nowPlayingInfoCenter.nowPlayingInfo = null
        notificationTokens.forEach { notificationCenter.removeObserver(it) }
        notificationTokens.clear()
        remoteCommandTokens.forEach { (command, token) -> command.removeTarget(token) }
        remoteCommandTokens.clear()
        configured = false
        onPlay = null
        onPause = null
        onSeek = null
        onNext = null
        onPrevious = null
    }

    private fun registerAudioSessionNotifications() {
        notificationTokens += notificationCenter.addObserverForName(
            name = platform.AVFAudio.AVAudioSessionInterruptionNotification,
            `object` = audioSession,
            queue = NSOperationQueue.mainQueue()
        ) { notification ->
            val type = (notification?.userInfo?.get(
                platform.AVFAudio.AVAudioSessionInterruptionTypeKey
            ) as? Number)?.toLong()?.toULong()
            if (type == platform.AVFAudio.AVAudioSessionInterruptionTypeBegan) {
                onPause?.invoke()
            } else if (type == platform.AVFAudio.AVAudioSessionInterruptionTypeEnded) {
                activate()
                val options = (notification?.userInfo?.get(
                    platform.AVFAudio.AVAudioSessionInterruptionOptionKey
                ) as? Number)?.toLong()?.toULong() ?: 0uL
                val shouldResume = (options and
                    platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume) != 0uL
                if (shouldResume) onPlay?.invoke()
            }
        }
    }

    private fun registerRemoteCommands() {
        remoteCommandTokens += remoteCommandCenter.playCommand to
            remoteCommandCenter.playCommand.addTargetWithHandler {
            onPlay?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandTokens += remoteCommandCenter.pauseCommand to
            remoteCommandCenter.pauseCommand.addTargetWithHandler {
            onPause?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandTokens += remoteCommandCenter.togglePlayPauseCommand to
            remoteCommandCenter.togglePlayPauseCommand.addTargetWithHandler {
            if (lastIsPlaying) {
                onPause?.invoke()
            } else {
                onPlay?.invoke()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandTokens += remoteCommandCenter.changePlaybackPositionCommand to
            remoteCommandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val position = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
            if (position != null && position.isFinite() && position >= 0) {
                onSeek?.invoke(position)
                MPRemoteCommandHandlerStatusSuccess
            } else {
                MPRemoteCommandHandlerStatusCommandFailed
            }
        }
        remoteCommandTokens += remoteCommandCenter.nextTrackCommand to
            remoteCommandCenter.nextTrackCommand.addTargetWithHandler {
            onNext?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }
        remoteCommandTokens += remoteCommandCenter.previousTrackCommand to
            remoteCommandCenter.previousTrackCommand.addTargetWithHandler {
            onPrevious?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }
    }
}
