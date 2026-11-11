package org.spotube.millennium.composables

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.TooltipHost
import com.composeunstyled.TooltipPanel
import com.composeunstyled.UnstyledTooltip
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Visual, layout and motion properties of a [MillenniumTooltip].
 *
 * @property background Panel fill; null uses the palette panel brush.
 * @property shape Corner shape of the tooltip bubble (10dp rounded by default).
 * @property foreground Text colour; null uses the palette text colour.
 * @property textStyle Style of the tooltip text (12sp with 17sp leading by default).
 * @property maxWidth Maximum bubble width (280dp); longer text wraps.
 * @property padding Padding inside the bubble (12dp).
 * @property borderColor Hairline border colour; null uses the palette border colour.
 * @property borderWidth Border thickness (1dp).
 * @property shadowElevation Drop shadow depth (6dp).
 * @property innerGlowColor Inner highlight; null uses the palette highlight at 50%.
 * @property hoverDelayMillis Delay in milliseconds before the tooltip appears (350 by default).
 * @property sideOffset Gap between the anchor and the bubble (8dp).
 * @property openAnimationMillis Duration of the enter fade in milliseconds (150); the scale and slide use springs.
 * @property closeAnimationMillis Duration of the exit fade, scale and slide in milliseconds (110).
 * @property enterScale Initial scale the bubble grows from when appearing (0.90).
 * @property exitScale Target scale the bubble shrinks to when disappearing (0.94).
 * @property alpha Whole-bubble opacity.
 * @see MillenniumTooltipTheme
 * @see defaultMillenniumTooltipTheme
 */
@Immutable
data class MillenniumTooltipThemeProperties(
    val background: Brush? = null,
    val shape: Shape,
    val foreground: Color? = null,
    val textStyle: TextStyle,
    val maxWidth: Dp,
    val padding: PaddingValues,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val hoverDelayMillis: Long,
    val sideOffset: Dp,
    val openAnimationMillis: Int = 150,
    val closeAnimationMillis: Int = 110,
    val enterScale: Float = 0.90f,
    val exitScale: Float = 0.94f,
    val alpha: Float,
)

/**
 * Per-state theme of [MillenniumTooltip].
 *
 * @property states Properties per [MillenniumControlState]. [MillenniumTooltip] always reads the default state.
 */
@Immutable
data class MillenniumTooltipTheme(
    val states: MillenniumControlStateProperty<MillenniumTooltipThemeProperties>,
)

/**
 * Creates the default [MillenniumTooltipTheme] installed as `MillenniumTheme.tooltipTheme`.
 *
 * Defaults: a 10dp rounded glass bubble up to 280dp wide, 12dp padding, 6dp shadow, a 350ms hover delay, an 8dp
 * offset from the anchor, and a spring-scaled enter with a brisk fade-out exit. Colours come from the palette.
 *
 * @return A fresh theme instance.
 */
fun defaultMillenniumTooltipTheme(): MillenniumTooltipTheme {
    val base = MillenniumTooltipThemeProperties(
        shape = RoundedCornerShape(10.dp),
        textStyle = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
        maxWidth = 280.dp,
        padding = PaddingValues(12.dp),
        borderWidth = 1.dp,
        shadowElevation = 6.dp,
        hoverDelayMillis = 350,
        sideOffset = 8.dp,
        alpha = 1f,
    )
    return MillenniumTooltipTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base,
            pressed = base.copy(shadowElevation = 3.dp),
            disabled = base.copy(alpha = 0.55f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * Installs the overlay host that tooltips render into.
 *
 * Place it once above all content that contains [MillenniumTooltip] anchors, typically near the root of the app.
 * It is a thin wrapper around Compose Unstyled's `TooltipHost`; it adds no visuals of its own. A tooltip outside of
 * a host has nowhere to draw its panel.
 *
 * Example:
 * ```kotlin
 * MillenniumTooltipHost(Modifier.fillMaxSize()) {
 *     MillenniumTooltip("Play the next track") {
 *         MillenniumIconButton(MillenniumIcons.Music, contentDescription = "Next", onClick = { })
 *     }
 * }
 * ```
 *
 * @param modifier Modifier applied to the host container.
 * @param content The app content that may contain tooltips.
 * @see MillenniumTooltip
 */
@Composable
fun MillenniumTooltipHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    TooltipHost(modifier, content)

/**
 * A glass hint bubble attached to an anchor composable.
 *
 * Wraps Compose Unstyled's `UnstyledTooltip`, which owns when the tooltip appears (pointer hover after
 * `hoverDelayMillis`, and keyboard focus) and where it is placed relative to the anchor. Millennium styles the panel:
 * a pearl/smoked glass surface with border, glow and shadow, entering with a spring scale and slide and leaving with
 * a brisk fade. Use it for short, supplementary text; do not hide essential information in it.
 *
 * Requires a [MillenniumTooltipHost] higher in the composition. Only the default theme state is used.
 *
 * Example:
 * ```kotlin
 * MillenniumTooltip(text = "Save to your library") {
 *     MillenniumButton(onClick = { }) { Text("Save") }
 * }
 * ```
 *
 * @param text Plain text shown in the bubble.
 * @param background Overrides the bubble fill; null uses the theme, then the palette panel brush.
 * @param shape Overrides the bubble shape; null uses the theme.
 * @param foreground Overrides the text colour; null uses the theme, then the palette text colour.
 * @param textStyle Overrides the text style; null uses the theme.
 * @param maxWidth Overrides the maximum bubble width; null uses the theme.
 * @param padding Overrides the bubble padding; null uses the theme.
 * @param borderColor Overrides the border colour; null uses the theme, then the palette border colour.
 * @param borderWidth Overrides the border thickness; null uses the theme.
 * @param shadowElevation Overrides the shadow depth; null uses the theme.
 * @param innerGlowColor Overrides the inner highlight; null uses the theme, then the palette highlight.
 * @param hoverDelayMillis Overrides the hover delay in milliseconds; null uses the theme.
 * @param sideOffset Overrides the anchor gap; null uses the theme.
 * @param openAnimationMillis Overrides the enter fade duration; null uses the theme.
 * @param closeAnimationMillis Overrides the exit animation duration; null uses the theme.
 * @param enterScale Overrides the initial enter scale; null uses the theme.
 * @param exitScale Overrides the exit target scale; null uses the theme.
 * @param alpha Overrides the bubble opacity; null uses the theme.
 * @param theme Per-state theme; defaults to `MillenniumTheme.tooltipTheme`.
 * @param anchor The composable the tooltip describes; hovering or focusing it shows the tooltip.
 * @see MillenniumTooltipHost
 * @see MillenniumTooltipTheme
 */
@Composable
fun MillenniumTooltip(
    text: String,
    background: Brush? = null,
    shape: Shape? = null,
    foreground: Color? = null,
    textStyle: TextStyle? = null,
    maxWidth: Dp? = null,
    padding: PaddingValues? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    hoverDelayMillis: Long? = null,
    sideOffset: Dp? = null,
    openAnimationMillis: Int? = null,
    closeAnimationMillis: Int? = null,
    enterScale: Float? = null,
    exitScale: Float? = null,
    alpha: Float? = null,
    theme: MillenniumTooltipTheme = MillenniumTheme.tooltipTheme,
    anchor: @Composable () -> Unit,
) {
    val properties = theme.states.default
    val colors = MillenniumTheme.colors
    val resolvedBackground = background ?: properties.background ?: millenniumPanelBrush(colors)
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: colors.border
    val resolvedGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.5f)
    val resolvedForeground = foreground ?: properties.foreground ?: colors.text
    val resolvedOpenMillis = openAnimationMillis ?: properties.openAnimationMillis
    val resolvedCloseMillis = closeAnimationMillis ?: properties.closeAnimationMillis
    val resolvedEnterScale = enterScale ?: properties.enterScale
    val resolvedExitScale = exitScale ?: properties.exitScale
    val origin = TransformOrigin(0.5f, 1f)
    UnstyledTooltip(
        hoverDelayMillis = hoverDelayMillis ?: properties.hoverDelayMillis,
        sideOffset = sideOffset ?: properties.sideOffset,
        panel = {
            TooltipPanel(
                modifier = Modifier.widthIn(max = maxWidth ?: properties.maxWidth).millenniumSurface(
                    background = resolvedBackground,
                    shape = shape ?: properties.shape,
                    borderColor = resolvedBorderColor,
                    borderWidth = borderWidth ?: properties.borderWidth,
                    shadowElevation = shadowElevation ?: properties.shadowElevation,
                    innerGlowColor = resolvedGlowColor,
                ).padding(padding ?: properties.padding).alpha(alpha ?: properties.alpha),
                enter = fadeIn(tween(resolvedOpenMillis, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = resolvedEnterScale,
                        transformOrigin = origin,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetY = { it / 4 },
                    ),
                exit = fadeOut(tween(resolvedCloseMillis, easing = FastOutLinearInEasing)) +
                    scaleOut(
                        targetScale = resolvedExitScale,
                        transformOrigin = origin,
                        animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
                    ) +
                    slideOutVertically(
                        animationSpec = tween(resolvedCloseMillis, easing = FastOutLinearInEasing),
                        targetOffsetY = { it / 6 },
                    ),
            ) {
                Text(text, style = textStyle ?: properties.textStyle, color = resolvedForeground)
            }
        },
        anchor = anchor,
    )
}

@Preview
@Composable
private fun MillenniumTooltipPreview() = MillenniumPreview {
    MillenniumTooltip("A hint of the past. Built for today.") {
        MillenniumButton(onClick = {}) { Text("Hover or focus for a hint") }
    }
}
