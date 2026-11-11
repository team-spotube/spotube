package org.spotube.millennium.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import org.spotube.millennium.theme.collectIsKeyboardFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.TextSelectionColors
import com.composeunstyled.Text
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledTextField
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumPreview
import org.spotube.millennium.theme.millenniumBevelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.millennium.theme.millenniumInteractionSurface
import androidx.compose.foundation.hoverable

/**
 * Visual and layout properties of a [MillenniumTextField] in one interaction state.
 *
 * Nullable colour/brush properties are derived from the active palette ([MillenniumTheme.colors]) when null, so a
 * default theme follows light/dark and the selected palette automatically. Instances are normally created through
 * [defaultMillenniumTextFieldTheme] and tweaked with `copy`.
 *
 * @property background Fill of the field box; null uses a sunken bevel brush (`millenniumBevelBrush(sunken = true)`).
 * @property shape Corner shape of the field box (also used for the interaction lighting).
 * @property textStyle Style of the typed text. Its colour is only used when [foreground] is null.
 * @property labelStyle Style of the optional label drawn above the box.
 * @property placeholderStyle Style of the placeholder shown while the field is empty.
 * @property errorStyle Style of the error message drawn below the box.
 * @property foreground Colour of text and label; null falls back to the style colour, then to the palette text
 * colour (muted when disabled).
 * @property placeholderColor Colour of the placeholder; null falls back to the style colour, then the palette muted colour.
 * @property cursorColor Caret colour; null uses the palette accent.
 * @property selectionColor Text-selection highlight; null uses the accent at 25% alpha.
 * @property errorColor Colour of the error message; null uses the palette danger colour.
 * @property borderColor Hairline border colour; null picks accent for hovered/focused/pressed, danger for error and the
 * palette border colour otherwise. Colour changes animate over [stateAnimationMillis].
 * @property borderWidth Border thickness (1dp by default, 2dp when focused or in error).
 * @property minimumHeight Minimum height of the field box (52dp by default, comfortably above a 44dp touch target).
 * @property contentPadding Padding between the box edge and the editable text.
 * @property contentSpacing Vertical gap between label, box and error message.
 * @property stateAnimationMillis Duration in milliseconds of border colour and interaction lighting transitions.
 * @property alpha Whole-field opacity (0.45 in the default disabled state).
 * @property insetShadeColor Inner shade drawn along the top/left edge for the recessed look; null uses text colour at 9%.
 * @property innerGlowColor Inner glow along the box edge; null uses the border colour at 40%.
 * @property bottomRimColor Light rim along the bottom edge; null uses the palette highlight at 80%.
 * @see MillenniumTextFieldTheme
 * @see defaultMillenniumTextFieldTheme
 */
@Immutable
data class MillenniumTextFieldThemeProperties(
    val background: Brush? = null,
    val shape: Shape,
    val textStyle: TextStyle,
    val labelStyle: TextStyle,
    val placeholderStyle: TextStyle,
    val errorStyle: TextStyle,
    val foreground: Color? = null,
    val placeholderColor: Color? = null,
    val cursorColor: Color? = null,
    val selectionColor: Color? = null,
    val errorColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Dp,
    val minimumHeight: Dp,
    val contentPadding: PaddingValues,
    val contentSpacing: Dp,
    val stateAnimationMillis: Int,
    val alpha: Float = 1f,
    val insetShadeColor: Color? = null,
    val innerGlowColor: Color? = null,
    val bottomRimColor: Color? = null,
)

/**
 * Per-state theme of [MillenniumTextField].
 *
 * @property states Properties resolved per [MillenniumControlState]. [MillenniumTextField] resolves Disabled, Error,
 * Pressed, Focused (keyboard-only), Hovered or Default; the Selected state is not used by the text field.
 * @see MillenniumTextFieldThemeProperties
 * @see MillenniumTheme
 */
@Immutable
data class MillenniumTextFieldTheme(
    val states: MillenniumControlStateProperty<MillenniumTextFieldThemeProperties>,
)

/**
 * Creates the default [MillenniumTextFieldTheme] installed as `MillenniumTheme.textFieldTheme`.
 *
 * Defaults: 7dp rounded recessed box, 52dp minimum height, 14dp content padding, 14sp text with 21sp leading,
 * Medium-weight 14sp label, 12sp error text, 1dp border that grows to 2dp when focused or invalid, 130ms state
 * animation, and 0.45 alpha when disabled. All colours are derived from the active palette.
 *
 * To customise, copy the per-state bag and rebuild the theme:
 *
 * ```kotlin
 * val base = defaultMillenniumTextFieldTheme()
 * val roomy = base.copy(
 *     states = base.states.copy(
 *         default = base.states.default.copy(minimumHeight = 60.dp),
 *     ),
 * )
 * MillenniumTextField(state = rememberTextFieldState(), theme = roomy)
 * ```
 *
 * @return A fresh theme instance; the function has no side effects.
 * @see MillenniumTextField
 */
fun defaultMillenniumTextFieldTheme(): MillenniumTextFieldTheme {
    val base = MillenniumTextFieldThemeProperties(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelStyle = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        ),
        placeholderStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        errorStyle = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
        borderWidth = 1.dp,
        minimumHeight = 52.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        contentSpacing = 8.dp,
        stateAnimationMillis = 130,
    )
    return MillenniumTextFieldTheme(
        states = MillenniumControlStateProperty(
            default = base,
            hovered = base,
            focused = base.copy(borderWidth = 2.dp),
            pressed = base,
            disabled = base.copy(alpha = 0.45f),
            selected = base,
            error = base.copy(borderWidth = 2.dp),
        ),
    )
}

/**
 * A single-line Aero text field with an optional label, placeholder and error message.
 *
 * Built on Compose Unstyled's `UnstyledTextField`, so text editing, selection, IME handling and the accessibility
 * label come from the primitive. Millennium adds the recessed bevel box, accent border and interaction lighting.
 * Use it for short single-line input; multi-line input is not supported (line limits are fixed to single line).
 *
 * Behaviour:
 * - Text is owned by the caller through a [TextFieldState]; the field never copies it.
 * - The visual state resolves in this priority: disabled, error (non-null [error]), pressed, keyboard focused,
 *   hovered, default. The focus border only appears for keyboard/accessibility focus
 *   (`collectIsKeyboardFocusedAsState`), not after a pointer press.
 * - Border colour animates over `stateAnimationMillis`; the box also receives the shared interaction lighting.
 * - [label] is drawn above the box when not blank and is passed to the primitive as its accessibility label.
 *   [error] is drawn below the box in the danger colour and switches the border to the error treatment.
 * - The box is at least 52dp tall by default, and the whole field gets the theme alpha when disabled.
 *
 * Example:
 * ```kotlin
 * val email = rememberTextFieldState()
 * val invalid = email.text.isNotEmpty() && '@' !in email.text
 * MillenniumTextField(
 *     state = email,
 *     label = "Email",
 *     placeholder = "you@example.com",
 *     error = if (invalid) "Enter a valid email address" else null,
 *     modifier = Modifier.fillMaxWidth(),
 * )
 * ```
 *
 * @param state Caller-owned text and selection state, usually from `rememberTextFieldState()`.
 * @param label Optional label shown above the field and used as the accessibility label; null or blank hides it.
 * @param modifier Modifier applied to the whole field (label, box and error message).
 * @param placeholder Hint text shown while the field is empty. Empty by default.
 * @param enabled When false the field is not editable or hoverable and shows the disabled state.
 * @param error Error message shown below the box; non-null switches to the error state (null means valid).
 * @param background Overrides the box fill; null uses the theme, then a sunken bevel brush.
 * @param shape Overrides the box shape; null uses the theme.
 * @param textStyle Overrides the typed-text style; null uses the theme.
 * @param labelStyle Overrides the label style; null uses the theme.
 * @param placeholderStyle Overrides the placeholder style; null uses the theme.
 * @param errorStyle Overrides the error-message style; null uses the theme.
 * @param foreground Overrides text and label colour; null uses the theme, then the palette text colour.
 * @param placeholderColor Overrides the placeholder colour; null uses the theme, then the palette muted colour.
 * @param cursorColor Overrides the caret colour; null uses the theme, then the palette accent.
 * @param selectionColor Overrides the selection highlight; null uses the theme, then accent at 25% alpha.
 * @param errorColor Overrides the error message colour; null uses the theme, then the palette danger colour.
 * @param borderColor Overrides the border colour in every state; null lets the state decide (accent, danger, border).
 * @param borderWidth Overrides the border thickness; null uses the state's theme value.
 * @param minimumHeight Overrides the minimum box height; null uses the theme (52dp).
 * @param contentPadding Overrides the padding around the editable text; null uses the theme.
 * @param contentSpacing Overrides the gap between label, box and error; null uses the theme.
 * @param stateAnimationMillis Overrides the border/lighting animation duration; null uses the theme.
 * @param insetShadeColor Overrides the inner top/left shade; null uses the theme, then a faint text-colour shade.
 * @param innerGlowColor Overrides the inner glow; null uses the theme, then the border colour at 40%.
 * @param bottomRimColor Overrides the bottom highlight rim; null uses the theme, then the palette highlight.
 * @param alpha Overrides the whole-field opacity; null uses the state's theme value.
 * @param theme Per-state theme; defaults to `MillenniumTheme.textFieldTheme`. Nullable overrides above take
 * precedence over it (`param ?: themeValue ?: fallback`).
 * @see MillenniumTextFieldTheme
 * @see defaultMillenniumTextFieldTheme
 * @see MillenniumAutocomplete
 */
@Composable
fun MillenniumTextField(
    state: TextFieldState,
    label: String? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    error: String? = null,
    background: Brush? = null,
    shape: Shape? = null,
    textStyle: TextStyle? = null,
    labelStyle: TextStyle? = null,
    placeholderStyle: TextStyle? = null,
    errorStyle: TextStyle? = null,
    foreground: Color? = null,
    placeholderColor: Color? = null,
    cursorColor: Color? = null,
    selectionColor: Color? = null,
    errorColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    minimumHeight: Dp? = null,
    contentPadding: PaddingValues? = null,
    contentSpacing: Dp? = null,
    stateAnimationMillis: Int? = null,
    insetShadeColor: Color? = null,
    innerGlowColor: Color? = null,
    bottomRimColor: Color? = null,
    alpha: Float? = null,
    theme: MillenniumTextFieldTheme = MillenniumTheme.textFieldTheme,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsKeyboardFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val controlState = when {
        !enabled -> MillenniumControlState.Disabled
        error != null -> MillenniumControlState.Error
        pressed -> MillenniumControlState.Pressed
        focused -> MillenniumControlState.Focused
        hovered -> MillenniumControlState.Hovered
        else -> MillenniumControlState.Default
    }
    val properties = theme.states.resolve(controlState)
    val colors = MillenniumTheme.colors
    val resolvedTextStyle = textStyle ?: properties.textStyle
    val resolvedForeground = foreground
        ?: properties.foreground
        ?: resolvedTextStyle.color.takeUnless { it == Color.Unspecified }
        ?: if (controlState == MillenniumControlState.Disabled) colors.muted else colors.text
    val resolvedPlaceholderStyle = placeholderStyle ?: properties.placeholderStyle
    val resolvedPlaceholderColor = placeholderColor
        ?: properties.placeholderColor
        ?: resolvedPlaceholderStyle.color.takeUnless { it == Color.Unspecified }
        ?: colors.muted
    val resolvedCursorColor = cursorColor ?: properties.cursorColor ?: colors.accent
    val resolvedSelectionColor =
        selectionColor ?: properties.selectionColor ?: colors.accent.copy(alpha = 0.25f)
    val resolvedErrorColor = errorColor ?: properties.errorColor ?: colors.danger
    val resolvedBorderColor = borderColor ?: properties.borderColor ?: when (controlState) {
        MillenniumControlState.Focused, MillenniumControlState.Hovered, MillenniumControlState.Pressed -> colors.accent
        MillenniumControlState.Error -> colors.danger
        else -> colors.border
    }
    val animatedBorder by animateColorAsState(
        targetValue = resolvedBorderColor,
        animationSpec = tween(stateAnimationMillis ?: properties.stateAnimationMillis),
    )
    UnstyledTextField(
        state = state,
        modifier = modifier.alpha(alpha ?: properties.alpha),
        enabled = enabled,
        accessibilityLabel = label,
        interactionSource = interaction,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = resolvedTextStyle.copy(color = resolvedForeground),
        cursorBrush = SolidColor(resolvedCursorColor),
        selectionColors = TextSelectionColors(resolvedCursorColor, resolvedSelectionColor),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                contentSpacing ?: properties.contentSpacing
            )
        ) {
            if (!label.isNullOrBlank())
                Text(
                    label,
                    style = (labelStyle ?: properties.labelStyle).copy(color = resolvedForeground)
                )
            Box(
                Modifier
                    .hoverable(interaction, enabled)
                    .fillMaxWidth()
                    .heightIn(min = minimumHeight ?: properties.minimumHeight)
                    .millenniumSurface(
                        background = background ?: properties.background ?: millenniumBevelBrush(
                            colors,
                            sunken = true
                        ),
                        shape = shape ?: properties.shape,
                        borderColor = animatedBorder,
                        borderWidth = borderWidth ?: properties.borderWidth,
                        insetShadeColor = insetShadeColor ?: properties.insetShadeColor
                        ?: colors.text.copy(alpha = 0.09f),
                        innerGlowColor = innerGlowColor ?: properties.innerGlowColor
                        ?: colors.border.copy(alpha = 0.4f),
                        bottomRimColor = bottomRimColor ?: properties.bottomRimColor
                        ?: colors.highlight.copy(alpha = 0.8f),
                    )
                    .millenniumInteractionSurface(controlState, shape ?: properties.shape, animationMillis = stateAnimationMillis ?: properties.stateAnimationMillis),
                contentAlignment = Alignment.CenterStart,
            ) {
                TextInput(
                    Modifier.fillMaxWidth().padding(contentPadding ?: properties.contentPadding),
                    placeholder = {
                        Text(
                            placeholder,
                            style = resolvedPlaceholderStyle.copy(color = resolvedPlaceholderColor),
                            color = resolvedPlaceholderColor,
                        )
                    },
                )
            }
            if (error != null) {
                Text(
                    error,
                    style = (errorStyle ?: properties.errorStyle).copy(color = resolvedErrorColor),
                    color = resolvedErrorColor,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MillenniumTextFieldPreview() = MillenniumPreview {
    MillenniumTextField(
        state = rememberTextFieldState("Alex"),
        label = "Display name",
        placeholder = "Your name",
    )
}
