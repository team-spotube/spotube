package org.spotube.millennium.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledVerticalSeparator
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumPreview

/**
 * Visual properties of a [MillenniumSeparator].
 *
 * @property color Line colour; null uses the palette border colour.
 * @property thickness Line thickness (1dp by default).
 * @property alpha Opacity of the separator.
 * @property highlightColor Colour of the light line drawn on the trailing edge (bottom or right); null uses the
 * palette highlight at 65%.
 * @property highlightWidth Thickness of that highlight line (0.5dp by default); 0dp disables it.
 * @see MillenniumSeparatorTheme
 * @see defaultMillenniumSeparatorTheme
 */
@Immutable
data class MillenniumSeparatorThemeProperties(
    val color: androidx.compose.ui.graphics.Color? = null,
    val thickness: Dp,
    val alpha: Float,
    val highlightColor: Color? = null,
    val highlightWidth: Dp = 0.5.dp,
)

/**
 * Per-state theme of [MillenniumSeparator].
 *
 * @property states Properties per [MillenniumControlState]; [MillenniumSeparator] resolves the Default state.
 */
@Immutable
data class MillenniumSeparatorTheme(
    val states: MillenniumControlStateProperty<MillenniumSeparatorThemeProperties>,
)

/**
 * Creates the default [MillenniumSeparatorTheme] installed as `MillenniumTheme.separatorTheme`.
 *
 * Defaults: a 1dp palette-border line paired with a 0.5dp highlight, giving the engraved "paired hairline" look.
 *
 * @return A fresh theme instance.
 */
fun defaultMillenniumSeparatorTheme(): MillenniumSeparatorTheme {
    val base = MillenniumSeparatorThemeProperties(thickness = 1.dp, alpha = 1f)
    return MillenniumSeparatorTheme(
        MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(thickness = 2.dp),
            pressed = base.copy(alpha = 0.7f),
            disabled = base.copy(alpha = 0.4f),
            selected = base,
            error = base,
        ),
    )
}

/**
 * A hairline divider with a paired highlight line, horizontal by default.
 *
 * Built on Compose Unstyled's horizontal and vertical separators. The dark line is drawn first and a thin light
 * highlight is drawn on the trailing edge (bottom for horizontal, right for vertical) so the rule looks engraved.
 * A horizontal separator fills the available width; give a vertical one a height, for example `Modifier.height(28.dp)`.
 *
 * Example:
 * ```kotlin
 * Column {
 *     MillenniumText("Above")
 *     MillenniumSeparator()
 *     MillenniumText("Below")
 * }
 * Row {
 *     MillenniumText("Left")
 *     MillenniumSeparator(Modifier.height(24.dp), vertical = true)
 *     MillenniumText("Right")
 * }
 * ```
 *
 * @param modifier Modifier applied to the separator.
 * @param vertical When true draws a vertical divider; otherwise a horizontal one.
 * @param color Overrides the line colour; null uses the theme, then the palette border colour.
 * @param thickness Overrides the line thickness; null uses the theme.
 * @param alpha Overrides the opacity; null uses the theme.
 * @param highlightColor Overrides the trailing highlight colour; null uses the theme, then the palette highlight.
 * @param highlightWidth Overrides the highlight thickness; null uses the theme. Zero or less hides it.
 * @param theme Per-state theme; defaults to `MillenniumTheme.separatorTheme`.
 * @see MillenniumSeparatorTheme
 */
@Composable
fun MillenniumSeparator(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    color: Color? = null,
    thickness: Dp? = null,
    alpha: Float? = null,
    highlightColor: Color? = null,
    highlightWidth: Dp? = null,
    theme: MillenniumSeparatorTheme = MillenniumTheme.separatorTheme,
) {
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val color = color ?: properties.color ?: MillenniumTheme.colors.border
    val highlight = highlightColor ?: properties.highlightColor ?: MillenniumTheme.colors.highlight.copy(alpha = 0.65f)
    val highlightWidth = highlightWidth ?: properties.highlightWidth
    val beveledModifier = modifier.alpha(alpha ?: properties.alpha).drawWithContent {
        drawContent()
        if (highlightWidth > 0.dp) {
            val stroke = highlightWidth.toPx()
            if (vertical) {
                val x = size.width - stroke / 2f
                drawLine(highlight, Offset(x, 0f), Offset(x, size.height), stroke)
            } else {
                val y = size.height - stroke / 2f
                drawLine(highlight, Offset(0f, y), Offset(size.width, y), stroke)
            }
        }
    }
    if (vertical) {
        UnstyledVerticalSeparator(color, beveledModifier, thickness ?: properties.thickness)
    } else {
        UnstyledHorizontalSeparator(color, beveledModifier, thickness ?: properties.thickness)
    }
}

@Preview
@Composable
private fun MillenniumSeparatorPreview() = MillenniumPreview {
    MillenniumText("Above the line")
    MillenniumSeparator()
    MillenniumText("Below the line")
    Column {
        MillenniumText("Leading edge")
        MillenniumSeparator(Modifier.height(28.dp), vertical = true)
        MillenniumText("Trailing edge")
    }
}
