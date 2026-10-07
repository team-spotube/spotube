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

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlin.math.roundToInt

private val ToggleWidth = 44.dp
private val ToggleHeight = 24.dp
private val ThumbSize = 20.dp

private data class ResolvedToggleState(
    val colors: BaseUITheme.ButtonColors,
    val shape: Shape,
    val shadow: BaseUITheme.Shadow,
    val border: BaseUITheme.Border,
)

@Composable
private fun resolveToggleState(
    style: BaseUITheme.ButtonStyle,
    isPressed: Boolean,
    isHovered: Boolean,
): ResolvedToggleState {
    return when {
        isPressed -> ResolvedToggleState(
            style.colors.pressed,
            style.shape.pressed,
            style.shadow.pressed,
            style.border.pressed,
        )

        isHovered -> ResolvedToggleState(
            style.colors.hovered,
            style.shape.hovered,
            style.shadow.hovered,
            style.border.hovered,
        )

        else -> ResolvedToggleState(
            style.colors.normal,
            style.shape.normal,
            style.shadow.normal,
            style.border.normal,
        )
    }
}

@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    theme: BaseUITheme.ToggleTheme? = null,
) {
    val baseTheme = LocalBaseUITheme.current.toggle
    val toggleTheme = theme ?: baseTheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()

    val style = if (checked) toggleTheme.checked else toggleTheme.unchecked
    val resolved = resolveToggleState(style, isPressed, isHovered)

    val thumbColor = when {
        isPressed -> toggleTheme.thumb.pressed
        isHovered -> toggleTheme.thumb.hovered
        else -> toggleTheme.thumb.normal
    }

    val thumbShadowState = when {
        isPressed -> toggleTheme.thumbShadow.pressed
        isHovered -> toggleTheme.thumbShadow.hovered
        else -> toggleTheme.thumbShadow.normal
    }

    val borderColor = when {
        !enabled -> resolved.colors.foreground.copy(alpha = 0.38f)
        else -> resolved.border.color
    }

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbProgress",
    )

    val thumbScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isHovered -> 1.05f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 150),
        label = "thumbScale",
    )

    val lift = if (isHovered && !isPressed && onCheckedChange != null) (-1).dp else 0.dp
    val density = LocalDensity.current
    val thumbOffsetXPx by animateDpAsState(
        targetValue = with(density) {
            val trackWidth = ToggleWidth - ThumbSize
            max((thumbProgress * trackWidth.toPx()).toDp() - 4.dp, 2.dp)
        },
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffsetX",
    )

    Box(
        modifier = modifier
            .width(ToggleWidth)
            .height(ToggleHeight)
            .graphicsLayer {
                translationY = lift.toPx()
            }
            .then(
                if (resolved.shadow.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = resolved.shadow.elevation,
                        shape = resolved.shape,
                        ambientColor = resolved.shadow.ambientColor,
                        spotColor = resolved.shadow.spotColor,
                    )
                } else Modifier
            )
            .clip(resolved.shape)
            .background(resolved.colors.background, resolved.shape)
            .border(BorderStroke(resolved.border.width, borderColor), resolved.shape)
            .highlight(resolved.colors.highlight)
            .then(
                if (onCheckedChange != null) {
                    Modifier
                        .hoverable(interactionSource = source, enabled = enabled)
                        .clickable(
                            enabled = enabled,
                            interactionSource = source,
                            indication = null,
                            onClick = { onCheckedChange(!checked) },
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetXPx)
                .size(ThumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(
                    elevation = thumbShadowState.elevation,
                    shape = CircleShape,
                    ambientColor = thumbShadowState.ambientColor,
                    spotColor = thumbShadowState.spotColor,
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            thumbColor.copy(alpha = 0.95f),
                            thumbColor,
                        ),
                        center = Offset(0f, -0.55f),
                        radius = 1.1f,
                    ),
                    shape = CircleShape,
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@BaseUIPhonePreview
@Composable
private fun TogglePreview() {
    BaseUIPreview {
        var scrobbling by remember { mutableStateOf(true) }
        var offlineMode by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TogglePreviewRow(
                label = "Scrobble listened tracks",
                checked = scrobbling,
                onCheckedChange = { scrobbling = it },
            )
            TogglePreviewRow(
                label = "Offline mode",
                checked = offlineMode,
                onCheckedChange = { offlineMode = it },
            )
            TogglePreviewRow(
                label = "Discord rich presence",
                checked = true,
                enabled = false,
                onCheckedChange = {},
            )
        }
    }
}

@Composable
private fun TogglePreviewRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Toggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
