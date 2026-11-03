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

package org.spotube.spotube.modules.lyrics


import org.spotube.millennium.composables.MillenniumBarsLoader
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumSegmentedButton
import org.spotube.millennium.composables.MillenniumSegmentedButtonDefaults
import org.spotube.millennium.composables.MillenniumSegmentedButtonRow
import org.spotube.millennium.theme.MillenniumTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import org.spotube.spotube.core.ui.theming.AppTheme

import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.ui.component.ApplicationBackButton
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error
import spotube.composeapp.generated.resources.lyrics_mode_plain
import spotube.composeapp.generated.resources.lyrics_mode_synced
import spotube.composeapp.generated.resources.lyrics_no_plain
import spotube.composeapp.generated.resources.lyrics_no_synced
import spotube.composeapp.generated.resources.common_close

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MillenniumTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.accent.copy(alpha = 0.14f),
                    0.55f to colors.background,
                    1f to colors.background
                )
            )
    ) {
        val isWide = maxWidth >= 840.dp

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
                    if (onClose != null) {
                        MillenniumIconButton(
                            onClick = onClose,
                            style = MillenniumIconButtonStyle.Quiet
                        ) {
                            MillenniumIcon(
                                imageVector = Iconsax.IconsaxArrowDown4,
                                contentDescription = stringResource(Res.string.common_close)
                            )
                        }
                    } else {
                        ApplicationBackButton()
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MillenniumSegmentedButtonRow(Modifier.widthIn(max = 280.dp)) {
                        MillenniumSegmentedButton(
                            selected = uiState.mode == LyricType.SYNCED,
                            onClick = { viewModel.setMode(LyricType.SYNCED) },
                            shape = MillenniumSegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(Res.string.lyrics_mode_synced)) }
                        MillenniumSegmentedButton(
                            selected = uiState.mode == LyricType.STATIC,
                            onClick = { viewModel.setMode(LyricType.STATIC) },
                            shape = MillenniumSegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(Res.string.lyrics_mode_plain)) }
                    }
                }
                Spacer(Modifier.width(48.dp))
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackPane(uiState, Modifier.weight(0.38f))
                    Column(Modifier.weight(0.62f).fillMaxHeight()) {
                        LyricsBody(uiState, viewModel)
                    }
                }
            } else {
                CompactTrackHeader(
                    uiState,
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    LyricsBody(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LyricsBody(uiState: LyricsUiState, viewModel: LyricsViewModel) {
    when {
        uiState.isLoading -> CenteredMessage {
            MillenniumBarsLoader(size = 48.dp)
        }

        uiState.error != null -> CenteredMessage {
            Text(
                text = uiState.error ?: stringResource(Res.string.common_unknown_error),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.danger,
                textAlign = TextAlign.Center
            )
        }

        uiState.mode == LyricType.SYNCED -> SyncedLyricsContent(
            syncedLyrics = uiState.syncedLyrics ?: emptyList(),
            currentIndex = uiState.currentIndex,
            onLineClick = viewModel::seekToLine
        )

        uiState.mode == LyricType.STATIC -> PlainLyricsContent(
            plainLyrics = uiState.plainLyrics ?: ""
        )
    }
}

@Composable
private fun CoverArt(url: String, size: Dp, radius: Dp, elevation: Dp, modifier: Modifier = Modifier) {
    val colors = MillenniumTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .millenniumSurface(
                background = millenniumPanelBrush(colors),
                shape = RoundedCornerShape(radius),
                borderColor = colors.border,
                borderWidth = 1.dp,
                shadowElevation = elevation
            )
    ) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun TrackPane(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoverArt(
            url = uiState.coverUrl,
            size = 280.dp,
            radius = 24.dp,
            elevation = 14.dp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = uiState.trackTitle,
            style = AppTheme.typography.headlineSmall,
            color = MillenniumTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiState.trackArtists,
            style = AppTheme.typography.bodyLarge,
            color = MillenniumTheme.colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTrackHeader(uiState: LyricsUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = uiState.coverUrl, size = 48.dp, radius = 12.dp, elevation = 4.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = uiState.trackTitle,
                style = AppTheme.typography.titleSmall,
                color = MillenniumTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.trackArtists,
                style = AppTheme.typography.bodySmall,
                color = MillenniumTheme.colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColumnScope.CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ColumnScope.SyncedLyricsContent(
    syncedLyrics: List<LyricsLine>,
    currentIndex: Int,
    onLineClick: (LyricsLine) -> Unit) {
    if (syncedLyrics.isEmpty()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_synced),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.muted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    SyncedLyricsList(
        lines = syncedLyrics,
        currentIndex = currentIndex,
        onLineClick = onLineClick,
        modifier = Modifier.weight(1f).fillMaxWidth()
    )
}

@Composable
private fun ColumnScope.PlainLyricsContent(
    plainLyrics: String) {
    if (plainLyrics.isBlank()) {
        CenteredMessage {
            Text(
                text = stringResource(Res.string.lyrics_no_plain),
                style = AppTheme.typography.bodyLarge,
                color = MillenniumTheme.colors.muted,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
    ) {
        item {
            Text(
                text = plainLyrics,
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.typography.titleMedium.copy(lineHeight = 34.sp),
                color = MillenniumTheme.colors.text,
                textAlign = TextAlign.Center
            )
        }
    }
}
