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

package dev.krtirtho.spotube.modules.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerInterface
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.LoopState
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.jam.JamRole
import dev.krtirtho.spotube.core.jam.JamRoomService
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.IconButton
import dev.krtirtho.spotube.core.ui.base.Slider
import dev.krtirtho.spotube.core.ui.base.VariableIconButton
import dev.krtirtho.spotube.core.ui.base.VariableIconButtonVariant
import dev.krtirtho.spotube.core.ui.base.copyShape
import dev.krtirtho.spotube.core.ui.base.invertedButtonStyle
import dev.krtirtho.spotube.modules.downloads.DownloadProgressIcon
import dev.krtirtho.spotube.modules.downloads.DownloadsViewModel
import dev.krtirtho.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksViewModel
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxCd
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxInformation
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicFilter
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxPrevious
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeatMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeateMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeateOne
import dev.krtirtho.spotube.resources.iconsax.IconsaxShuffle
import dev.krtirtho.spotube.resources.iconsax.IconsaxVolumeCross
import dev.krtirtho.spotube.resources.iconsax.IconsaxVolumeHigh
import dev.krtirtho.spotube.resources.iconsax.IconsaxVolumeLow
import dev.krtirtho.spotube.resources.iconsax.InconsaxClock
import dev.krtirtho.spotube.resources.iconsax.SwapHorizontal2
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_go_to_album
import spotube.composeapp.generated.resources.player_sleep_timer
import spotube.composeapp.generated.resources.player_track_details
import kotlin.time.Duration.Companion.milliseconds


// The main player on large screens shown at the bottom of the screen permanently. It should be
// below the sidebar. And should be similarly give a floating vibe with a translucent background
// just like the AppBottombar.
// 
// On the left it contains the album art, song title, artists and like btn grouped together
// On the center it contains the playback controls and the progress bar
// On the right it contains the queue btn, download button btn, alternative track source button
// and three dots for options grouped together and below these buttons the volume slider will be shown
@Composable
fun AppLargePlayer(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    onQueue: () -> Unit = {},
    onAlternativeSource: () -> Unit = {},
    onMoreOptions: () -> Unit = {},
    onLyrics: () -> Unit = {},
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    downloadsViewModel: DownloadsViewModel = koinViewModel(),
    playerOptionsViewModel: PlayerOptionsViewModel = koinViewModel(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    ),
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val jamRoomService: JamRoomService = koinInject()
    val isJamGuest by jamRoomService.role
        .map { it == JamRole.Guest }
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val currentTrack = (currentEntry as? QueueEntry.StreamingTrack)?.track
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val navigationCommands: NavigationCommands = koinInject()
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(if (playerUiState.volume > 0f) playerUiState.volume else 0.6f) }
    val coverModel = playerUiState.coverUrl.takeIf { it.isNotBlank() }

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    LaunchedEffect(playerUiState.progress, isSeeking) {
        if (!isSeeking) {
            seekProgress = playerUiState.progress
        }
    }

    LaunchedEffect(playerUiState.volume) {
        if (playerUiState.volume > 0f) {
            lastNonZeroVolume = playerUiState.volume
        }
    }

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

    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Gray,
            thickness = 1.dp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hazeEffect(state = hazeState) {
                    blurEffect {
                        blurRadius = 20.dp
                        colorEffects = listOf(
                            HazeColorEffect.tint(surfaceTint)
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = playerUiState.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = playerUiState.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = playerUiState.artists,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    PlayerHeartButton(
                        audioPlayerQueue = audioPlayerQueue,
                        savedTracksViewModel = savedTracksViewModel,
                    )
                }

                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatPlayerTime(playerUiState.position),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatPlayerTime(playerUiState.displayDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariableIconButton(
                            onClick = ::onShuffleToggle,
                            enabled = !isJamGuest,
                            variant = if (playerUiState.isShuffling) VariableIconButtonVariant.Outline else VariableIconButtonVariant.Ghost
                        ) {
                            Icon(
                                Iconsax.IconsaxShuffle,
                                contentDescription = if (playerUiState.isShuffling) "Disable shuffle" else "Enable shuffle",
                                tint = if (playerUiState.isShuffling) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        GhostIconButton(onClick = ::onSkipPrevious, enabled = !isJamGuest) {
                            Icon(Iconsax.IconsaxPrevious, contentDescription = "Previous")
                        }
                        IconButton(
                            onClick = ::onPlayPause,
                            theme = invertedButtonStyle().copyShape(CircleShape),
                            modifier = Modifier
                                .size(50.dp),
                        ) {
                            Icon(
                                if (playerUiState.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                                contentDescription = if (playerUiState.isPlaying) "Pause" else "Play or pause",
                            )
                        }
                        GhostIconButton(onClick = ::onSkipNext, enabled = !isJamGuest) {
                            Icon(Iconsax.IconsaxNext, contentDescription = "Next")
                        }
                        VariableIconButton(
                            onClick = ::onLoopToggle,
                            enabled = !isJamGuest,
                            variant = if (playerUiState.loopState == LoopState.NONE) VariableIconButtonVariant.Ghost else VariableIconButtonVariant.Outline
                        ) {
                            Icon(
                                imageVector = when (playerUiState.loopState) {
                                    LoopState.NONE -> Iconsax.IconsaxRepeateMusic
                                    LoopState.ONE -> Iconsax.IconsaxRepeateOne
                                    LoopState.ALL -> Iconsax.IconsaxRepeatMusic
                                },
                                contentDescription = "Loop mode ${playerUiState.loopState.name}",
                                tint = if (playerUiState.loopState == LoopState.NONE) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhostIconButton(onClick = onQueue) {
                            Icon(Iconsax.IconsaxMusicFilter, contentDescription = "Queue")
                        }
                        GhostIconButton(onClick = {
                            val track = (currentEntry as? QueueEntry.StreamingTrack)?.track
                            if (track != null) {
                                downloadsViewModel.downloadTrack(track)
                            }
                        }) {
                            DownloadProgressIcon(
                                track = (currentEntry as? QueueEntry.StreamingTrack)?.track,
                                icon = Iconsax.IconsaxDirectboxReceive,
                                contentDescription = "Download",
                            )
                        }
                        GhostIconButton(onClick = onAlternativeSource) {
                            Icon(Iconsax.SwapHorizontal2, contentDescription = "Alternative source")
                        }
                        GhostIconButton(onClick = onLyrics) {
                            Icon(Iconsax.IconsaxMusic, contentDescription = "Lyrics")
                        }
                        Box {
                            GhostIconButton(
                                onClick = {
                                    playerOptionsViewModel.showMoreOptionsMenu()
                                    onMoreOptions()
                                }
                            ) {
                                Icon(Iconsax.Iconsax3DotsMore, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = playerOptionsUiState.isMoreOptionsMenuOpen,
                                onDismissRequest = playerOptionsViewModel::dismissMoreOptionsMenu,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.player_go_to_album)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Iconsax.IconsaxCd,
                                            contentDescription = null,
                                        )
                                    },
                                    enabled = currentTrack?.album?.id != null,
                                    onClick = {
                                        playerOptionsViewModel.dismissMoreOptionsMenu()
                                        currentTrack?.album?.id?.let { albumId ->
                                            navigationCommands.navigateTo(Routes.Album(albumId))
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.player_sleep_timer)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Iconsax.InconsaxClock,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        playerOptionsViewModel.dismissMoreOptionsMenu()
                                        playerOptionsViewModel.showSleepTimerDialog()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.player_track_details)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Iconsax.IconsaxInformation,
                                            contentDescription = null,
                                        )
                                    },
                                    enabled = currentTrack != null,
                                    onClick = {
                                        playerOptionsViewModel.dismissMoreOptionsMenu()
                                        playerOptionsViewModel.showTrackDetails()
                                    },
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhostIconButton(
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
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = if (playerUiState.volume <= 0f) "Unmute" else "Mute"
                            )
                        }
                        Slider(
                            value = playerUiState.volume,
                            onValueChange = {
                                lastNonZeroVolume = it
                                scope.launch {
                                    audioPlayer.setVolume(it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.62f)
                        )
                    }
                }
            }
        }
    }

}
