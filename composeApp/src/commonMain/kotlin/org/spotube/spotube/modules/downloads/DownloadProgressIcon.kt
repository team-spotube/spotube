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

package org.spotube.spotube.modules.downloads


import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadProgressIcon(
    track: MetadataTrack?,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
    tint: Color = AppTheme.colors.onSurface,
    progressTint: Color = AppTheme.colors.primary) {
    val downloadsViewModel = koinViewModel<DownloadsViewModel>()
    val downloads by downloadsViewModel.downloads.collectAsStateWithLifecycle()

    val currentDownload = track?.let { t ->
        downloads.firstOrNull { item ->
            item.track?.id == t.id && item.status is DownloadStatus.Downloading
        }
    }

    val progress by animateFloatAsState(
        targetValue = currentDownload?.progress ?: 0f,
        label = "download_progress"
    )

    if (currentDownload != null && progress > 0f) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(size)) {
                val diameter = this.size.minDimension
                val stroke = strokeWidth.toPx()
                drawArc(
                    color = progressTint,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(diameter - stroke, diameter - stroke))
            }
            MillenniumIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.6f))
        }
    } else {
        MillenniumIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.size(size))
    }
}

@Composable
fun DownloadBadgeIndicator(
    modifier: Modifier = Modifier) {
    val downloadsViewModel = koinViewModel<DownloadsViewModel>()
    val downloads by downloadsViewModel.downloads.collectAsStateWithLifecycle()

    val activeDownloads = downloads.count { it.status is DownloadStatus.Downloading }

    if (activeDownloads > 0) {
        Box(
            modifier = modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary)
        )
    }
}
