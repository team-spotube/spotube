package org.spotube.millennium.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme

/**
 * Named text styles shared across Millennium components.
 *
 * These are the typographic roles of the design system. [MillenniumText] reads [body]; the other roles are
 * available to callers (for example `MillenniumTheme.textTheme.states.default.title`) and to component themes.
 * All default styles use [FontFamily.SansSerif].
 *
 * @property body Running text, 14sp with 21sp leading; the base style of [MillenniumText].
 * @property label Form and control labels, 14sp Medium with 20sp leading.
 * @property button Button captions, 14sp SemiBold with 20sp leading.
 * @property title Section titles, 22sp SemiBold with 29sp leading.
 * @property headline Large headings, 32sp Bold with 40sp leading.
 * @property caption Secondary and helper text, 12sp with 17sp leading.
 * @property avatar Base style for avatar initials (Bold, no size: the avatar scales the size from its diameter).
 * @see MillenniumTextTheme
 * @see defaultMillenniumTextTheme
 */
@Immutable
data class MillenniumTextThemeProperties(
    val body: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val caption: TextStyle,
    val avatar: TextStyle,
)

/**
 * Theme of the shared typography scale.
 *
 * @property states Typography per [MillenniumControlState]. The default factory uses
 * [MillenniumControlStateProperty.all], so every state shares the same styles; [MillenniumText] resolves the
 * Default state.
 * @see MillenniumTextThemeProperties
 */
@Immutable
data class MillenniumTextTheme(
    val states: MillenniumControlStateProperty<MillenniumTextThemeProperties>,
)

/**
 * Creates the default [MillenniumTextTheme] installed as `MillenniumTheme.textTheme`.
 *
 * All states share one set of styles (body, label, button, title, headline, caption, avatar) on the platform
 * sans-serif font. Replace roles with `copy` to change the font family or scale:
 *
 * ```kotlin
 * val base = defaultMillenniumTextTheme().states.default
 * val theme = MillenniumTextTheme(
 *     MillenniumControlStateProperty.all(base.copy(body = base.body.copy(fontFamily = FontFamily.Serif))),
 * )
 * MillenniumText("Serif body", theme = theme)
 * ```
 *
 * @return A fresh theme instance.
 */
fun defaultMillenniumTextTheme() = MillenniumTextTheme(
    states = MillenniumControlStateProperty.all(
        MillenniumTextThemeProperties(
            body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
            label = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            button = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            title = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
            headline = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
            caption = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
            avatar = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        ),
    ),
)

/**
 * Themed plain text built on Compose Unstyled's `Text`.
 *
 * Use it for ordinary strings in Millennium layouts so they pick up the palette text colour, the body font family
 * and a consistent leading. Colour, size and weight have explicit parameters; everything else comes from [style]
 * or the theme's body style.
 *
 * Leading: unless [lineHeight] is given, the line height is the body style's leading scaled by
 * `size / body.fontSize`, so larger explicit sizes do not produce overlapping lines.
 *
 * Example:
 * ```kotlin
 * MillenniumText("Now playing", size = 22.sp, weight = FontWeight.SemiBold)
 * MillenniumText("Pearl surfaces, a fine bevel.", color = MillenniumTheme.colors.muted)
 * ```
 *
 * @param text The string to render.
 * @param modifier Modifier applied to the text.
 * @param color Text colour; defaults to the palette text colour. Unlike other components this is a non-null
 * parameter with a palette default rather than a theme override.
 * @param size Font size; defaults to the body style's size (14sp).
 * @param weight Font weight; defaults to the body style's weight or Normal.
 * @param style Optional style whose line height and font family are used as the base instead of the theme body style.
 * Its colour, size and weight are not applied; use the explicit parameters for those.
 * @param lineHeight Explicit line height; null scales the base leading with [size].
 * @param theme Typography theme; defaults to `MillenniumTheme.textTheme`. Only its Default-state body style is read.
 * @see MillenniumTextTheme
 * @see MillenniumStyledContent
 */
@Composable
fun MillenniumText(
    text: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color = MillenniumTheme.colors.text,
    size: TextUnit = MillenniumTheme.textTheme.states.default.body.fontSize,
    weight: FontWeight = MillenniumTheme.textTheme.states.default.body.fontWeight ?: FontWeight.Normal,
    style: TextStyle? = null,
    lineHeight: TextUnit? = null,
    theme: MillenniumTextTheme = MillenniumTheme.textTheme,
) {
    val baseStyle = style ?: theme.states.resolve(MillenniumControlState.Default).body
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = lineHeight ?: millenniumTextLineHeight(size, baseStyle.fontSize, baseStyle.lineHeight),
        fontWeight = weight,
        fontFamily = baseStyle.fontFamily,
    )
}

/**
 * Scales the configured leading with an explicit font size instead of letting lines overlap.
 *
 * Returns `size * baseLineHeight / baseSize` when all three values are in `sp` and [baseSize] is positive;
 * otherwise returns [baseLineHeight] unchanged.
 *
 * @param size The font size actually used for the text.
 * @param baseSize The font size the base leading was designed for.
 * @param baseLineHeight The base style's line height.
 * @return The line height to use for [size].
 */
internal fun millenniumTextLineHeight(size: TextUnit, baseSize: TextUnit, baseLineHeight: TextUnit): TextUnit =
    if (size.isSp && baseSize.isSp && baseLineHeight.isSp && baseSize.value > 0f) {
        (size.value * baseLineHeight.value / baseSize.value).sp
    } else baseLineHeight
