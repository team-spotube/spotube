package org.spotube.millennium.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Maps a control state to (hover level, press level), each 0 or 1: hovered and focused give
 * `1 to 0`, pressed gives `0 to 1`, every other state `0 to 0`.
 */
internal fun millenniumInteractionLevels(state: MillenniumControlState): Pair<Float, Float> = when (state) {
    MillenniumControlState.Hovered, MillenniumControlState.Focused -> 1f to 0f
    MillenniumControlState.Pressed -> 0f to 1f
    else -> 0f to 0f
}

/**
 * Glass catches light on hover; pressing reverses the bevel and shades the face inward.
 * Drawn below content, on the control's actual surface (not on labelled choice rows).
 * Nothing is drawn at rest (both levels 0); the levels animate with a tween.
 *
 * ```kotlin
 * Box(Modifier.millenniumInteractionSurface(state, RoundedCornerShape(10.dp)))
 * ```
 *
 * @receiver the surface modifier chain.
 * @param state Current resolved [MillenniumControlState].
 * @param shape Shape the lighting follows.
 * @param tint Colour of the accent lighting. Defaults to [MillenniumColors.accent].
 * @param animationMillis Tween duration in milliseconds for hover and press levels. Default 120.
 */
@Composable
internal fun Modifier.millenniumInteractionSurface(
    state: MillenniumControlState,
    shape: Shape,
    tint: Color = MillenniumTheme.colors.accent,
    animationMillis: Int = 120,
): Modifier {
    val (hoverTarget, pressTarget) = millenniumInteractionLevels(state)
    val hover by animateFloatAsState(hoverTarget, tween(animationMillis))
    val press by animateFloatAsState(pressTarget, tween(animationMillis))
    return drawWithCache {
        val face = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.20f * hover),
            0.18f to Color.White.copy(alpha = 0.08f * hover),
            0.45f to Color.Black.copy(alpha = 0.16f * press),
            0.70f to tint.copy(alpha = 0.08f * hover + 0.08f * press),
            1f to tint.copy(alpha = 0.20f * hover + 0.12f * press),
        )
        val rim = Brush.verticalGradient(
            0f to if (press > hover) Color.Black.copy(alpha = 0.42f * press) else Color.White.copy(alpha = 0.65f * hover),
            0.48f to tint.copy(alpha = 0.50f * hover + 0.35f * press),
            1f to Color.White.copy(alpha = 0.18f * hover + 0.55f * press),
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawWithContent {
            if (hover > 0f || press > 0f) {
                drawOutline(outline, face)
                val stroke = 1.dp.toPx()
                if (size.minDimension > stroke * 4f) {
                    inset(stroke) {
                        drawOutline(shape.createOutline(size, layoutDirection, this), rim, style = Stroke(stroke))
                    }
                }
            }
            drawContent()
        }
    }
}

/**
 * Gradient stops (fraction to colour) for Quiet controls: accent-tinted enamel, not a flat
 * translucent Material wash. The pressed variant is reversed (dark at the top) and stronger.
 *
 * @param colors Palette to blend toward [MillenniumColors.accent].
 * @param pressed `true` for the inverted pressed lighting.
 */
internal fun millenniumQuietEnamelStops(colors: MillenniumColors, pressed: Boolean): List<Pair<Float, Color>> {
    val top = lerp(colors.raised, colors.accent, if (pressed) 0.22f else 0.06f)
    val middle = lerp(colors.surface, colors.accent, if (pressed) 0.16f else 0.10f)
    val lower = lerp(colors.inset, colors.accent, if (pressed) 0.12f else 0.20f)
    return if (pressed) listOf(0f to lower, 0.35f to middle, 1f to top)
    else listOf(0f to top, 0.46f to middle, 0.49f to lower, 1f to middle)
}
