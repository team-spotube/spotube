package org.spotube.millennium.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.composeunstyled.BottomSheetState
import com.composeunstyled.DragIndication
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledBottomSheet
import com.composeunstyled.rememberBottomSheetState
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
 * Appearance of one control state of [MillenniumBottomSheet].
 *
 * Normally produced by [defaultMillenniumBottomSheetTheme] and customised with `copy`. The composable
 * resolves only [MillenniumControlState.Default] from the owning [MillenniumBottomSheetTheme].
 *
 * @property background Brush of the sheet; `null` derives the palette panel brush (pearl/smoke glass).
 * @property foreground Default content colour; `null` uses the palette text colour.
 * @property shape Shape of the sheet; the default rounds only the top corners.
 * @property maximumWidth Maximum sheet width in dp; the sheet is centered at the top of its area.
 * @property contentPadding Padding inside the sheet around the handle and content.
 * @property contentSpacing Vertical gap in dp between the drag handle and the content, and between content
 * children.
 * @property handleSize Width and height of the drag handle pill.
 * @property handleColor Handle colour; `null` uses the palette muted colour.
 * @property borderColor Hairline colour; `null` uses the palette border colour.
 * @property borderWidth Hairline thickness in dp.
 * @property shadowElevation Shadow elevation in dp.
 * @property innerGlowColor Inner highlight colour; `null` uses the palette highlight at 55% alpha.
 * @property alpha Overall opacity multiplier (1 = opaque).
 */
@Immutable
data class MillenniumBottomSheetThemeProperties(
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
    val alpha: Float,
)

/**
 * Theme for [MillenniumBottomSheet], grouping [MillenniumBottomSheetThemeProperties] per control state and
 * installed as [MillenniumTheme.bottomSheetTheme].
 *
 * @property states State-resolved properties; the sheet resolves [MillenniumControlState.Default].
 * @see defaultMillenniumBottomSheetTheme
 */
@Immutable
data class MillenniumBottomSheetTheme(
    val states: MillenniumControlStateProperty<MillenniumBottomSheetThemeProperties>,
)

/**
 * Creates the default bottom-sheet theme: 18dp top-rounded pearl/smoke glass, at most 640dp wide, 24dp
 * padding, 16dp spacing, a 40x5dp handle, a 1dp hairline and a 10dp shadow. Focused and error states use a
 * 2dp border, pressed lowers the shadow to 6dp and disabled fades to 55%.
 *
 * Customise with `copy`:
 * ```kotlin
 * val base = defaultMillenniumBottomSheetTheme()
 * val theme = base.copy(states = base.states.copy(default = base.states.default.copy(maximumWidth = 480.dp)))
 * ```
 *
 * @return A new [MillenniumBottomSheetTheme].
 */
fun defaultMillenniumBottomSheetTheme(): MillenniumBottomSheetTheme {
    val base = MillenniumBottomSheetThemeProperties(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        maximumWidth = 640.dp,
        contentPadding = PaddingValues(24.dp),
        contentSpacing = 16.dp,
        handleSize = DpSize(40.dp, 5.dp),
        borderWidth = 1.dp,
        shadowElevation = 10.dp,
        alpha = 1f,
    )
    return MillenniumBottomSheetTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base.copy(shadowElevation = 6.dp),
            disabled = base.copy(alpha = 0.55f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * A non-modal, draggable bottom sheet in pearl/smoke glass with a tactile drag handle.
 *
 * Use it for persistent supplementary content that sits over the page without blocking it (for example a
 * mini-player queue). For a blocking sheet with a scrim use [MillenniumModalBottomSheet].
 *
 * Behaviour: wraps Compose Unstyled's `UnstyledBottomSheet`, which owns dragging, detents and the sheet
 * semantics; position is controlled entirely through the caller-owned [state]. The sheet fills the size
 * given by [modifier] (it uses a full-size box internally), centers a panel capped at [maximumWidth] at the
 * top and pads its content with the system navigation bars. The handle is a `DragIndication` that reacts to
 * hover and press with an interaction highlight and the action cursor. [content] is wrapped so that text
 * and icons inherit the resolved foreground colour.
 *
 * ```kotlin
 * val sheetState = rememberBottomSheetState(initialDetent = SheetDetent.Hidden)
 * Box(Modifier.fillMaxSize()) {
 *     MainScreen()
 *     MillenniumButton(onClick = { sheetState.targetDetent = SheetDetent.FullyExpanded }) {
 *         MillenniumText("Show queue")
 *     }
 *     MillenniumBottomSheet(sheetState, Modifier.fillMaxSize()) {
 *         MillenniumText("Up next", weight = FontWeight.SemiBold)
 *         MillenniumText("Three tracks queued", color = MillenniumTheme.colors.muted)
 *     }
 * }
 * ```
 *
 * @param state Caller-owned Compose Unstyled sheet state (create with `rememberBottomSheetState`); controls
 * the current detent.
 * @param modifier Modifier applied to the sheet host.
 * @param background Overrides the sheet brush; `null` falls back to the theme, then the panel brush.
 * @param foreground Overrides the content colour; `null` falls back to the theme, then the palette text colour.
 * @param shape Overrides the sheet shape; `null` falls back to the theme.
 * @param maximumWidth Overrides the maximum sheet width; `null` falls back to the theme.
 * @param contentPadding Overrides the inner padding; `null` falls back to the theme.
 * @param contentSpacing Overrides the vertical spacing; `null` falls back to the theme.
 * @param handleSize Overrides the handle size; `null` falls back to the theme.
 * @param handleColor Overrides the handle colour; `null` falls back to the theme, then the palette muted colour.
 * @param borderColor Overrides the hairline colour; `null` falls back to the theme, then the palette border.
 * @param borderWidth Overrides the hairline thickness; `null` falls back to the theme.
 * @param shadowElevation Overrides the shadow elevation; `null` falls back to the theme.
 * @param innerGlowColor Overrides the inner glow; `null` falls back to the theme, then the palette highlight.
 * @param alpha Overrides the overall opacity; `null` falls back to the theme.
 * @param theme Theme supplying defaults; defaults to [MillenniumTheme.bottomSheetTheme].
 * @param content Sheet content, laid out in a centered [ColumnScope] below the handle.
 * @see MillenniumModalBottomSheet
 * @see MillenniumBottomSheetTheme
 */
@Composable
fun MillenniumBottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    background: Brush? = null,
    foreground: Color? = null,
    shape: Shape? = null,
    maximumWidth: Dp? = null,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    handleSize: DpSize? = null,
    handleColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp? = null,
    innerGlowColor: Color? = null,
    alpha: Float? = null,
    theme: MillenniumBottomSheetTheme = MillenniumTheme.bottomSheetTheme,
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
    UnstyledBottomSheet(state, modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            Sheet(
                Modifier.widthIn(max = maximumWidth ?: properties.maximumWidth).fillMaxWidth().millenniumSurface(
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

/** Preview of a fully expanded bottom sheet. */
@Preview
@Composable
private fun MillenniumBottomSheetPreview() = MillenniumPreview {
    val state = rememberBottomSheetState(initialDetent = SheetDetent.FullyExpanded)
    MillenniumBottomSheet(state) {
        MillenniumText("Hello from down here.", weight = FontWeight.SemiBold)
        MillenniumText("A softly raised sheet with a tactile handle.", color = MillenniumTheme.colors.muted)
    }
}
