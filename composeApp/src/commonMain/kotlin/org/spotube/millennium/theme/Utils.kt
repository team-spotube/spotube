package org.spotube.millennium.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalContentColor
import com.composeunstyled.LocalTextStyle

/**
 * States understood by every Millennium control. Each component decides how they look by supplying a
 * [MillenniumControlStateProperty], and decides which state wins when several apply (the precedence
 * is implemented by each component; it is not enforced here). Components typically give disabled
 * highest priority, then pressed, then keyboard focus (see [collectIsKeyboardFocusedAsState]), then
 * hover, then selected, then default; check the component source for the exact order.
 *
 * @see MillenniumControlStateProperty.resolve
 */
enum class MillenniumControlState {
    /** Idle, enabled and not interacted with. */
    Default,

    /** Pointer is over the control. */
    Hovered,

    /** Keyboard/accessibility focus (pointer-press focus does not count; see [millenniumIsKeyboardFocusRing]). */
    Focused,

    /** Control is being pressed. */
    Pressed,

    /** Control is not interactive. */
    Disabled,

    /** Control is chosen/checked/current (tab, row, segment, checkbox). */
    Selected,

    /** Control holds invalid input. */
    Error,
}

/**
 * A component-owned value for every [MillenniumControlState]. Entries not supplied fall back to
 * [default], so a theme only specifies what differs.
 *
 * ```kotlin
 * val border = MillenniumControlStateProperty(
 *     default = Color.Gray,
 *     hovered = Color.DarkGray,
 *     disabled = Color.LightGray,
 * )
 * val c = border.resolve(MillenniumControlState.Pressed) // Color.Gray (falls back to default)
 * val tweaked = border.copy(focused = Color.Blue)
 * ```
 *
 * @param T The value type (colour, brush, dp, text style, a properties class...).
 * @property default Value for [MillenniumControlState.Default] and the fallback for every other state.
 * @property hovered Value for [MillenniumControlState.Hovered]; defaults to [default].
 * @property focused Value for [MillenniumControlState.Focused]; defaults to [default].
 * @property pressed Value for [MillenniumControlState.Pressed]; defaults to [default].
 * @property disabled Value for [MillenniumControlState.Disabled]; defaults to [default].
 * @property selected Value for [MillenniumControlState.Selected]; defaults to [default].
 * @property error Value for [MillenniumControlState.Error]; defaults to [default].
 */
@Immutable
data class MillenniumControlStateProperty<T>(
    val default: T,
    val hovered: T = default,
    val focused: T = default,
    val pressed: T = default,
    val disabled: T = default,
    val selected: T = default,
    val error: T = default,
) {
    companion object {
        /**
         * Builds a property by evaluating [stateCb] once for every [MillenniumControlState].
         *
         * ```kotlin
         * val alpha = MillenniumControlStateProperty.resolveWith { state ->
         *     if (state == MillenniumControlState.Disabled) 0.4f else 1f
         * }
         * ```
         *
         * @param stateCb Produces the value for the given state.
         * @return a property with an explicit value for each state.
         */
        fun <T> resolveWith(stateCb: (state: MillenniumControlState) -> T) =
            MillenniumControlStateProperty(
                default = stateCb(MillenniumControlState.Default),
                hovered = stateCb(MillenniumControlState.Hovered),
                focused = stateCb(MillenniumControlState.Focused),
                pressed = stateCb(MillenniumControlState.Pressed),
                disabled = stateCb(MillenniumControlState.Disabled),
                selected = stateCb(MillenniumControlState.Selected),
                error = stateCb(MillenniumControlState.Error),
            )

        /**
         * Creates a property that yields [value] for every state.
         *
         * ```kotlin
         * val shape = MillenniumControlStateProperty.all(RoundedCornerShape(8.dp))
         * ```
         *
         * @param value Value used for all states.
         */
        fun <T> all(value: T) = MillenniumControlStateProperty(value)
    }

    /**
     * Returns the value for [state].
     *
     * @param state The state to look up.
     * @return the matching property value (which equals [default] unless overridden).
     */
    fun resolve(state: MillenniumControlState): T = when (state) {
        MillenniumControlState.Default -> default
        MillenniumControlState.Hovered -> hovered
        MillenniumControlState.Focused -> focused
        MillenniumControlState.Pressed -> pressed
        MillenniumControlState.Disabled -> disabled
        MillenniumControlState.Selected -> selected
        MillenniumControlState.Error -> error
    }
}

/**
 * Convex Aqua gel: bright cap, crisp reflection break and a luminous lower lip, as a vertical
 * gradient with six stops (at 0, 0.08, 0.46, 0.49, 0.80 and 1.0). Every stop lies between [top] and
 * [bottom], which keeps label contrast predictable.
 *
 * ```kotlin
 * val c = MillenniumTheme.colors
 * Box(Modifier.background(gelBrush(c.accentTop, c.accentBottom)))
 * ```
 *
 * @param top Bright cap colour (typically [MillenniumColors.accentTop]).
 * @param bottom Deep lower colour (typically [MillenniumColors.accentBottom]).
 * @return a vertical gradient [Brush].
 * @see millenniumBevelBrush
 */
fun gelBrush(top: Color, bottom: Color): Brush = Brush.verticalGradient(
    *millenniumGelStops(top, bottom).toTypedArray(),
)

/**
 * The gel stops behind [gelBrush] as (fraction, colour) pairs. Every stop stays between the
 * endpoints, preserving the tested label contrast.
 */
internal fun millenniumGelStops(top: Color, bottom: Color): List<Pair<Float, Color>> = listOf(
    0f to lerp(top, bottom, 0.18f),
    0.08f to top,
    0.46f to lerp(top, bottom, 0.06f),
    0.49f to lerp(top, bottom, 0.70f),
    0.80f to bottom,
    1f to lerp(bottom, top, 0.32f),
)

/**
 * A lighter, surface-blended version of the control gel. Used for selected rows
 * (e.g. [org.spotube.millennium.composables.MillenniumListTile]) that want a quieter
 * selection than a primary button's full-strength gel.
 *
 * ```kotlin
 * Modifier.background(millenniumSelectedGelBrush(MillenniumTheme.colors, strength = 0.5f))
 * ```
 *
 * @param colors Palette whose gel endpoints and [MillenniumColors.surface] are blended.
 * @param strength Blend amount toward the gel, coerced to 0..1 (0 = plain surface, 1 = full gel).
 *   Default 0.35.
 * @return a vertical gradient [Brush].
 * @see gelBrush
 */
fun millenniumSelectedGelBrush(colors: MillenniumColors, strength: Float = 0.35f): Brush =
    Brush.verticalGradient(*millenniumSelectedGelStops(colors, strength).toTypedArray())

/** The individual stops behind [millenniumSelectedGelBrush] (fraction to colour), exposed for contrast tests. */
internal fun millenniumSelectedGelStops(colors: MillenniumColors, strength: Float = 0.35f): List<Pair<Float, Color>> {
    val amount = strength.coerceIn(0f, 1f)
    return millenniumGelStops(colors.accentTop, colors.accentBottom)
        .map { (stop, color) -> stop to lerp(colors.surface, color, amount) }
}

/**
 * Surface brush used by component-local default themes: the accent gel, a raised neutral bevel, or a
 * sunken neutral bevel.
 *
 * ```kotlin
 * val c = MillenniumTheme.colors
 * val raised = millenniumBevelBrush(c)
 * val well = millenniumBevelBrush(c, sunken = true)
 * val primary = millenniumBevelBrush(c, accent = true)
 * ```
 *
 * @param colors Palette to build from.
 * @param accent When `true`, returns [gelBrush] of the accent endpoints and ignores [sunken].
 * @param sunken When `true` (and not [accent]), returns a recessed gradient from [MillenniumColors.inset]
 *   toward [MillenniumColors.surface]; otherwise a raised gradient with a reflection break.
 * @return a vertical gradient [Brush].
 */
fun millenniumBevelBrush(
    colors: MillenniumColors,
    accent: Boolean = false,
    sunken: Boolean = false,
): Brush = if (accent) {
    gelBrush(colors.accentTop, colors.accentBottom)
} else {
    millenniumNeutralBrush(colors, sunken)
}

/** The three neutral colours (raised or sunken) behind [millenniumBevelBrush]. Stay within tested surfaces. */
internal fun millenniumNeutralStops(colors: MillenniumColors, sunken: Boolean): List<Color> =
    if (sunken) listOf(colors.inset, lerp(colors.inset, colors.surface, 0.55f), colors.surface)
    else listOf(colors.raised, colors.surface, lerp(colors.surface, colors.inset, 0.65f))

private fun millenniumNeutralBrush(colors: MillenniumColors, sunken: Boolean): Brush {
    val stops = millenniumNeutralStops(colors, sunken)
    return if (sunken) Brush.verticalGradient(0f to stops[0], 0.18f to stops[1], 1f to stops[2])
    else Brush.verticalGradient(
        0f to stops[0],
        0.47f to stops[1],
        0.50f to stops[2],
        1f to stops[1],
    )
}

/**
 * Pearl enamel / smoked glass panel brush, distinct from the stronger control gel: a bright top edge
 * easing into [MillenniumColors.surface], with a slightly darker lower edge.
 *
 * ```kotlin
 * Box(Modifier.millenniumSurface(millenniumPanelBrush(MillenniumTheme.colors), RoundedCornerShape(12.dp)))
 * ```
 *
 * @param colors Palette to build from.
 * @return a vertical gradient [Brush].
 */
fun millenniumPanelBrush(colors: MillenniumColors): Brush = Brush.verticalGradient(
    0f to colors.raised,
    0.12f to lerp(colors.raised, colors.surface, 0.45f),
    0.22f to colors.surface,
    0.85f to colors.surface,
    1f to lerp(colors.surface, colors.inset, 0.35f),
)

/**
 * Applies one complete surface layer (shadow, clip, fill, border and optional rim/orb effects) in a
 * single pass, avoiding a second background/bevel pass.
 *
 * Order: optional drop shadow(s) behind, then clip to [shape], fill with [background], border, then
 * (when any effect is requested) the orb below the content and the rims above it.
 *
 * Translucent fills: the drop shadow is drawn *behind* the fill, so a translucent [background] lets
 * the shadow bleed through as a darker inner box. Pass `opaqueFill = false` for translucent washes;
 * the shadow is then skipped entirely.
 *
 * ```kotlin
 * val c = MillenniumTheme.colors
 * Box(
 *     Modifier.millenniumSurface(
 *         background = millenniumPanelBrush(c),
 *         shape = RoundedCornerShape(12.dp),
 *         borderColor = c.border,
 *         borderWidth = 1.dp,
 *         shadowElevation = 4.dp,
 *         innerGlowColor = c.highlight.copy(alpha = 0.6f),
 *     ),
 * )
 *
 * // A hover wash must not cast a shadow:
 * Box(Modifier.millenniumSurface(SolidColor(c.accent.copy(alpha = 0.1f)), shape, opaqueFill = false))
 * ```
 *
 * @receiver the modifier chain to extend.
 * @param background Fill brush, drawn clipped to [shape].
 * @param shape Shape used for shadow, clip, fill, border and rims.
 * @param borderColor Border colour; no border is drawn when `null`.
 * @param borderWidth Border width; no border is drawn when 0.dp.
 * @param shadowElevation Drop-shadow elevation; 0.dp (default) draws none. Also skipped when
 *   [opaqueFill] is `false`.
 * @param opaqueFill Whether [background] fully covers what is behind it. The drop shadow is drawn
 *   behind the fill, so a translucent fill would reveal it as an inner box. Pass `false` for
 *   translucent washes to skip the shadow. Default `true`.
 * @param shadowColor Ambient and spot shadow colour when [shadowColors] is not used. Default 16% black.
 * @param shadowColors When non-empty, one shadow layer is drawn per colour (stacked) instead of
 *   [shadowColor]. `null` or empty uses [shadowColor].
 * @param innerGlowColor Colour of the 1px inner rim that follows the shape and fades out by about
 *   65% of the height; `null` draws no rim unless other effects are set.
 * @param innerGlowWidth Stroke width of the inner rim. Default 1.dp.
 * @param innerGlowInset Horizontal inset of the legacy straight highlight line; used only when
 *   [innerGlowFollowsShape] is `false`.
 * @param innerGlowTop Distance of the rim from the top edge (the larger of this and [borderWidth] is
 *   used as the rim inset). Default 1.dp.
 * @param innerGlowFollowsShape `true` (default) draws the rim along the shape outline; `false` draws a
 *   straight horizontal highlight line (legacy, opt-in).
 * @param bottomRimColor Colour at the bottom end of the rim gradient (a lower lip); `null` for none.
 * @param insetShadeColor Colour of a 3dp shade along the top inner perimeter, fading out by 40%
 *   height, for recessed fields; `null` for none.
 * @param orbColor Colour of the Aqua glass orb overlay (translucent dome with curved lower edge, top
 *   arc and lower reflection arc), drawn between fill and content; `null` for none.
 * @param orbBottomFraction Fraction of the height (0..1) where the dome ends. Default 0.55.
 * @param orbDepthFraction Fraction of the height by which the dome's lower edge curves downward.
 *   Default 0.16.
 * @return the extended [Modifier].
 * @see gelBrush
 * @see millenniumPanelBrush
 */
fun Modifier.millenniumSurface(
    background: Brush,
    shape: Shape,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    opaqueFill: Boolean = true,
    shadowColor: Color = Color.Black.copy(alpha = 0.16f),
    shadowColors: List<Color>? = null,
    innerGlowColor: Color? = null,
    innerGlowWidth: Dp = 1.dp,
    innerGlowInset: Dp = 8.dp,
    innerGlowTop: Dp = 1.dp,
    innerGlowFollowsShape: Boolean = true,
    bottomRimColor: Color? = null,
    insetShadeColor: Color? = null,
    orbColor: Color? = null,
    orbBottomFraction: Float = 0.55f,
    orbDepthFraction: Float = 0.16f,
): Modifier {
    var result = this
    if (shadowElevation > 0.dp && opaqueFill) {
        val colors = shadowColors?.takeIf { it.isNotEmpty() } ?: listOf(shadowColor)
        colors.forEach { color ->
            result = result.shadow(
                shadowElevation,
                shape,
                ambientColor = color,
                spotColor = color,
            )
        }
    }
    result = result.clip(shape).background(background, shape)
    if (borderColor != null && borderWidth > 0.dp) {
        result = result.border(borderWidth, borderColor, shape)
    }
    if (innerGlowColor != null || bottomRimColor != null || insetShadeColor != null || orbColor != null) {
        result = result.drawWithContent {
            // The Aqua glass orb sits above the fill but below the content: a hazy
            // top dome with a curved meniscus edge, a bright inner top rim and a
            // soft reflection along the lower rim.
            if (orbColor != null) {
                val domeBottom = size.height * orbBottomFraction
                val domeDepth = size.height * orbDepthFraction
                val dome = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, domeBottom)
                    quadraticTo(size.width / 2f, domeBottom + domeDepth, 0f, domeBottom)
                    close()
                }
                drawPath(
                    path = dome,
                    brush = Brush.verticalGradient(
                        0f to orbColor,
                        0.78f to orbColor.copy(alpha = orbColor.alpha * 0.82f),
                        0.95f to orbColor.copy(alpha = orbColor.alpha * 0.3f),
                        1f to Color.Transparent,
                        startY = 0f,
                        endY = domeBottom,
                    ),
                )
                val minSide = size.minDimension
                val arcInset = minSide * 0.10f
                drawArc(
                    color = orbColor.copy(alpha = (orbColor.alpha + 0.3f).coerceAtMost(1f)),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = Size(size.width - arcInset * 2f, size.height - arcInset * 2f),
                    style = Stroke(width = minSide * 0.045f),
                )
                val reflectionInset = minSide * 0.08f
                drawArc(
                    color = orbColor.copy(alpha = orbColor.alpha * 0.55f),
                    startAngle = 25f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(reflectionInset, reflectionInset),
                    size = Size(size.width - reflectionInset * 2f, size.height - reflectionInset * 2f),
                    style = Stroke(width = minSide * 0.06f),
                )
            }
            drawContent()
            val stroke = innerGlowWidth.toPx()
            val edgeInset = maxOf(borderWidth.toPx(), innerGlowTop.toPx()) + stroke / 2f
            // Shade only the inner perimeter, never the input's label or value.
            if (insetShadeColor != null && size.minDimension > edgeInset * 4f) {
                inset(edgeInset) {
                    drawOutline(
                        shape.createOutline(size, layoutDirection, this),
                        brush = Brush.verticalGradient(0f to insetShadeColor, 0.4f to Color.Transparent),
                        style = Stroke(3.dp.toPx()),
                    )
                }
            }
            if (innerGlowFollowsShape && stroke > 0f && size.minDimension > edgeInset * 2f) {
                // A fine inner rim follows rounded corners instead of a floating
                // horizontal line. It fades before the lower edge, keeping the
                // highlight off the label and avoiding a second surface layer.
                inset(edgeInset) {
                    drawOutline(
                        outline = shape.createOutline(size, layoutDirection, this),
                        brush = Brush.verticalGradient(
                            0f to (innerGlowColor ?: Color.Transparent),
                            0.3f to (innerGlowColor?.copy(alpha = innerGlowColor.alpha * 0.35f) ?: Color.Transparent),
                            0.65f to Color.Transparent,
                            1f to (bottomRimColor ?: Color.Transparent),
                        ),
                        style = Stroke(stroke),
                    )
                }
            } else if (innerGlowColor != null && !innerGlowFollowsShape && size.width > innerGlowInset.toPx() * 2f) {
                drawLine(
                    color = innerGlowColor,
                    start = Offset(innerGlowInset.toPx(), innerGlowTop.toPx()),
                    end = Offset(size.width - innerGlowInset.toPx(), innerGlowTop.toPx()),
                    strokeWidth = stroke,
                )
            }
        }
    }
    return result
}

/**
 * Provides a merged text style and content colour to [content] through Compose Unstyled's
 * `LocalTextStyle` and `LocalContentColor`.
 *
 * ```kotlin
 * MillenniumStyledContent(textStyle = TextStyle(fontSize = 14.sp), foreground = colors.text) {
 *     BasicText("Label")
 * }
 * ```
 *
 * @param textStyle Style merged onto the inherited text style.
 * @param foreground Colour applied to both the text style and the content colour.
 * @param content Content that inherits the style and colour.
 */
@Composable
internal fun MillenniumStyledContent(
    textStyle: TextStyle,
    foreground: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(textStyle).copy(color = foreground),
        LocalContentColor provides foreground,
        content = content,
    )
}
