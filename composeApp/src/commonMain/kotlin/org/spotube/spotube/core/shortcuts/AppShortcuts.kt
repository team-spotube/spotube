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

package org.spotube.spotube.core.shortcuts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import kotlin.time.Duration

/** Everything a keyboard shortcut can trigger. */
enum class ShortcutAction {
    PlayPause,
    Next,
    Previous,
    SeekForward,
    SeekBackward,
    VolumeUp,
    VolumeDown,
    ToggleShuffle,
    ToggleLoop,
    OpenSettings,
    OpenSearch,
    OpenHome,
    OpenLibrary,
    OpenLyrics,
    Back
}

/**
 * Maps a pressed key combination to a [ShortcutAction]. Pure, so it is unit-testable.
 * Ctrl is treated the same as Cmd (Meta) so the macOS bindings follow platform convention.
 */
fun resolveShortcut(
    key: Key,
    ctrl: Boolean,
    shift: Boolean,
    alt: Boolean): ShortcutAction? = when {
    !ctrl && !shift && !alt -> when (key) {
        Key.Spacebar -> ShortcutAction.PlayPause
        Key.DirectionRight -> ShortcutAction.SeekForward
        Key.DirectionLeft -> ShortcutAction.SeekBackward
        Key.DirectionUp -> ShortcutAction.VolumeUp
        Key.DirectionDown -> ShortcutAction.VolumeDown
        else -> null
    }
    alt && !ctrl && !shift && key == Key.DirectionLeft -> ShortcutAction.Back
    ctrl && !alt && !shift -> when (key) {
        Key.Comma -> ShortcutAction.OpenSettings
        Key.DirectionRight -> ShortcutAction.Next
        Key.DirectionLeft -> ShortcutAction.Previous
        Key.DirectionUp -> ShortcutAction.VolumeUp
        Key.DirectionDown -> ShortcutAction.VolumeDown
        Key.S -> ShortcutAction.ToggleShuffle
        Key.R -> ShortcutAction.ToggleLoop
        Key.K, Key.F -> ShortcutAction.OpenSearch
        Key.H -> ShortcutAction.OpenHome
        Key.L -> ShortcutAction.OpenLibrary
        else -> null
    }
    ctrl && shift && !alt && key == Key.L -> ShortcutAction.OpenLyrics
    else -> null
}

/**
 * Executes in-app keyboard shortcuts. Fed from the shell's key-event hook; it owns the
 * logic so no composable has to decide what a key does.
 */
class AppShortcuts(
    private val audioPlayer: AudioPlayerInterface,
    private val jamRoomService: JamRoomService,
    private val navigationCommands: NavigationCommands) {
    private val logger = Logger.withTag("AppShortcuts")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Returns true when [event] was a shortcut and has been handled. Unmodified keys are
     * ignored while [textInputActive], so typing and moving the caret keep working.
     */
    fun onKeyEvent(event: KeyEvent, textInputActive: Boolean = false): Boolean {
        if (event.isTypedSpace()) {
            if (textInputActive) return false
            logger.d { "Shortcut ${ShortcutAction.PlayPause}" }
            perform(ShortcutAction.PlayPause)
            return true
        }
        if (event.type != KeyEventType.KeyDown) return false
        val ctrl = event.isCtrlPressed || event.isMetaPressed
        if (textInputActive && !ctrl && !event.isAltPressed) return false
        val action = resolveShortcut(
            key = event.key,
            ctrl = ctrl,
            shift = event.isShiftPressed,
            alt = event.isAltPressed) ?: return false
        logger.d { "Shortcut $action" }
        perform(action)
        return true
    }

    fun perform(action: ShortcutAction) {
        val isJamGuest = jamRoomService.role.value == JamRole.Guest
        when (action) {
            ShortcutAction.PlayPause -> scope.launch {
                if (audioPlayer.playerStateFlow.value == PlayerState.PLAYING) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.play()
                }
            }
            ShortcutAction.Next -> if (!isJamGuest) scope.launch { audioPlayer.skipToNext() }
            ShortcutAction.Previous -> if (!isJamGuest) scope.launch { audioPlayer.skipToPrevious() }
            ShortcutAction.SeekForward -> if (!isJamGuest) seekBy(SEEK_FRACTION)
            ShortcutAction.SeekBackward -> if (!isJamGuest) seekBy(-SEEK_FRACTION)
            ShortcutAction.VolumeUp -> scope.launch {
                audioPlayer.setVolume((audioPlayer.volumeFlow.value + VOLUME_STEP).coerceIn(0f, 1f))
            }
            ShortcutAction.VolumeDown -> scope.launch {
                audioPlayer.setVolume((audioPlayer.volumeFlow.value - VOLUME_STEP).coerceIn(0f, 1f))
            }
            ShortcutAction.ToggleShuffle -> if (!isJamGuest) scope.launch {
                audioPlayer.shuffle(!audioPlayer.shuffleModeFlow.value)
            }
            ShortcutAction.ToggleLoop -> if (!isJamGuest) scope.launch {
                audioPlayer.loop(audioPlayer.loopStateFlow.value.next())
            }
            ShortcutAction.OpenSettings -> navigationCommands.navigateTo(Routes.Settings)
            ShortcutAction.OpenSearch -> navigationCommands.navigateTo(Routes.Search)
            ShortcutAction.OpenHome -> navigationCommands.navigateTo(Routes.Home)
            ShortcutAction.OpenLibrary -> navigationCommands.navigateTo(Routes.Library)
            ShortcutAction.OpenLyrics -> navigationCommands.navigateTo(Routes.Lyrics)
            ShortcutAction.Back -> navigationCommands.pop()
        }
    }

    private fun seekBy(fraction: Float) {
        val duration = audioPlayer.durationFlow.value
        if (duration <= Duration.ZERO) return
        scope.launch {
            val target = audioPlayer.positionFlow.value + duration * fraction.toDouble()
            audioPlayer.seekTo(target.coerceIn(Duration.ZERO, duration))
        }
    }

    private companion object {
        /** Share of the track a plain left/right press moves by. */
        const val SEEK_FRACTION = 0.05f
        const val VOLUME_STEP = 0.05f
    }
}

/**
 * On the desktop (Tao) window layer, text-producing keys such as Space arrive only as a
 * synthetic "typed character" event ([KeyEventType.Unknown] with the code point), never as
 * a [Key.Spacebar] key-down. Other platforms deliver a normal key-down.
 */
fun KeyEvent.isTypedSpace(): Boolean =
    type == KeyEventType.Unknown && utf16CodePoint == ' '.code &&
        !isCtrlPressed && !isMetaPressed && !isAltPressed

fun KeyEvent.isPlainSpaceKey(): Boolean =
    key == Key.Spacebar && !isCtrlPressed && !isMetaPressed && !isAltPressed && !isShiftPressed
