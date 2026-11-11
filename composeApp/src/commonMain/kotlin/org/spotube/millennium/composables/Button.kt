package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumQuietEnamelStops

/**
 * Visual emphasis of a [MillenniumButton].
 *
 * Each entry selects one of the four independently themed variants of [MillenniumButtonTheme]
 * (`primary`, `secondary`, `quiet`, `danger`).
 *
 * - [Primary]: blue gel capsule with a white label; the single main call to action of a view.
 * - [Secondary]: pearl/enamel bevel with the regular text colour; for ordinary actions next to a primary one.
 * - [Quiet]: transparent at rest, reveals an accent-tinted enamel face on hover/focus/press; for
 *   toolbars and low-emphasis actions.
 * - [Danger]: red gel; for destructive or irreversible actions.
 *
 * @see MillenniumButton
 * @see MillenniumButtonTheme
 */
enum class MillenniumButtonStyle {
    /** Main call to action: accent gel with an on-accent label. */
    Primary,

    /** Standard action: enamel bevel with the regular text colour. */
    Secondary,

    /** Low emphasis: transparent until hovered, focused or pressed. */
    Quiet,

    /** Destructive action: danger-coloured gel. */
    Danger,
}

/**
 * Backwards-friendly name for callers that think of button styles as variants.
 *
 * @see MillenniumButtonStyle
 */
typealias MillenniumButtonVariant = MillenniumButtonStyle

/**
 * Motion settings for button-like controls; shared by [MillenniumButton] and
 * [MillenniumIconButton] (and its theme).
 *
 * The scale values drive a spring on the whole control; the duration drives the colour tweens
 * (border, inner glow, foreground).
 *
 * @property stateChangeMillis Duration in milliseconds of the border/glow/foreground colour tween
 * when the interaction state changes. Default `150`.
 * @property hoverScale Target scale while hovered (enabled only). `1f` disables the hover grow.
 * Default `1.015f`.
 * @property pressedScale Target scale while pressed (enabled only). Values below `1f` give the
 * restrained "push in" feel. Default `0.985f`.
 */
@Immutable
data class MillenniumButtonMotion(
    val stateChangeMillis: Int = 150,
    val hoverScale: Float = 1.015f,
    val pressedScale: Float = 0.985f,
)

/**
 * Complete, state-specific button appearance and layout configuration.
 *
 * One instance describes a single (variant, state) cell; [MillenniumButtonTheme] stores a
 * [MillenniumControlStateProperty] of these per variant. Properties typed as nullable fall back to
 * values derived from the active palette ([MillenniumTheme.colors]) and the variant.
 *
 * @property background Face fill. `null` = variant default (accent gel for Primary, bevel for
 * Secondary, enamel on hover/focus/press or transparent for Quiet, red gel for Danger).
 * @property foreground Label/icon colour. `null` = the text style colour if specified, otherwise
 * on-accent (Primary), surface (Danger) or text colour.
 * @property textStyle Label typography; the default is 14sp / 20sp semi-bold.
 * @property shape Outline of the surface and the interaction lighting (default 10dp rounded
 * rectangle for Primary, 8dp for the others).
 * @property padding Content padding between the surface edge and the content row.
 * @property contentSpacing Horizontal gap between children of the content row. Default `6.dp`.
 * @property minSize Minimum width and height; the default 48 x 44dp keeps the accessible target size.
 * @property borderWidth Hairline border thickness (0 for Quiet at rest).
 * @property borderColor Border colour. `null` = accent when focused/selected, danger when in error,
 * otherwise variant-dependent.
 * @property shadows Drop-shadow colour layers. `null` = a single 16% black layer.
 * @property shadowElevation Drop-shadow elevation; animated with a spring between states.
 * @property innerGlowColor Colour of the top inner highlight. `null` = a translucent palette highlight
 * (or inset colour while pressed).
 * @property innerGlowWidth Thickness of the inner highlight line. Default `1.dp`.
 * @property innerGlowInset Horizontal inset of the straight highlight when it does not follow the shape.
 * Default `8.dp`.
 * @property innerGlowTop Offset of the highlight from the top edge. Default `1.dp`.
 * @property alpha Overall opacity of the control (the default disabled state uses `0.45f`).
 * @property motion Scale and colour-tween configuration, see [MillenniumButtonMotion].
 * @property innerGlowFollowsShape When `true` (default) the inner highlight follows the rounded shape
 * instead of drawing a straight line.
 * @property bottomRimColor Colour of the lower inner rim. `null` = faint palette highlight for
 * every style except Quiet (which has none).
 */
@Immutable
data class MillenniumButtonThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val textStyle: TextStyle,
    val shape: Shape,
    val padding: PaddingValues,
    val contentSpacing: Dp = 6.dp,
    val minSize: DpSize,
    val borderWidth: Dp,
    val borderColor: Color? = null,
    val shadows: List<Color>? = null,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val innerGlowWidth: Dp = 1.dp,
    val innerGlowInset: Dp = 8.dp,
    val innerGlowTop: Dp = 1.dp,
    val alpha: Float = 1f,
    val motion: MillenniumButtonMotion = MillenniumButtonMotion(),
    val innerGlowFollowsShape: Boolean = true,
    val bottomRimColor: Color? = null,
)

/**
 * Theme for [MillenniumButton]: four independently configurable variants, each with all control states.
 *
 * Installed as [MillenniumTheme.buttonTheme]; replace it for a subtree or pass an instance to a
 * single button through its `theme` parameter.
 *
 * ```kotlin
 * val base = defaultMillenniumButtonTheme()
 * val compact = base.copy(
 *     primary = base.primary.copy(
 *         default = base.primary.default.copy(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)),
 *     ),
 * )
 * MillenniumButton(onClick = {}, theme = compact) { Text("Compact") }
 * ```
 *
 * @property primary State properties for [MillenniumButtonStyle.Primary].
 * @property secondary State properties for [MillenniumButtonStyle.Secondary].
 * @property quiet State properties for [MillenniumButtonStyle.Quiet].
 * @property danger State properties for [MillenniumButtonStyle.Danger].
 * @see defaultMillenniumButtonTheme
 */
@Immutable
data class MillenniumButtonTheme(
    val primary: MillenniumControlStateProperty<MillenniumButtonThemeProperties>,
    val secondary: MillenniumControlStateProperty<MillenniumButtonThemeProperties>,
    val quiet: MillenniumControlStateProperty<MillenniumButtonThemeProperties>,
    val danger: MillenniumControlStateProperty<MillenniumButtonThemeProperties>,
)

/**
 * Creates the default [MillenniumButtonTheme].
 *
 * All four variants share a 14sp semi-bold label, 18 x 10dp padding, a 48 x 44dp minimum size and a
 * 1dp border (Quiet has none at rest and gains one on hover). Primary uses a 10dp corner radius, the
 * others 8dp. Shadow elevation is 5dp at rest, 7dp hovered and 0dp pressed/disabled (Quiet has none at
 * rest); focused, selected and error states use a 2dp border; disabled uses `alpha = 0.45f`. Colours are
 * left `null` so they resolve from the active palette.
 *
 * Customise with `copy` on the returned theme or on individual state properties.
 *
 * @return A new theme with default values for every variant and state.
 * @see MillenniumButtonTheme
 */
fun defaultMillenniumButtonTheme(): MillenniumButtonTheme {
    fun variant(style: MillenniumButtonStyle): MillenniumControlStateProperty<MillenniumButtonThemeProperties> {
        val isQuiet = style == MillenniumButtonStyle.Quiet
        val base = MillenniumButtonThemeProperties(
            textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            shape = RoundedCornerShape(if (style == MillenniumButtonStyle.Primary) 10.dp else 8.dp),
            padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            minSize = DpSize(48.dp, 44.dp),
            borderWidth = if (isQuiet) 0.dp else 1.dp,
            shadowElevation = if (isQuiet) 0.dp else 5.dp,
        )
        return MillenniumControlStateProperty(
            default = base,
            hovered = base.copy(borderWidth = if (isQuiet) 1.dp else base.borderWidth, shadowElevation = 7.dp),
            focused = base.copy(borderWidth = 2.dp),
            pressed = base.copy(borderWidth = maxOf(1.dp, base.borderWidth), shadowElevation = 0.dp),
            disabled = base.copy(alpha = 0.45f, shadowElevation = 0.dp),
            selected = base.copy(borderWidth = 2.dp),
            error = base.copy(borderWidth = 2.dp),
        )
    }

    return MillenniumButtonTheme(
        primary = variant(MillenniumButtonStyle.Primary),
        secondary = variant(MillenniumButtonStyle.Secondary),
        quiet = variant(MillenniumButtonStyle.Quiet),
        danger = variant(MillenniumButtonStyle.Danger),
    )
}

private fun MillenniumButtonTheme.forStyle(style: MillenniumButtonStyle) = when (style) {
    MillenniumButtonStyle.Primary -> primary
    MillenniumButtonStyle.Secondary -> secondary
    MillenniumButtonStyle.Quiet -> quiet
    MillenniumButtonStyle.Danger -> danger
}

/**
 * A glossy Aero button with four emphasis levels ([MillenniumButtonStyle]).
 *
 * Use it for any text (or text plus icon) action. Built on Compose Unstyled `UnstyledButton`, so
 * click, keyboard activation (Enter/Space), button semantics and the disabled state come from the
 * primitive; Millennium adds the gel/enamel surface, hover and press lighting and spring motion.
 *
 * Behaviour:
 * - The interaction state resolves in the order disabled, pressed, keyboard-focused, hovered, default.
 *   The focus ring uses `collectIsKeyboardFocusedAsState`, so it appears only for keyboard/directional
 *   navigation and not after a mouse or touch press.
 * - The surface scales with a spring (hover grows, press shrinks, per [MillenniumButtonMotion]); the
 *   border and inner-glow colours cross-fade; the shadow elevation animates.
 * - The minimum size defaults to 48 x 44dp, meeting the 44dp touch-target guideline. The hand cursor
 *   covers content and padding when enabled.
 * - Primary labels get a subtle text shadow in the accent colour; gel stops are contrast-tested for
 *   white labels.
 * - The [content] is placed in a centered [Row] with [contentSpacing] between children and inherits
 *   the resolved text style and foreground colour, so plain `Text` and icons follow the variant.
 *
 * Every nullable parameter overrides a single theme property for this instance; it resolves as
 * `param ?: themeValue ?: fallback`, so passing nothing keeps the theme's look.
 *
 * ```kotlin
 * var saved by remember { mutableStateOf(false) }
 *
 * Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
 *     MillenniumButton(onClick = { saved = true }) { Text(if (saved) "Saved" else "Save") }
 *     MillenniumButton(onClick = { saved = false }, style = MillenniumButtonStyle.Secondary) {
 *         Text("Undo")
 *     }
 *     MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Danger, enabled = saved) {
 *         Text("Delete")
 *     }
 * }
 * ```
 *
 * @param onClick Called when the button is activated by click, tap or keyboard. Not called when disabled.
 * @param modifier Modifier applied to the outer button (before the Millennium surface is added).
 * @param enabled When `false` the button ignores input, is exposed as disabled, uses the theme's
 * disabled state (default 45% alpha) and does not show a hand cursor.
 * @param loading When `true` the button behaves as disabled and an indeterminate spinner in the
 * foreground colour replaces [content]; the content stays laid out (invisibly) so the size is kept.
 * @param style Emphasis variant; selects which of the theme's four variants is used.
 * @param background Overrides the face brush; `null` = the theme's brush or the variant default.
 * @param foreground Overrides the label/icon colour; `null` falls back to the theme or the variant default.
 * @param textStyle Overrides the label typography; `null` falls back to the theme's style.
 * @param shape Overrides the outline shape; `null` falls back to the theme.
 * @param padding Overrides the content padding; `null` falls back to the theme.
 * @param contentSpacing Overrides the gap between content children; `null` falls back to the theme.
 * @param minSize Overrides the minimum width/height; `null` falls back to the theme (48 x 44dp).
 * Reducing it below 44dp height weakens the accessible target.
 * @param borderWidth Overrides the border thickness for every state; `null` falls back to the state's value.
 * @param borderColor Overrides the border colour; `null` falls back to the theme or the palette-derived colour.
 * @param shadowColors Overrides the drop-shadow colour layers; `null` falls back to the theme or 16% black.
 * @param shadowElevation Overrides the shadow elevation for every state; `null` falls back to the state's value.
 * @param innerGlowColor Overrides the inner highlight colour; `null` falls back to the theme or palette highlight.
 * @param innerGlowWidth Overrides the inner highlight thickness; `null` falls back to the theme.
 * @param innerGlowInset Overrides the horizontal inset of a straight highlight; `null` falls back to the theme.
 * @param innerGlowTop Overrides the highlight's offset from the top edge; `null` falls back to the theme.
 * @param innerGlowFollowsShape Overrides whether the highlight follows the shape; `null` falls back to the theme.
 * @param bottomRimColor Overrides the lower inner rim colour; `null` falls back to the theme or a
 * faint highlight (none for Quiet).
 * @param alpha Overrides the overall opacity; `null` falls back to the state's value.
 * @param motion Overrides hover/press scale and tween duration; `null` falls back to the theme.
 * @param theme Theme supplying the variant/state properties; defaults to [MillenniumTheme.buttonTheme].
 * @param content Button content, laid out in a centered row; usually `Text` and/or an icon.
 * @see MillenniumIconButton
 * @see MillenniumButtonStyle
 * @see MillenniumButtonTheme
 */
@Composable
fun MillenniumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: MillenniumButtonStyle = MillenniumButtonStyle.Primary,
    background: Brush? = null,
    foreground: Color? = null,
    textStyle: TextStyle? = null,
    shape: Shape? = null,
    padding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    minSize: DpSize? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    shadowColors: List<Color>? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    innerGlowWidth: Dp? = null,
    innerGlowInset: Dp? = null,
    innerGlowTop: Dp? = null,
    innerGlowFollowsShape: Boolean? = null,
    bottomRimColor: Color? = null,
    alpha: Float? = null,
    motion: MillenniumButtonMotion? = null,
    theme: MillenniumButtonTheme = MillenniumTheme.buttonTheme,
    content: @Composable () -> Unit,
) {
    val interactive = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val state = when {
        !interactive -> MillenniumControlState.Disabled
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val properties = theme.forStyle(style).resolve(state)
    val colors = MillenniumTheme.colors
    val resolvedMotion = motion ?: properties.motion
    val resolvedTextStyle = textStyle ?: properties.textStyle
    val resolvedForeground = foreground
        ?: properties.foreground
        ?: resolvedTextStyle.color.takeUnless { it == Color.Unspecified }
        ?: when (style) {
            MillenniumButtonStyle.Primary -> colors.onAccent
            MillenniumButtonStyle.Danger -> colors.surface
            else -> colors.text
        }
    val resolvedBackground = background ?: properties.background ?: when (style) {
        MillenniumButtonStyle.Primary -> gelBrush(colors.accentTop, colors.accentBottom)
        MillenniumButtonStyle.Secondary -> millenniumBevelBrush(colors)
        MillenniumButtonStyle.Quiet -> if (state == MillenniumControlState.Hovered || state == MillenniumControlState.Focused || state == MillenniumControlState.Pressed) {
            Brush.verticalGradient(*millenniumQuietEnamelStops(colors, pressed).toTypedArray())
        } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        MillenniumButtonStyle.Danger -> gelBrush(
            colors.danger,
            lerp(colors.danger, if (colors.surface.luminance() > colors.danger.luminance()) Color.Black else Color.White, 0.20f),
        )
    }
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: when (state) {
        MillenniumControlState.Focused, MillenniumControlState.Selected -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> when (style) {
            MillenniumButtonStyle.Primary -> colors.accentBottom
            MillenniumButtonStyle.Secondary -> colors.border
            MillenniumButtonStyle.Quiet -> if (hovered || pressed) colors.accent.copy(alpha = 0.55f) else Color.Transparent
            MillenniumButtonStyle.Danger -> colors.danger
        }
    }
    val resolvedInnerGlowColor = innerGlowColor ?: properties.innerGlowColor ?: when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused -> colors.highlight.copy(alpha = 0.60f)
        MillenniumControlState.Pressed -> colors.inset.copy(alpha = 0.32f)
        else -> colors.highlight.copy(alpha = 0.70f)
    }
    val resolvedShadowColors = shadowColors ?: properties.shadows ?: listOf(Color.Black.copy(alpha = 0.16f))
    val targetScale = when {
        !interactive -> 1f
        pressed -> resolvedMotion.pressedScale
        hovered -> resolvedMotion.hoverScale
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
    )
    val elevation by animateDpAsState(
        targetValue = shadowElevation ?: properties.shadowElevation,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = resolvedBorderColor,
        animationSpec = tween(resolvedMotion.stateChangeMillis),
    )
    val animatedInnerGlowColor by animateColorAsState(
        targetValue = resolvedInnerGlowColor,
        animationSpec = tween(resolvedMotion.stateChangeMillis),
    )
    val resolvedMinSize = minSize ?: properties.minSize
    UnstyledButton(
        onClick = onClick,
        enabled = interactive,
        interactionSource = interaction,
        indication = null,
        modifier = modifier
            .millenniumActionPointer(interactive)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .defaultMinSize(minWidth = resolvedMinSize.width, minHeight = resolvedMinSize.height)
            .alpha(alpha ?: properties.alpha)
            .millenniumSurface(
                background = resolvedBackground,
                shape = shape ?: properties.shape,
                borderColor = animatedBorderColor,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = elevation,
                opaqueFill = style != MillenniumButtonStyle.Quiet || state != MillenniumControlState.Default,
                shadowColors = resolvedShadowColors,
                innerGlowColor = animatedInnerGlowColor,
                innerGlowWidth = innerGlowWidth ?: properties.innerGlowWidth,
                innerGlowInset = innerGlowInset ?: properties.innerGlowInset,
                innerGlowTop = innerGlowTop ?: properties.innerGlowTop,
                innerGlowFollowsShape = innerGlowFollowsShape ?: properties.innerGlowFollowsShape,
                bottomRimColor = bottomRimColor ?: properties.bottomRimColor
                    ?: if (style != MillenniumButtonStyle.Quiet) colors.highlight.copy(alpha = 0.28f) else null,
            )
            .millenniumInteractionSurface(state, shape ?: properties.shape,
                tint = if (style == MillenniumButtonStyle.Danger) colors.danger else colors.accent,
                animationMillis = resolvedMotion.stateChangeMillis),
        contentPadding = padding ?: properties.padding,
    ) {
        val labelStyle = if (style == MillenniumButtonStyle.Primary && resolvedTextStyle.shadow == null) {
            resolvedTextStyle.copy(shadow = Shadow(colors.accentBottom, Offset(0f, 1f), 1f))
        } else resolvedTextStyle
        MillenniumStyledContent(labelStyle, resolvedForeground) {
            MillenniumLoadingContent(loading, resolvedForeground, spinnerSize = 18.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(contentSpacing ?: properties.contentSpacing),
                ) { content() }
            }
        }
    }
}

/**
 * Internal: while [loading], hides [content] (still measured, so the button keeps its size) under an
 * indeterminate spinner drawn in [color]. The spinner is silent to accessibility; the button itself is
 * reported disabled. Shared by [MillenniumButton] and [MillenniumIconButton].
 */
@Composable
internal fun MillenniumLoadingContent(
    loading: Boolean,
    color: Color,
    spinnerSize: Dp,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.alpha(if (loading) 0f else 1f)) { content() }
        if (loading) {
            Box(Modifier.clearAndSetSemantics {}) {
                MillenniumCircularProgress(
                    size = spinnerSize,
                    thickness = spinnerSize / 7,
                    color = color,
                    trackColor = color.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MillenniumButtonPreview() = MillenniumPreview {
    MillenniumButton(onClick = {}) { Text("Continue") }
}
