/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumCircularProgress
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.library.local.LocalCollectionScreen
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import spotube.composeapp.generated.resources.*

@Composable
fun LocalAlbumScreen(albumName: String, viewModel: LocalAlbumViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        LocalAlbumUiState.Loading -> Column {
            ApplicationMainBar(backButton = true)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MillenniumCircularProgress(size = 40.dp) }
        }
        LocalAlbumUiState.NotFound -> Column {
            ApplicationMainBar(backButton = true)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.local_album_not_found))
            }
        }
        is LocalAlbumUiState.Loaded -> LocalCollectionScreen(
            title = current.album.name,
            description = stringResource(
                Res.string.local_album_subtitle,
                current.artistsLabel,
                pluralStringResource(Res.plurals.library_local_tracks_count, current.tracks.size, current.tracks.size)
            ),
            artwork = current.album.coverBytes,
            placeholderIcon = Iconsax.IconsaxCd,
            tracks = current.tracks,
            onPlay = viewModel::play,
            onShuffle = viewModel::shufflePlay,
            onAddToQueue = viewModel::addToQueue,
            onTrackClick = viewModel::playFromTrack,
            isPlaying = current.isPlaying,
            sharedElementKey = "local_album_art_${current.album.key}"
        )
    }
}
