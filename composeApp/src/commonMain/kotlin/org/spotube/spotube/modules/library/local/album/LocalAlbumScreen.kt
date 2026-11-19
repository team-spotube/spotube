/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local.album

import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.component.cards.PlayableListTile
import org.spotube.spotube.core.ui.misc.TextWithShimmer
import org.spotube.spotube.core.ui.misc.shimmerApply
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxPauseCircle
import org.spotube.spotube.resources.iconsax.IconsaxPlayCircle2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import spotube.composeapp.generated.resources.*

/**
 * The detail screen of one synthetic local album.
 *
 * @param albumName the album's normalized key, as carried by [org.spotube.spotube.core.navigation.Routes.LocalAlbum].
 * @param viewModel owns the album lookup and playback; the screen only renders its state.
 */
@Composable
fun LocalAlbumScreen(
    albumName: String,
    viewModel: LocalAlbumViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bottomInset = LocalAppShellBottomInset.current

    Column(Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = true,
            title = {
                Text(
                    when (val current = state) {
                        is LocalAlbumUiState.Loaded -> current.album.name
                        is LocalAlbumUiState.NotFound -> ""
                        LocalAlbumUiState.Loading -> stringResource(Res.string.album_loading_title)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                val loaded = state as? LocalAlbumUiState.Loaded
                if (loaded != null) {
                    MillenniumIconButton(
                        onClick = viewModel::play,
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        AppIcon(
                            imageVector = if (loaded.isPlaying) Iconsax.IconsaxPauseCircle
                            else Iconsax.IconsaxPlayCircle2,
                            contentDescription = stringResource(
                                if (loaded.isPlaying) Res.string.player_pause_content_description
                                else Res.string.player_play_content_description
                            )
                        )
                    }
                    MillenniumIconButton(
                        onClick = viewModel::addToQueue,
                        style = MillenniumIconButtonStyle.Quiet
                    ) {
                        AppIcon(
                            imageVector = Iconsax.IconsaxAddSquare,
                            contentDescription = stringResource(
                                Res.string.player_add_to_queue_content_description
                            )
                        )
                    }
                }
            }
        )

        Box(Modifier.weight(1f).fillMaxSize()) {
            when (val current = state) {
                LocalAlbumUiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(bottom = bottomInset),
                    contentAlignment = Alignment.Center
                ) {
                    MillenniumCircularProgress(size = 40.dp)
                }

                LocalAlbumUiState.NotFound -> Box(
                    Modifier.fillMaxSize().padding(bottom = bottomInset),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(Res.string.local_album_not_found),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }

                is LocalAlbumUiState.Loaded -> LocalAlbumContent(
                    state = current,
                    onPlay = viewModel::play,
                    onShufflePlay = viewModel::shufflePlay,
                    onPlayTrack = viewModel::playFromTrack,
                    bottomInset = bottomInset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LocalAlbumContent(
    state: LocalAlbumUiState.Loaded,
    onPlay: () -> Unit,
    onShufflePlay: () -> Unit,
    onPlayTrack: (String) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier
) {
    val album = state.album
    val trackCount = album.trackCount

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "local_album_header") {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LocalAlbumArtwork(album.name, album.coverBytes)
                TextWithShimmer(
                    text = album.name,
                    style = AppTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextWithShimmer(
                    text = stringResource(
                        Res.string.local_album_subtitle,
                        state.artistsLabel,
                        pluralStringResource(
                            Res.plurals.library_local_tracks_count,
                            trackCount,
                            trackCount
                        )
                    ),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MillenniumButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f)
                    ) {
                        AppIcon(
                            imageVector = if (state.isPlaying) Iconsax.IconsaxPauseCircle
                            else Iconsax.IconsaxPlayCircle2,
                            contentDescription = stringResource(
                                if (state.isPlaying) Res.string.player_pause_content_description
                                else Res.string.player_play_content_description
                            )
                        )
                        TextWithShimmer(
                            text = stringResource(
                                if (state.isPlaying) Res.string.player_pause_content_description
                                else Res.string.player_play_content_description
                            )
                        )
                    }
                    MillenniumButton(
                        onClick = onShufflePlay,
                        style = MillenniumButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        AppIcon(
                            imageVector = Iconsax.IconsaxShuffle,
                            contentDescription = stringResource(Res.string.collection_details_shuffle_play)
                        )
                        TextWithShimmer(text = stringResource(Res.string.collection_details_shuffle))
                    }
                }
            }
        }
        items(state.tracks, key = { it.path }) { track ->
            PlayableListTile(
                track.title,
                track.subtitle ?: stringResource(Res.string.media_unknown_artist),
                imageModel = track.artwork,
                onClick = { onPlayTrack(track.path) },
                onPlay = { onPlayTrack(track.path) },
                compact = true,
                trailingContent = {
                    track.durationLabel?.let {
                        Text(
                            it,
                            style = AppTheme.typography.labelSmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LocalAlbumArtwork(name: String, coverBytes: ByteArray?) {
    Box(
        modifier = Modifier
            .size(180.dp)
            .millenniumSurface(
                background = millenniumPanelBrush(MillenniumTheme.colors),
                shape = RoundedCornerShape(16.dp),
                borderColor = MillenniumTheme.colors.border,
                borderWidth = 1.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .shimmerApply(),
        contentAlignment = Alignment.Center
    ) {
        val cover = coverBytes?.takeIf { it.isNotEmpty() }
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AppIcon(
                imageVector = Iconsax.IconsaxCd,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AppTheme.colors.onSurfaceVariant
            )
        }
    }
}
