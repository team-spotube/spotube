package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledIcon
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumQuietEnamelStops

/**
 * Visual emphasis of a [MillenniumIconButton]; each entry selects one variant of
 * [MillenniumIconButtonTheme].
 *
 * @see MillenniumIconButton
 */
enum class MillenniumIconButtonStyle {
    /** Main action: circular accent glass orb with a white icon. Use once per view. */
    Primary,

    /** Standard action: pearl enamel face with the regular text colour. */
    Secondary,

    /** Low emphasis: transparent at rest, glassy accent-tinted enamel on hover/focus/press. For toolbars. */
    Quiet,

    /** Destructive action: red glass orb with a white icon. */
    Danger,
}

/**
 * Complete, state-specific icon-button appearance and layout configuration.
 *
 * One instance describes a single (variant, state) cell of [MillenniumIconButtonTheme]. Nullable
 * properties are derived from the active palette when `null`.
 *
 * @property background Face brush. `null` = orb body stops for orb variants, otherwise accent gel
 * (Primary), bevel (Secondary), animated enamel (Quiet) or red gel (Danger).
 * @property foreground Icon/content colour. `null` = white for orb variants, on-accent/surface for
 * non-orb Primary/Danger, accent while Quiet is hovered/focused/pressed, otherwise the text colour.
 * @property shape Outline of the surface; default [CircleShape].
 * @property size Fixed width and height of the button; default 44 x 44dp (the minimum accessible target).
 * @property iconSize Size of an [ImageVector] icon; default `20.dp`.
 * @property contentPadding Padding value kept for theme completeness; the button currently lays content
 * out with zero padding and centers it.
 * @property borderWidth Border thickness (0 for Quiet at rest, 1dp otherwise, 2dp when focused).
 * @property borderColor Border colour. `null` = accent when focused/selected, danger in error, else a
 * variant-specific colour. For orbs a metallic rim is drawn when `null` and not keyboard-focused.
 * @property shadows Drop-shadow colour layers. `null` = a single 16% black layer.
 * @property shadowElevation Drop-shadow elevation, spring-animated between states.
 * @property innerGlowColor Top inner highlight colour. `null` = palette highlight for non-orb,
 * non-Quiet variants and none for orbs and Quiet.
 * @property bottomRimColor Lower inner rim colour. `null` = faint highlight for non-orb, non-Quiet variants.
 * @property orb Draws the spherical glass reflections used by filled variants.
 * @property orbColor Colour of the orb reflections. `null` = white at [orbStrength] alpha.
 * @property orbStrength Alpha of the white reflections when [orbColor] is `null`; raised on hover and
 * reduced on press in the defaults.
 * @property alpha Overall opacity (the default disabled state uses `0.45f`).
 * @property motion Motion settings; defaults to no hover scale-up in the theme (the composable still
 * enforces at least `1.04f` on hover) and a `0.94f` press scale.
 */
@Immutable
data class MillenniumIconButtonThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape,
    val size: DpSize,
    val iconSize: Dp,
    val contentPadding: PaddingValues,
    val borderWidth: Dp,
    val borderColor: Color? = null,
    val shadows: List<Color>? = null,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val bottomRimColor: Color? = null,
    /** Draws the spherical glass reflections used by filled variants. */
    val orb: Boolean = false,
    val orbColor: Color? = null,
    val orbStrength: Float = 0.55f,
    val alpha: Float = 1f,
    val motion: MillenniumButtonMotion = MillenniumButtonMotion(hoverScale = 1.04f, pressedScale = 0.94f),
)

/**
 * Theme for [MillenniumIconButton]: four independently configurable variants, each with all control states.
 *
 * Installed as [MillenniumTheme.iconButtonTheme]; pass an instance to a single button through its
 * `theme` parameter to override it locally.
 *
 * ```kotlin
 * val base = defaultMillenniumIconButtonTheme()
 * val big = base.copy(
 *     secondary = base.secondary.copy(
 *         default = base.secondary.default.copy(size = DpSize(56.dp, 56.dp), iconSize = 26.dp),
 *     ),
 * )
 * ```
 *
 * @property primary State properties for [MillenniumIconButtonStyle.Primary].
 * @property secondary State properties for [MillenniumIconButtonStyle.Secondary].
 * @property quiet State properties for [MillenniumIconButtonStyle.Quiet].
 * @property danger State properties for [MillenniumIconButtonStyle.Danger].
 * @see defaultMillenniumIconButtonTheme
 */
@Immutable
data class MillenniumIconButtonTheme(
    val primary: MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>,
    val secondary: MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>,
    val quiet: MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>,
    val danger: MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>,
)

/**
 * Creates the default [MillenniumIconButtonTheme].
 *
 * Every variant is a 44 x 44dp [CircleShape] with a 20dp icon. Primary and Danger enable the glass
 * orb (strength 0.48, 0.62 on hover, 80% of rest on press); Secondary is enamel; Quiet has no border
 * or shadow at rest. Shadows are 5dp at rest (3dp hovered for non-filled, 7dp for filled), 0dp when
 * pressed or disabled; focused/selected/error use a 2dp border; disabled uses `alpha = 0.45f`.
 *
 * Customise with `copy` on the result or on individual state properties.
 *
 * @return A new theme with default values for every variant and state.
 * @see MillenniumIconButtonTheme
 */
fun defaultMillenniumIconButtonTheme(): MillenniumIconButtonTheme {
    fun variant(style: MillenniumIconButtonStyle): MillenniumControlStateProperty<MillenniumIconButtonThemeProperties> {
        val filled = style == MillenniumIconButtonStyle.Primary || style == MillenniumIconButtonStyle.Danger
        val base = MillenniumIconButtonThemeProperties(
            shape = CircleShape,
            size = DpSize(44.dp, 44.dp),
            iconSize = 20.dp,
            contentPadding = PaddingValues(10.dp),
            borderWidth = when {
                style == MillenniumIconButtonStyle.Quiet -> 0.dp
                filled -> 1.dp
                else -> 1.dp
            },
            shadowElevation = if (style == MillenniumIconButtonStyle.Quiet) 0.dp else 5.dp,
            orb = filled,
            orbStrength = 0.48f,
        )
        return MillenniumControlStateProperty(
            default = base,
            hovered = base.copy(borderWidth = 1.dp, shadowElevation = if (filled) 7.dp else 3.dp, orbStrength = 0.62f),
            focused = base.copy(borderWidth = maxOf(base.borderWidth, 2.dp)),
            pressed = base.copy(borderWidth = maxOf(base.borderWidth, 1.dp), shadowElevation = 0.dp, orbStrength = base.orbStrength * 0.8f),
            disabled = base.copy(alpha = 0.45f, shadowElevation = 0.dp),
            selected = base.copy(borderWidth = 2.dp),
            error = base.copy(borderWidth = 2.dp),
        )
    }

    return MillenniumIconButtonTheme(
        primary = variant(MillenniumIconButtonStyle.Primary),
        secondary = variant(MillenniumIconButtonStyle.Secondary),
        quiet = variant(MillenniumIconButtonStyle.Quiet),
        danger = variant(MillenniumIconButtonStyle.Danger),
    )
}

private fun MillenniumIconButtonTheme.forStyle(style: MillenniumIconButtonStyle) = when (style) {
    MillenniumIconButtonStyle.Primary -> primary
    MillenniumIconButtonStyle.Secondary -> secondary
    MillenniumIconButtonStyle.Quiet -> quiet
    MillenniumIconButtonStyle.Danger -> danger
}

/**
 * A compact, circular control holding a single icon. Filled variants
 * ([MillenniumIconButtonStyle.Primary], [MillenniumIconButtonStyle.Danger]) carry the
 * Vista-style glass orb; [MillenniumIconButtonStyle.Secondary] is enamel and
 * [MillenniumIconButtonStyle.Quiet] is transparent at rest and becomes glassy enamel on hover/press.
 *
 * Built on Compose Unstyled `UnstyledButton`, so click, keyboard activation and button semantics come
 * from the primitive. The state resolves as disabled, pressed, keyboard-focused, hovered, default; the
 * focus ring uses `collectIsKeyboardFocusedAsState` and is therefore not drawn after pointer presses.
 * The default target is 44 x 44dp. Hover scales up (at least `1.04f`) and press shrinks via springs;
 * colours cross-fade. [contentDescription] is applied as the semantics description when non-null;
 * icon-only buttons should always supply one.
 *
 * Every nullable parameter overrides one theme property for this instance and resolves as
 * `param ?: themeValue ?: fallback`.
 *
 * ```kotlin
 * var favorite by remember { mutableStateOf(false) }
 * MillenniumIconButton(
 *     icon = MillenniumIcons.Sparkle,
 *     contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
 *     onClick = { favorite = !favorite },
 *     style = if (favorite) MillenniumIconButtonStyle.Primary else MillenniumIconButtonStyle.Secondary,
 * )
 * ```
 *
 * @param icon Vector drawn centered at [iconSize] in the resolved foreground colour.
 * @param contentDescription Accessibility label for the button; pass `null` only when the surrounding
 * UI already labels it.
 * @param onClick Called on click, tap or keyboard activation; not called when disabled.
 * @param modifier Modifier applied to the button.
 * @param enabled When `false` the button ignores input, uses the disabled state and shows no hand cursor.
 * @param loading When `true` the button behaves as disabled and an indeterminate spinner replaces the icon.
 * @param style Emphasis variant, see [MillenniumIconButtonStyle].
 * @param background Overrides the face brush; `null` falls back to the theme or the variant default.
 * @param foreground Overrides the icon colour; `null` falls back to the theme or the variant default.
 * @param shape Overrides the outline; `null` falls back to the theme ([CircleShape]).
 * @param size Overrides the fixed button size; `null` falls back to the theme (44 x 44dp).
 * @param iconSize Overrides the icon size; `null` falls back to the theme (20dp).
 * @param borderWidth Overrides the border thickness; `null` falls back to the state's value.
 * @param borderColor Overrides the border colour; `null` falls back to the theme or palette.
 * @param shadows Overrides the shadow colour layers; `null` falls back to the theme or 16% black.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the state's value.
 * @param innerGlowColor Overrides the top inner highlight; `null` falls back to the theme or palette.
 * @param bottomRimColor Overrides the lower inner rim; `null` falls back to the theme or palette.
 * @param orb Forces the glass orb on or off; `null` falls back to the theme (on for Primary/Danger).
 * @param orbColor Overrides the orb reflection colour; `null` falls back to the theme or white.
 * @param orbStrength Overrides the white reflection alpha; `null` falls back to the state's value.
 * @param alpha Overrides the overall opacity; `null` falls back to the state's value.
 * @param motion Overrides scale and tween settings; `null` falls back to the theme.
 * @param theme Theme supplying variant/state properties; defaults to [MillenniumTheme.iconButtonTheme].
 * @see MillenniumButton
 * @see MillenniumIconButtonStyle
 */
@Composable
fun MillenniumIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: MillenniumIconButtonStyle = MillenniumIconButtonStyle.Primary,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    size: DpSize? = null,
    iconSize: Dp? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    shadows: List<Color>? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    bottomRimColor: Color? = null,
    orb: Boolean? = null,
    orbColor: Color? = null,
    orbStrength: Float? = null,
    alpha: Float? = null,
    motion: MillenniumButtonMotion? = null,
    theme: MillenniumIconButtonTheme = MillenniumTheme.iconButtonTheme,
) {
    IconButtonContainer(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = style,
        icon = icon,
        contentDescription = contentDescription,
        content = null,
        theme = theme,
        background = background,
        foreground = foreground,
        shape = shape,
        size = size,
        iconSize = iconSize,
        borderWidth = borderWidth,
        borderColor = borderColor,
        shadows = shadows,
        shadowElevation = shadowElevation,
        innerGlowColor = innerGlowColor,
        bottomRimColor = bottomRimColor,
        orb = orb,
        orbColor = orbColor,
        orbStrength = orbStrength,
        alpha = alpha,
        motion = motion,
    )
}

/**
 * Fully custom icon-button content; inherits the variant foreground through `LocalTextStyle`.
 *
 * Use this overload for glyph text, badges or composed icons. It shares all surface, interaction,
 * motion and accessibility behaviour with the [ImageVector] overload; the [content] is centered in the
 * button and receives the resolved foreground colour (and the default text style) through
 * `MillenniumStyledContent`. [iconSize] is accepted for parity but is only applied to the
 * [ImageVector] overload.
 *
 * ```kotlin
 * var count by remember { mutableStateOf(0) }
 * MillenniumIconButton(
 *     onClick = { count++ },
 *     style = MillenniumIconButtonStyle.Secondary,
 *     contentDescription = "Increment, current value $count",
 * ) { Text("$count") }
 * ```
 *
 * @param onClick Called on click, tap or keyboard activation; not called when disabled.
 * @param modifier Modifier applied to the button.
 * @param enabled When `false` the button ignores input and uses the disabled state.
 * @param loading When `true` the button behaves as disabled and an indeterminate spinner replaces [content].
 * @param style Emphasis variant, see [MillenniumIconButtonStyle].
 * @param contentDescription Accessibility label; defaults to `null`, so supply one for non-text content.
 * @param background Overrides the face brush; `null` falls back to the theme or variant default.
 * @param foreground Overrides the content colour; `null` falls back to the theme or variant default.
 * @param shape Overrides the outline; `null` falls back to the theme.
 * @param size Overrides the fixed button size; `null` falls back to the theme (44 x 44dp).
 * @param iconSize Unused by this overload (kept for signature parity); `null` falls back to the theme.
 * @param borderWidth Overrides the border thickness; `null` falls back to the state's value.
 * @param borderColor Overrides the border colour; `null` falls back to the theme or palette.
 * @param shadows Overrides the shadow colour layers; `null` falls back to the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the state's value.
 * @param innerGlowColor Overrides the top inner highlight; `null` falls back to the theme or palette.
 * @param bottomRimColor Overrides the lower inner rim; `null` falls back to the theme or palette.
 * @param orb Forces the glass orb on or off; `null` falls back to the theme.
 * @param orbColor Overrides the orb reflection colour; `null` falls back to the theme or white.
 * @param orbStrength Overrides the white reflection alpha; `null` falls back to the state's value.
 * @param alpha Overrides the overall opacity; `null` falls back to the state's value.
 * @param motion Overrides scale and tween settings; `null` falls back to the theme.
 * @param theme Theme supplying variant/state properties; defaults to [MillenniumTheme.iconButtonTheme].
 * @param content Composable content centered in the button.
 * @see MillenniumIconButton
 */
@Composable
fun MillenniumIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: MillenniumIconButtonStyle = MillenniumIconButtonStyle.Primary,
    contentDescription: String? = null,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    size: DpSize? = null,
    iconSize: Dp? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    shadows: List<Color>? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    bottomRimColor: Color? = null,
    orb: Boolean? = null,
    orbColor: Color? = null,
    orbStrength: Float? = null,
    alpha: Float? = null,
    motion: MillenniumButtonMotion? = null,
    theme: MillenniumIconButtonTheme = MillenniumTheme.iconButtonTheme,
    content: @Composable () -> Unit,
) {
    IconButtonContainer(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = style,
        icon = null,
        contentDescription = contentDescription,
        content = content,
        theme = theme,
        background = background,
        foreground = foreground,
        shape = shape,
        size = size,
        iconSize = iconSize,
        borderWidth = borderWidth,
        borderColor = borderColor,
        shadows = shadows,
        shadowElevation = shadowElevation,
        innerGlowColor = innerGlowColor,
        bottomRimColor = bottomRimColor,
        orb = orb,
        orbColor = orbColor,
        orbStrength = orbStrength,
        alpha = alpha,
        motion = motion,
    )
}

/** Shared implementation behind both [MillenniumIconButton] overloads (exactly one of icon/content is set). */
@Composable
private fun IconButtonContainer(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    style: MillenniumIconButtonStyle,
    icon: ImageVector?,
    contentDescription: String?,
    content: (@Composable () -> Unit)?,
    theme: MillenniumIconButtonTheme,
    background: Brush?,
    foreground: Color?,
    shape: Shape?,
    size: DpSize?,
    iconSize: Dp?,
    borderWidth: Dp?,
    borderColor: Color?,
    shadows: List<Color>?,
    shadowElevation: Dp?,
    innerGlowColor: Color?,
    bottomRimColor: Color?,
    orb: Boolean?,
    orbColor: Color?,
    orbStrength: Float?,
    alpha: Float?,
    motion: MillenniumButtonMotion?,
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
    val colors = MillenniumTheme.colors
    val properties = theme.forStyle(style).resolve(state)
    val resolvedOrb = orb ?: properties.orb
    val orbTop = if (style == MillenniumIconButtonStyle.Danger) colors.danger else colors.accentTop
    val orbBottom = if (style == MillenniumIconButtonStyle.Danger) {
        lerp(colors.danger, Color.Black, 0.20f)
    } else {
        colors.accentBottom
    }
    val resolvedMotion = motion ?: properties.motion
    val resolvedForeground = foreground ?: properties.foreground ?: when (style) {
        MillenniumIconButtonStyle.Primary -> if (resolvedOrb) Color.White else colors.onAccent
        MillenniumIconButtonStyle.Danger -> if (resolvedOrb) Color.White else colors.surface
        MillenniumIconButtonStyle.Quiet -> when (state) {
            MillenniumControlState.Hovered, MillenniumControlState.Focused,
            MillenniumControlState.Pressed -> colors.accent
            else -> colors.text
        }
        else -> colors.text
    }
    val quietVisibility by animateFloatAsState(
        targetValue = if (state == MillenniumControlState.Hovered || state == MillenniumControlState.Focused || state == MillenniumControlState.Pressed) 1f else 0f,
        animationSpec = tween(resolvedMotion.stateChangeMillis),
    )
    val quietStops = millenniumQuietEnamelStops(colors, state == MillenniumControlState.Pressed)
        .map { (stop, color) -> stop to color.copy(alpha = color.alpha * quietVisibility) }
    val animatedForeground by animateColorAsState(
        targetValue = resolvedForeground,
        animationSpec = tween(resolvedMotion.stateChangeMillis),
    )
    val resolvedBackground = background ?: properties.background ?: if (resolvedOrb) {
        Brush.verticalGradient(*millenniumIconOrbStops(orbTop, orbBottom).toTypedArray())
    } else when (style) {
        MillenniumIconButtonStyle.Primary -> gelBrush(colors.accentTop, colors.accentBottom)
        MillenniumIconButtonStyle.Secondary -> millenniumBevelBrush(colors)
        MillenniumIconButtonStyle.Quiet -> Brush.verticalGradient(*quietStops.toTypedArray())
        MillenniumIconButtonStyle.Danger -> gelBrush(
            colors.danger,
            lerp(colors.danger, if (colors.surface.luminance() > colors.danger.luminance()) Color.Black else Color.White, 0.20f),
        )
    }
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: when (state) {
        MillenniumControlState.Focused, MillenniumControlState.Selected -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> when (style) {
            MillenniumIconButtonStyle.Primary -> if (resolvedOrb) lerp(colors.accentBottom, Color.Black, 0.45f) else colors.accentBottom
            MillenniumIconButtonStyle.Secondary -> colors.border
            MillenniumIconButtonStyle.Quiet -> if (hovered || pressed) colors.accent.copy(alpha = 0.55f) else Color.Transparent
            MillenniumIconButtonStyle.Danger -> lerp(colors.danger, Color.Black, 0.35f)
        }
    }
    val resolvedInnerGlowColor = innerGlowColor ?: properties.innerGlowColor
        ?: if (resolvedOrb || style == MillenniumIconButtonStyle.Quiet) null else when (state) {
        MillenniumControlState.Hovered, MillenniumControlState.Focused -> colors.highlight.copy(alpha = 0.60f)
        MillenniumControlState.Pressed -> colors.inset.copy(alpha = 0.32f)
        else -> colors.highlight.copy(alpha = 0.70f)
    }
    val resolvedOrbColor = if (resolvedOrb) {
        orbColor ?: properties.orbColor ?: Color.White.copy(alpha = orbStrength ?: properties.orbStrength)
    } else {
        null
    }
    val scale by animateFloatAsState(
        targetValue = when {
            !interactive -> 1f
            pressed -> resolvedMotion.pressedScale
            hovered -> resolvedMotion.hoverScale
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
    )
    val elevation by animateDpAsState(
        shadowElevation ?: properties.shadowElevation,
        spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = resolvedBorderColor,
        animationSpec = tween(resolvedMotion.stateChangeMillis),
    )
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    UnstyledButton(
        onClick = onClick,
        enabled = interactive,
        interactionSource = interaction,
        indication = null,
        contentPadding = PaddingValues(0.dp),
        contentAlignment = Alignment.Center,
        modifier = modifier
            .millenniumActionPointer(interactive)
            .then(semantics)
            .size(size ?: properties.size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(alpha ?: properties.alpha)
            .millenniumSurface(
                background = resolvedBackground,
                shape = shape ?: properties.shape,
                borderColor = animatedBorderColor,
                borderWidth = borderWidth ?: properties.borderWidth,
                shadowElevation = elevation,
                opaqueFill = style != MillenniumIconButtonStyle.Quiet || resolvedOrb,
                shadowColors = shadows ?: properties.shadows ?: listOf(Color.Black.copy(alpha = 0.16f)),
                innerGlowColor = resolvedInnerGlowColor,
                bottomRimColor = bottomRimColor ?: properties.bottomRimColor
                    ?: if (!resolvedOrb && style != MillenniumIconButtonStyle.Quiet) colors.highlight.copy(alpha = 0.28f) else null,
            )
            .then(
                if (resolvedOrbColor != null) Modifier.millenniumIconOrb(
                    reflection = resolvedOrbColor,
                    tint = orbTop,
                    shape = shape ?: properties.shape,
                    borderWidth = borderWidth ?: properties.borderWidth,
                    borderColor = if (borderColor != null || properties.borderColor != null || focused) animatedBorderColor else null,
                ) else Modifier,
            )
            .millenniumInteractionSurface(state, shape ?: properties.shape,
                tint = if (style == MillenniumIconButtonStyle.Danger) colors.danger else colors.accent,
                animationMillis = resolvedMotion.stateChangeMillis),
    ) {
        MillenniumLoadingContent(loading, animatedForeground, spinnerSize = iconSize ?: properties.iconSize) {
            if (icon != null) {
                UnstyledIcon(icon, null, Modifier.size(iconSize ?: properties.iconSize), animatedForeground)
            } else {
                MillenniumStyledContent(TextStyle.Default, animatedForeground) { content?.invoke() }
            }
        }
    }
}

/**
 * Continuous spherical shading, deliberately without the button gel's hard halfway break.
 *
 * Returns five vertical gradient stops (position to colour) from a dark body at the top to the
 * brighter [top] accent at the lip. The body is [bottom] darkened 52% toward black, so every accent
 * keeps a white icon readable. Internal; used for the orb background of filled icon buttons.
 *
 * @param top Illuminated lower-lip colour (the accent's bright stop).
 * @param bottom Accent base colour from which the dark body is derived.
 * @return Stops suitable for `Brush.verticalGradient(*stops.toTypedArray())`.
 */
internal fun millenniumIconOrbStops(top: Color, bottom: Color): List<Pair<Float, Color>> {
    // Every accent uses deep jewel glass. Bright amber/citron belong at the illuminated lip,
    // not across the face; keeping the body dark also gives every orb a white icon.
    val body = lerp(bottom, Color.Black, 0.52f)
    return listOf(
        0f to lerp(bottom, body, 0.65f),
        0.28f to body,
        0.60f to body,
        0.82f to lerp(body, top, 0.40f),
        1f to top,
    )
}

/**
 * Cached, size-relative glass layers; the surface clips them to the caller's shape.
 *
 * Draws a lower light pool, a curved translucent dome, the orb rim (metallic unless [borderColor] is
 * given) and a thin inner rim, then the content.
 */
private fun Modifier.millenniumIconOrb(
    reflection: Color,
    tint: Color,
    shape: Shape,
    borderWidth: Dp,
    borderColor: Color?,
): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val rim = maxOf(borderWidth.toPx(), size.minDimension * 0.025f)
    val outline = shape.createOutline(size, layoutDirection, this)
    val dome = Path().apply {
        moveTo(width * 0.08f, height * 0.37f)
        cubicTo(width * 0.12f, -height * 0.04f, width * 0.88f, -height * 0.04f, width * 0.92f, height * 0.37f)
        cubicTo(width * 0.94f, height * 0.49f, width * 0.81f, height * 0.39f, width * 0.70f, height * 0.38f)
        cubicTo(width * 0.48f, height * 0.32f, width * 0.28f, height * 0.43f, width * 0.12f, height * 0.44f)
        cubicTo(width * 0.08f, height * 0.45f, width * 0.07f, height * 0.41f, width * 0.08f, height * 0.37f)
        close()
    }
    val domeBrush = Brush.radialGradient(
        0f to reflection,
        0.50f to reflection.copy(alpha = reflection.alpha * 0.42f),
        1f to reflection.copy(alpha = reflection.alpha * 0.03f),
        center = Offset(width * 0.46f, height * 0.04f),
        radius = size.minDimension * 0.63f,
    )
    // A light pool below the sphere, not a white smile painted across its face.
    val glowTint = if (tint.blue > tint.red && tint.blue > tint.green) {
        lerp(tint, Color(0xFF65E8FF), 0.65f)
    } else {
        lerp(tint, Color.White, 0.25f)
    }
    val lowerGlow = Brush.radialGradient(
        0f to glowTint.copy(alpha = reflection.alpha * 0.95f),
        0.40f to glowTint.copy(alpha = reflection.alpha * 0.65f),
        0.78f to glowTint.copy(alpha = reflection.alpha * 0.10f),
        1f to Color.Transparent,
        center = Offset(width * 0.5f, height * 1.12f),
        radius = size.minDimension * 0.66f,
    )
    val rimBrush = Brush.linearGradient(
        0f to Color(0xFF26364F),
        0.28f to Color(0xFF66748A),
        0.62f to Color(0xFFC6D4DA),
        1f to Color(0xFF89949D),
        start = Offset.Zero,
        end = Offset(width * 0.65f, height),
    )
    val innerRim = Brush.verticalGradient(
        0f to reflection.copy(alpha = reflection.alpha * 0.70f),
        0.35f to Color.Transparent,
        0.75f to Color.Transparent,
        1f to glowTint.copy(alpha = reflection.alpha * 0.75f),
    )
    onDrawWithContent {
        drawRect(lowerGlow)
        drawPath(dome, domeBrush)
        if (borderWidth > 0.dp) {
            if (borderColor != null) drawOutline(outline, borderColor, style = Stroke(rim * 2f))
            else drawOutline(outline, rimBrush, style = Stroke(rim * 2f))
        }
        val innerInset = rim * 1.7f
        if (size.minDimension > innerInset * 2f) {
            val innerSize = Size(width - innerInset * 2f, height - innerInset * 2f)
            val innerOutline = shape.createOutline(innerSize, layoutDirection, this)
            inset(innerInset) {
                drawOutline(innerOutline, innerRim, style = Stroke(size.minDimension * 0.018f))
            }
        }
        drawContent()
    }
}

@Preview
@Composable
private fun MillenniumIconButtonPreview() = MillenniumPreview {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MillenniumIconButton(MillenniumIcons.Sparkle, "Favorite", {})
        MillenniumIconButton(MillenniumIcons.Music, "Play", {}, style = MillenniumIconButtonStyle.Secondary)
        MillenniumIconButton(MillenniumIcons.Chevron, "More", {}, style = MillenniumIconButtonStyle.Quiet)
        MillenniumIconButton(MillenniumIcons.Check, "Delete", {}, style = MillenniumIconButtonStyle.Danger)
    }
}
