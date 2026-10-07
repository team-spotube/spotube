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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SliderTrackHeight = 7.dp
private val SliderThumbSize = 24.dp
private val SliderMinHeight = 32.dp

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onValueChangeFinished: () -> Unit = {},
    theme: BaseUITheme.SliderTheme? = null,
) {
    val baseTheme = LocalBaseUITheme.current.slider
    val sliderTheme = theme ?: baseTheme
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    var sliderWidth by remember { mutableIntStateOf(0) }

    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range > 0f) ((value - valueRange.start) / range).coerceIn(0f, 1f) else 0f

    val stateColor = when {
        isPressed -> sliderTheme.trackActiveColor.pressed
        isHovered -> sliderTheme.trackActiveColor.hovered
        else -> sliderTheme.trackActiveColor.normal
    }
    val inactiveColor = when {
        isPressed -> sliderTheme.trackInactiveColor.pressed
        isHovered -> sliderTheme.trackInactiveColor.hovered
        else -> sliderTheme.trackInactiveColor.normal
    }
    val thumbColor = when {
        isPressed -> sliderTheme.thumbColor.pressed
        isHovered -> sliderTheme.thumbColor.hovered
        else -> sliderTheme.thumbColor.normal
    }
    val thumbShadowState = when {
        isPressed -> sliderTheme.thumbShadow.pressed
        isHovered -> sliderTheme.thumbShadow.hovered
        else -> sliderTheme.thumbShadow.normal
    }

    val trackActiveColor = if (enabled) stateColor else stateColor.copy(alpha = 0.38f)
    val trackInactiveColor = if (enabled) inactiveColor else inactiveColor.copy(alpha = 0.38f)
    val resolvedThumbColor = if (enabled) thumbColor else thumbColor.copy(alpha = 0.38f)

    val thumbScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.15f
            isHovered -> 1.08f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 150),
        label = "thumbScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SliderMinHeight)
            .onSizeChanged { sliderWidth = it.width }
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput

                val stepSize = if (steps > 0) range / (steps + 1) else 0f
                val thumbHalfPx = with(density) { SliderThumbSize.toPx() / 2f }

                fun valueFromX(x: Float): Float {
                    val width = size.width.toFloat()
                    val trackWidth = width - 2 * thumbHalfPx
                    if (trackWidth <= 0f) return valueRange.start
                    val raw = ((x - thumbHalfPx) / trackWidth) * range + valueRange.start
                    val coerced = raw.coerceIn(valueRange.start, valueRange.endInclusive)
                    return if (steps > 0) {
                        val stepIndex = ((coerced - valueRange.start) / stepSize).roundToInt()
                        (valueRange.start + stepIndex * stepSize).coerceIn(
                            valueRange.start,
                            valueRange.endInclusive,
                        )
                    } else {
                        coerced
                    }
                }

                detectTapGestures { offset ->
                    onValueChange(valueFromX(offset.x))
                    onValueChangeFinished()
                }
            }
            .pointerInput(enabled, valueRange, steps) {
                if (!enabled) return@pointerInput

                val stepSize = if (steps > 0) range / (steps + 1) else 0f
                val thumbHalfPx = with(density) { SliderThumbSize.toPx() / 2f }

                fun valueFromX(x: Float): Float {
                    val width = size.width.toFloat()
                    val trackWidth = width - 2 * thumbHalfPx
                    if (trackWidth <= 0f) return valueRange.start
                    val raw = ((x - thumbHalfPx) / trackWidth) * range + valueRange.start
                    val coerced = raw.coerceIn(valueRange.start, valueRange.endInclusive)
                    return if (steps > 0) {
                        val stepIndex = ((coerced - valueRange.start) / stepSize).roundToInt()
                        (valueRange.start + stepIndex * stepSize).coerceIn(
                            valueRange.start,
                            valueRange.endInclusive,
                        )
                    } else {
                        coerced
                    }
                }

                detectHorizontalDragGestures(
                    onDragEnd = { onValueChangeFinished() },
                ) { change, _ ->
                    change.consume()
                    onValueChange(valueFromX(change.position.x))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SliderTrackHeight)
                .align(Alignment.CenterStart),
        ) {
            val thumbHalfPx = SliderThumbSize.toPx() / 2f
            val trackY = size.height / 2f
            val trackStart = thumbHalfPx
            val trackEnd = size.width - thumbHalfPx
            val trackWidth = trackEnd - trackStart

            if (trackWidth > 0f) {
                val trackRadius = CornerRadius(size.height / 2f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            trackInactiveColor.copy(alpha = 0.45f),
                            trackInactiveColor,
                            trackInactiveColor.copy(alpha = 0.7f),
                        ),
                    ),
                    topLeft = Offset(trackStart, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = trackRadius,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(trackStart + size.height / 2f, 1.dp.toPx()),
                    end = Offset(trackEnd - size.height / 2f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                val activeEnd = trackStart + trackWidth * fraction
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            trackActiveColor.copy(alpha = 0.75f),
                            trackActiveColor,
                            trackActiveColor.copy(alpha = 0.9f),
                        ),
                    ),
                    topLeft = Offset(trackStart, 0f),
                    size = Size((activeEnd - trackStart).coerceAtLeast(0f), size.height),
                    cornerRadius = trackRadius,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(trackStart + size.height / 2f, 1.dp.toPx()),
                    end = Offset(activeEnd - size.height / 2f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                if (steps > 0 && enabled) {
                    val stepSpacing = trackWidth / (steps + 1)
                    for (i in 1 downTo steps) {
                        val x = trackStart + stepSpacing * i
                        drawCircle(
                            color = if (x <= activeEnd) {
                                sliderTheme.stepActiveColor
                            } else {
                                sliderTheme.stepInactiveColor
                            },
                            radius = 2.dp.toPx(),
                            center = Offset(x, trackY),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(SliderThumbSize)
                .offset {
                    val thumbHalfPx = with(density) { SliderThumbSize.toPx() / 2f }
                    val trackWidth = sliderWidth.toFloat() - 2 * thumbHalfPx
                    val x = thumbHalfPx + trackWidth * fraction - thumbHalfPx
                    IntOffset(x.roundToInt(), 0)
                }
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
                            resolvedThumbColor.copy(alpha = 0.95f),
                            resolvedThumbColor,
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
private fun SliderPreview() {
    BaseUIPreview {
        var volume by remember { mutableFloatStateOf(0.65f) }
        var progress by remember { mutableFloatStateOf(0.3f) }
        var quality by remember { mutableFloatStateOf(2f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SliderPreviewField(title = "Volume", value = "${(volume * 100).toInt()}%") {
                Slider(value = volume, onValueChange = { volume = it })
            }
            SliderPreviewField(title = "Progress", value = "${(progress * 100).toInt()}%") {
                Slider(value = progress, onValueChange = { progress = it })
            }
            SliderPreviewField(title = "Quality", value = "${quality.toInt()} / 4") {
                Slider(
                    value = quality,
                    onValueChange = { quality = it },
                    valueRange = 0f..4f,
                    steps = 3,
                )
            }
            SliderPreviewField(title = "Disabled", value = "40%") {
                Slider(value = 0.4f, onValueChange = {}, enabled = false)
            }
        }
    }
}

@Composable
private fun SliderPreviewField(
    title: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}
