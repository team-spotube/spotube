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

package org.spotube.spotube.core.systemtray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import dev.nucleusframework.application.NucleusApplicationScope
import dev.nucleusframework.composenativetray.tray.api.Tray
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.tray_exit
import spotube.composeapp.generated.resources.tray_hide_window
import spotube.composeapp.generated.resources.tray_like_track
import spotube.composeapp.generated.resources.tray_loop
import spotube.composeapp.generated.resources.tray_loop_all
import spotube.composeapp.generated.resources.tray_loop_off
import spotube.composeapp.generated.resources.tray_loop_one
import spotube.composeapp.generated.resources.tray_mute
import spotube.composeapp.generated.resources.tray_next_track
import spotube.composeapp.generated.resources.tray_previous_track
import spotube.composeapp.generated.resources.tray_show_window
import spotube.composeapp.generated.resources.tray_shuffle
import spotube.composeapp.generated.resources.tray_unlike_track
import spotube.composeapp.generated.resources.tray_unmute
import spotube.composeapp.generated.resources.tray_volume
import spotube.composeapp.generated.resources.tray_volume_down
import spotube.composeapp.generated.resources.tray_volume_up

/**
 * Renders the system tray icon + menu via Compose Native Tray (no AWT/Swing).
 *
 * Must be composed inside `nucleusApplication { }` and kept in composition
 * while the tray should be visible (the menu is fully reactive via
 * [SystemTrayService.state]).
 */
@Composable
fun NucleusApplicationScope.SystemTray(
    isWindowVisible: Boolean,
    onToggleWindowVisibility: () -> Unit,
    onExit: () -> Unit) {
    val trayService = koinInject<SystemTrayService>()
    val state by trayService.state.collectAsState()

    // Tray menu builders aren't composable, so resolve every label up front.
    val hideWindowLabel = stringResource(Res.string.tray_hide_window)
    val showWindowLabel = stringResource(Res.string.tray_show_window)
    val playLabel = stringResource(Res.string.player_play_content_description)
    val pauseLabel = stringResource(Res.string.player_pause_content_description)
    val nextTrackLabel = stringResource(Res.string.tray_next_track)
    val previousTrackLabel = stringResource(Res.string.tray_previous_track)
    val shuffleLabel = stringResource(Res.string.tray_shuffle)
    val loopLabel = stringResource(Res.string.tray_loop)
    val loopOffLabel = stringResource(Res.string.tray_loop_off)
    val loopOneLabel = stringResource(Res.string.tray_loop_one)
    val loopAllLabel = stringResource(Res.string.tray_loop_all)
    val volumeLabel = stringResource(Res.string.tray_volume, (state.volume * 100).toInt())
    val volumeUpLabel = stringResource(Res.string.tray_volume_up)
    val volumeDownLabel = stringResource(Res.string.tray_volume_down)
    val muteLabel = stringResource(Res.string.tray_mute)
    val unmuteLabel = stringResource(Res.string.tray_unmute)
    val likeTrackLabel = stringResource(Res.string.tray_like_track)
    val unlikeTrackLabel = stringResource(Res.string.tray_unlike_track)
    val exitLabel = stringResource(Res.string.tray_exit)

    Tray(
        icon = Iconsax.IconsaxMusic,
        tooltip = "Spotube",
        primaryAction = onToggleWindowVisibility) {
        Item(label = if (isWindowVisible) hideWindowLabel else showWindowLabel) {
            onToggleWindowVisibility()
        }
        Divider()

        Item(label = if (state.isPlaying) pauseLabel else playLabel) {
            trayService.togglePlayPause()
        }
        Item(label = nextTrackLabel) { trayService.skipToNext() }
        Item(label = previousTrackLabel) { trayService.skipToPrevious() }
        Divider()

        CheckableItem(
            label = shuffleLabel,
            checked = state.shuffleEnabled,
            onCheckedChange = trayService::setShuffle)
        SubMenu(label = loopLabel) {
            Item(label = loopOffLabel) { trayService.setLoop(LoopState.NONE) }
            Item(label = loopOneLabel) { trayService.setLoop(LoopState.ONE) }
            Item(label = loopAllLabel) { trayService.setLoop(LoopState.ALL) }
        }
        Divider()

        Item(label = volumeLabel)
        Item(label = volumeUpLabel) { trayService.increaseVolume() }
        Item(label = volumeDownLabel) { trayService.decreaseVolume() }
        Item(label = if (state.volume <= 0f) unmuteLabel else muteLabel) { trayService.toggleMute() }
        Divider()

        if (state.currentTrackId != null) {
            Item(label = if (state.isCurrentTrackLiked) unlikeTrackLabel else likeTrackLabel) {
                trayService.toggleLike()
            }
        }
        Divider()

        Item(label = exitLabel) { onExit() }
    }
}
