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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.Slider
import dev.krtirtho.spotube.core.ui.base.copyShape
import dev.krtirtho.spotube.core.ui.base.invertedButtonStyle
import dev.krtirtho.spotube.modules.downloads.DownloadProgressIcon
import dev.krtirtho.spotube.modules.downloads.DownloadStatus
import dev.krtirtho.spotube.modules.downloads.DownloadsViewModel
import dev.krtirtho.spotube.modules.lyrics.LyricsViewModel
import dev.krtirtho.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import dev.krtirtho.spotube.modules.saved_tracks.SavedTracksViewModel
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxArrowDown4
import dev.krtirtho.spotube.resources.iconsax.IconsaxArrowSquareUp
import dev.krtirtho.spotube.resources.iconsax.IconsaxCd
import dev.krtirtho.spotube.resources.iconsax.IconsaxCheckCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxCloseSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxDirectboxReceive
import dev.krtirtho.spotube.resources.iconsax.IconsaxMusicFilter
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxPause
import dev.krtirtho.spotube.resources.iconsax.IconsaxPlay
import dev.krtirtho.spotube.resources.iconsax.IconsaxPrevious
import dev.krtirtho.spotube.resources.iconsax.IconsaxRefreshRight
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeatMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeateMusic
import dev.krtirtho.spotube.resources.iconsax.IconsaxRepeateOne
import dev.krtirtho.spotube.resources.iconsax.IconsaxShuffle
import dev.krtirtho.spotube.resources.iconsax.InconsaxClock
import dev.krtirtho.spotube.resources.iconsax.SwapHorizontal2
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds


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
@OptIn(ExperimentalMaterial3Api::class)
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
    ),
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
    val snackbarHostState = remember { SnackbarHostState() }
    val albumArtModifier =
        rememberSharedAlbumArtModifier(sharedTransitionScope, animatedVisibilityScope)
    val coverModel = playerUiState.coverUrl.takeIf { it.isNotBlank() }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(playerUiState.progress) }
    val moreOptionsSheetState = rememberModalBottomSheetState()

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (playerOptionsUiState.isMoreOptionsSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = {
                    playerOptionsViewModel.dismissMoreOptionsSheet()
                },
                sheetState = moreOptionsSheetState
            ) {
                // Grid of Options
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        OptionTile(
                            icon = Iconsax.SwapHorizontal2,
                            label = "Alternative Source",
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                onAlternativeSource()
                            },
                        )
                    }
                    item {
                        val download = currentDownload
                        val status = download?.status
                        val statusLabel = when (status) {
                            is DownloadStatus.Completed -> "Downloaded"
                            is DownloadStatus.Failed -> "Failed"
                            is DownloadStatus.Cancelled -> "Cancelled"
                            is DownloadStatus.Queued -> "Queued"
                            is DownloadStatus.Downloading -> "Downloading..."
                            null -> "Download"
                        }
                        val statusColor = when (status) {
                            is DownloadStatus.Completed -> MaterialTheme.colorScheme.primary
                            is DownloadStatus.Failed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    playerOptionsViewModel.dismissMoreOptionsSheet()
                                    when (status) {
                                        is DownloadStatus.Completed -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Already downloaded")
                                            }
                                        }

                                        is DownloadStatus.Failed, is DownloadStatus.Cancelled -> {
                                            download?.let { downloadsViewModel.retry(it.id) }
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Retrying download...")
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
                                                        message = "Downloading ${track.title}"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    onDownloadTrack()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 1.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (status is DownloadStatus.Downloading) {
                                    DownloadProgressIcon(
                                        track = currentTrack,
                                        icon = Iconsax.IconsaxDirectboxReceive,
                                        contentDescription = "Download",
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                } else {
                                    val icon = when (status) {
                                        is DownloadStatus.Completed -> Iconsax.IconsaxCheckCircle
                                        is DownloadStatus.Failed -> Iconsax.IconsaxCloseSquare
                                        is DownloadStatus.Cancelled -> Iconsax.IconsaxRefreshRight
                                        is DownloadStatus.Queued -> Iconsax.InconsaxClock
                                        null -> Iconsax.IconsaxDirectboxReceive
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = statusLabel,
                                        modifier = Modifier.size(28.dp),
                                        tint = when (status) {
                                            is DownloadStatus.Completed -> MaterialTheme.colorScheme.primary
                                            is DownloadStatus.Failed -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = statusColor,
                                )
                            }
                        }
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.InconsaxClock,
                            label = "Sleep Timer",
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showSleepTimerDialog()
                                onSleepTimer()
                            },
                        )
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.IconsaxCd,
                            label = "Go to Album",
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                val albumId = currentTrack?.album?.id
                                if (albumId != null) {
                                    navigationCommands.navigateTo(Routes.Album(albumId))
                                }
                                onGoToAlbum()
                            },
                        )
                    }
                    item {
                        OptionTile(
                            icon = Iconsax.Iconsax3DotsMore,
                            label = "Track Details",
                            onClick = {
                                playerOptionsViewModel.dismissMoreOptionsSheet()
                                playerOptionsViewModel.showTrackDetails()
                            },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GhostIconButton(onClick = onCollapse) {
                    Icon(Iconsax.IconsaxArrowDown4, contentDescription = "Collapse player")
                }
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                GhostIconButton(onClick = playerOptionsViewModel::showMoreOptionsSheet) {
                    Icon(Iconsax.Iconsax3DotsMore, contentDescription = "Player options")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .aspectRatio(1f)
                        .then(albumArtModifier)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = playerUiState.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
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
                    savedTracksViewModel = savedTracksViewModel,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = playerUiState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = playerUiState.artists,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(
                    onClick = onQueue,
                    theme = LocalBaseUITheme.current.iconButtons.outline.copyShape(CircleShape)
                ) {
                    Icon(Iconsax.IconsaxMusicFilter, contentDescription = "Queue")
                }
            }

            Column(modifier = Modifier.padding(top = 20.dp)) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostIconButton(onClick = ::onShuffleToggle, enabled = !isJamGuest) {
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
                            .size(72.dp),
                    ) {
                        Icon(
                            if (playerUiState.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                            contentDescription = if (playerUiState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    GhostIconButton(onClick = ::onSkipNext, enabled = !isJamGuest) {
                        Icon(Iconsax.IconsaxNext, contentDescription = "Next")
                    }
                    GhostIconButton(onClick = ::onLoopToggle, enabled = !isJamGuest) {
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

            LyricsPreviewCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                audioPlayer = audioPlayer,
                onExpand = onExpandLyrics,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberSharedAlbumArtModifier(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier

    with(sharedTransitionScope) {
        return Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "player_album_art"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@Composable
private fun LyricsPreviewCard(
    modifier: Modifier = Modifier,
    audioPlayer: AudioPlayerInterface,
    onExpand: () -> Unit,
    viewModel: LyricsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncedLyrics = uiState.syncedLyrics ?: emptyList()
    val isLoading = uiState.isLoading

    val listState = rememberLazyListState()
    val positionMillis by audioPlayer.positionFlow.collectAsState(initial = 0.milliseconds)

    val currentIndex by remember {
        derivedStateOf {
            if (syncedLyrics.isEmpty()) return@derivedStateOf -1
            var idx = 0
            for (i in syncedLyrics.indices) {
                if (syncedLyrics[i].time <= positionMillis.inWholeMilliseconds) {
                    idx = i
                } else {
                    break
                }
            }
            idx
        }
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && syncedLyrics.isNotEmpty()) {
            val centerIndex = currentIndex.coerceIn(0, syncedLyrics.lastIndex)
            listState.animateScrollToItem(centerIndex, scrollOffset = -50)
        }
    }

    if (syncedLyrics.isEmpty() || isLoading) return

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GhostIconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Iconsax.IconsaxArrowSquareUp,
                        contentDescription = "Expand lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val displayLines = syncedLyrics.take(6)
                itemsIndexed(displayLines) { index, line ->
                    val isCurrent =
                        index == currentIndex.coerceIn(0, displayLines.lastIndex.coerceAtLeast(0))
                    Text(
                        text = line.text.ifBlank { "..." },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
