/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.base.BaseUITheme.AutoCompleteMenuTheme
import dev.krtirtho.spotube.core.ui.base.BaseUITheme.CheckBoxTheme
import dev.krtirtho.spotube.core.ui.base.BaseUITheme.ChipTabTheme
import dev.krtirtho.spotube.core.ui.base.BaseUITheme.SliderTheme
import dev.krtirtho.spotube.core.ui.base.BaseUITheme.TextFieldTheme

@Stable
data class BaseUITheme(
    val buttons: ButtonVariants,
    val iconButtons: ButtonVariants,
    val textField: TextFieldTheme,
    val autoCompleteMenuTheme: AutoCompleteMenuTheme,
    val slider: SliderTheme,
    val checkBox: CheckBoxTheme,
    val chipTab: ChipTabTheme,
    val toggle: ToggleTheme,
    val card: CardTheme,
    val listRowTile: ListRowTheme,
    val dialog: DialogTheme,
    val dropdownMenu: DropdownMenuTheme,
) {

    @Stable
    data class InteractionState<T>(
        val normal: T,
        val hovered: T,
        val pressed: T,
        val focused: T,
    ) {
        companion object {
            fun <T> fromSingleValue(value: T): InteractionState<T> {
                return InteractionState(value, value, value, value)
            }
        }
    }

    @Stable
    data class AdvancedInteractionState<T>(
        val normal: T,
        val hovered: T,
        val pressed: T,
        val focused: T,
        val selected: T,
        val disabled: T,
    ) {
        companion object {
            fun <T> fromSingleValue(value: T): AdvancedInteractionState<T> {
                return AdvancedInteractionState(value, value, value, value, value, value)
            }
        }
    }

    @Stable
    data class Border(
        val color: Color,
        val width: Dp,
    )

    @Stable
    data class Shadow(
        val elevation: Dp,
        val clip: Boolean = elevation > 0.dp,
        val ambientColor: Color,
        val spotColor: Color,
    )

    @Stable
    data class ButtonVariants(
        val primary: ButtonStyle,
        val secondary: ButtonStyle,
        val outline: ButtonStyle,
        val ghost: ButtonStyle,
    )

    @Stable
    data class ButtonStyle(
        val colors: InteractionState<ButtonColors>,
        val shape: InteractionState<Shape>,
        val shadow: InteractionState<Shadow>,
        val border: InteractionState<Border>,
        val padding: InteractionState<PaddingValues>,
    )

    @Stable
    data class ButtonColors(
        val background: Brush,
        val foreground: Color,
        val highlight: Color,
    )

    data class CardTheme(
        val background: Brush,
        val shape: Shape,
        val border: Border,
        val padding: PaddingValues,
        val shadow: Shadow,
    )

    data class ListRowTheme(
        val background: AdvancedInteractionState<Brush>,
        val shape: AdvancedInteractionState<Shape>,
        val border: AdvancedInteractionState<Border>,
        val shadow: AdvancedInteractionState<Shadow>,
        val padding: PaddingValues,
        val textStyle: AdvancedInteractionState<TextStyle>,
        val foreground: AdvancedInteractionState<Color>,
    )

    data class TextFieldTheme(
        val background: InteractionState<Brush>,
        val highlight: InteractionState<Color>,
        val shape: InteractionState<Shape>,
        val border: InteractionState<Border>,
        val shadow: InteractionState<Shadow>,
        val padding: PaddingValues,
        val textStyle: TextStyle,
        val cursor: Brush,
        val foreground: InteractionState<Color>,
    )

    data class AutoCompleteMenuTheme(
        val background: Brush,
        val shape: Shape,
        val border: Border,
        val padding: PaddingValues,
        val shadowElevation: Dp,
    )

    data class DialogTheme(
        val background: Brush,
        val scrim: Color,
        val shape: Shape,
        val border: Border,
        val shadow: Shadow,
        val padding: PaddingValues,
    )

    data class DropdownMenuTheme(
        val background: Brush,
        val shape: Shape,
        val border: Border,
        val shadow: Shadow,
        val itemHoverBackground: Brush,
        val itemForeground: Color,
        val itemHighlight: Color,
        val padding: PaddingValues,
        val itemPadding: PaddingValues,
    )

    data class SliderTheme(
        val trackActiveColor: InteractionState<Color>,
        val trackInactiveColor: InteractionState<Color>,
        val thumbColor: InteractionState<Color>,
        val thumbShadow: InteractionState<Shadow>,
        val stepActiveColor: Color,
        val stepInactiveColor: Color,
    )

    data class CheckBoxTheme(
        val selected: ButtonStyle,
        val unselected: ButtonStyle,
        val checkmarkColor: Color,
    )

    data class ChipTabTheme(
        val selected: ButtonStyle,
        val unselected: ButtonStyle,
    )

    data class ToggleTheme(
        val checked: ButtonStyle,
        val unchecked: ButtonStyle,
        val thumb: InteractionState<Color>,
        val thumbShadow: InteractionState<Shadow>,
    )
}

val LocalBaseUITheme = staticCompositionLocalOf<BaseUITheme> {
    throw IllegalStateException("No BaseUITheme provided")
}

@Composable
fun rememberBaseUITheme(): BaseUITheme {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f

    // ------------------------------------------------------------------
    // Frutiger-Aero glass palette.
    //
    // Surfaces are glossy, top-lit gradients with a strong white specular
    // highlight and translucent "glass" edges, lifted by soft, coloured
    // shadows — the aqua/glass language of Frutiger-Aero.
    // ------------------------------------------------------------------
    val glassTop = if (isLight) {
        Color.White
    } else {
        lerp(scheme.surfaceContainerHigh, Color.White, 0.10f)
    }
    val glassBottom = if (isLight) {
        lerp(scheme.surface, Color.White, 0.04f)
    } else {
        scheme.surfaceContainer
    }
    val glassPressed = if (isLight) {
        lerp(scheme.surfaceContainerHighest, Color.White, 0.06f)
    } else {
        lerp(scheme.surfaceContainerHighest, Color.Black, 0.08f)
    }

    val gloss = if (isLight) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.18f)
    val glossSoft = if (isLight) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
    val glassEdge = if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.22f)
    val glassBorder = if (isLight) {
        lerp(scheme.outlineVariant, Color.White, 0.55f)
    } else {
        scheme.outlineVariant.copy(alpha = 0.5f)
    }
    val shadowColor = scheme.onSurface

    fun glassBrush(top: Color, bottom: Color): Brush =
        Brush.verticalGradient(listOf(top, bottom))

    fun aqua(accent: Color): Brush = Brush.verticalGradient(
        listOf(
            lerp(accent, Color.White, 0.45f),
            accent,
            lerp(accent, Color.Black, 0.14f),
        )
    )

    val defaultShape = RoundedCornerShape(16.dp)
    val defaultShapeState =
        BaseUITheme.InteractionState<Shape>(defaultShape, defaultShape, defaultShape, defaultShape)
    val noBorder = BaseUITheme.Border(Color.Transparent, 0.dp)
    val noBorderState = BaseUITheme.InteractionState(noBorder, noBorder, noBorder, noBorder)
    val noShadow = BaseUITheme.Shadow(0.dp, false, Color.Transparent, Color.Transparent)
    val noShadowState = BaseUITheme.InteractionState(noShadow, noShadow, noShadow, noShadow)
    val defaultButtonPadding = BaseUITheme.InteractionState(
        PaddingValues(horizontal = 22.dp, vertical = 10.dp),
        PaddingValues(horizontal = 22.dp, vertical = 10.dp),
        PaddingValues(horizontal = 22.dp, vertical = 10.dp),
        PaddingValues(horizontal = 22.dp, vertical = 10.dp),
    )
    val iconButtonPadding = BaseUITheme.InteractionState(
        PaddingValues(9.dp),
        PaddingValues(9.dp),
        PaddingValues(9.dp),
        PaddingValues(9.dp),
    )

    // --- Glass (outline) -------------------------------------------------
    val outlineColors = BaseUITheme.InteractionState(
        normal = BaseUITheme.ButtonColors(
            background = glassBrush(glassTop, glassBottom),
            foreground = scheme.onSurface,
            highlight = gloss,
        ),
        hovered = BaseUITheme.ButtonColors(
            background = glassBrush(glassTop, glassBottom),
            foreground = scheme.onSurface,
            highlight = Color.White.copy(alpha = if (isLight) 0.9f else 0.26f),
        ),
        pressed = BaseUITheme.ButtonColors(
            background = glassBrush(glassPressed, glassPressed),
            foreground = scheme.onSurface,
            highlight = glossSoft,
        ),
        focused = BaseUITheme.ButtonColors(
            background = glassBrush(glassTop, glassBottom),
            foreground = scheme.onSurface,
            highlight = gloss,
        ),
    )
    val glassShadow = BaseUITheme.InteractionState(
        normal = BaseUITheme.Shadow(
            8.dp,
            true,
            shadowColor.copy(alpha = 0.16f),
            shadowColor.copy(alpha = 0.2f),
        ),
        hovered = BaseUITheme.Shadow(
            14.dp,
            true,
            shadowColor.copy(alpha = 0.22f),
            shadowColor.copy(alpha = 0.26f),
        ),
        pressed = BaseUITheme.Shadow(
            2.dp,
            true,
            shadowColor.copy(alpha = 0.1f),
            shadowColor.copy(alpha = 0.12f),
        ),
        focused = BaseUITheme.Shadow(
            8.dp,
            true,
            shadowColor.copy(alpha = 0.16f),
            shadowColor.copy(alpha = 0.2f),
        ),
    )
    val glassBorderState = BaseUITheme.InteractionState(
        normal = BaseUITheme.Border(glassBorder, 1.dp),
        hovered = BaseUITheme.Border(glassEdge, 1.dp),
        pressed = BaseUITheme.Border(glassBorder.copy(alpha = 0.7f), 1.dp),
        focused = BaseUITheme.Border(glassEdge, 1.dp),
    )

    // --- Aqua accent (primary) -------------------------------------------
    val primaryEdge = lerp(scheme.primary, Color.White, 0.5f)
    val primaryColors = BaseUITheme.InteractionState(
        normal = BaseUITheme.ButtonColors(aqua(scheme.primary), scheme.onPrimary, gloss),
        hovered = BaseUITheme.ButtonColors(
            aqua(scheme.primary),
            scheme.onPrimary,
            Color.White.copy(alpha = if (isLight) 0.9f else 0.3f),
        ),
        pressed = BaseUITheme.ButtonColors(
            glassBrush(
                lerp(scheme.primary, Color.Black, 0.16f),
                lerp(scheme.primary, Color.Black, 0.24f),
            ),
            scheme.onPrimary,
            glossSoft,
        ),
        focused = BaseUITheme.ButtonColors(aqua(scheme.primary), scheme.onPrimary, gloss),
    )
    val primaryShadow = BaseUITheme.InteractionState(
        normal = BaseUITheme.Shadow(
            10.dp,
            true,
            scheme.primary.copy(alpha = 0.35f),
            scheme.primary.copy(alpha = 0.42f),
        ),
        hovered = BaseUITheme.Shadow(
            16.dp,
            true,
            scheme.primary.copy(alpha = 0.45f),
            scheme.primary.copy(alpha = 0.5f),
        ),
        pressed = BaseUITheme.Shadow(
            3.dp,
            true,
            scheme.primary.copy(alpha = 0.22f),
            scheme.primary.copy(alpha = 0.28f),
        ),
        focused = BaseUITheme.Shadow(
            10.dp,
            true,
            scheme.primary.copy(alpha = 0.35f),
            scheme.primary.copy(alpha = 0.42f),
        ),
    )
    val primaryBorder = BaseUITheme.InteractionState(
        normal = BaseUITheme.Border(primaryEdge, 1.dp),
        hovered = BaseUITheme.Border(primaryEdge, 1.dp),
        pressed = BaseUITheme.Border(primaryEdge.copy(alpha = 0.8f), 1.dp),
        focused = BaseUITheme.Border(primaryEdge, 1.dp),
    )

    // --- Secondary (soft aqua glass) -------------------------------------
    val secondaryBase = scheme.secondaryContainer
    val secondaryColors = BaseUITheme.InteractionState(
        normal = BaseUITheme.ButtonColors(
            glassBrush(lerp(secondaryBase, Color.White, 0.5f), secondaryBase),
            scheme.onSecondaryContainer,
            gloss,
        ),
        hovered = BaseUITheme.ButtonColors(
            glassBrush(lerp(secondaryBase, Color.White, 0.55f), secondaryBase),
            scheme.onSecondaryContainer,
            Color.White.copy(alpha = if (isLight) 0.85f else 0.24f),
        ),
        pressed = BaseUITheme.ButtonColors(
            glassBrush(
                lerp(secondaryBase, Color.Black, 0.08f),
                lerp(secondaryBase, Color.Black, 0.14f),
            ),
            scheme.onSecondaryContainer,
            glossSoft,
        ),
        focused = BaseUITheme.ButtonColors(
            glassBrush(lerp(secondaryBase, Color.White, 0.5f), secondaryBase),
            scheme.onSecondaryContainer,
            gloss,
        ),
    )
    val secondaryShadow = BaseUITheme.InteractionState(
        normal = BaseUITheme.Shadow(
            8.dp,
            true,
            secondaryBase.copy(alpha = 0.4f),
            secondaryBase.copy(alpha = 0.5f),
        ),
        hovered = BaseUITheme.Shadow(
            14.dp,
            true,
            secondaryBase.copy(alpha = 0.5f),
            secondaryBase.copy(alpha = 0.6f),
        ),
        pressed = BaseUITheme.Shadow(
            2.dp,
            true,
            secondaryBase.copy(alpha = 0.3f),
            secondaryBase.copy(alpha = 0.35f),
        ),
        focused = BaseUITheme.Shadow(
            8.dp,
            true,
            secondaryBase.copy(alpha = 0.4f),
            secondaryBase.copy(alpha = 0.5f),
        ),
    )
    val secondaryBorder = BaseUITheme.InteractionState(
        normal = BaseUITheme.Border(lerp(secondaryBase, Color.White, 0.5f), 1.dp),
        hovered = BaseUITheme.Border(lerp(secondaryBase, Color.White, 0.6f), 1.dp),
        pressed = BaseUITheme.Border(lerp(secondaryBase, Color.White, 0.4f), 1.dp),
        focused = BaseUITheme.Border(lerp(secondaryBase, Color.White, 0.5f), 1.dp),
    )

    // --- Ghost -----------------------------------------------------------
    val ghostColors = BaseUITheme.InteractionState(
        normal = BaseUITheme.ButtonColors(
            background = glassBrush(Color.Transparent, Color.Transparent),
            foreground = scheme.onSurface,
            highlight = Color.Transparent,
        ),
        hovered = BaseUITheme.ButtonColors(
            background = glassBrush(
                lerp(scheme.surfaceContainerHigh, Color.White, 0.1f),
                scheme.surfaceContainer,
            ),
            foreground = scheme.onSurface,
            highlight = glossSoft,
        ),
        pressed = BaseUITheme.ButtonColors(
            background = glassBrush(glassPressed, glassPressed),
            foreground = scheme.onSurface,
            highlight = Color.Transparent,
        ),
        focused = BaseUITheme.ButtonColors(
            background = glassBrush(Color.Transparent, Color.Transparent),
            foreground = scheme.onSurface,
            highlight = Color.Transparent,
        ),
    )

    val outlineStyle = BaseUITheme.ButtonStyle(
        outlineColors,
        defaultShapeState,
        glassShadow,
        glassBorderState,
        defaultButtonPadding
    )
    val primaryStyle = BaseUITheme.ButtonStyle(
        primaryColors,
        defaultShapeState,
        primaryShadow,
        primaryBorder,
        defaultButtonPadding
    )
    val secondaryStyle = BaseUITheme.ButtonStyle(
        secondaryColors,
        defaultShapeState,
        secondaryShadow,
        secondaryBorder,
        defaultButtonPadding
    )
    val ghostStyle = BaseUITheme.ButtonStyle(
        ghostColors,
        defaultShapeState,
        noShadowState,
        noBorderState,
        defaultButtonPadding
    )

    val outlineIconStyle = BaseUITheme.ButtonStyle(
        outlineColors,
        defaultShapeState,
        glassShadow,
        glassBorderState,
        iconButtonPadding
    )
    val primaryIconStyle = BaseUITheme.ButtonStyle(
        primaryColors,
        defaultShapeState,
        primaryShadow,
        primaryBorder,
        iconButtonPadding
    )
    val secondaryIconStyle = BaseUITheme.ButtonStyle(
        secondaryColors,
        defaultShapeState,
        secondaryShadow,
        secondaryBorder,
        iconButtonPadding
    )
    val ghostIconStyle = BaseUITheme.ButtonStyle(
        ghostColors,
        defaultShapeState,
        noShadowState,
        noBorderState,
        iconButtonPadding
    )

    val textFieldForeground = BaseUITheme.InteractionState(
        normal = scheme.onSurface,
        hovered = scheme.onSurface,
        pressed = scheme.onSurface,
        focused = scheme.onSurface,
    )

    val checkBoxShape = RoundedCornerShape(7.dp)
    val checkBoxShapeState =
        BaseUITheme.InteractionState<Shape>(checkBoxShape, checkBoxShape, checkBoxShape, checkBoxShape)
    val noPadding =
        BaseUITheme.InteractionState(PaddingValues(0.dp), PaddingValues(0.dp), PaddingValues(0.dp), PaddingValues(0.dp))

    val chipTabShape = RoundedCornerShape(999.dp)
    val chipTabShapeState =
        BaseUITheme.InteractionState<Shape>(chipTabShape, chipTabShape, chipTabShape, chipTabShape)
    val chipTabPadding = BaseUITheme.InteractionState(
        PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    )

    val toggleShape = RoundedCornerShape(50)
    val toggleShapeState =
        BaseUITheme.InteractionState<Shape>(toggleShape, toggleShape, toggleShape, toggleShape)
    val togglePadding = BaseUITheme.InteractionState(
        PaddingValues(2.dp),
        PaddingValues(2.dp),
        PaddingValues(2.dp),
        PaddingValues(2.dp),
    )

    return BaseUITheme(
        buttons = BaseUITheme.ButtonVariants(
            primary = primaryStyle,
            secondary = secondaryStyle,
            outline = outlineStyle,
            ghost = ghostStyle,
        ),
        iconButtons = BaseUITheme.ButtonVariants(
            primary = primaryIconStyle,
            secondary = secondaryIconStyle,
            outline = outlineIconStyle,
            ghost = ghostIconStyle,
        ),
        textField = TextFieldTheme(
            background = BaseUITheme.InteractionState(
                normal = glassBrush(glassTop, glassBottom),
                hovered = glassBrush(glassTop, glassBottom),
                pressed = glassBrush(glassPressed, glassPressed),
                focused = glassBrush(glassTop, glassBottom),
            ),
            highlight = BaseUITheme.InteractionState(
                normal = gloss,
                hovered = gloss,
                pressed = glossSoft,
                focused = Color.White.copy(alpha = if (isLight) 0.85f else 0.22f),
            ),
            shape = BaseUITheme.InteractionState(
                normal = defaultShape,
                hovered = defaultShape,
                pressed = defaultShape,
                focused = defaultShape,
            ),
            border = BaseUITheme.InteractionState(
                normal = BaseUITheme.Border(glassBorder, 1.dp),
                hovered = BaseUITheme.Border(glassEdge, 1.dp),
                pressed = BaseUITheme.Border(glassBorder.copy(alpha = 0.7f), 1.dp),
                focused = BaseUITheme.Border(primaryEdge, 1.5.dp),
            ),
            shadow = BaseUITheme.InteractionState(
                normal = BaseUITheme.Shadow(
                    8.dp,
                    true,
                    shadowColor.copy(alpha = 0.14f),
                    shadowColor.copy(alpha = 0.18f)
                ),
                hovered = BaseUITheme.Shadow(
                    10.dp,
                    true,
                    shadowColor.copy(alpha = 0.18f),
                    shadowColor.copy(alpha = 0.22f)
                ),
                pressed = BaseUITheme.Shadow(
                    4.dp,
                    true,
                    shadowColor.copy(alpha = 0.1f),
                    shadowColor.copy(alpha = 0.12f)
                ),
                focused = BaseUITheme.Shadow(
                    10.dp,
                    true,
                    scheme.primary.copy(alpha = 0.22f),
                    scheme.primary.copy(alpha = 0.28f)
                ),
            ),
            padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            textStyle = TextStyle.Default,
            cursor = SolidColor(scheme.primary),
            foreground = textFieldForeground,
        ),
        autoCompleteMenuTheme = AutoCompleteMenuTheme(
            background = glassBrush(glassTop, glassBottom),
            shape = RoundedCornerShape(16.dp),
            border = BaseUITheme.Border(glassBorder, 1.dp),
            padding = PaddingValues(0.dp),
            shadowElevation = 16.dp,
        ),
        slider = SliderTheme(
            trackActiveColor = BaseUITheme.InteractionState(
                normal = scheme.primary,
                hovered = scheme.primary,
                pressed = scheme.primary,
                focused = scheme.primary,
            ),
            trackInactiveColor = BaseUITheme.InteractionState(
                normal = glassBorder,
                hovered = glassBorder,
                pressed = glassBorder,
                focused = glassBorder,
            ),
            thumbColor = BaseUITheme.InteractionState(
                normal = scheme.primary,
                hovered = scheme.primary,
                pressed = scheme.primary,
                focused = scheme.primary,
            ),
            thumbShadow = BaseUITheme.InteractionState(
                normal = BaseUITheme.Shadow(
                    5.dp,
                    true,
                    scheme.primary.copy(alpha = 0.4f),
                    scheme.primary.copy(alpha = 0.45f)
                ),
                hovered = BaseUITheme.Shadow(
                    8.dp,
                    true,
                    scheme.primary.copy(alpha = 0.45f),
                    scheme.primary.copy(alpha = 0.5f)
                ),
                pressed = BaseUITheme.Shadow(
                    2.dp,
                    true,
                    scheme.primary.copy(alpha = 0.25f),
                    scheme.primary.copy(alpha = 0.3f)
                ),
                focused = BaseUITheme.Shadow(
                    5.dp,
                    true,
                    scheme.primary.copy(alpha = 0.4f),
                    scheme.primary.copy(alpha = 0.45f)
                ),
            ),
            stepActiveColor = Color.White.copy(alpha = 0.7f),
            stepInactiveColor = scheme.onSurface.copy(alpha = 0.25f),
        ),
        checkBox = CheckBoxTheme(
            selected = BaseUITheme.ButtonStyle(
                colors = primaryColors,
                shape = checkBoxShapeState,
                shadow = primaryShadow,
                border = primaryBorder,
                padding = noPadding,
            ),
            unselected = BaseUITheme.ButtonStyle(
                colors = outlineColors,
                shape = checkBoxShapeState,
                shadow = glassShadow,
                border = glassBorderState,
                padding = noPadding,
            ),
            checkmarkColor = scheme.onPrimary,
        ),
        chipTab = ChipTabTheme(
            selected = BaseUITheme.ButtonStyle(
                colors = primaryColors,
                shape = chipTabShapeState,
                shadow = primaryShadow,
                border = primaryBorder,
                padding = chipTabPadding,
            ),
            unselected = BaseUITheme.ButtonStyle(
                colors = outlineColors,
                shape = chipTabShapeState,
                shadow = glassShadow,
                border = glassBorderState,
                padding = chipTabPadding,
            ),
        ),
        toggle = BaseUITheme.ToggleTheme(
            checked = BaseUITheme.ButtonStyle(
                colors = primaryColors,
                shape = toggleShapeState,
                shadow = primaryShadow,
                border = primaryBorder,
                padding = togglePadding,
            ),
            unchecked = BaseUITheme.ButtonStyle(
                colors = outlineColors,
                shape = toggleShapeState,
                shadow = glassShadow,
                border = glassBorderState,
                padding = togglePadding,
            ),
            thumb = BaseUITheme.InteractionState(
                normal = Color.White,
                hovered = Color.White,
                pressed = Color.White,
                focused = Color.White,
            ),
            thumbShadow = BaseUITheme.InteractionState(
                normal = BaseUITheme.Shadow(
                    3.dp,
                    true,
                    shadowColor.copy(alpha = 0.2f),
                    shadowColor.copy(alpha = 0.25f)
                ),
                hovered = BaseUITheme.Shadow(
                    4.dp,
                    true,
                    shadowColor.copy(alpha = 0.28f),
                    shadowColor.copy(alpha = 0.32f)
                ),
                pressed = BaseUITheme.Shadow(
                    1.dp,
                    true,
                    shadowColor.copy(alpha = 0.12f),
                    shadowColor.copy(alpha = 0.15f)
                ),
                focused = BaseUITheme.Shadow(
                    3.dp,
                    true,
                    shadowColor.copy(alpha = 0.2f),
                    shadowColor.copy(alpha = 0.25f)
                ),
            ),
        ),
        card = BaseUITheme.CardTheme(
            background = glassBrush(glassTop, glassBottom),
            shape = RoundedCornerShape(20.dp),
            border = BaseUITheme.Border(glassBorder, 1.dp),
            padding = PaddingValues(0.dp),
            shadow = BaseUITheme.Shadow(
                12.dp,
                true,
                shadowColor.copy(alpha = 0.16f),
                shadowColor.copy(alpha = 0.2f)
            ),
        ),
        listRowTile = BaseUITheme.ListRowTheme(
            background = BaseUITheme.AdvancedInteractionState(
                normal = glassBrush(Color.Transparent, Color.Transparent),
                hovered = glassBrush(glassTop, glassBottom),
                pressed = glassBrush(glassPressed, glassPressed),
                focused = glassBrush(glassTop, glassBottom),
                selected = glassBrush(
                    lerp(scheme.primaryContainer, Color.White, 0.4f),
                    scheme.primaryContainer,
                ),
                disabled = glassBrush(
                    glassTop.copy(alpha = 0.5f),
                    glassBottom.copy(alpha = 0.5f),
                ),
            ),
            shape = BaseUITheme.AdvancedInteractionState(
                normal = RoundedCornerShape(10.dp),
                hovered = RoundedCornerShape(10.dp),
                pressed = RoundedCornerShape(10.dp),
                focused = RoundedCornerShape(10.dp),
                selected = RoundedCornerShape(10.dp),
                disabled = RoundedCornerShape(10.dp),
            ),
            border = BaseUITheme.AdvancedInteractionState(
                normal = BaseUITheme.Border(Color.Transparent, 0.dp),
                hovered = BaseUITheme.Border(glassBorder, 1.dp),
                pressed = BaseUITheme.Border(Color.Transparent, 0.dp),
                focused = BaseUITheme.Border(glassBorder, 1.dp),
                selected = BaseUITheme.Border(Color.Transparent, 0.dp),
                disabled = BaseUITheme.Border(Color.Transparent, 0.dp),
            ),
            shadow = BaseUITheme.AdvancedInteractionState(
                normal = noShadow,
                hovered = BaseUITheme.Shadow(
                    6.dp,
                    true,
                    shadowColor.copy(alpha = 0.12f),
                    shadowColor.copy(alpha = 0.16f)
                ),
                pressed = noShadow,
                focused = noShadow,
                selected = noShadow,
                disabled = noShadow,
            ),
            padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            textStyle = BaseUITheme.AdvancedInteractionState(
                normal = TextStyle.Default,
                hovered = TextStyle.Default,
                pressed = TextStyle.Default,
                focused = TextStyle.Default,
                selected = TextStyle.Default,
                disabled = TextStyle.Default,
            ),
            foreground = BaseUITheme.AdvancedInteractionState(
                normal = scheme.onSurface,
                hovered = scheme.onSurface,
                pressed = scheme.onSurface,
                focused = scheme.onSurface,
                selected = scheme.onSurface,
                disabled = scheme.onSurface.copy(alpha = 0.38f),
            ),
        ),
        dialog = BaseUITheme.DialogTheme(
            background = glassBrush(glassTop, glassBottom),
            scrim = Color.Black.copy(alpha = 0.42f),
            shape = RoundedCornerShape(24.dp),
            border = BaseUITheme.Border(glassEdge, 1.dp),
            shadow = BaseUITheme.Shadow(
                28.dp,
                true,
                shadowColor.copy(alpha = 0.3f),
                shadowColor.copy(alpha = 0.4f)
            ),
            padding = PaddingValues(24.dp),
        ),
        dropdownMenu = BaseUITheme.DropdownMenuTheme(
            background = glassBrush(glassTop, glassBottom),
            shape = RoundedCornerShape(16.dp),
            border = BaseUITheme.Border(glassBorder, 1.dp),
            shadow = BaseUITheme.Shadow(
                16.dp,
                true,
                shadowColor.copy(alpha = 0.22f),
                shadowColor.copy(alpha = 0.28f)
            ),
            itemHoverBackground = glassBrush(glassTop, glassBottom),
            itemForeground = scheme.onSurface,
            itemHighlight = gloss,
            padding = PaddingValues(6.dp),
            itemPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        ),
    )
}

// Helper functions for easier theme overrides

fun BaseUITheme.ButtonStyle.copyShape(shape: Shape): BaseUITheme.ButtonStyle =
    copy(shape = BaseUITheme.InteractionState.fromSingleValue(shape))

fun BaseUITheme.ButtonStyle.copyShadow(shadow: BaseUITheme.Shadow): BaseUITheme.ButtonStyle =
    copy(shadow = BaseUITheme.InteractionState.fromSingleValue(shadow))

fun BaseUITheme.ButtonStyle.copyBorder(border: BaseUITheme.Border): BaseUITheme.ButtonStyle =
    copy(border = BaseUITheme.InteractionState.fromSingleValue(border))

fun BaseUITheme.ButtonStyle.copyPadding(padding: PaddingValues): BaseUITheme.ButtonStyle =
    copy(padding = BaseUITheme.InteractionState.fromSingleValue(padding))

fun BaseUITheme.ButtonStyle.copyColors(colors: BaseUITheme.ButtonColors): BaseUITheme.ButtonStyle =
    copy(colors = BaseUITheme.InteractionState.fromSingleValue(colors))

@Composable
fun invertedButtonStyle(): BaseUITheme.ButtonStyle {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val shadowColor = scheme.onSurface

    val invertedColors = BaseUITheme.ButtonColors(
        background = Brush.verticalGradient(listOf(scheme.onSurface, scheme.onSurface)),
        foreground = scheme.surface,
        highlight = if (isLight) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
    )
    val pressedColors = BaseUITheme.ButtonColors(
        background = Brush.verticalGradient(
            listOf(
                scheme.onSurfaceVariant,
                scheme.onSurfaceVariant
            )
        ),
        foreground = scheme.surface,
        highlight = if (isLight) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
    )
    val border = BaseUITheme.Border(scheme.onSurfaceVariant, 0.5.dp)
    val shadow = BaseUITheme.Shadow(
        elevation = 6.dp,
        clip = true,
        ambientColor = shadowColor.copy(alpha = 0.2f),
        spotColor = shadowColor.copy(alpha = 0.25f),
    )

    return BaseUITheme.ButtonStyle(
        colors = BaseUITheme.InteractionState(
            normal = invertedColors,
            hovered = invertedColors,
            pressed = pressedColors,
            focused = invertedColors,
        ),
        shape = BaseUITheme.InteractionState.fromSingleValue(RoundedCornerShape(14.dp)),
        shadow = BaseUITheme.InteractionState.fromSingleValue(shadow),
        border = BaseUITheme.InteractionState.fromSingleValue(border),
        padding = BaseUITheme.InteractionState.fromSingleValue(PaddingValues(8.dp)),
    )
}
