package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.UnstyledSlider
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
 * State-specific appearance and layout of a [MillenniumSlider].
 *
 * @property trackBackground Track brush; `null` = sunken bevel.
 * @property activeTrackBackground Filled-portion brush; `null` = accent gel.
 * @property thumbBackground Thumb brush; `null` = raised bevel.
 * @property trackShape Outline of the track and its filled portion; default fully rounded (50%).
 * @property thumbShape Thumb outline; default 6dp rounded rectangle.
 * @property trackHeight Track thickness; default `10.dp`.
 * @property thumbSize Resting thumb size; default `24.dp`. The track is inset by half of the default
 * state's value so the fill endpoint meets the thumb centre.
 * @property activeThumbSize Thumb size while pressed or dragged; default `26.dp`.
 * @property touchTargetHeight Minimum height of the whole slider; default `44.dp`.
 * @property trackBorderColor Track border; `null` = danger in error, otherwise the palette border.
 * @property trackBorderWidth Track border thickness; `1.dp`, `2.dp` in error.
 * @property thumbBorderColor Thumb border; `null` = accent when focused/hovered/pressed, danger in error, else palette border.
 * @property thumbBorderWidth Thumb border thickness; `1.dp`, `2.dp` when focused or in error.
 * @property thumbElevation Thumb drop-shadow elevation; `4.dp`, `2.dp` while pressed.
 * @property thumbGlowColor Thumb inner highlight; `null` = palette highlight at 55%.
 * @property stateAnimationMillis Duration in ms of the thumb border tween and interaction lighting. Default `120`.
 * @property alpha Overall opacity; `0.45f` when disabled.
 * @property thumbBottomRimColor Thumb lower inner rim; `null` = palette highlight at 60%.
 * @property trackInsetShadeColor Inner shade at the track's top edge; `null` = text colour at 10%.
 */
@Immutable
data class MillenniumSliderThemeProperties(
    val trackBackground: Brush? = null,
    val activeTrackBackground: Brush? = null,
    val thumbBackground: Brush? = null,
    val trackShape: Shape,
    val thumbShape: Shape,
    val trackHeight: Dp,
    val thumbSize: Dp,
    val activeThumbSize: Dp,
    val touchTargetHeight: Dp,
    val trackBorderColor: Color? = null,
    val trackBorderWidth: Dp,
    val thumbBorderColor: Color? = null,
    val thumbBorderWidth: Dp,
    val thumbElevation: Dp,
    val thumbGlowColor: Color? = null,
    val stateAnimationMillis: Int,
    val alpha: Float = 1f,
    val thumbBottomRimColor: Color? = null,
    val trackInsetShadeColor: Color? = null,
)

/**
 * Theme for [MillenniumSlider]: one [MillenniumSliderThemeProperties] per control state.
 * Installed as [MillenniumTheme.sliderTheme].
 *
 * @property states Properties for each control state (default, hovered, focused, pressed, disabled,
 * selected, error).
 * @see defaultMillenniumSliderTheme
 */
@Immutable
data class MillenniumSliderTheme(
    val states: MillenniumControlStateProperty<MillenniumSliderThemeProperties>,
)

/**
 * Creates the default [MillenniumSliderTheme].
 *
 * A 10dp fully rounded recessed track with a gel fill, a 24dp rounded-rectangle thumb that grows to
 * 26dp while dragged, a 44dp minimum touch height, 4dp thumb shadow (2dp pressed) and 120ms tweens.
 * Focused uses a 2dp thumb border; error uses 2dp track and thumb borders; disabled uses
 * `alpha = 0.45f`. Colours are `null` and follow the palette. Customise with `copy`:
 *
 * ```kotlin
 * val base = defaultMillenniumSliderTheme()
 * val thick = base.copy(states = base.states.copy(default = base.states.default.copy(trackHeight = 14.dp)))
 * ```
 *
 * @return A new default theme.
 */
fun defaultMillenniumSliderTheme(): MillenniumSliderTheme {
    val base = MillenniumSliderThemeProperties(
        trackShape = RoundedCornerShape(50),
        thumbShape = RoundedCornerShape(6.dp),
        trackHeight = 10.dp,
        thumbSize = 24.dp,
        activeThumbSize = 26.dp,
        touchTargetHeight = 44.dp,
        trackBorderWidth = 1.dp,
        thumbBorderWidth = 1.dp,
        thumbElevation = 4.dp,
        stateAnimationMillis = 120,
    )
    return MillenniumSliderTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(thumbBorderWidth = 2.dp),
            pressed = base.copy(thumbElevation = 2.dp),
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(trackBorderWidth = 2.dp, thumbBorderWidth = 2.dp),
        ),
    )
}

/**
 * A horizontal value slider with a recessed gel track and a beveled thumb.
 *
 * Use it to pick a continuous value (or discrete steps) from a range, such as volume or progress
 * scrubbing. The value is caller-owned: pass [value] and update it in [onValueChange].
 *
 * Built on Compose Unstyled `UnstyledSlider`, which provides drag/tap handling, slider semantics and
 * keyboard adjustment; [label] is applied as the content description. The slider fills the available
 * width with a 44dp default minimum height. State resolves as disabled, pressed/dragged,
 * keyboard-focused (`collectIsKeyboardFocusedAsState`, no ring after pointer presses), hovered,
 * default. The thumb grows from [thumbSize] to [activeThumbSize] with a spring while pressed or dragged,
 * its border colour tweens and its shadow animates. The track is inset by half a thumb so the filled
 * portion ends at the thumb centre. The hand cursor is shown while enabled.
 *
 * Nullable parameters override single theme properties (`param ?: themeValue ?: fallback`).
 *
 * ```kotlin
 * var volume by remember { mutableStateOf(0.68f) }
 * MillenniumSlider(
 *     value = volume,
 *     onValueChange = { volume = it },
 *     label = "Volume",
 *     onValueChangeFinished = { println("volume settled at $volume") },
 * )
 * ```
 *
 * @param value Current value, expected within [valueRange]; caller-owned.
 * @param onValueChange Called continuously with the new value while the user drags or presses keys.
 * @param label Accessibility description of the slider (announced by screen readers); required.
 * @param modifier Modifier applied to the slider (after the hand cursor and before fill-width).
 * @param enabled When `false` the slider ignores input and uses the disabled state (45% alpha by default).
 * @param valueRange Inclusive range of selectable values; default `0f..1f`.
 * @param steps Number of discrete stops between the range ends; `0` (default) means continuous.
 * @param onValueChangeFinished Called when a drag or key interaction ends; `null` (default) for none.
 * @param trackBackground Overrides the track brush; `null` falls back to the theme or sunken bevel.
 * @param activeTrackBackground Overrides the filled-portion brush; `null` falls back to the theme or accent gel.
 * @param thumbBackground Overrides the thumb brush; `null` falls back to the theme or raised bevel.
 * @param trackShape Overrides the track outline; `null` falls back to the theme.
 * @param thumbShape Overrides the thumb outline; `null` falls back to the theme.
 * @param trackHeight Overrides the track thickness; `null` falls back to the theme (10dp).
 * @param thumbSize Overrides the resting thumb size; `null` falls back to the theme (24dp).
 * @param activeThumbSize Overrides the pressed/dragged thumb size; `null` falls back to the theme (26dp).
 * @param touchTargetHeight Overrides the minimum slider height; `null` falls back to the theme (44dp).
 * @param trackBorderColor Overrides the track border colour; `null` falls back to the theme or palette.
 * @param trackBorderWidth Overrides the track border thickness; `null` falls back to the state's value.
 * @param thumbBorderColor Overrides the thumb border colour; `null` falls back to the theme or palette.
 * @param thumbBorderWidth Overrides the thumb border thickness; `null` falls back to the state's value.
 * @param thumbElevation Overrides the thumb shadow elevation; `null` falls back to the state's value.
 * @param thumbGlowColor Overrides the thumb inner highlight; `null` falls back to the theme or palette.
 * @param thumbBottomRimColor Overrides the thumb lower inner rim; `null` falls back to the theme or palette.
 * @param trackInsetShadeColor Overrides the track inner shade; `null` falls back to the theme or 10% text colour.
 * @param stateAnimationMillis Overrides the tween duration; `null` falls back to the theme.
 * @param alpha Overrides the overall opacity; `null` falls back to the state's value.
 * @param theme Theme supplying state properties; defaults to [MillenniumTheme.sliderTheme].
 * @see MillenniumSliderTheme
 * @see MillenniumProgress
 */
@Composable
fun MillenniumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackBackground: Brush? = null,
    activeTrackBackground: Brush? = null,
    thumbBackground: Brush? = null,
    trackShape: Shape? = null,
    thumbShape: Shape? = null,
    trackHeight: Dp? = null,
    thumbSize: Dp? = null,
    activeThumbSize: Dp? = null,
    touchTargetHeight: Dp? = null,
    trackBorderColor: Color? = null,
    trackBorderWidth: Dp? = null,
    thumbBorderColor: Color? = null,
    thumbBorderWidth: Dp? = null,
    thumbElevation: Dp? = null,
    thumbGlowColor: Color? = null,
    thumbBottomRimColor: Color? = null,
    trackInsetShadeColor: Color? = null,
    stateAnimationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumSliderTheme = MillenniumTheme.sliderTheme,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val dragged by interaction.collectIsDraggedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val controlState = when {
        !enabled -> MillenniumControlState.Disabled
        dragged || pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(controlState)
    val colors = MillenniumTheme.colors
    val resolvedTrackBackground = trackBackground ?: properties.trackBackground ?: millenniumBevelBrush(colors, sunken = true)
    val resolvedActiveTrackBackground = activeTrackBackground
        ?: properties.activeTrackBackground ?: gelBrush(colors.accentTop, colors.accentBottom)
    val resolvedTrackBorderColor = trackBorderColor ?: properties.trackBorderColor
        ?: if (controlState == MillenniumControlState.Error) colors.danger else colors.border
    val baseThumbSize = thumbSize ?: theme.states.default.thumbSize
    // UnstyledSlider's tap handler captures `enabled` and `onValueChangeFinished` once and never
    // restarts on their change: a slider created disabled ignores taps forever, and a tap reports
    // to a stale callback. Recreate it when `enabled` flips and route the callback through state.
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    key(enabled) {
        UnstyledSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.millenniumActionPointer(enabled).fillMaxWidth()
                .hoverable(interaction, enabled)
                .heightIn(min = touchTargetHeight ?: properties.touchTargetHeight)
                .alpha(alpha ?: properties.alpha).semantics { contentDescription = label },
            enabled = enabled,
            interactionSource = interaction,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = { currentOnValueChangeFinished?.invoke() },
            track = { state ->
                // The thumb travels between half-thumb insets; inset the track the same way so
                // the active fill endpoint lines up with the thumb center.
                Box(Modifier.fillMaxWidth().padding(horizontal = baseThumbSize / 2)) {
                    Box(
                        Modifier.fillMaxWidth().height(trackHeight ?: properties.trackHeight).millenniumSurface(
                            background = resolvedTrackBackground,
                            shape = trackShape ?: properties.trackShape,
                            borderColor = resolvedTrackBorderColor,
                            borderWidth = trackBorderWidth ?: properties.trackBorderWidth,
                            insetShadeColor = trackInsetShadeColor ?: properties.trackInsetShadeColor
                                ?: colors.text.copy(alpha = 0.1f),
                        )
                        .millenniumInteractionSurface(controlState, trackShape ?: properties.trackShape, animationMillis = stateAnimationMillis ?: properties.stateAnimationMillis),
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(state.fraction)
                                .background(resolvedActiveTrackBackground, trackShape ?: properties.trackShape),
                        )
                    }
                }
            },
            thumb = { state ->
                val thumbState = when {
                    !enabled -> MillenniumControlState.Disabled
                    state.isDragging || state.isPressed -> MillenniumControlState.Pressed
                    focused -> MillenniumControlState.Focused
                    hovered -> MillenniumControlState.Hovered
                    else -> MillenniumControlState.Default
                }
                val thumbProperties = theme.states.resolve(thumbState)
                val resolvedThumbBackground = thumbBackground ?: thumbProperties.thumbBackground
                    ?: millenniumBevelBrush(colors)
                val resolvedThumbBorderColor = thumbBorderColor ?: thumbProperties.thumbBorderColor ?: when (thumbState) {
                    MillenniumControlState.Focused, MillenniumControlState.Selected,
                    MillenniumControlState.Hovered, MillenniumControlState.Pressed -> colors.accent
                    MillenniumControlState.Error -> colors.danger
                    else -> colors.border
                }
                val animatedThumbBorderColor by animateColorAsState(
                    targetValue = resolvedThumbBorderColor,
                    animationSpec = tween(stateAnimationMillis ?: thumbProperties.stateAnimationMillis),
                )
                val resolvedThumbGlowColor = thumbGlowColor ?: thumbProperties.thumbGlowColor
                    ?: colors.highlight.copy(alpha = 0.55f)
                val targetSize = if (state.isPressed || state.isDragging) {
                    activeThumbSize ?: thumbProperties.activeThumbSize
                } else {
                    thumbSize ?: thumbProperties.thumbSize
                }
                val animatedSize by animateDpAsState(
                    targetSize,
                    spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
                )
                val animatedElevation by animateDpAsState(
                    thumbElevation ?: thumbProperties.thumbElevation,
                    spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                )
                Box(Modifier.size(baseThumbSize), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(animatedSize).millenniumSurface(
                            background = resolvedThumbBackground,
                            shape = thumbShape ?: thumbProperties.thumbShape,
                            borderColor = animatedThumbBorderColor,
                            borderWidth = thumbBorderWidth ?: thumbProperties.thumbBorderWidth,
                            shadowElevation = animatedElevation,
                            innerGlowColor = resolvedThumbGlowColor,
                            bottomRimColor = thumbBottomRimColor ?: thumbProperties.thumbBottomRimColor
                                ?: colors.highlight.copy(alpha = 0.6f),
                        )
                        .millenniumInteractionSurface(thumbState, thumbShape ?: thumbProperties.thumbShape, animationMillis = stateAnimationMillis ?: thumbProperties.stateAnimationMillis),
                    )
                }
            },
        )
    }
}

@Preview
@Composable
private fun MillenniumSliderPreview() = MillenniumPreview {
    MillenniumSlider(value = 0.68f, onValueChange = {}, label = "Volume")
}
