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


import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxMoon
import org.spotube.spotube.resources.iconsax.IconsaxInformation
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.AnchorSide
import com.composeunstyled.Text
import com.composeunstyled.LocalTextStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadProgressIcon
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
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
import org.spotube.spotube.resources.iconsax.SwapHorizontal2
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSlider
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_go_to_album
import spotube.composeapp.generated.resources.player_sleep_timer
import spotube.composeapp.generated.resources.player_track_details
import kotlin.time.Duration.Companion.milliseconds
import spotube.composeapp.generated.resources.player_alternative_source_content_description
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_download
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.player_more_options_content_description
import spotube.composeapp.generated.resources.player_mute_content_description
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_or_pause_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_unmute_content_description
import spotube.composeapp.generated.resources.player_volume_label
import spotube.composeapp.generated.resources.queue_title

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
    )
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
    val coverModel = playerUiState.coverModel

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

    // A translucent tint (not near-opaque) so the blurred art behind still reads through, and
    // the material's watery caustics sit on top of it.
    val surfaceTint = AppTheme.colors.surface.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        MillenniumSeparator(modifier = Modifier.fillMaxWidth())
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
                            .background(AppTheme.colors.surfaceContainerHighest)
                    ) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = playerUiState.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = playerUiState.title,
                            style = AppTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PlayerArtistsText(
                            artists = playerUiState.artistList,
                            onArtistClick = { artistId ->
                                navigationCommands.navigateTo(Routes.Artist(artistId))
                            },
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    PlayerHeartButton(
                        audioPlayerQueue = audioPlayerQueue,
                        savedTracksViewModel = savedTracksViewModel
                    )
                }

                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
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

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MillenniumIconButton(
                            style = MillenniumIconButtonStyle.Quiet, onClick = onQueue
                        ) {
                            MillenniumIcon(Iconsax.IconsaxMusicFilter, contentDescription = stringResource(Res.string.queue_title))
                        }
                        MillenniumIconButton(
                            style = MillenniumIconButtonStyle.Quiet,
                            onClick = {
                                val track = (currentEntry as? QueueEntry.StreamingTrack)?.track
                                if (track != null) {
                                    downloadsViewModel.downloadTrack(track)
                                }
                            }) {
                            DownloadProgressIcon(
                                track = (currentEntry as? QueueEntry.StreamingTrack)?.track,
                                icon = Iconsax.IconsaxDirectboxReceive,
                                contentDescription = stringResource(Res.string.player_download)
                            )
                        }
                        MillenniumIconButton(
                            style = MillenniumIconButtonStyle.Quiet, onClick = onAlternativeSource
                        ) {
                            MillenniumIcon(
                                Iconsax.SwapHorizontal2,
                                contentDescription = stringResource(Res.string.player_alternative_source_content_description)
                            )
                        }
                        MillenniumIconButton(
                            style = MillenniumIconButtonStyle.Quiet, onClick = onLyrics
                        ) {
                            MillenniumIcon(Iconsax.IconsaxMusic, contentDescription = stringResource(Res.string.player_lyrics))
                        }
                        val goToAlbumLabel = stringResource(Res.string.player_go_to_album)
                        val sleepTimerLabel = stringResource(Res.string.player_sleep_timer)
                        val trackDetailsLabel = stringResource(Res.string.player_track_details)
                        MillenniumDropdownMenu(
                            expanded = playerOptionsUiState.isMoreOptionsMenuOpen,
                            onExpandedChange = { expanded ->
                                if (!expanded) playerOptionsViewModel.dismissMoreOptionsMenu()
                            },
                            side = AnchorSide.Top,
                            anchor = {
                                MillenniumIconButton(
                                    style = MillenniumIconButtonStyle.Quiet,
                                    onClick = {
                                        playerOptionsViewModel.showMoreOptionsMenu()
                                        onMoreOptions()
                                    }
                                ) {
                                    MillenniumIcon(
                                        Iconsax.Iconsax3DotsMore,
                                        contentDescription = stringResource(Res.string.player_more_options_content_description)
                                    )
                                }
                            }) {
                            MillenniumDropdownMenuItem(
                                label = goToAlbumLabel,
                                icon = Iconsax.IconsaxCd,
                                onClick = {
                                    playerOptionsViewModel.dismissMoreOptionsMenu()
                                    currentTrack?.album?.id?.let { albumId ->
                                        navigationCommands.navigateTo(Routes.Album(albumId))
                                    }
                                })
                            MillenniumDropdownMenuItem(
                                label = sleepTimerLabel,
                                icon = Iconsax.IconsaxMoon,
                                onClick = {
                                    playerOptionsViewModel.dismissMoreOptionsMenu()
                                    playerOptionsViewModel.showSleepTimerDialog()
                                })
                            MillenniumDropdownMenuItem(
                                label = trackDetailsLabel,
                                icon = Iconsax.IconsaxInformation,
                                onClick = {
                                    playerOptionsViewModel.dismissMoreOptionsMenu()
                                    playerOptionsViewModel.showTrackDetails()
                                })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            modifier = Modifier.fillMaxWidth(0.62f)
                        )
                    }
                }
            }
        }
    }

}
