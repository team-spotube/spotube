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

package org.spotube.spotube.core.ui.theming.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.PlayerState

/**
 * Live player facts a theme can branch on, as `ctx.player.*`.
 *
 * Deliberately coarse: only values that change on user actions, never the playback position, so a
 * theme cannot make the whole UI recompose every tick.
 */
fun playerThemeContext(
    state: PlayerState,
    hasTrack: Boolean,
    shuffle: Boolean,
    loop: LoopState,
): Map<String, Any?> = mapOf(
    "player" to mapOf(
        "isPlaying" to (state == PlayerState.PLAYING),
        "isPaused" to (state == PlayerState.PAUSED),
        "isBuffering" to (state == PlayerState.BUFFERING),
        "hasTrack" to hasTrack,
        "shuffle" to shuffle,
        "loop" to loop.name.lowercase(),
    ),
)

/** Publishes [playerThemeContext] for `SpotubeTheme`. */
class ThemeContextService(audioPlayer: AudioPlayerInterface) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val context: StateFlow<Map<String, Any?>> = combine(
        audioPlayer.playerStateFlow,
        audioPlayer.currentMediaItemFlow,
        audioPlayer.shuffleModeFlow,
        audioPlayer.loopStateFlow,
    ) { state, item, shuffle, loop -> playerThemeContext(state, item != null, shuffle, loop) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())
}
