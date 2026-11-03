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

package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.Play
import compose.icons.feathericons.PlusSquare
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun TrackCard(
    track: MetadataTrack,
    modifier: Modifier = Modifier,
    audioPlayerQueue: AudioPlayerQueue = koinInject()) {
    val scope = rememberCoroutineScope()
    val queueEntry = QueueEntry.StreamingTrack(track = track, url = "")

    PlayableCard(
        title = track.title,
        subtitle = track.artists.joinToString { it.name },
        imageURL = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url,
        onPlay = {
            scope.launch {
                audioPlayerQueue.load(
                    entries = listOf(queueEntry),
                    autoPlay = true,
                    startPosition = 0,
                    collectionEntry = null)
            }
        },
        onAddToQueue = {
            scope.launch {
                audioPlayerQueue.addToQueue(queueEntry)
            }
        },
        modifier = modifier
    )
}

