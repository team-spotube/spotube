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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val RadioSize = 22.dp
private val RadioDotSize = 10.dp

private data class ResolvedRadioState(
    val colors: BaseUITheme.ButtonColors,
    val shape: Shape,
    val shadow: BaseUITheme.Shadow,
    val border: BaseUITheme.Border,
)

@Composable
private fun resolveRadioState(
    style: BaseUITheme.ButtonStyle,
    isPressed: Boolean,
    isHovered: Boolean,
): ResolvedRadioState {
    return when {
        isPressed -> ResolvedRadioState(style.colors.pressed, style.shape.pressed, style.shadow.pressed, style.border.pressed)
        isHovered -> ResolvedRadioState(style.colors.hovered, style.shape.hovered, style.shadow.hovered, style.border.hovered)
        else -> ResolvedRadioState(style.colors.normal, style.shape.normal, style.shadow.normal, style.border.normal)
    }
}

@Composable
fun Radio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.CheckBoxTheme? = null,
) {
    val baseTheme = LocalBaseUITheme.current.checkBox
    val radioTheme = theme ?: baseTheme
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()

    val style = if (selected) radioTheme.selected else radioTheme.unselected
    val resolved = resolveRadioState(style, isPressed, isHovered)
    val lift = if (isHovered && !isPressed && onClick != null) (-1).dp else 0.dp

    val dotProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "radioDot",
    )

    Box(
        modifier = modifier
            .size(RadioSize)
            .graphicsLayer {
                translationY = lift.toPx()
                shape = CircleShape
                clip = true
            }
            .then(
                if (resolved.shadow.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = resolved.shadow.elevation,
                        shape = CircleShape,
                        ambientColor = resolved.shadow.ambientColor,
                        spotColor = resolved.shadow.spotColor,
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .background(resolved.colors.background, CircleShape)
            .border(BorderStroke(0.5.dp, resolved.border.color), CircleShape)
            .highlight(resolved.colors.highlight)
            .then(
                if (onClick != null) {
                    Modifier
                        .hoverable(interactionSource = source, enabled = enabled)
                        .clickable(
                            interactionSource = source,
                            indication = ripple(bounded = true, radius = RadioSize / 2),
                            enabled = enabled,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RadioDotSize * dotProgress)
                .clip(CircleShape)
                .background(radioTheme.checkmarkColor, CircleShape),
        )
    }
}

@BaseUIPhonePreview
@Composable
private fun RadioPreview() {
    BaseUIPreview {
        val options = listOf("Automatic", "High", "Lossless", "Data saver")
        var selected by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEachIndexed { index, label ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Radio(
                        selected = selected == index,
                        onClick = { selected = index },
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Radio(selected = true, onClick = {}, enabled = false)
                Text("Disabled", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
