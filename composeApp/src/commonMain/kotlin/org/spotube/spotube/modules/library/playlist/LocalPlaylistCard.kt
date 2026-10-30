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

package org.spotube.spotube.modules.library.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.overflow.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import com.composeunstyled.Typography
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.ui.component.AsyncImage
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxMoreVertical
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.plurals.library_local_tracks_count

@Composable
fun LocalPlaylistCard(
    playlist: LocalPlaylist,
    modifier: Modifier = Modifier,
    listMode: Boolean = false,
    onClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val trackCount = playlist.trackPaths.size
    val artworkUrl = if (playlist.trackPaths.isNotEmpty()) {
        // Try to get artwork from the first track
        "" // Will be resolved by the AsyncImage
    } else ""
    
    PlayableCard(
        title = playlist.name,
        subtitle = pluralStringResource(Res.plurals.library_local_tracks_count, trackCount, trackCount),
        imageUrl = artworkUrl,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        listMode = listMode,
        onClick = onClick,
        onPlayClick = onPlayClick,
        trailingContent = {
            MillenniumIconButton(
                icon = Iconsax.IconsaxMoreVertical,
                contentDescription = stringResource(Res.string.common_more_options),
                onClick = onMoreClick,
                style = MillenniumIconButtonStyle.Quiet
            )
        },
        onLongClick = onMoreClick,
        subtitleContent = {
            if (playlist.description != null && playlist.description!.isNotBlank()) {
                Text(
                    text = playlist.description!,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    )
}