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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

enum class CheckBoxState {
    SELECTED,
    UNSELECTED,
    INDETERMINATE,
    @Deprecated("Use INDETERMINATE", ReplaceWith("CheckBoxState.INDETERMINATE"))
    CLEAR;
}

private val CheckBoxSize = 22.dp

private data class ResolvedCheckBoxState(
    val colors: BaseUITheme.ButtonColors,
    val shape: Shape,
    val shadow: BaseUITheme.Shadow,
    val border: BaseUITheme.Border,
)

@Composable
private fun resolveCheckBoxState(
    style: BaseUITheme.ButtonStyle,
    isPressed: Boolean,
    isHovered: Boolean,
): ResolvedCheckBoxState {
    return when {
        isPressed -> ResolvedCheckBoxState(style.colors.pressed, style.shape.pressed, style.shadow.pressed, style.border.pressed)
        isHovered -> ResolvedCheckBoxState(style.colors.hovered, style.shape.hovered, style.shadow.hovered, style.border.hovered)
        else -> ResolvedCheckBoxState(style.colors.normal, style.shape.normal, style.shadow.normal, style.border.normal)
    }
}

@Composable
fun CheckBox(
    state: CheckBoxState = CheckBoxState.UNSELECTED,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    theme: BaseUITheme.CheckBoxTheme? = null,
) {
    val baseTheme = LocalBaseUITheme.current.checkBox
    val checkBoxTheme = theme ?: baseTheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()

    val isSelected = state == CheckBoxState.SELECTED
    val isIndeterminate = state == CheckBoxState.INDETERMINATE
    val isFilled = isSelected || isIndeterminate

    val style = if (isFilled) checkBoxTheme.selected else checkBoxTheme.unselected
    val resolved = resolveCheckBoxState(style, isPressed, isHovered)
    val lift = if (isHovered && !isPressed && onClick != null) (-1).dp else 0.dp

    val borderColor = when {
        !enabled -> resolved.colors.foreground.copy(alpha = 0.38f)
        else -> resolved.border.color
    }

    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "checkProgress",
    )
    val dashProgress by animateFloatAsState(
        targetValue = if (isIndeterminate) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "dashProgress",
    )

    Box(
        modifier = modifier
            .size(CheckBoxSize)
            .graphicsLayer { translationY = lift.toPx() }
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
            .border(BorderStroke(0.5.dp, borderColor), resolved.shape)
            .highlight(resolved.colors.highlight)
            .then(
                if (onClick != null) {
                    Modifier
                        .hoverable(interactionSource = source, enabled = enabled)
                        .clickable(
                            interactionSource = source,
                            indication = ripple(),
                            enabled = enabled,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { alpha = checkProgress },
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 2.dp.toPx()
                    val path = Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w * 0.2f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.74f)
                        lineTo(w * 0.82f, h * 0.28f)
                    }
                    drawPath(
                        path = path,
                        color = checkBoxTheme.checkmarkColor,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        } else if (isIndeterminate) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 2.dp)
                    .graphicsLayer { alpha = dashProgress }
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                checkBoxTheme.checkmarkColor,
                                checkBoxTheme.checkmarkColor.copy(alpha = 0.92f),
                            )
                        ),
                    ),
            )
        }
    }
}

@BaseUIPhonePreview
@Composable
private fun CheckBoxPreview() {
    BaseUIPreview {
        var wifi by remember { mutableStateOf(true) }
        var bluetooth by remember { mutableStateOf(false) }
        var downloads by remember { mutableStateOf(true) }

        val allEnabled = wifi && bluetooth && downloads
        val anyEnabled = wifi || bluetooth || downloads
        val allState = when {
            allEnabled -> CheckBoxState.SELECTED
            anyEnabled -> CheckBoxState.INDETERMINATE
            else -> CheckBoxState.UNSELECTED
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CheckBoxPreviewRow(
                state = allState,
                label = "All playback sources",
                onClick = {
                    val target = !allEnabled
                    wifi = target
                    bluetooth = target
                    downloads = target
                },
            )
            Spacer(Modifier.height(4.dp))
            CheckBoxPreviewRow(
                state = if (wifi) CheckBoxState.SELECTED else CheckBoxState.UNSELECTED,
                label = "Wi-Fi",
                onClick = { wifi = !wifi },
            )
            CheckBoxPreviewRow(
                state = if (bluetooth) CheckBoxState.SELECTED else CheckBoxState.UNSELECTED,
                label = "Bluetooth",
                onClick = { bluetooth = !bluetooth },
            )
            CheckBoxPreviewRow(
                state = if (downloads) CheckBoxState.SELECTED else CheckBoxState.UNSELECTED,
                label = "Download over cellular",
                onClick = { downloads = !downloads },
            )
            CheckBoxPreviewRow(CheckBoxState.SELECTED, "Disabled selected", enabled = false)
            CheckBoxPreviewRow(CheckBoxState.UNSELECTED, "Disabled unselected", enabled = false)
        }
    }
}

@Composable
private fun CheckBoxPreviewRow(
    state: CheckBoxState,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(state = state, onClick = onClick, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
