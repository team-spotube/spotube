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

package org.spotube.spotube.modules.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxVolumeCross
import org.spotube.spotube.resources.iconsax.IconsaxVolumeHigh
import org.spotube.spotube.resources.iconsax.IconsaxVolumeLow
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_mute_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_or_pause_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_unmute_content_description
import spotube.composeapp.generated.resources.player_volume_label
import kotlin.time.Duration.Companion.milliseconds

// Playback controls shared by the large player and the full screen player, so both drive the
// audio player the same way.

// The seek slider with the elapsed and total time below it
@Composable
internal fun PlayerSeekBar(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
    audioPlayer: AudioPlayerInterface = koinInject()
) {
    val scope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }

    LaunchedEffect(playerUiState.progress, isSeeking) {
        if (!isSeeking) {
            seekProgress = playerUiState.progress
        }
    }

    fun onSeekFinished() {
        val durationMillis = playerUiState.seekDuration.inWholeMilliseconds
        if (durationMillis <= 0L) return
        scope.launch {
            audioPlayer.seekTo(
                (durationMillis * seekProgress.coerceIn(
                    0f,
                    1f
                )).toLong().milliseconds
            )
        }
    }

    Column(modifier = modifier) {
        MillenniumSlider(
            value = seekProgress,
            onValueChange = {
                seekProgress = it
                isSeeking = true
            },
            onValueChangeFinished = {
                isSeeking = false
                onSeekFinished()
            },
            enabled = playerUiState.seekDuration.inWholeMilliseconds > 0L,
            label = stringResource(Res.string.player_seek_label),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPlayerTime(playerUiState.position),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant
            )
            Text(
                text = formatPlayerTime(playerUiState.displayDuration),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurfaceVariant
            )
        }
    }
}

// Shuffle, previous, play/pause, next and loop. Jam guests can only play/pause.
@Composable
internal fun PlayerTransportControls(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    playIconSize: Dp = Dp.Unspecified,
    audioPlayer: AudioPlayerInterface = koinInject(),
    jamRoomService: JamRoomService = koinInject()
) {
    val isJamGuest by jamRoomService.role
        .map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()

    fun onPlayPause() {
        scope.launch {
            if (playerUiState.isPlaying) {
                audioPlayer.pause()
            } else {
                audioPlayer.play()
            }
        }
    }

    fun onSkipPrevious() {
        if (isJamGuest) return
        scope.launch { audioPlayer.skipToPrevious() }
    }

    fun onSkipNext() {
        if (isJamGuest) return
        scope.launch { audioPlayer.skipToNext() }
    }

    fun onShuffleToggle() {
        if (isJamGuest) return
        scope.launch { audioPlayer.shuffle(!playerUiState.isShuffling) }
    }

    fun onLoopToggle() {
        if (isJamGuest) return
        scope.launch { audioPlayer.loop(playerUiState.loopState.next()) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
    ) {
        MillenniumIconButton(
            onClick = ::onShuffleToggle,
            enabled = !isJamGuest,
            style = if (playerUiState.isShuffling) MillenniumIconButtonStyle.Secondary else MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(
                Iconsax.IconsaxShuffle,
                contentDescription = stringResource(if (playerUiState.isShuffling) Res.string.player_disable_shuffle_content_description else Res.string.player_enable_shuffle_content_description),
                tint = if (playerUiState.isShuffling) {
                    AppTheme.colors.primary
                } else {
                    AppTheme.colors.onSurfaceVariant
                }
            )
        }
        MillenniumIconButton(
            onClick = ::onSkipPrevious,
            enabled = !isJamGuest,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(Iconsax.IconsaxPrevious, contentDescription = stringResource(Res.string.player_previous_content_description))
        }
        MillenniumIconButton(
            onClick = ::onPlayPause,
            style = MillenniumIconButtonStyle.Primary
        ) {
            MillenniumIcon(
                if (playerUiState.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                contentDescription = stringResource(if (playerUiState.isPlaying) Res.string.player_pause_content_description else Res.string.player_play_or_pause_content_description),
                modifier = if (playIconSize == Dp.Unspecified) Modifier else Modifier.size(playIconSize),
                tint = LocalTextStyle.current.color
            )
        }
        MillenniumIconButton(
            onClick = ::onSkipNext,
            enabled = !isJamGuest,
            style = MillenniumIconButtonStyle.Quiet
        ) {
            MillenniumIcon(Iconsax.IconsaxNext, contentDescription = stringResource(Res.string.player_next_content_description))
        }
        MillenniumIconButton(
            onClick = ::onLoopToggle,
            enabled = !isJamGuest,
            style = if (playerUiState.loopState == LoopState.NONE) MillenniumIconButtonStyle.Quiet else MillenniumIconButtonStyle.Secondary
        ) {
            MillenniumIcon(
                imageVector = when (playerUiState.loopState) {
                    LoopState.NONE -> Iconsax.IconsaxRepeateMusic
                    LoopState.ONE -> Iconsax.IconsaxRepeateOne
                    LoopState.ALL -> Iconsax.IconsaxRepeatMusic
                },
                contentDescription = stringResource(when (playerUiState.loopState) { LoopState.NONE -> Res.string.player_loop_mode_none; LoopState.ONE -> Res.string.player_loop_mode_one; LoopState.ALL -> Res.string.player_loop_mode_all }),
                tint = if (playerUiState.loopState == LoopState.NONE) {
                    AppTheme.colors.onSurfaceVariant
                } else {
                    AppTheme.colors.primary
                }
            )
        }
    }
}

// The mute toggle followed by the volume slider. Unmuting restores the last non-zero volume.
@Composable
internal fun PlayerVolumeControl(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
    sliderModifier: Modifier = Modifier,
    audioPlayer: AudioPlayerInterface = koinInject()
) {
    val scope = rememberCoroutineScope()
    var lastNonZeroVolume by remember { mutableFloatStateOf(if (playerUiState.volume > 0f) playerUiState.volume else 0.6f) }

    LaunchedEffect(playerUiState.volume) {
        if (playerUiState.volume > 0f) {
            lastNonZeroVolume = playerUiState.volume
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        MillenniumIconButton(
            style = MillenniumIconButtonStyle.Quiet,
            onClick = {
                scope.launch {
                    if (playerUiState.volume <= 0f) {
                        audioPlayer.setVolume(lastNonZeroVolume)
                    } else {
                        lastNonZeroVolume = playerUiState.volume
                        audioPlayer.setVolume(0f)
                    }
                }
            }
        ) {
            val volumeIcon = when {
                playerUiState.volume <= 0f -> Iconsax.IconsaxVolumeCross
                playerUiState.volume < 0.5f -> Iconsax.IconsaxVolumeLow
                else -> Iconsax.IconsaxVolumeHigh
            }
            MillenniumIcon(
                imageVector = volumeIcon,
                contentDescription = stringResource(if (playerUiState.volume <= 0f) Res.string.player_unmute_content_description else Res.string.player_mute_content_description)
            )
        }
        MillenniumSlider(
            value = playerUiState.volume,
            onValueChange = {
                lastNonZeroVolume = it
                scope.launch {
                    audioPlayer.setVolume(it)
                }
            },
            label = stringResource(Res.string.player_volume_label),
            modifier = sliderModifier
        )
    }
}
