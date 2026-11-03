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

package org.spotube.spotube.modules.shell.alternative_track


import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import org.spotube.millennium.composables.MillenniumSpokeSpinner
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumListTile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.alternative_track_active_source
import spotube.composeapp.generated.resources.alternative_track_confidence
import spotube.composeapp.generated.resources.alternative_track_empty
import spotube.composeapp.generated.resources.alternative_track_no_track
import spotube.composeapp.generated.resources.alternative_track_searching
import spotube.composeapp.generated.resources.alternative_track_title

@Composable
fun AlternativeTrackContent(
    viewModel: AlternativeTrackContentViewModel = koinViewModel<AlternativeTrackContentViewModel>(),
    modifier: Modifier = Modifier) {
    val uiState by viewModel.alternativeTrackUiState.collectAsState()

    LaunchedEffect(uiState.currentTrack?.id) {
        viewModel.loadAlternatives()
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.alternative_track_title),
                style = AppTheme.typography.titleLarge)

            val currentTrack = uiState.currentTrack
            if (currentTrack == null) {
                Text(
                    text = stringResource(Res.string.alternative_track_no_track),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant)
                return@Column
            }

            if (uiState.isLoading && uiState.alternatives.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    MillenniumSpokeSpinner(size = 24.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.alternative_track_searching),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onSurfaceVariant)
                }
                return@Column
            }

            if (uiState.alternatives.isEmpty()) {
                Text(
                    text = stringResource(Res.string.alternative_track_empty),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant)
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsWithUniqueKeys(
                    items = uiState.alternatives,
                    id = { source -> source.id }) { source ->
                    AlternativeSourceRow(
                        source = source,
                        isActive = source.id == uiState.activeSourceId,
                        onClick = { viewModel.selectAlternative(source) })
                }
            }
        }
    }
}

@Composable
private fun AlternativeSourceRow(
    source: AudioSource,
    isActive: Boolean,
    onClick: () -> Unit) {
    val thumbnailUrl = source.thumbnails.maxByOrNull { it.width * it.height }?.url
    val rowColor = if (isActive) {
        AppTheme.colors.secondaryContainer
    } else {
        AppTheme.colors.surfaceContainerHigh
    }

    MillenniumListTile(
        onClick = onClick,
        selected = isActive,
        leading = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppTheme.shapes.small)
                    .background(AppTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                } else {
                    Text(
                        text = source.title.firstOrNull()?.uppercase() ?: "?",
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.onSurfaceVariant)
                }
            }
        },
        title = {
            Text(
                text = source.title,
                style = AppTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        },
        subtitle = {
            Column {
                val subtitle = buildString {
                    source.artist?.let { append(it) }
                    if (source.album != null) {
                        if (isNotEmpty()) append(" - ")
                        append(source.album)
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = stringResource(Res.string.alternative_track_confidence, (source.confidence * 100).toInt()),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant)
            }
        },
        trailing = {
            if (isActive) {
                MillenniumIcon(
                    Iconsax.IconsaxCheckCircle,
                    contentDescription = stringResource(Res.string.alternative_track_active_source),
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(22.dp))
            }
        })
}
