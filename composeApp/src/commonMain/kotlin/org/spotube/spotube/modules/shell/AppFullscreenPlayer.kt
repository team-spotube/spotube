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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.WindowFullscreenButton
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.saved_tracks.SAVED_TRACKS_COLLECTION_ID
import org.spotube.spotube.modules.saved_tracks.SavedTracksViewModel
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.player_fullscreen_close_content_description
import spotube.composeapp.generated.resources.player_lyrics
import spotube.composeapp.generated.resources.queue_title

// The "now playing" view on large screens, opened from the large player. It covers the whole
// window with the album art in the middle over a blurred copy of it, like the expanded player
// does on small screens.
//
// The top bar has no title, so it stays clear of the macOS window buttons, and on the right it
// has the window full screen toggle and the button to close the view (Esc too)
// Below the album art the song title and artists are shown with the like button on the right
// Then the progress bar, and below it the queue and lyrics buttons on the left, the playback
// controls in the center and the volume slider on the right
@Composable
fun AppFullscreenPlayer(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onQueue: () -> Unit = {},
    onLyrics: () -> Unit = {},
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    savedTracksViewModel: SavedTracksViewModel = koinViewModel<SavedTracksViewModel>(
        key = SAVED_TRACKS_COLLECTION_ID,
        parameters = { parametersOf() }
    )
) {
    val playerUiState = rememberPlayerUiState(audioPlayer, audioPlayerQueue)
    val navigationCommands: NavigationCommands = koinInject()
    val focusRequester = remember { FocusRequester() }
    val background = AppTheme.colors.background

    // Takes the keyboard focus so Esc reaches this view
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(background)
            // Keeps clicks from reaching the screen underneath
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {})
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        AsyncImage(
            model = playerUiState.coverModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(96.dp, BlurredEdgeTreatment.Rectangle),
            contentScale = ContentScale.Crop
        )
        // Tones the art down towards the theme background, so the text stays readable in light
        // and dark themes alike
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(background.copy(alpha = 0.45f), background.copy(alpha = 0.9f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ApplicationMainBar(
                actions = {
                    WindowFullscreenButton()
                    MillenniumIconButton(
                        onClick = onClose,
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        MillenniumIcon(
                            Iconsax.IconsaxArrowDown4,
                            contentDescription = stringResource(Res.string.player_fullscreen_close_content_description)
                        )
                    }
                },
                backButton = false,
                transparent = true
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = playerUiState.coverModel,
                        contentDescription = playerUiState.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playerUiState.title,
                            style = AppTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PlayerArtistsText(
                            artists = playerUiState.artistList,
                            onArtistClick = { artistId ->
                                navigationCommands.navigateTo(Routes.Artist(artistId))
                                // The page opens beneath this view, so get out of the way
                                onClose()
                            },
                            style = AppTheme.typography.bodyLarge,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    PlayerHeartButton(
                        audioPlayerQueue = audioPlayerQueue,
                        savedTracksViewModel = savedTracksViewModel
                    )
                }

                PlayerSeekBar(
                    playerUiState = playerUiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    audioPlayer = audioPlayer
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MillenniumIconButton(
                            onClick = onQueue,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(Iconsax.IconsaxMusicFilter, contentDescription = stringResource(Res.string.queue_title))
                        }
                        MillenniumIconButton(
                            onClick = onLyrics,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(Iconsax.IconsaxMusic, contentDescription = stringResource(Res.string.player_lyrics))
                        }
                    }
                    PlayerTransportControls(
                        playerUiState = playerUiState,
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        playIconSize = 30.dp,
                        audioPlayer = audioPlayer
                    )
                    PlayerVolumeControl(
                        playerUiState = playerUiState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        sliderModifier = Modifier.width(120.dp),
                        audioPlayer = audioPlayer
                    )
                }
            }
        }
    }
}
