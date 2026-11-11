package org.spotube.millennium.composables

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.composeunstyled.DragIndication
import com.composeunstyled.ModalBottomSheetState
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumInteractionSurface
import org.spotube.millennium.theme.millenniumActionPointer
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.MillenniumStyledContent
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumPanelBrush

/**
 * Appearance of one control state of [MillenniumModalBottomSheet].
 *
 * Normally produced by [defaultMillenniumModalBottomSheetTheme] and customised with `copy`. The composable
 * resolves only [MillenniumControlState.Default] from the owning [MillenniumModalBottomSheetTheme].
 *
 * @property background Brush of the sheet; `null` derives the palette panel brush (pearl/smoke glass).
 * @property foreground Default content colour; `null` uses the palette text colour.
 * @property shape Shape of the sheet; the default rounds only the top corners.
 * @property maximumWidth Maximum sheet width in dp; the sheet is centered horizontally.
 * @property contentPadding Padding inside the sheet around the handle and content.
 * @property contentSpacing Vertical gap in dp between the drag handle and the content, and between content
 * children.
 * @property handleSize Width and height of the drag handle pill.
 * @property handleColor Handle colour; `null` uses the palette muted colour.
 * @property borderColor Hairline colour; `null` uses the palette border colour.
 * @property borderWidth Hairline thickness in dp.
 * @property shadowElevation Shadow elevation in dp.
 * @property innerGlowColor Inner highlight colour; `null` uses the palette highlight at 55% alpha.
 * @property scrimColor Colour of the dimming scrim behind the sheet; `null` uses the palette scrim colour.
 * @property animationMillis Scrim fade-in duration in ms; the fade-out uses 80% of it (minimum 120 ms).
 * @property alpha Overall opacity multiplier (1 = opaque).
 */
@Immutable
data class MillenniumModalBottomSheetThemeProperties(
    val background: Brush? = null,
    val foreground: Color? = null,
    val shape: Shape,
    val maximumWidth: Dp,
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val handleSize: DpSize,
    val handleColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val innerGlowColor: Color? = null,
    val scrimColor: Color? = null,
    val animationMillis: Int,
    val alpha: Float,
)

/**
 * Theme for [MillenniumModalBottomSheet], grouping [MillenniumModalBottomSheetThemeProperties] per control
 * state and installed as [MillenniumTheme.modalBottomSheetTheme].
 *
 * @property states State-resolved properties; the sheet resolves [MillenniumControlState.Default].
 * @see defaultMillenniumModalBottomSheetTheme
 */
@Immutable
data class MillenniumModalBottomSheetTheme(
    val states: MillenniumControlStateProperty<MillenniumModalBottomSheetThemeProperties>,
)

/**
 * Creates the default modal bottom-sheet theme: 18dp top-rounded pearl/smoke glass, at most 640dp wide,
 * 24dp padding, 16dp spacing, a 40x5dp handle, a 1dp hairline, a 12dp shadow and a 220ms scrim fade. Focused
 * and error states use a 2dp border, pressed lowers the shadow to 8dp and disabled fades to 55%.
 *
 * Customise with `copy`:
 * ```kotlin
 * val base = defaultMillenniumModalBottomSheetTheme()
 * val theme = base.copy(states = base.states.copy(default = base.states.default.copy(animationMillis = 300)))
 * ```
 *
 * @return A new [MillenniumModalBottomSheetTheme].
 */
fun defaultMillenniumModalBottomSheetTheme(): MillenniumModalBottomSheetTheme {
    val base = MillenniumModalBottomSheetThemeProperties(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        maximumWidth = 640.dp,
        contentPadding = PaddingValues(24.dp),
        contentSpacing = 16.dp,
        handleSize = DpSize(40.dp, 5.dp),
        borderWidth = 1.dp,
        shadowElevation = 12.dp,
        animationMillis = 220,
        alpha = 1f,
    )
    return MillenniumModalBottomSheetTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base.copy(shadowElevation = 8.dp),
            disabled = base.copy(alpha = 0.55f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * A modal, draggable bottom sheet in pearl/smoke glass over a dimming scrim.
 *
 * Use it for focused tasks or choices that should block the page behind them (share, filters, confirmations).
 * For a non-blocking sheet use [MillenniumBottomSheet].
 *
 * Behaviour: wraps Compose Unstyled's `UnstyledModalBottomSheet`, which presents the sheet as a modal
 * overlay and owns detents, dragging, scrim dismissal and the modal semantics; showing and hiding is driven
 * by the caller-owned [state]. The scrim is a Compose Unstyled `Scrim` with the action cursor, fading in over
 * [animationMillis] and out over 80% of it (minimum 120 ms). The panel is capped at [maximumWidth],
 * centered, and padded with the navigation bars. The handle is a `DragIndication` with hover and press
 * feedback. No dismissal properties are passed, so Compose Unstyled's defaults apply: tapping the scrim
 * (click outside) and pressing Escape or Back dismiss the sheet.
 *
 * ```kotlin
 * val sheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.Hidden)
 * MillenniumButton(onClick = { sheetState.targetDetent = SheetDetent.FullyExpanded }) {
 *     MillenniumText("Share")
 * }
 * MillenniumModalBottomSheet(sheetState) {
 *     MillenniumText("Share this playlist", weight = FontWeight.SemiBold)
 *     MillenniumButton(onClick = { sheetState.targetDetent = SheetDetent.Hidden }) { MillenniumText("Done") }
 * }
 * ```
 *
 * @param state Caller-owned Compose Unstyled modal-sheet state (create with `rememberModalBottomSheetState`).
 * @param modifier Applied to the sheet panel itself (before the width cap, surface and padding).
 * @param contentPadding Overrides the inner padding; `null` falls back to the theme.
 * @param background Overrides the sheet brush; `null` falls back to the theme, then the panel brush.
 * @param foreground Overrides the content colour; `null` falls back to the theme, then the palette text colour.
 * @param shape Overrides the sheet shape; `null` falls back to the theme.
 * @param maximumWidth Overrides the maximum sheet width; `null` falls back to the theme.
 * @param contentSpacing Overrides the vertical spacing; `null` falls back to the theme.
 * @param handleSize Overrides the handle size; `null` falls back to the theme.
 * @param handleColor Overrides the handle colour; `null` falls back to the theme, then the palette muted colour.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then the palette highlight.
 * @param scrimColor Overrides the scrim colour; `null` falls back to the theme, then the palette scrim colour.
 * @param animationMillis Overrides the scrim fade-in duration in ms; `null` falls back to the theme.
 * @param alpha Overrides the overall opacity; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.modalBottomSheetTheme].
 * @param content Sheet content, laid out in a centered [ColumnScope] below the handle.
 * @see MillenniumBottomSheet
 * @see MillenniumModalBottomSheetTheme
 */
@Composable
fun MillenniumModalBottomSheet(
    state: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    maximumWidth: Dp? = null,
    contentSpacing: Dp? = null,
    handleSize: DpSize? = null,
    handleColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    scrimColor: Color? = null,
    animationMillis: Int? = null,
    alpha: Float? = null,
    theme: MillenniumModalBottomSheetTheme = MillenniumTheme.modalBottomSheetTheme,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MillenniumTheme.colors
    val handleInteraction = remember { MutableInteractionSource() }
    val handleHovered by handleInteraction.collectIsHoveredAsState()
    val handlePressed by handleInteraction.collectIsPressedAsState()
    val handleState = when {
        handlePressed -> MillenniumControlState.Pressed
        handleHovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(MillenniumControlState.Default)
    val resolvedBackground = background ?: properties.background ?: millenniumPanelBrush(colors)
    val resolvedForeground = foreground ?: properties.foreground ?: colors.text
    val resolvedHandleColor = handleColor ?: properties.handleColor ?: colors.muted
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: colors.border
    val resolvedGlowColor = innerGlowColor ?: properties.innerGlowColor ?: colors.highlight.copy(alpha = 0.55f)
    val resolvedScrimColor = scrimColor ?: properties.scrimColor ?: colors.scrim
    val resolvedAnimationMillis = animationMillis ?: properties.animationMillis
    UnstyledModalBottomSheet(
        state = state,
        overlay = {
            Scrim(
                modifier = Modifier.millenniumActionPointer(),
                scrimColor = resolvedScrimColor,
                enter = fadeIn(animationSpec = tween(resolvedAnimationMillis, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween((resolvedAnimationMillis * 0.8f).toInt().coerceAtLeast(120), easing = FastOutLinearInEasing)),
            )
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            Sheet(
                modifier.widthIn(max = maximumWidth ?: properties.maximumWidth).fillMaxWidth().millenniumSurface(
                    background = resolvedBackground,
                    shape = shape ?: properties.shape,
                    borderColor = resolvedBorderColor,
                    borderWidth = borderWidth ?: properties.borderWidth,
                    shadowElevation = shadowElevation ?: properties.shadowElevation,
                    innerGlowColor = resolvedGlowColor,
                ).alpha(alpha ?: properties.alpha),
            ) {
                Column(
                    Modifier.padding(contentPadding ?: properties.contentPadding).navigationBarsPadding(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        contentSpacing ?: properties.contentSpacing,
                    ),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    DragIndication(
                        Modifier.millenniumActionPointer().size(handleSize ?: properties.handleSize).background(resolvedHandleColor, CircleShape)
                            .millenniumInteractionSurface(handleState, CircleShape),
                        interactionSource = handleInteraction,
                    )
                    MillenniumStyledContent(TextStyle(color = resolvedForeground), resolvedForeground) {
                        content()
                    }
                }
            }
        }
    }
}

/** Preview of a fully expanded modal bottom sheet. */
@Preview
@Composable
private fun MillenniumModalBottomSheetPreview() = MillenniumPreview {
    val state = rememberModalBottomSheetState(initialDetent = SheetDetent.FullyExpanded)
    MillenniumModalBottomSheet(state) {
        MillenniumText("Your next favorite track", weight = FontWeight.SemiBold)
        MillenniumText("A modal sheet with a soft glass finish.", color = MillenniumTheme.colors.muted)
    }
}
