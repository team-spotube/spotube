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


import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.millennium.composables.MillenniumSnackbarHost
import org.spotube.millennium.composables.MillenniumSnackbarHostState
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumModalBottomSheet
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import com.composeunstyled.SheetDetent
import com.composeunstyled.rememberModalBottomSheetState

import com.composeunstyled.Text
import com.composeunstyled.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.jam.JamRole
import org.spotube.spotube.core.jam.JamRoomService
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.downloads.DownloadProgressIcon
import org.spotube.spotube.modules.downloads.DownloadStatus
import org.spotube.spotube.modules.downloads.DownloadsViewModel
import org.spotube.spotube.modules.lyrics.LyricsViewModel
import org.spotube.spotube.modules.lyrics.SyncedLyricsList
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxArrowSquareUp
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateOne
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.InconsaxClock
import org.spotube.spotube.resources.iconsax.SwapHorizontal2
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds
import org.spotube.spotube.core.i18n.getString
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.downloads_status_cancelled
import spotube.composeapp.generated.resources.downloads_status_failed
import spotube.composeapp.generated.resources.downloads_status_queued
import spotube.composeapp.generated.resources.player_alternative_source
import spotube.composeapp.generated.resources.player_collapse_content_description
import spotube.composeapp.generated.resources.player_disable_shuffle_content_description
import spotube.composeapp.generated.resources.player_download
import spotube.composeapp.generated.resources.player_download_already_downloaded
import spotube.composeapp.generated.resources.player_download_retrying
import spotube.composeapp.generated.resources.player_download_started
import spotube.composeapp.generated.resources.player_download_status_downloaded
import spotube.composeapp.generated.resources.player_download_status_downloading
import spotube.composeapp.generated.resources.player_enable_shuffle_content_description
import spotube.composeapp.generated.resources.player_expand_lyrics_content_description
import spotube.composeapp.generated.resources.player_go_to_album
import spotube.composeapp.generated.resources.player_loop_mode_all
import spotube.composeapp.generated.resources.player_loop_mode_none
import spotube.composeapp.generated.resources.player_loop_mode_one
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_now_playing
import spotube.composeapp.generated.resources.player_options_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import spotube.composeapp.generated.resources.player_seek_label
import spotube.composeapp.generated.resources.player_sleep_timer
import spotube.composeapp.generated.resources.player_track_details
import spotube.composeapp.generated.resources.queue_title

// The expanded player on small screens
// on the top left it has a angle/chevron down that can be used to collapse the player back to the
// floating player
// on the top right it has a three dots for options
// The top bar can be used to swipe down and close the player back to the floating player. It should
// follow the user's finger while swiping down and should have a nice animation when collapsing back
// to the floating player
// On the middle the album art stays rounded, centered and takes up most of the space
// Below the album art, on the left the song title and artists are shown and on the right the like
// button is shown
// below that we have the playback controls and the progress bar, which should be similar to the
// large player but with a different layout to fit the smaller screen
// And at the very end we will have the volume slider
@Composable
fun AppExpandedPlayer(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onCollapse: () -> Unit = {},
    onQueue: () -> Unit = {},
    onAlternativeSource: () -> Unit = {},
    onExpandLyrics: () -> Unit = {},
    onDownloadTrack: () -> Unit = {},
    onGoToAlbum: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
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
    val downloadsViewModel: DownloadsViewModel = koinViewModel()
    val navigationCommands: NavigationCommands = koinInject()
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerOptionsUiState by playerOptionsViewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack = remember(currentEntry) {
        (currentEntry as? QueueEntry.StreamingTrack)?.track
    }
    val downloads by downloadsViewModel.downloads.collectAsStateWithLifecycle()
    val currentDownload = remember(currentTrack, downloads) {
        currentTrack?.let { t ->
            downloads.lastOrNull { it.track?.id == t.id }
        }
    }
    val snackbarHostState = remember { MillenniumSnackbarHostState() }
    val albumArtModifier =
        rememberSharedAlbumArtModifier(sharedTransitionScope, animatedVisibilityScope)
    val coverModel = playerUiState.coverModel
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }
    val moreOptionsSheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded
    )

    PlayerOptionDialogs(viewModel = playerOptionsViewModel)

    LaunchedEffect(playerUiState.progress, isSeeking) {
        if (!isSeeking) {
            seekProgress = playerUiState.progress
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

    LaunchedEffect(moreOptionsSheetState.bottomSheetState.currentDetent) {
        if (moreOptionsSheetState.bottomSheetState.currentDetent == SheetDetent.Hidden) {
            playerOptionsViewModel.dismissMoreOptionsSheet()
        }
    }

    LaunchedEffect(playerOptionsUiState.isMoreOptionsSheetOpen) {
        if (playerOptionsUiState.isMoreOptionsSheetOpen) {
            moreOptionsSheetState.animateTo(SheetDetent.FullyExpanded)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (playerOptionsUiState.isMoreOptionsSheetOpen) {
            MillenniumModalBottomSheet(
                state = moreOptionsSheetState,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                contentSpacing = 4.dp
            ) {
                // Grid of Options
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OptionTile(
                            icon = Iconsax.SwapHorizontal2,
                            label = stringResource(Res.string.player_alternative_source),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                onAlternativeSource()
                            })
                    }
                    item {
                        val download = currentDownload
                        val status = download?.status
                        val statusLabel = when (status) {
                            is DownloadStatus.Completed -> stringResource(Res.string.player_download_status_downloaded)
                            is DownloadStatus.Failed -> stringResource(Res.string.downloads_status_failed)
                            is DownloadStatus.Cancelled -> stringResource(Res.string.downloads_status_cancelled)
                            is DownloadStatus.Queued -> stringResource(Res.string.downloads_status_queued)
                            is DownloadStatus.Downloading -> stringResource(Res.string.player_download_status_downloading)
                            null -> stringResource(Res.string.player_download)
                        }
                        val statusColor = when (status) {
                            is DownloadStatus.Completed -> AppTheme.colors.primary
                            is DownloadStatus.Failed -> AppTheme.colors.error
                            else -> AppTheme.colors.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .millenniumSurface(
                                    background = millenniumPanelBrush(MillenniumTheme.colors),
                                    shape = RoundedCornerShape(14.dp),
                                    borderColor = MillenniumTheme.colors.border,
                                    borderWidth = 1.dp,
                                    shadowElevation = 2.dp
                                )
                                .clickable {
                                    playerOptionsViewModel.dismissMoreOptionsSheet()
                                    when (status) {
                                        is DownloadStatus.Completed -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(getString(Res.string.player_download_already_downloaded))
                                            }
                                        }

                                        is DownloadStatus.Failed, is DownloadStatus.Cancelled -> {
                                            download?.let { downloadsViewModel.retry(it.id) }
                                            scope.launch {
                                                snackbarHostState.showSnackbar(getString(Res.string.player_download_retrying))
                                            }
                                        }

                                        is DownloadStatus.Downloading, is DownloadStatus.Queued -> {
                                            // already in progress
                                        }

                                        null -> {
                                            val track = currentTrack
                                            if (track != null) {
                                                downloadsViewModel.downloadTrack(track)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = getString(Res.string.player_download_started, track.title)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    onDownloadTrack()
                                }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (status is DownloadStatus.Downloading) {
                                    DownloadProgressIcon(
                                        track = currentTrack,
                                        icon = Iconsax.IconsaxDirectboxReceive,
                                        contentDescription = stringResource(Res.string.player_download),
                                        modifier = Modifier.size(28.dp),
                                        tint = AppTheme.colors.onSurface
                                    )
                                } else {
                                    val icon = when (status) {
                                        is DownloadStatus.Completed -> Iconsax.IconsaxCheckCircle
                                        is DownloadStatus.Failed -> Iconsax.IconsaxCloseSquare
                                        is DownloadStatus.Cancelled -> Iconsax.IconsaxRefreshRight
                                        is DownloadStatus.Queued -> Iconsax.InconsaxClock
                                        null -> Iconsax.IconsaxDirectboxReceive
                                    }
                                    MillenniumIcon(
                                        imageVector = icon,
                                        contentDescription = statusLabel,
                                        modifier = Modifier.size(28.dp),
                                        tint = when (status) {
                                            is DownloadStatus.Completed -> AppTheme.colors.primary
                                            is DownloadStatus.Failed -> AppTheme.colors.error
                                            else -> AppTheme.colors.onSurface
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = statusLabel,
                                    style = AppTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = statusColor
                                )
                            }
                        }
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.InconsaxClock,
                            label = stringResource(Res.string.player_sleep_timer),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showSleepTimerDialog()
                                onSleepTimer()
                            })
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.IconsaxCd,
                            label = stringResource(Res.string.player_go_to_album),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                val albumId = currentTrack?.album?.id
                                if (albumId != null) {
                                    navigationCommands.navigateTo(Routes.Album(albumId))
                                    onCollapse()
                                }
                                onGoToAlbum()
                            })
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.Iconsax3DotsMore,
                            label = stringResource(Res.string.player_track_details),
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showTrackDetails()
                            })
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MillenniumIconButton(
                    onClick = onCollapse,
                    style = MillenniumIconButtonStyle.Quiet
                ) {
                    MillenniumIcon(
                        Iconsax.IconsaxArrowDown4,
                        contentDescription = stringResource(Res.string.player_collapse_content_description)
                    )
                }
                Text(
                    text = stringResource(Res.string.player_now_playing),
                    style = AppTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                MillenniumIconButton(
                    onClick = playerOptionsViewModel::showMoreOptionsSheet,
                    style = MillenniumIconButtonStyle.Quiet
                ) {
                    MillenniumIcon(Iconsax.Iconsax3DotsMore, contentDescription = stringResource(Res.string.player_options_content_description))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .aspectRatio(1f)
                        .then(albumArtModifier)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AppTheme.colors.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = playerUiState.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerHeartButton(
                    audioPlayerQueue = audioPlayerQueue,
                    savedTracksViewModel = savedTracksViewModel
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playerUiState.title,
                        style = AppTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    PlayerArtistsText(
                        artists = playerUiState.artistList,
                        onArtistClick = { artistId ->
                            navigationCommands.navigateTo(Routes.Artist(artistId))
                            // The page opens beneath the player, so get out of the way
                            onCollapse()
                        },
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                MillenniumIconButton(
                    onClick = onQueue,
                    style = MillenniumIconButtonStyle.Secondary
                ) {
                    MillenniumIcon(Iconsax.IconsaxMusicFilter, contentDescription = stringResource(Res.string.queue_title))
                }
            }

            Column(modifier = Modifier.padding(top = 20.dp)) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MillenniumIconButton(
                        onClick = ::onShuffleToggle,
                        enabled = !isJamGuest,
                        style = MillenniumIconButtonStyle.Quiet
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
                            contentDescription = stringResource(if (playerUiState.isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description),
                            modifier = Modifier.size(30.dp),
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
                        style = MillenniumIconButtonStyle.Quiet
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

            LyricsPreviewCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                onExpand = onExpandLyrics
            )
        }

        MillenniumSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberSharedAlbumArtModifier(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier

    with(sharedTransitionScope) {
        return Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "player_album_art"),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@Composable
private fun LyricsPreviewCard(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncedLyrics = uiState.syncedLyrics ?: emptyList()
    val isLoading = uiState.isLoading
    if (syncedLyrics.isEmpty() || isLoading) return

    MillenniumCard(
        modifier = modifier.clickable(onClick = onExpand),
        contentPadding = PaddingValues(12.dp),
        contentSpacing = 8.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.player_lyrics),
                style = AppTheme.typography.labelMedium,
                color = MillenniumTheme.colors.muted
            )
            MillenniumIconButton(
                onClick = onExpand,
                style = MillenniumIconButtonStyle.Quiet
            ) {
                MillenniumIcon(
                    Iconsax.IconsaxArrowSquareUp,
                    contentDescription = stringResource(Res.string.player_expand_lyrics_content_description),
                    tint = MillenniumTheme.colors.muted
                )
            }
        }

        // Passive preview: no onLineClick, so lines never seek playback.
        SyncedLyricsList(
            lines = syncedLyrics,
            currentIndex = uiState.currentIndex,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

@Composable
private fun OptionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MillenniumCard(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(12.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MillenniumIcon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = AppTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}
