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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.millennium.theme.MillenniumTheme

private const val LYRICS_BLANK_LINE = "\u266A"

/**
 * Time-synced lyrics list that keeps the active line vertically centered.
 *
 * With [onLineClick] it renders Millennium list tiles (the active one is the selected tile);
 * without it, it renders a passive compact preview.
 */
@Composable
fun SyncedLyricsList(
    lines: List<LyricsLine>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    onLineClick: ((LyricsLine) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val colors = MillenniumTheme.colors

    BoxWithConstraints(modifier = modifier) {
        // Half-viewport padding lets the first/last lines reach the center too.
        val edgePadding = maxHeight / 2

        LaunchedEffect(currentIndex, lines.size) {
            if (currentIndex !in lines.indices) return@LaunchedEffect
            val visible = listState.layoutInfo.visibleItemsInfo
            val itemHeight = visible.firstOrNull { it.index == currentIndex }?.size
                ?: visible.firstOrNull()?.size
                ?: 0
            listState.animateScrollToItem(index = currentIndex, scrollOffset = itemHeight / 2)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().then(
                if (onLineClick != null) Modifier.fadingEdges(maxHeight * 0.18f) else Modifier
            ),
            contentPadding = PaddingValues(vertical = edgePadding),
            verticalArrangement = Arrangement.spacedBy(if (onLineClick != null) 4.dp else 2.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> "$index-${line.time}" }) { index, line ->
                val isCurrent = index == currentIndex
                val text = line.text.ifBlank { LYRICS_BLANK_LINE }
                val color by animateColorAsState(
                    when {
                        isCurrent -> colors.accent
                        index < currentIndex -> colors.muted.copy(alpha = 0.55f)
                        else -> colors.muted
                    }
                )
                if (onLineClick != null) {
                    ImmersiveLyricLine(
                        text = text,
                        distance = kotlin.math.abs(index - currentIndex),
                        isCurrent = isCurrent,
                        onClick = { onLineClick(line) }
                    )
                } else {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = AppTheme.typography.bodyMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = color
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveLyricLine(
    text: String,
    distance: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val colors = MillenniumTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val targetAlpha = when (distance) {
        0 -> 1f
        1 -> 0.7f
        2 -> 0.5f
        else -> 0.32f
    }
    val alpha by animateFloatAsState(targetAlpha, tween(300))
    val scale by animateFloatAsState(if (isCurrent) 1f else 0.9f, tween(300))
    val color by animateColorAsState(if (isCurrent) colors.accent else colors.text, tween(300))
    val hoverBackground by animateColorAsState(
        if (hovered && !isCurrent) colors.accent.copy(alpha = 0.10f) else Color.Transparent
    )

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = if (hovered) maxOf(alpha, 0.85f) else alpha
            }
            .clip(RoundedCornerShape(14.dp))
            .background(hoverBackground)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        style = AppTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = color
        ),
        textAlign = TextAlign.Center
    )
}

/** Fades the top and bottom [fade] of the content to transparent. */
private fun Modifier.fadingEdges(fade: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fraction = (fade.toPx() / size.height).coerceIn(0f, 0.5f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                fraction to Color.Black,
                1f - fraction to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
