package org.spotube.millennium.composables

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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.RadioButton
import com.composeunstyled.SelectedIndicator
import com.composeunstyled.Text
import com.composeunstyled.UnstyledRadioGroup
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
 * State-specific appearance and layout of each option in a [MillenniumRadioGroup].
 *
 * @property indicatorBackground Unselected indicator brush; `null` = sunken bevel.
 * @property selectedIndicatorBackground Selected indicator brush; `null` = accent gel.
 * @property indicatorShape Indicator outline; default fully rounded (50%).
 * @property indicatorSize Indicator width and height; default 22 x 22dp.
 * @property dotSize Diameter of the inner selection dot; default `8.dp`.
 * @property dotColor Selection dot colour; `null` = on-accent.
 * @property indicatorBorderColor Indicator border; `null` = accent when focused/hovered/pressed/selected,
 * danger in error, otherwise the palette border.
 * @property indicatorBorderWidth Indicator border thickness; `1.dp`, `2.dp` when focused or in error.
 * @property minimumTouchHeight Minimum height of each option row; default `44.dp`.
 * @property optionPadding Padding around each option's content.
 * @property optionSpacing Gap between indicator and label; default `12.dp`.
 * @property groupSpacing Vertical gap between options; default `4.dp`. Read from the default state only.
 * @property labelStyle Option label typography; default 14sp / 20sp normal.
 * @property labelColor Label colour; `null` = text colour.
 * @property selectedLabelColor Label colour of the selected option; `null` = text colour.
 * @property stateAnimationMillis Duration in ms of border, label and alpha tweens and the dot's pop-in. Default `120`.
 * @property alpha Opacity of the option; default `1f` (the default disabled state uses `0.45f`).
 * @property indicatorGlowColor Indicator inner highlight; `null` = palette highlight at 32% when selected, none otherwise.
 * @property indicatorElevation Indicator drop-shadow elevation; default `0.dp`.
 */
@Immutable
data class MillenniumRadioGroupThemeProperties(
    val indicatorBackground: Brush? = null,
    val selectedIndicatorBackground: Brush? = null,
    val indicatorShape: Shape,
    val indicatorSize: DpSize,
    val dotSize: Dp,
    val dotColor: Color? = null,
    val indicatorBorderColor: Color? = null,
    val indicatorBorderWidth: Dp,
    val minimumTouchHeight: Dp,
    val optionPadding: PaddingValues,
    val optionSpacing: Dp,
    val groupSpacing: Dp,
    val labelStyle: TextStyle,
    val labelColor: Color? = null,
    val selectedLabelColor: Color? = null,
    val stateAnimationMillis: Int,
    val alpha: Float = 1f,
    val indicatorGlowColor: Color? = null,
    val indicatorElevation: Dp = 0.dp,
)

/**
 * Theme for [MillenniumRadioGroup]: one [MillenniumRadioGroupThemeProperties] per control state.
 * Installed as [MillenniumTheme.radioGroupTheme].
 *
 * @property states Properties for each control state (default, hovered, focused, pressed, disabled,
 * selected, error).
 * @see defaultMillenniumRadioGroupTheme
 */
@Immutable
data class MillenniumRadioGroupTheme(
    val states: MillenniumControlStateProperty<MillenniumRadioGroupThemeProperties>,
)

/**
 * Creates the default [MillenniumRadioGroupTheme].
 *
 * 22dp round indicators with an 8dp dot, 1dp border (2dp focused or in error), 12dp indicator-label
 * spacing, 4dp between options, 44dp minimum option height, 14sp label and 120ms tweens. Disabled uses
 * `alpha = 0.45f`. Colours are `null` and follow the palette. Customise with `copy`:
 *
 * ```kotlin
 * val base = defaultMillenniumRadioGroupTheme()
 * val roomy = base.copy(states = base.states.copy(default = base.states.default.copy(groupSpacing = 12.dp)))
 * ```
 *
 * @return A new default theme.
 */
fun defaultMillenniumRadioGroupTheme(): MillenniumRadioGroupTheme {
    val base = MillenniumRadioGroupThemeProperties(
        indicatorShape = RoundedCornerShape(50),
        indicatorSize = DpSize(22.dp, 22.dp),
        dotSize = 8.dp,
        indicatorBorderWidth = 1.dp,
        minimumTouchHeight = 44.dp,
        optionPadding = PaddingValues(0.dp),
        optionSpacing = 12.dp,
        groupSpacing = 4.dp,
        labelStyle = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        ),
        stateAnimationMillis = 120,
    )
    return MillenniumRadioGroupTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(indicatorBorderWidth = 2.dp),
            pressed = base,
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(indicatorBorderWidth = 2.dp),
        ),
    )
}

/**
 * A vertical group of mutually exclusive options rendered as beveled radio buttons.
 *
 * Use it to pick exactly one value from a short list. Selection is caller-owned: pass the current
 * [value], update it in [onValueChange] and supply [options] of any type [T].
 *
 * Built on Compose Unstyled `UnstyledRadioGroup` and `RadioButton`, which provide the radio-group
 * semantics, per-option selection, [label] as the group's accessibility label and arrow-key
 * navigation behaviour of the primitive. Each option row has a 44dp default minimum height. Option
 * state resolves as pressed, keyboard-focused (`collectIsKeyboardFocusedAsState`, no ring after pointer
 * presses), hovered, selected, default. Feedback stays on the small indicator (spring scale, border
 * colour, interaction lighting) instead of washing the row; the selection dot pops in with a spring.
 * The hand cursor is shown over options.
 *
 * [label] names the group for accessibility; option labels are always drawn. Pass `enabled = false` to dim
 * and disable every option.
 *
 * Nullable parameters override single theme properties (`param ?: themeValue ?: fallback`).
 *
 * ```kotlin
 * var quality by remember { mutableStateOf("Original") }
 * MillenniumRadioGroup(
 *     value = quality,
 *     onValueChange = { quality = it },
 *     options = listOf("Original", "Remastered", "Hi-fi"),
 *     label = "Playback quality",
 * )
 * ```
 *
 * @param T Option type; compared with `==` against [value] to determine the selection.
 * @param value The currently selected option; caller-owned.
 * @param onValueChange Called with the option the user selects.
 * @param options The options, shown top to bottom in order.
 * @param label Accessibility label of the group; also required for option labels to be rendered (see note).
 * @param modifier Modifier applied to the group container.
 * @param optionLabel Maps an option to its display text; defaults to `toString()`.
 * @param indicatorBackground Overrides the unselected indicator brush; `null` falls back to the theme or sunken bevel.
 * @param selectedIndicatorBackground Overrides the selected indicator brush; `null` falls back to the theme or accent gel.
 * @param indicatorShape Overrides the indicator outline; `null` falls back to the theme.
 * @param indicatorSize Overrides the indicator size; `null` falls back to the theme (22dp).
 * @param dotSize Overrides the selection dot size; `null` falls back to the theme (8dp).
 * @param dotColor Overrides the selection dot colour; `null` falls back to the theme or on-accent.
 * @param indicatorBorderColor Overrides the border colour; `null` falls back to the theme or palette.
 * @param indicatorBorderWidth Overrides the border thickness; `null` falls back to the state's value.
 * @param indicatorGlowColor Overrides the indicator inner highlight; `null` falls back to the theme or palette.
 * @param indicatorElevation Overrides the indicator shadow elevation; `null` falls back to the theme.
 * @param minimumTouchHeight Overrides the minimum option height; `null` falls back to the theme (44dp).
 * @param optionPadding Overrides the padding around each option; `null` falls back to the theme.
 * @param optionSpacing Overrides the gap between indicator and label; `null` falls back to the theme.
 * @param groupSpacing Overrides the vertical gap between options; `null` falls back to the theme's default state.
 * @param labelStyle Overrides the option label typography; `null` falls back to the theme.
 * @param labelColor Overrides the option label colour; `null` falls back to the theme or text colour.
 * @param selectedLabelColor Overrides the selected option's label colour; `null` falls back to the theme or text colour.
 * @param stateAnimationMillis Overrides the tween duration; `null` falls back to the theme.
 * @param alpha Overrides the option opacity; `null` falls back to the state's value.
 * @param enabled When `false` every option is dimmed, ignores input and resolves the disabled theme state.
 * @param theme Theme supplying state properties; defaults to [MillenniumTheme.radioGroupTheme].
 * @see MillenniumCheckbox
 * @see MillenniumRadioGroupTheme
 */
@Composable
fun <T> MillenniumRadioGroup(
    value: T,
    onValueChange: (T) -> Unit,
    options: List<T>,
    label: String? = null,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    indicatorBackground: Brush? = null,
    selectedIndicatorBackground: Brush? = null,
    indicatorShape: Shape? = null,
    indicatorSize: DpSize? = null,
    dotSize: Dp? = null,
    dotColor: Color? = null,
    indicatorBorderColor: Color? = null,
    indicatorBorderWidth: Dp? = null,
    indicatorGlowColor: Color? = null,
    indicatorElevation: Dp? = null,
    minimumTouchHeight: Dp? = null,
    optionPadding: PaddingValues? = null,
    optionSpacing: Dp? = null,
    groupSpacing: Dp? = null,
    labelStyle: TextStyle? = null,
    labelColor: Color? = null,
    selectedLabelColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    enabled: Boolean = true,
    theme: MillenniumRadioGroupTheme = MillenniumTheme.radioGroupTheme,
) {
    val colors = MillenniumTheme.colors
    val properties = theme.states.default
    UnstyledRadioGroup(value, onValueChange, modifier, accessibilityLabel = label) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                groupSpacing ?: properties.groupSpacing
            )
        ) {
            options.forEach { option ->
                val selected = value == option
                val interaction = remember(option) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val focused by interaction.collectIsKeyboardFocusedAsState()
                val hovered by interaction.collectIsHoveredAsState()
                val state = when {
                    !enabled -> MillenniumControlState.Disabled
                    pressed -> MillenniumControlState.Pressed
                    focused -> MillenniumControlState.Focused
                    hovered -> MillenniumControlState.Hovered
                    selected -> MillenniumControlState.Selected
                    else -> MillenniumControlState.Default
                }
                val itemProperties = theme.states.resolve(state)
                val resolvedMillis = stateAnimationMillis ?: itemProperties.stateAnimationMillis
                val animatedAlpha = animateFloatAsState(
                    targetValue = alpha ?: itemProperties.alpha,
                    animationSpec = tween(resolvedMillis),
                ).value
                val targetIndicatorBorderColor =
                    indicatorBorderColor ?: itemProperties.indicatorBorderColor ?: when (state) {
                        MillenniumControlState.Focused -> colors.accent
                        MillenniumControlState.Hovered, MillenniumControlState.Pressed -> colors.accent
                        MillenniumControlState.Selected -> colors.accent
                        MillenniumControlState.Error -> colors.danger
                        else -> colors.border
                    }
                val resolvedIndicatorBorderColor by animateColorAsState(
                    targetValue = targetIndicatorBorderColor,
                    animationSpec = tween(resolvedMillis),
                )
                val indicatorScale by animateFloatAsState(
                    targetValue = when {
                        pressed -> 0.90f
                        hovered -> 1.05f
                        else -> 1f
                    },
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                )
                val resolvedLabelColor = labelColor ?: itemProperties.labelColor
                ?: if (state == MillenniumControlState.Disabled) colors.muted else colors.text
                val resolvedSelectedLabelColor =
                    selectedLabelColor ?: itemProperties.selectedLabelColor ?: colors.text
                val animatedLabelColor by animateColorAsState(
                    targetValue = if (selected) resolvedSelectedLabelColor else resolvedLabelColor,
                    animationSpec = tween(resolvedMillis),
                )
                RadioButton(
                    value = option,
                    modifier = Modifier.millenniumActionPointer().heightIn(
                        min = minimumTouchHeight ?: itemProperties.minimumTouchHeight
                    )
                        .alpha(animatedAlpha)
                        .padding(optionPadding ?: itemProperties.optionPadding),
                    // Keep hover feedback on the small control; a row-wide ripple
                    // creates a distracting blue slab behind the option label.
                    indication = null,
                    interactionSource = interaction,
                    enabled = enabled,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            optionSpacing ?: itemProperties.optionSpacing
                        ),
                    ) {
                        Box(
                            Modifier.size(indicatorSize ?: itemProperties.indicatorSize)
                                .graphicsLayer { scaleX = indicatorScale; scaleY = indicatorScale }
                                .millenniumSurface(
                                    background = if (selected) selectedIndicatorBackground
                                        ?: itemProperties.selectedIndicatorBackground
                                        ?: gelBrush(colors.accentTop, colors.accentBottom)
                                    else indicatorBackground ?: itemProperties.indicatorBackground
                                    ?: millenniumBevelBrush(colors, sunken = true),
                                    shape = indicatorShape ?: itemProperties.indicatorShape,
                                    borderColor = resolvedIndicatorBorderColor,
                                    borderWidth = indicatorBorderWidth
                                        ?: itemProperties.indicatorBorderWidth,
                                    shadowElevation = indicatorElevation
                                        ?: itemProperties.indicatorElevation,
                                    innerGlowColor = indicatorGlowColor
                                        ?: itemProperties.indicatorGlowColor
                                        ?: if (selected) colors.highlight.copy(alpha = 0.32f) else null,
                                )
                                .millenniumInteractionSurface(state, indicatorShape ?: itemProperties.indicatorShape, animationMillis = resolvedMillis),
                            contentAlignment = Alignment.Center,
                        ) {
                            SelectedIndicator(
                                enter = scaleIn(
                                    initialScale = 0.3f,
                                    animationSpec = spring(
                                        dampingRatio = 0.52f,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                ) + fadeIn(tween(resolvedMillis, easing = FastOutSlowInEasing)),
                                exit = scaleOut(
                                    targetScale = 0.3f,
                                    animationSpec = tween((resolvedMillis * 0.75f).toInt().coerceAtLeast(70), easing = FastOutLinearInEasing),
                                ) + fadeOut(tween((resolvedMillis * 0.75f).toInt().coerceAtLeast(70), easing = FastOutLinearInEasing)),
                            ) {
                                Box(
                                    Modifier.size(dotSize ?: itemProperties.dotSize)
                                        .background(
                                            dotColor ?: itemProperties.dotColor ?: colors.onAccent,
                                            CircleShape
                                        ),
                                )
                            }
                        }
                        CompositionLocalProvider(
                            LocalTextStyle provides LocalTextStyle.current.merge(
                                labelStyle ?: itemProperties.labelStyle
                            )
                                .copy(color = animatedLabelColor),
                        ) { Text(optionLabel(option)) }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MillenniumRadioGroupPreview() = MillenniumPreview {
    MillenniumRadioGroup(
        value = "Original",
        onValueChange = {},
        options = listOf("Original", "Remastered", "Hi-fi"),
        label = "Playback quality",
    )
}
