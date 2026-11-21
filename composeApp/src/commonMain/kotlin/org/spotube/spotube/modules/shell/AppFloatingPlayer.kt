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


import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumProgress
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape

import org.spotube.spotube.core.ui.theming.engine.AppComponents
import org.spotube.spotube.core.ui.theming.engine.AppSlots
import org.spotube.spotube.core.ui.theming.engine.AppState
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxPause
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_play_content_description
import spotube.composeapp.generated.resources.player_pause_content_description
import spotube.composeapp.generated.resources.player_nothing_playing
import spotube.composeapp.generated.resources.player_next_content_description
import spotube.composeapp.generated.resources.player_previous_content_description
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface

internal val FloatingPlayerContentHeight = 64.dp
internal val FloatingPlayerBottomBleed = 16.dp

// The floating player on small screens that appears above the floating AppBottombar
//
// on left the album art, song title, artists grouped together
// on right the playback controls
// top border is actually the progress bar
// users can skip to next or previous track by swiping left or right on the player
//
// the player can be expanded to the full screen by swiping up on the player, and can be collapsed
// back to the floating player by swiping down on the player
@Composable
fun AppFloatingPlayer(
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val scope = rememberCoroutineScope()
    val albumArtModifier =
        rememberSharedAlbumArtModifier(sharedTransitionScope, animatedVisibilityScope)

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
        scope.launch { audioPlayer.skipToPrevious() }
    }

    fun onSkipNext() {
        scope.launch { audioPlayer.skipToNext() }
    }

    FloatingPlayerContent(
        playerUiState = playerUiState,
        modifier = modifier,
        albumArtModifier = albumArtModifier,
        onPlayPause = ::onPlayPause,
        onSkipPrevious = ::onSkipPrevious,
        onSkipNext = ::onSkipNext,
        heartButton = {
            PlayerHeartButton(
                audioPlayerQueue = audioPlayerQueue,
                savedTracksViewModel = savedTracksViewModel
            )
        }
    )
}

@Composable
internal fun FloatingPlayerContent(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
    albumArtModifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    heartButton: @Composable () -> Unit
) {
    val coverModel = playerUiState.coverModel
    val root = AppComponents.slot(AppSlots.FloatingPlayerRoot)[AppState.Default]
    val art = AppComponents.slot(AppSlots.FloatingPlayerArtwork)[AppState.Default]
    val info = AppComponents.slot(AppSlots.FloatingPlayerInfo)[AppState.Default]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingPlayerContentHeight + FloatingPlayerBottomBleed)
            .padding(horizontal = root.horizontalMargin)
            .millenniumSurface(
                background = root.background,
                shape = root.shape,
                borderColor = root.borderColor,
                borderWidth = root.borderWidth,
                shadowElevation = root.shadowElevation,
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Only the surface extends beneath the navigation bar, not the controls.
                .padding(bottom = FloatingPlayerBottomBleed)
        ) {
            MillenniumProgress(
                progress = playerUiState.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(root.progressHeight)
            )
            Spacer(modifier = Modifier.height(root.progressSpacing))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(root.contentPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(art.size)
                            .then(albumArtModifier)
                            .clip(art.shape)
                            .background(art.background),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverModel != null) AsyncImage(
                            model = coverModel,
                            contentDescription = playerUiState.title,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        else AppIcon(
                            Iconsax.IconsaxMusic, contentDescription = null,
                            tint = art.placeholderTint, modifier = Modifier.size(art.placeholderSize)
                        )
                    }
                    Column(modifier = Modifier.padding(start = art.spacing)) {
                        Text(
                            text = if (playerUiState.queue.isEmpty()) stringResource(Res.string.player_nothing_playing) else playerUiState.title,
                            style = info.titleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playerUiState.queue.isNotEmpty()) Text(
                            text = playerUiState.artists,
                            style = info.subtitleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(root.controlsSpacing)
                ) {
                    heartButton()
                    MillenniumIconButton(
                        onClick = onSkipPrevious,
                        enabled = playerUiState.queue.isNotEmpty(),
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        AppIcon(
                            Iconsax.IconsaxPrevious,
                            contentDescription = stringResource(Res.string.player_previous_content_description)
                        )
                    }
                    MillenniumIconButton(
                        onClick = onPlayPause,
                        enabled = playerUiState.queue.isNotEmpty(),
                        style = MillenniumIconButtonStyle.Secondary
                    ) {
                        AppIcon(
                            if (playerUiState.isPlaying) Iconsax.IconsaxPause else Iconsax.IconsaxPlay,
                            contentDescription = stringResource(if (playerUiState.isPlaying) Res.string.player_pause_content_description else Res.string.player_play_content_description)
                        )
                    }
                    MillenniumIconButton(
                        onClick = onSkipNext,
                        enabled = playerUiState.queue.isNotEmpty(),
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        AppIcon(
                            Iconsax.IconsaxNext,
                            contentDescription = stringResource(Res.string.player_next_content_description)
                        )
                    }
                }
            }
        }
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
