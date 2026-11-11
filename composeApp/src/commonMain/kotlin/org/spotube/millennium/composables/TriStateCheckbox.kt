package org.spotube.millennium.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import com.composeunstyled.UnstyledTriStateCheckbox
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.gelBrush
import org.spotube.millennium.theme.millenniumBevelBrush

/**
 * State-specific appearance and layout of a [MillenniumTriStateCheckbox].
 *
 * Every property implements the matching member of [MillenniumCheckFaceStyle]; see that interface for
 * the meaning of each `@property` (units, null = derived from the active palette). Required values
 * have no default; use [defaultMillenniumTriStateCheckboxTheme] as a starting point.
 */
@Immutable
data class MillenniumTriStateCheckboxThemeProperties(
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
 * Theme for [MillenniumTriStateCheckbox]: one [MillenniumTriStateCheckboxThemeProperties] per control
 * state. Installed as [MillenniumTheme.triStateCheckboxTheme]; independent from the two-state
 * [MillenniumCheckboxTheme].
 *
 * @property states Properties for each control state (default, hovered, focused, pressed, disabled,
 * selected, error).
 * @see defaultMillenniumTriStateCheckboxTheme
 */
@Immutable
data class MillenniumTriStateCheckboxTheme(
    val states: MillenniumControlStateProperty<MillenniumTriStateCheckboxThemeProperties>,
)

/**
 * Creates the default [MillenniumTriStateCheckboxTheme].
 *
 * Same defaults as [defaultMillenniumCheckboxTheme]: 22dp box with 5dp corners, 1dp border (2dp when
 * focused or in error), 16dp check, 11 x 2dp dash, 12dp label spacing, 44dp minimum row height,
 * 120ms tween and `faceAlpha = 0.45f` when disabled. Customise with `copy`.
 *
 * @return A new default theme.
 */
fun defaultMillenniumTriStateCheckboxTheme(): MillenniumTriStateCheckboxTheme {
    val base = MillenniumTriStateCheckboxThemeProperties(
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
    return MillenniumTriStateCheckboxTheme(
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
 * A beveled checkbox with three values (off, on, indeterminate) and an optional label.
 *
 * Use it as a "select all" parent that summarises a group of child checkboxes: derive [value] from the
 * children and let [onClick] set them all. Unlike [MillenniumCheckbox] it reports a click rather than a
 * new boolean, because the next value depends on the caller's rule.
 *
 * Built on Compose Unstyled `UnstyledTriStateCheckbox`, which supplies tri-state toggle semantics,
 * click/Space activation and [label] as the accessibility label. The row has a 44dp default minimum
 * height. State resolves as disabled, pressed, keyboard-focused (`collectIsKeyboardFocusedAsState`, no
 * ring after pointer presses), hovered, selected (any non-off value), default. The box shows a check for
 * `On` and a dash for `Indeterminate`, animating between them with a spring pop.
 *
 * Nullable parameters override single theme properties (`param ?: themeValue ?: fallback`).
 *
 * ```kotlin
 * val children = remember { mutableStateListOf(true, false, true) }
 * val parent = when {
 *     children.all { it } -> ToggleableState.On
 *     children.none { it } -> ToggleableState.Off
 *     else -> ToggleableState.Indeterminate
 * }
 * MillenniumTriStateCheckbox(
 *     value = parent,
 *     onClick = {
 *         val target = parent != ToggleableState.On
 *         children.indices.forEach { children[it] = target }
 *     },
 *     label = "Select all",
 * )
 * ```
 *
 * @param value Current [ToggleableState]; caller-owned.
 * @param onClick Called when the user activates the control; the caller decides the next value. Not
 * called when disabled.
 * @param label Text beside the box and accessibility label; pass `null` (or blank) for no visible text.
 * @param modifier Modifier applied to the interactive row.
 * @param enabled When `false` the control ignores input, dims the box and mutes the label.
 * @param faceBackground Overrides the unselected box brush; `null` falls back to the theme or sunken bevel.
 * @param selectedFaceBackground Overrides the on/indeterminate box brush; `null` falls back to the theme or accent gel.
 * @param selectedFaceForeground Overrides the mark colour; `null` falls back to the theme or on-accent.
 * @param faceShape Overrides the box outline; `null` falls back to the theme.
 * @param faceSize Overrides the box size; `null` falls back to the theme (22dp).
 * @param markSize Overrides the check icon size; `null` falls back to the theme.
 * @param dashSize Overrides the indeterminate dash size; `null` falls back to the theme.
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
 * @param theme Theme supplying state properties; defaults to [MillenniumTheme.triStateCheckboxTheme].
 * @see MillenniumCheckbox
 * @see MillenniumTriStateCheckboxTheme
 */
@Composable
fun MillenniumTriStateCheckbox(
    value: ToggleableState,
    onClick: () -> Unit,
    label: String?,
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
    theme: MillenniumTriStateCheckboxTheme = MillenniumTheme.triStateCheckboxTheme,
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
        value != ToggleableState.Off -> MillenniumControlState.Selected
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
    UnstyledTriStateCheckbox(
        value = value,
        onClick = onClick,
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
            MillenniumCheckFace(value, faceProperties, state)
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
private fun MillenniumTriStateCheckboxPreview() = MillenniumPreview {
    MillenniumTriStateCheckbox(
        value = ToggleableState.Indeterminate,
        onClick = {},
        label = "Select the collection"
    )
}
