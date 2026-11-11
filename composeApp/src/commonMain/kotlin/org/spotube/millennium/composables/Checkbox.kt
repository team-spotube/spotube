package org.spotube.millennium.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import com.composeunstyled.UnstyledCheckbox
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface

/**
 * Shared face anatomy for the binary and tri-state checkbox controls.
 *
 * Implemented by [MillenniumCheckboxThemeProperties] and the tri-state counterpart so the internal
 * face renderer can draw both. Not intended to be implemented by callers.
 *
 * @property faceBackground Unselected face brush; `null` = sunken bevel.
 * @property selectedFaceBackground Face brush when checked or indeterminate; `null` = accent gel.
 * @property selectedFaceForeground Colour of the check mark / dash; `null` = on-accent.
 * @property faceShape Outline of the box.
 * @property faceSize Size of the box.
 * @property markSize Size of the check-mark icon.
 * @property dashSize Size of the indeterminate dash.
 * @property faceBorderColor Border colour; `null` = accent while hovered/pressed/focused, else border
 * (unchecked) or accent-bottom (checked).
 * @property faceBorderWidth Border thickness.
 * @property faceGlowColor Inner highlight colour; `null` = palette highlight at 55%.
 * @property faceElevation Drop-shadow elevation of the box.
 * @property faceAlpha Opacity of the box.
 * @property stateChangeMillis Duration in ms for border colour and mark transitions.
 * @property labelForeground Label colour; `null` = text colour (muted when disabled).
 * @property labelStyle Label typography merged into the current text style.
 * @property minimumTouchHeight Minimum height of the whole interactive row.
 * @property contentPadding Padding around the box and label row.
 * @property contentSpacing Gap between box and label.
 */
interface MillenniumCheckFaceStyle {
    val faceBackground: Brush?
    val selectedFaceBackground: Brush?
    val selectedFaceForeground: Color?
    val faceShape: Shape
    val faceSize: DpSize
    val markSize: Dp
    val dashSize: DpSize
    val faceBorderColor: Color?
    val faceBorderWidth: Dp
    val faceGlowColor: Color?
    val faceElevation: Dp
    val faceAlpha: Float
    val stateChangeMillis: Int
    val labelForeground: Color?
    val labelStyle: TextStyle
    val minimumTouchHeight: Dp
    val contentPadding: PaddingValues
    val contentSpacing: Dp
}

/**
 * State-specific appearance and layout of a [MillenniumCheckbox].
 *
 * Every property implements the matching member of [MillenniumCheckFaceStyle]; see that interface for
 * the meaning of each `@property` (units, null = derived from the active palette). Required values
 * (shape, sizes, border width, elevation, alpha, durations, label style, touch height, padding,
 * spacing) have no default; use [defaultMillenniumCheckboxTheme] as a starting point.
 */
@Immutable
data class MillenniumCheckboxThemeProperties(
    override val faceBackground: Brush? = null,
    override val selectedFaceBackground: Brush? = null,
    override val selectedFaceForeground: Color? = null,
    override val faceShape: Shape,
    override val faceSize: DpSize,
    override val markSize: Dp,
    override val dashSize: DpSize,
    override val faceBorderColor: Color? = null,
    override val faceBorderWidth: Dp,
    override val faceGlowColor: Color? = null,
    override val faceElevation: Dp,
    override val faceAlpha: Float,
    override val stateChangeMillis: Int,
    override val labelForeground: Color? = null,
    override val labelStyle: TextStyle,
    override val minimumTouchHeight: Dp,
    override val contentPadding: PaddingValues,
    override val contentSpacing: Dp,
) : MillenniumCheckFaceStyle

/**
 * Theme for [MillenniumCheckbox]: one [MillenniumCheckboxThemeProperties] per control state.
 * Installed as [MillenniumTheme.checkboxTheme].
 *
 * @property states Properties for each control state (default, hovered, focused, pressed, disabled,
 * selected, error).
 * @see defaultMillenniumCheckboxTheme
 */
@Immutable
data class MillenniumCheckboxTheme(
    val states: MillenniumControlStateProperty<MillenniumCheckboxThemeProperties>,
)

/**
 * Creates the default [MillenniumCheckboxTheme].
 *
 * A 22dp box with 5dp corners, 1dp border (2dp when focused or in error), a 16dp check mark, an 11 x 2dp
 * dash, 12dp spacing to the label, a 44dp minimum row height and a 120ms state tween. Disabled uses
 * `faceAlpha = 0.45f`. All colours are `null` and follow the palette. Customise with `copy`:
 *
 * ```kotlin
 * val base = defaultMillenniumCheckboxTheme()
 * val large = base.copy(
 *     states = base.states.copy(default = base.states.default.copy(faceSize = DpSize(28.dp, 28.dp))),
 * )
 * ```
 *
 * @return A new default theme.
 */
fun defaultMillenniumCheckboxTheme(): MillenniumCheckboxTheme {
    val base = MillenniumCheckboxThemeProperties(
        faceShape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
        faceSize = DpSize(22.dp, 22.dp),
        markSize = 16.dp,
        dashSize = DpSize(11.dp, 2.dp),
        faceBorderWidth = 1.dp,
        faceElevation = 0.dp,
        faceAlpha = 1f,
        stateChangeMillis = 120,
        labelStyle = TextStyle(fontWeight = FontWeight.Normal),
        minimumTouchHeight = 44.dp,
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 12.dp,
    )
    return MillenniumCheckboxTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(faceBorderWidth = 2.dp),
            pressed = base.copy(faceElevation = 0.dp),
            disabled = base.copy(faceAlpha = 0.45f),
            selected = base,
            error = base.copy(faceBorderWidth = 2.dp),
        ),
    )
}

/**
 * Draws the beveled checkbox box and its animated mark; shared by [MillenniumCheckbox] and
 * [MillenniumTriStateCheckbox].
 *
 * The face is a gel (selected) or sunken bevel (off) surface. It springs to 0.90 scale when pressed and
 * 1.05 when hovered. Changes between values animate via `AnimatedContent`: the check or dash pops in
 * with a spring scale and fade and the old mark shrinks away. Internal: it is not interactive itself;
 * the enclosing `UnstyledCheckbox`/`UnstyledTriStateCheckbox` owns input and semantics.
 *
 * @param value Current [ToggleableState]: `On` draws the check, `Indeterminate` the dash, `Off` nothing.
 * @param properties Resolved face appearance.
 * @param state Resolved interaction state, used for border colour, scale and interaction lighting.
 */
@Composable
internal fun MillenniumCheckFace(
    value: ToggleableState,
    properties: MillenniumCheckFaceStyle,
    state: MillenniumControlState,
) {
    val colors = MillenniumTheme.colors
    val faceBackground = properties.faceBackground
        ?: if (value == ToggleableState.Off) millenniumBevelBrush(colors, sunken = true)
        else gelBrush(colors.accentTop, colors.accentBottom)
    val targetBorderColor = properties.faceBorderColor
        ?: when (state) {
            MillenniumControlState.Hovered, MillenniumControlState.Pressed, MillenniumControlState.Focused -> colors.accent
            else -> if (value == ToggleableState.Off) colors.border else colors.accentBottom
        }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(properties.stateChangeMillis),
    )
    val glowColor = properties.faceGlowColor ?: colors.highlight.copy(alpha = 0.55f)
    val selectedForeground = properties.selectedFaceForeground ?: colors.onAccent
    val selected = value != ToggleableState.Off
    val scale by animateFloatAsState(
        targetValue = when (state) {
            MillenniumControlState.Pressed -> 0.90f
            MillenniumControlState.Hovered -> 1.05f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
    )
    Box(
        Modifier
            .size(properties.faceSize)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .millenniumSurface(
                background = if (selected) properties.selectedFaceBackground
                    ?: gelBrush(colors.accentTop, colors.accentBottom) else faceBackground,
                shape = properties.faceShape,
                borderColor = borderColor,
                borderWidth = properties.faceBorderWidth,
                shadowElevation = properties.faceElevation,
                innerGlowColor = glowColor,
            )
            .millenniumInteractionSurface(state, properties.faceShape, animationMillis = properties.stateChangeMillis)
            .alpha(properties.faceAlpha),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.4f,
                    animationSpec = spring(
                        dampingRatio = 0.52f,
                        stiffness = Spring.StiffnessMedium
                    ),
                ) + fadeIn(
                    tween(
                        properties.stateChangeMillis,
                        easing = FastOutSlowInEasing
                    )
                )) togetherWith
                        (scaleOut(
                            targetScale = 0.4f,
                            animationSpec = tween(
                                (properties.stateChangeMillis * 0.7f).toInt().coerceAtLeast(60),
                                easing = FastOutLinearInEasing
                            ),
                        ) + fadeOut(
                            tween(
                                (properties.stateChangeMillis * 0.7f).toInt().coerceAtLeast(60),
                                easing = FastOutLinearInEasing
                            )
                        ))
            },
        ) { currentValue ->
            Box(Modifier.size(properties.faceSize), contentAlignment = Alignment.Center) {
                when (currentValue) {
                    ToggleableState.On -> MillenniumIcon(
                        MillenniumIcons.Check,
                        modifier = Modifier.size(properties.markSize),
                        tint = selectedForeground,
                    )

                    ToggleableState.Indeterminate -> Box(
                        Modifier
                            .size(properties.dashSize)
                            .background(selectedForeground, CircleShape),
                    )

                    ToggleableState.Off -> Unit
                }
            }
        }
    }
}

/**
 * A beveled two-state checkbox with an optional text label.
 *
 * Use it for independent on/off choices. For a parent that summarises children use
 * [MillenniumTriStateCheckbox]; for a switch-like immediate setting see [MillenniumToggleSwitch].
 *
 * Built on Compose Unstyled `UnstyledCheckbox`, which provides checkbox semantics, toggle on click and
 * Space, and [label] as the accessibility label. The whole row (box plus label) is the target with a
 * 44dp default minimum height. State resolves as disabled, pressed, keyboard-focused (via
 * `collectIsKeyboardFocusedAsState`, so no ring after a pointer press), hovered, selected, default.
 * Feedback stays on the box (spring scale, border colour, interaction lighting), not the whole row;
 * the check mark pops in with a spring. The hand cursor is shown while enabled. State is caller-owned.
 *
 * Nullable parameters override single theme properties (`param ?: themeValue ?: fallback`).
 *
 * ```kotlin
 * var keepSignedIn by remember { mutableStateOf(true) }
 * MillenniumCheckbox(
 *     checked = keepSignedIn,
 *     onCheckedChange = { keepSignedIn = it },
 *     label = "Remember this choice",
 * )
 * ```
 *
 * @param checked Whether the box is checked; caller-owned.
 * @param onCheckedChange Called with the new value when toggled; not called when disabled.
 * @param label Optional text shown beside the box and used as the accessibility label. Blank or `null`
 * draws no text.
 * @param modifier Modifier applied to the interactive row.
 * @param enabled When `false` the control ignores input, dims the box and mutes the label.
 * @param faceBackground Overrides the unchecked box brush; `null` falls back to the theme or sunken bevel.
 * @param selectedFaceBackground Overrides the checked box brush; `null` falls back to the theme or accent gel.
 * @param selectedFaceForeground Overrides the check mark colour; `null` falls back to the theme or on-accent.
 * @param faceShape Overrides the box outline; `null` falls back to the theme.
 * @param faceSize Overrides the box size; `null` falls back to the theme (22dp).
 * @param markSize Overrides the check icon size; `null` falls back to the theme.
 * @param dashSize Overrides the indeterminate dash size (unused for two-state values); `null` falls back to the theme.
 * @param faceBorderColor Overrides the border colour; `null` falls back to the theme or palette.
 * @param faceBorderWidth Overrides the border thickness; `null` falls back to the state's value.
 * @param faceGlowColor Overrides the inner highlight; `null` falls back to the theme or palette.
 * @param faceElevation Overrides the box shadow elevation; `null` falls back to the theme.
 * @param faceAlpha Overrides the box opacity; `null` falls back to the state's value.
 * @param stateChangeMillis Overrides the colour/mark tween duration; `null` falls back to the theme.
 * @param labelForeground Overrides the label colour; `null` falls back to the theme or text/muted colour.
 * @param labelStyle Overrides the label typography; `null` falls back to the theme.
 * @param minimumTouchHeight Overrides the minimum row height; `null` falls back to the theme (44dp).
 * @param contentPadding Overrides the padding around the row content; `null` falls back to the theme.
 * @param contentSpacing Overrides the gap between box and label; `null` falls back to the theme.
 * @param theme Theme supplying state properties; defaults to [MillenniumTheme.checkboxTheme].
 * @see MillenniumTriStateCheckbox
 * @see MillenniumCheckboxTheme
 */
@Composable
fun MillenniumCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    faceBackground: Brush? = null,
    selectedFaceBackground: Brush? = null,
    selectedFaceForeground: Color? = null,
    faceShape: Shape? = null,
    faceSize: DpSize? = null,
    markSize: Dp? = null,
    dashSize: DpSize? = null,
    faceBorderColor: Color? = null,
    faceBorderWidth: Dp? = null,
    faceGlowColor: Color? = null,
    faceElevation: Dp? = null,
    faceAlpha: Float? = null,
    stateChangeMillis: Int? = null,
    labelForeground: Color? = null,
    labelStyle: TextStyle? = null,
    minimumTouchHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    theme: MillenniumCheckboxTheme = MillenniumTheme.checkboxTheme,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsKeyboardFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val state = when {
        !enabled -> MillenniumControlState.Disabled
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        checked -> MillenniumControlState.Selected
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(state)
    val faceProperties = properties.copy(
        faceBackground = faceBackground ?: properties.faceBackground,
        selectedFaceBackground = selectedFaceBackground ?: properties.selectedFaceBackground,
        selectedFaceForeground = selectedFaceForeground ?: properties.selectedFaceForeground,
        faceShape = faceShape ?: properties.faceShape,
        faceSize = faceSize ?: properties.faceSize,
        markSize = markSize ?: properties.markSize,
        dashSize = dashSize ?: properties.dashSize,
        faceBorderColor = faceBorderColor ?: properties.faceBorderColor,
        faceBorderWidth = faceBorderWidth ?: properties.faceBorderWidth,
        faceGlowColor = faceGlowColor ?: properties.faceGlowColor,
        faceElevation = faceElevation ?: properties.faceElevation,
        faceAlpha = faceAlpha ?: properties.faceAlpha,
        stateChangeMillis = stateChangeMillis ?: properties.stateChangeMillis,
    )
    val labelColor = labelForeground ?: properties.labelForeground ?: when (state) {
        MillenniumControlState.Disabled -> MillenniumTheme.colors.muted
        else -> MillenniumTheme.colors.text
    }
    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.millenniumActionPointer(enabled)
            .heightIn(min = minimumTouchHeight ?: properties.minimumTouchHeight),
        enabled = enabled,
        accessibilityLabel = label,
        indication = null,
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding ?: properties.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                contentSpacing ?: properties.contentSpacing
            ),
        ) {
            MillenniumCheckFace(
                value = if (checked) ToggleableState.On else ToggleableState.Off,
                properties = faceProperties,
                state = state,
            )
            if (!label.isNullOrBlank()) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.merge(
                        labelStyle ?: properties.labelStyle
                    )
                        .copy(color = labelColor),
                ) { Text(label) }
            }
        }
    }
}

@Preview
@Composable
private fun MillenniumCheckboxPreview() = MillenniumPreview {
    MillenniumCheckbox(checked = true, onCheckedChange = {}, label = "Remember this choice")
}
