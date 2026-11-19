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

package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.spotube.millennium.composables.MillenniumAutocompleteTheme
import org.spotube.millennium.composables.MillenniumAvatarTheme
import org.spotube.millennium.composables.MillenniumBottomNavigationBarTheme
import org.spotube.millennium.composables.MillenniumBottomSheetTheme
import org.spotube.millennium.composables.MillenniumButtonMotion
import org.spotube.millennium.composables.MillenniumButtonTheme
import org.spotube.millennium.composables.MillenniumButtonThemeProperties
import org.spotube.millennium.composables.MillenniumCardTheme
import org.spotube.millennium.composables.MillenniumCheckboxTheme
import org.spotube.millennium.composables.MillenniumDialogTheme
import org.spotube.millennium.composables.MillenniumDisclosureTheme
import org.spotube.millennium.composables.MillenniumDrawerTheme
import org.spotube.millennium.composables.MillenniumDropdownMenuTheme
import org.spotube.millennium.composables.MillenniumIconButtonTheme
import org.spotube.millennium.composables.MillenniumIconButtonThemeProperties
import org.spotube.millennium.composables.MillenniumListTileTheme
import org.spotube.millennium.composables.MillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.MillenniumProgressTheme
import org.spotube.millennium.composables.MillenniumRadioGroupTheme
import org.spotube.millennium.composables.MillenniumScaffoldTheme
import org.spotube.millennium.composables.MillenniumScrollbarTheme
import org.spotube.millennium.composables.MillenniumSegmentedButtonTheme
import org.spotube.millennium.composables.MillenniumSeparatorTheme
import org.spotube.millennium.composables.MillenniumSidebarTheme
import org.spotube.millennium.composables.MillenniumSliderTheme
import org.spotube.millennium.composables.MillenniumSnackbarTheme
import org.spotube.millennium.composables.MillenniumTabGroupTheme
import org.spotube.millennium.composables.MillenniumTextFieldTheme
import org.spotube.millennium.composables.MillenniumTextTheme
import org.spotube.millennium.composables.MillenniumTextThemeProperties
import org.spotube.millennium.composables.MillenniumToggleSwitchTheme
import org.spotube.millennium.composables.MillenniumTooltipTheme
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.MillenniumTriStateCheckboxTheme
import org.spotube.millennium.composables.defaultMillenniumModalBottomSheetTheme
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumControlState
import org.spotube.millennium.theme.MillenniumControlState.Default
import org.spotube.millennium.theme.MillenniumControlState.Disabled
import org.spotube.millennium.theme.MillenniumControlState.Error
import org.spotube.millennium.theme.MillenniumControlState.Focused
import org.spotube.millennium.theme.MillenniumControlState.Hovered
import org.spotube.millennium.theme.MillenniumControlState.Pressed
import org.spotube.millennium.theme.MillenniumControlState.Selected
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumThemeComponents
import org.spotube.millennium.theme.defaultMillenniumThemeComponents
import org.spotube.spotube.modules.settings.ModernAccent
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.bricolage_grotesque_bold
import spotube.composeapp.generated.resources.bricolage_grotesque_extrabold
import spotube.composeapp.generated.resources.geist_bold
import spotube.composeapp.generated.resources.geist_medium
import spotube.composeapp.generated.resources.geist_regular
import spotube.composeapp.generated.resources.geist_semibold

/*
 * The Modern interface style.
 *
 * Graphite surfaces that step up in small increments, one warm off-white for text, hairline
 * borders instead of bevels, softly rounded controls, and no gel, glow or drop shadow on controls.
 * The selected Millennium palette still picks the accent family; it is used sparingly (focus,
 * selection, progress), while primary actions are drawn in the text colour.
 */

/** The two faces of the Modern style. */
@Immutable
data class ModernFonts(
    /** Bricolage Grotesque: the wordmark, page titles and section titles. */
    val display: FontFamily,
    /** Geist: everything people read. */
    val text: FontFamily,
)

@Composable
fun rememberModernFonts(): ModernFonts = ModernFonts(
    display = FontFamily(
        Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
        Font(Res.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
    ),
    text = FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semibold, FontWeight.SemiBold),
        Font(Res.font.geist_bold, FontWeight.Bold),
    ),
)

// ---------- colour ----------

private fun modernDark(accent: Color) = MillenniumColors(
    background = Color(0xFF111113),
    surface = Color(0xFF18181B),
    raised = Color(0xFF222226),
    inset = Color(0xFF0B0B0D),
    text = Color(0xFFECEBE8),
    muted = Color(0xFFA9A8AD),
    border = Color(0x1FECEBE8),
    highlight = Color(0x0DFFFFFF),
    accent = accent,
    accentTop = accent,
    accentBottom = accent,
    onAccent = Color(0xFF111113),
    success = Color(0xFF7BCF8E),
    danger = Color(0xFFE8837A),
    scrim = Color(0xCC0B0B0D),
)

private fun modernLight(accent: Color) = MillenniumColors(
    background = Color(0xFFF3F2EF),
    surface = Color(0xFFFBFAF8),
    raised = Color(0xFFFFFFFF),
    inset = Color(0xFFE9E8E4),
    text = Color(0xFF18181B),
    muted = Color(0xFF5E5D63),
    border = Color(0x1F18181B),
    highlight = Color(0x99FFFFFF),
    accent = accent,
    accentTop = accent,
    accentBottom = accent,
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF2F7D4A),
    danger = Color(0xFFC0473C),
    scrim = Color(0x99111113),
)

/**
 * The Modern role sets, one light and one dark per Millennium palette family. Dark accents are
 * soft pastels that sit quietly on graphite; light accents are deep enough for white labels.
 */
val ModernColorSchemes = MillenniumColorSchemes(
    light = modernLight(Color(0xFF3D5BC7)),
    dark = modernDark(Color(0xFF9DB6F7)),
    orangeLight = modernLight(Color(0xFFB04A1C)),
    orangeDark = modernDark(Color(0xFFF0A57E)),
    limeLight = modernLight(Color(0xFF4A7A16)),
    limeDark = modernDark(Color(0xFFB8D98A)),
)

/**
 * The extra Modern accents: a soft tone for dark appearance and a deeper one for light, picked so
 * the label on an accent fill stays readable (dark text on the pastel, white on the deep tone).
 */
fun ModernAccent.tones(): Pair<Color, Color> = when (this) {
    ModernAccent.LAVENDER -> Color(0xFF6A54B8) to Color(0xFFB7A8E0)
    ModernAccent.ROSE -> Color(0xFFAD3A60) to Color(0xFFF0A6BC)
    ModernAccent.RED -> Color(0xFFB3352B) to Color(0xFFF2877F)
    ModernAccent.AMBER -> Color(0xFF855D00) to Color(0xFFF2C86B)
    ModernAccent.MINT -> Color(0xFF1C7650) to Color(0xFF8FDDB8)
    ModernAccent.TEAL -> Color(0xFF0E6B70) to Color(0xFF7FD3D6)
    ModernAccent.SKY -> Color(0xFF15699A) to Color(0xFF86D0F2)
    ModernAccent.ORCHID -> Color(0xFF8C3A8A) to Color(0xFFE2A4E1)
}

private val accentSchemes: Map<ModernAccent, MillenniumColorSchemes> =
    ModernAccent.entries.associateWith { accent ->
        val (light, dark) = accent.tones()
        // Every palette family resolves to the same accent, whichever one is stored.
        MillenniumColorSchemes(
            light = modernLight(light), dark = modernDark(dark),
            orangeLight = modernLight(light), orangeDark = modernDark(dark),
            limeLight = modernLight(light), limeDark = modernDark(dark),
        )
    }

/** The Modern role sets for an extra [accent], or the palette families' own when it is null. */
fun modernColorSchemes(accent: ModernAccent?): MillenniumColorSchemes =
    accent?.let(accentSchemes::getValue) ?: ModernColorSchemes

/** A segment fill: the active tab, the selected row. */
fun MillenniumColors.segment(): Color = lerp(background, text, 0.11f)

/** A raised card under the pointer. */
fun MillenniumColors.cardHover(): Color = lerp(surface, text, 0.04f)

private fun MillenniumColors.wash(alpha: Float): Color = text.copy(alpha = alpha)

// ---------- typography ----------

/** App text styles for the Modern style. */
fun modernTypography(fonts: ModernFonts): AppTypography {
    fun display(size: Int, line: Int, tracking: Double, weight: FontWeight = FontWeight.ExtraBold) = TextStyle(
        fontFamily = fonts.display, fontSize = size.sp, lineHeight = line.sp,
        fontWeight = weight, letterSpacing = tracking.sp
    )
    fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = fonts.text, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight
    )
    return AppTypography(
        displaySmall = display(44, 46, -1.5),
        headlineLarge = display(34, 36, -1.0),
        headlineMedium = display(28, 31, -0.7, FontWeight.Bold),
        headlineSmall = display(24, 28, -0.6, FontWeight.Bold),
        titleLarge = display(22, 26, -0.45, FontWeight.Bold),
        titleMedium = text(16, 22, FontWeight.SemiBold),
        titleSmall = text(14, 20, FontWeight.Medium),
        bodyLarge = text(16, 24),
        bodyMedium = text(14, 20),
        bodySmall = text(12, 16),
        labelLarge = text(14, 20, FontWeight.Medium),
        labelMedium = text(12, 16, FontWeight.Medium),
        labelSmall = text(11, 16, FontWeight.Medium),
    )
}

val ModernShapes = AppShapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

// ---------- components ----------

private fun <T> MillenniumControlStateProperty<T>.restyle(
    f: (MillenniumControlState, T) -> T
): MillenniumControlStateProperty<T> = MillenniumControlStateProperty(
    default = f(Default, default),
    hovered = f(Hovered, hovered),
    focused = f(Focused, focused),
    pressed = f(Pressed, pressed),
    disabled = f(Disabled, disabled),
    selected = f(Selected, selected),
    error = f(Error, error),
)

private val Pill = RoundedCornerShape(50)
/** Buttons: a plain rounded rectangle, not a pill. */
private val ButtonShape = RoundedCornerShape(10.dp)
private val None = Color.Transparent
private val Clear = SolidColor(Color.Transparent)
private val ring = 2.dp
private val NoShadow = listOf(Color.Transparent)

private fun focusWidth(state: MillenniumControlState, rest: androidx.compose.ui.unit.Dp) =
    if (state == Focused || state == Error) ring else rest

private fun focusColor(state: MillenniumControlState, c: MillenniumColors, rest: Color) = when (state) {
    Focused -> c.accent
    Error -> c.danger
    else -> rest
}

private fun TextStyle.inFamily(family: FontFamily) = copy(fontFamily = family)

/**
 * Flat component themes for one appearance, built from that appearance's [c] colours.
 * Starts from Millennium's defaults (so every layout metric is kept) and replaces only surfaces,
 * shapes, borders, glows, shadows and type.
 */
fun modernThemeComponents(c: MillenniumColors, fonts: ModernFonts): MillenniumThemeComponents {
    val base = defaultMillenniumThemeComponents()
    val seg = c.segment()
    val label = TextStyle(
        fontFamily = fonts.text, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, shadow = Shadow(Color.Transparent)
    )
    val flatMotion = MillenniumButtonMotion(stateChangeMillis = 150, hoverScale = 1f, pressedScale = 0.97f)

    fun MillenniumControlStateProperty<MillenniumButtonThemeProperties>.flat(
        fill: (MillenniumControlState) -> Color,
        foreground: Color,
        border: Color,
        borderWidth: androidx.compose.ui.unit.Dp
    ) = restyle { state, p ->
        p.copy(
            background = SolidColor(fill(state)),
            foreground = foreground,
            textStyle = label,
            shape = ButtonShape,
            borderWidth = focusWidth(state, borderWidth),
            borderColor = focusColor(state, c, border),
            shadows = NoShadow,
            shadowElevation = 0.dp,
            innerGlowColor = None,
            bottomRimColor = None,
            motion = flatMotion,
        )
    }

    val buttonTheme = MillenniumButtonTheme(
        primary = base.buttonTheme.primary.flat(
            fill = { if (it == Hovered || it == Pressed) lerp(c.text, c.background, 0.12f) else c.text },
            foreground = c.background, border = c.text, borderWidth = 0.dp
        ),
        secondary = base.buttonTheme.secondary.flat(
            fill = {
                when (it) {
                    Hovered -> c.wash(0.11f)
                    Pressed -> c.wash(0.14f)
                    else -> c.wash(0.07f)
                }
            },
            foreground = c.text, border = None, borderWidth = 0.dp
        ),
        quiet = base.buttonTheme.quiet.flat(
            fill = {
                when (it) {
                    Hovered -> c.wash(0.06f)
                    Pressed -> c.wash(0.10f)
                    else -> None
                }
            },
            foreground = c.text, border = None, borderWidth = 0.dp
        ),
        danger = base.buttonTheme.danger.flat(
            fill = { if (it == Hovered || it == Pressed) lerp(c.danger, c.background, 0.12f) else c.danger },
            foreground = c.background, border = c.danger, borderWidth = 0.dp
        ),
    )

    fun MillenniumControlStateProperty<MillenniumIconButtonThemeProperties>.flatIcon(
        fill: (MillenniumControlState) -> Color,
        foreground: Color,
        border: Color,
        borderWidth: androidx.compose.ui.unit.Dp
    ) = restyle { state, p ->
        p.copy(
            background = SolidColor(fill(state)),
            foreground = foreground,
            shape = CircleShape,
            borderWidth = focusWidth(state, borderWidth),
            borderColor = focusColor(state, c, border),
            shadows = NoShadow,
            shadowElevation = 0.dp,
            innerGlowColor = None,
            bottomRimColor = None,
            orb = false,
            motion = MillenniumButtonMotion(stateChangeMillis = 150, hoverScale = 1f, pressedScale = 0.94f),
        )
    }

    val iconButtonTheme = MillenniumIconButtonTheme(
        primary = base.iconButtonTheme.primary.flatIcon(
            fill = { if (it == Hovered || it == Pressed) lerp(c.text, c.background, 0.12f) else c.text },
            foreground = c.background, border = c.text, borderWidth = 0.dp
        ),
        secondary = base.iconButtonTheme.secondary.flatIcon(
            fill = {
                when (it) {
                    Hovered -> c.wash(0.11f)
                    Pressed -> c.wash(0.14f)
                    else -> c.wash(0.07f)
                }
            },
            foreground = c.text, border = None, borderWidth = 0.dp
        ),
        quiet = base.iconButtonTheme.quiet.flatIcon(
            fill = {
                when (it) {
                    Hovered -> c.wash(0.07f)
                    Pressed -> c.wash(0.11f)
                    else -> None
                }
            },
            foreground = c.text, border = None, borderWidth = 0.dp
        ),
        danger = base.iconButtonTheme.danger.flatIcon(
            fill = { c.danger }, foreground = c.background, border = c.danger, borderWidth = 0.dp
        ),
    )

    val textTheme = MillenniumTextTheme(
        states = MillenniumControlStateProperty.all(
            MillenniumTextThemeProperties(
                body = TextStyle(fontFamily = fonts.text, fontSize = 14.sp, lineHeight = 21.sp),
                label = TextStyle(fontFamily = fonts.text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                button = TextStyle(fontFamily = fonts.text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                title = TextStyle(fontFamily = fonts.display, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp),
                headline = TextStyle(fontFamily = fonts.display, fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp),
                caption = TextStyle(fontFamily = fonts.text, fontSize = 12.sp, lineHeight = 17.sp),
                avatar = TextStyle(fontFamily = fonts.text, fontWeight = FontWeight.Bold),
            )
        )
    )

    val cardTheme = MillenniumCardTheme(
        states = base.cardTheme.states.restyle { state, p ->
            p.copy(
                background = SolidColor(if (state == Hovered || state == Pressed) c.cardHover() else c.surface),
                shape = RoundedCornerShape(18.dp),
                contentTextStyle = p.contentTextStyle.inFamily(fonts.text),
                shadowElevation = 0.dp,
                shadowColor = None,
                borderColor = focusColor(state, c, c.border),
                borderWidth = focusWidth(state, 1.dp),
                innerGlowColor = None,
                bottomRimColor = None,
            )
        }
    )

    val textFieldTheme = MillenniumTextFieldTheme(
        states = base.textFieldTheme.states.restyle { state, p ->
            p.copy(
                background = SolidColor(c.surface),
                shape = RoundedCornerShape(14.dp),
                textStyle = p.textStyle.inFamily(fonts.text),
                labelStyle = p.labelStyle.inFamily(fonts.text),
                placeholderStyle = p.placeholderStyle.inFamily(fonts.text),
                errorStyle = p.errorStyle.inFamily(fonts.text),
                placeholderColor = c.muted,
                cursorColor = c.accent,
                selectionColor = c.accent.copy(alpha = 0.28f),
                borderColor = when (state) {
                    Focused, Selected -> c.accent.copy(alpha = 0.55f)
                    Error -> c.danger
                    Hovered -> c.wash(0.18f)
                    else -> c.border
                },
                borderWidth = 1.dp,
                insetShadeColor = None,
                innerGlowColor = None,
                bottomRimColor = None,
            )
        }
    )

    val sidebarTheme = MillenniumSidebarTheme(
        root = base.sidebarTheme.root.restyle { _, p ->
            p.copy(
                background = SolidColor(c.inset),
                borderColor = c.border,
                borderWidth = 0.dp,
                shadowElevation = 0.dp,
                innerGlowColor = None,
            )
        },
        header = base.sidebarTheme.header.restyle { _, p -> p.copy(dividerColor = None, background = Clear) },
        content = base.sidebarTheme.content,
        footer = base.sidebarTheme.footer.restyle { _, p -> p.copy(dividerColor = c.border, background = Clear) },
        group = base.sidebarTheme.group.restyle { state, p ->
            p.copy(
                labelStyle = TextStyle(fontFamily = fonts.text, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
                labelForeground = c.muted,
                labelBackground = Clear,
                labelHoveredBackground = SolidColor(c.wash(0.04f)),
                labelBorderColor = focusColor(state, c, None),
                labelBorderWidth = if (state == Focused) ring else 0.dp,
                labelInnerGlowColor = None,
            )
        },
        item = base.sidebarTheme.item.restyle { state, p ->
            p.copy(
                background = SolidColor(
                    when (state) {
                        Selected -> seg
                        Hovered -> c.wash(0.05f)
                        Pressed -> c.wash(0.08f)
                        else -> None
                    }
                ),
                foreground = if (state == Selected || state == Hovered || state == Pressed) c.text else c.muted,
                iconTint = if (state == Selected) c.accent else null,
                labelStyle = p.labelStyle.inFamily(fonts.text),
                shape = RoundedCornerShape(12.dp),
                borderColor = focusColor(state, c, None),
                borderWidth = if (state == Focused) ring else 0.dp,
                shadowElevation = 0.dp,
                innerGlowColor = None,
                selectedIndicatorColor = None,
            )
        },
        badge = base.sidebarTheme.badge.restyle { _, p ->
            p.copy(
                background = SolidColor(c.accent), foreground = c.onAccent,
                textStyle = p.textStyle.inFamily(fonts.text), borderWidth = 0.dp, innerGlowColor = None
            )
        },
        trigger = base.sidebarTheme.trigger.restyle { state, p ->
            p.copy(
                shape = CircleShape,
                background = SolidColor(if (state == Hovered || state == Pressed) c.wash(0.07f) else None),
                borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = None
            )
        },
    )

    val dialogTheme = MillenniumDialogTheme(
        states = base.dialogTheme.states.restyle { _, p ->
            p.copy(
                panelBackground = SolidColor(c.surface),
                shape = RoundedCornerShape(22.dp),
                titleStyle = TextStyle(fontFamily = fonts.display, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp),
                descriptionStyle = p.descriptionStyle.inFamily(fonts.text),
                panelBorderColor = c.border,
                panelBorderWidth = 1.dp,
                panelGlowColor = None,
                titleBarBackground = Clear,
                titleBarBorderColor = None,
                footerBackground = Clear,
                footerBorderColor = None,
            )
        }
    )

    val dropdownMenuTheme = MillenniumDropdownMenuTheme(
        states = base.dropdownMenuTheme.states.restyle { state, p ->
            p.copy(
                panelBackground = SolidColor(c.cardHover()),
                panelShape = RoundedCornerShape(14.dp),
                panelBorderColor = c.border,
                panelBorderWidth = 1.dp,
                panelGlowColor = None,
                itemBackground = SolidColor(
                    when (state) {
                        Hovered, Focused, Selected -> c.wash(0.07f)
                        Pressed -> c.wash(0.11f)
                        else -> None
                    }
                ),
                itemShape = RoundedCornerShape(9.dp),
                itemTextStyle = p.itemTextStyle.inFamily(fonts.text),
                itemBorderWidth = 0.dp,
                itemGlowColor = None,
                descriptionTextStyle = p.descriptionTextStyle.inFamily(fonts.text),
                shortcutTextStyle = p.shortcutTextStyle.inFamily(fonts.text),
                groupLabelStyle = TextStyle(fontFamily = fonts.text, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
            )
        }
    )

    val tooltipTheme = MillenniumTooltipTheme(
        states = base.tooltipTheme.states.restyle { _, p ->
            p.copy(
                background = SolidColor(c.text), foreground = c.background,
                shape = RoundedCornerShape(8.dp),
                textStyle = p.textStyle.inFamily(fonts.text),
                borderWidth = 0.dp, innerGlowColor = None
            )
        }
    )

    val segmentedButtonTheme = MillenniumSegmentedButtonTheme(
        states = base.segmentedButtonTheme.states.restyle { state, p ->
            p.copy(
                background = SolidColor(
                    when (state) {
                        Selected -> seg
                        Hovered -> c.wash(0.05f)
                        else -> None
                    }
                ),
                foreground = if (state == Selected || state == Hovered) c.text else c.muted,
                textStyle = p.textStyle.inFamily(fonts.text),
                borderColor = focusColor(state, c, c.border),
                innerGlowColor = None,
                motion = MillenniumButtonMotion(stateChangeMillis = 150, hoverScale = 1f, pressedScale = 0.98f),
            )
        }
    )

    val tabGroupTheme = MillenniumTabGroupTheme(
        states = base.tabGroupTheme.states.restyle { _, p ->
            p.copy(
                listBackground = SolidColor(c.surface),
                listShape = Pill,
                listBorderColor = c.border,
                listBorderWidth = 1.dp,
                listPadding = 4.dp,
                tabShape = Pill,
                selectedTabBackground = SolidColor(seg),
                unselectedTabBackground = Clear,
                selectedTabForeground = c.text,
                unselectedTabForeground = c.muted,
                tabTextStyle = p.tabTextStyle.inFamily(fonts.text),
                tabBorderWidth = 0.dp,
                tabElevation = 0.dp,
                tabGlowColor = None,
            )
        }
    )

    val toggleSwitchTheme = MillenniumToggleSwitchTheme(
        states = base.toggleSwitchTheme.states.restyle { state, p ->
            p.copy(
                trackBackground = SolidColor(lerp(c.surface, c.text, 0.14f)),
                selectedTrackBackground = SolidColor(c.accent),
                trackShape = Pill,
                trackBorderColor = focusColor(state, c, c.border),
                trackGlowColor = None,
                thumbBackground = SolidColor(Color.White),
                thumbShape = CircleShape,
                thumbElevation = 0.dp,
                thumbBorderColor = Color(0x14000000),
                thumbGlowColor = None,
                labelStyle = p.labelStyle.inFamily(fonts.text),
            )
        }
    )

    val sliderTheme = MillenniumSliderTheme(
        states = base.sliderTheme.states.restyle { state, p ->
            p.copy(
                trackBackground = SolidColor(c.wash(0.14f)),
                activeTrackBackground = SolidColor(c.accent),
                thumbBackground = SolidColor(c.text),
                trackShape = Pill,
                thumbShape = CircleShape,
                trackHeight = 4.dp,
                trackBorderWidth = 0.dp,
                thumbBorderColor = focusColor(state, c, None),
                thumbBorderWidth = if (state == Focused) ring else 0.dp,
                thumbElevation = 0.dp,
                thumbGlowColor = None,
                thumbBottomRimColor = None,
                trackInsetShadeColor = None,
            )
        }
    )

    val progressTheme = MillenniumProgressTheme(
        states = base.progressTheme.states.restyle { _, p ->
            p.copy(
                trackBackground = SolidColor(c.wash(0.14f)),
                progressBackground = SolidColor(c.accent),
                shape = Pill,
                borderWidth = 0.dp,
                innerGlowColor = None,
            )
        }
    )

    val listTileTheme = MillenniumListTileTheme(
        states = base.listTileTheme.states.restyle { state, p ->
            p.copy(
                titleStyle = p.titleStyle.inFamily(fonts.text),
                subtitleStyle = p.subtitleStyle.inFamily(fonts.text),
                subtitleForeground = c.muted,
                background = SolidColor(
                    when (state) {
                        Hovered -> c.wash(0.04f)
                        Pressed -> c.wash(0.07f)
                        else -> None
                    }
                ),
                selectedBackground = SolidColor(seg),
                selectedGelStrength = 0f,
                shape = RoundedCornerShape(12.dp),
                borderColor = if (state == Focused) c.accent.copy(alpha = 0.5f) else None,
                borderWidth = if (state == Focused) 1.5.dp else 0.dp,
                innerGlowColor = None,
            )
        }
    )

    val snackbarTheme = MillenniumSnackbarTheme(
        base.snackbarTheme.properties.copy(
            background = SolidColor(c.cardHover()),
            foreground = c.text,
            actionForeground = c.accent,
            shape = RoundedCornerShape(16.dp),
            borderColor = c.border,
            borderWidth = 1.dp,
            innerGlowColor = None,
        )
    )

    val bottomNavigationBarTheme = MillenniumBottomNavigationBarTheme(
        states = base.bottomNavigationBarTheme.states.restyle { _, p ->
            p.copy(
                background = SolidColor(c.surface.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(6.dp),
                borderColor = c.border,
                borderWidth = 1.dp,
                shadowElevation = 0.dp,
                innerGlowColor = None,
            )
        },
        item = base.bottomNavigationBarTheme.item.restyle { state, p ->
            p.copy(
                background = SolidColor(
                    when (state) {
                        Selected -> seg
                        Pressed -> c.wash(0.08f)
                        else -> None
                    }
                ),
                foreground = if (state == Selected) c.text else c.muted,
                shape = RoundedCornerShape(24.dp),
                labelStyle = p.labelStyle.inFamily(fonts.text),
                borderColor = focusColor(state, c, None),
                borderWidth = if (state == Focused) ring else 0.dp,
                shadowElevation = 0.dp,
                innerGlowColor = None,
            )
        },
    )

    val topAppBarTheme = MillenniumTopAppBarTheme(
        properties = base.topAppBarTheme.properties.copy(
            titleStyle = TextStyle(fontFamily = fonts.display, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp),
            subtitleStyle = base.topAppBarTheme.properties.subtitleStyle.inFamily(fonts.text),
            separatorColor = None,
        )
    )

    val scaffoldTheme = MillenniumScaffoldTheme(
        properties = base.scaffoldTheme.properties.copy(
            titleStyle = TextStyle(fontFamily = fonts.display, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp),
        )
    )

    val checkboxTheme = MillenniumCheckboxTheme(
        states = base.checkboxTheme.states.restyle { state, p ->
            p.copy(
                faceBackground = SolidColor(c.surface),
                selectedFaceBackground = SolidColor(c.accent),
                selectedFaceForeground = c.onAccent,
                faceShape = RoundedCornerShape(6.dp),
                faceBorderColor = focusColor(state, c, c.wash(0.22f)),
                faceGlowColor = None,
                faceElevation = 0.dp,
                labelStyle = p.labelStyle.inFamily(fonts.text),
            )
        }
    )

    val triStateCheckboxTheme = MillenniumTriStateCheckboxTheme(
        states = base.triStateCheckboxTheme.states.restyle { state, p ->
            p.copy(
                faceBackground = SolidColor(c.surface),
                selectedFaceBackground = SolidColor(c.accent),
                selectedFaceForeground = c.onAccent,
                faceShape = RoundedCornerShape(6.dp),
                faceBorderColor = focusColor(state, c, c.wash(0.22f)),
                faceGlowColor = None,
                faceElevation = 0.dp,
                labelStyle = p.labelStyle.inFamily(fonts.text),
            )
        }
    )

    val radioGroupTheme = MillenniumRadioGroupTheme(
        states = base.radioGroupTheme.states.restyle { state, p ->
            p.copy(
                indicatorBackground = SolidColor(c.surface),
                selectedIndicatorBackground = SolidColor(c.accent),
                dotColor = c.onAccent,
                indicatorBorderColor = focusColor(state, c, c.wash(0.22f)),
                indicatorGlowColor = None,
                indicatorElevation = 0.dp,
                labelStyle = p.labelStyle.inFamily(fonts.text),
            )
        }
    )

    val scrollbarTheme = MillenniumScrollbarTheme(
        states = base.scrollbarTheme.states.restyle { state, p ->
            p.copy(
                trackBackground = Clear,
                thumbBackground = SolidColor(c.wash(if (state == Hovered || state == Pressed) 0.28f else 0.16f)),
                shape = Pill,
                borderWidth = 0.dp,
            )
        }
    )

    val disclosureTheme = MillenniumDisclosureTheme(
        states = base.disclosureTheme.states.restyle { state, p ->
            p.copy(
                background = SolidColor(c.surface),
                shape = RoundedCornerShape(16.dp),
                borderColor = focusColor(state, c, c.border),
                borderWidth = 1.dp,
                shadowElevation = 0.dp,
                innerGlowColor = None,
                titleStyle = p.titleStyle.inFamily(fonts.text),
                contentStyle = p.contentStyle.inFamily(fonts.text),
            )
        }
    )

    val drawerTheme = MillenniumDrawerTheme(
        states = base.drawerTheme.states.restyle { _, p ->
            p.copy(background = SolidColor(c.surface), borderColor = c.border, innerGlowColor = None)
        }
    )

    val bottomSheetTheme = MillenniumBottomSheetTheme(
        states = base.bottomSheetTheme.states.restyle { _, p ->
            p.copy(
                background = SolidColor(c.surface), borderColor = c.border,
                handleColor = c.wash(0.22f), innerGlowColor = None
            )
        }
    )

    val modalPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
    val modalBottomSheetTheme = MillenniumModalBottomSheetTheme(
        states = defaultMillenniumModalBottomSheetTheme().states.restyle { _, p ->
            p.copy(
                background = SolidColor(c.surface), borderColor = c.border,
                handleColor = c.wash(0.22f), innerGlowColor = None,
                contentPadding = modalPadding, contentSpacing = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
        }
    )

    val separatorTheme = MillenniumSeparatorTheme(
        states = base.separatorTheme.states.restyle { _, p -> p.copy(color = c.border, highlightColor = None) }
    )

    val avatarTheme = MillenniumAvatarTheme(
        states = base.avatarTheme.states.restyle { _, p ->
            p.copy(
                textStyle = p.textStyle.inFamily(fonts.text),
                borderColor = c.border, shadowElevation = 0.dp, innerGlowColor = None
            )
        }
    )

    val autocompleteTheme = MillenniumAutocompleteTheme(
        states = base.autocompleteTheme.states.restyle { _, p ->
            p.copy(
                panelBackground = SolidColor(c.cardHover()),
                panelShape = RoundedCornerShape(14.dp),
                panelBorderColor = c.border,
                panelGlowColor = None,
                dividerColor = c.border,
                itemBackground = Clear,
                highlightedItemBackground = SolidColor(c.wash(0.07f)),
                itemShape = RoundedCornerShape(9.dp),
                itemTextStyle = p.itemTextStyle.inFamily(fonts.text),
            )
        }
    )

    return base.copy(
        textTheme = textTheme,
        buttonTheme = buttonTheme,
        cardTheme = cardTheme,
        checkboxTheme = checkboxTheme,
        triStateCheckboxTheme = triStateCheckboxTheme,
        toggleSwitchTheme = toggleSwitchTheme,
        textFieldTheme = textFieldTheme,
        sliderTheme = sliderTheme,
        radioGroupTheme = radioGroupTheme,
        tabGroupTheme = tabGroupTheme,
        avatarTheme = avatarTheme,
        disclosureTheme = disclosureTheme,
        dialogTheme = dialogTheme,
        dropdownMenuTheme = dropdownMenuTheme,
        tooltipTheme = tooltipTheme,
        progressTheme = progressTheme,
        scrollbarTheme = scrollbarTheme,
        separatorTheme = separatorTheme,
        bottomSheetTheme = bottomSheetTheme,
        modalBottomSheetTheme = modalBottomSheetTheme,
        drawerTheme = drawerTheme,
        sidebarTheme = sidebarTheme,
        bottomNavigationBarTheme = bottomNavigationBarTheme,
        autocompleteTheme = autocompleteTheme,
        listTileTheme = listTileTheme,
        scaffoldTheme = scaffoldTheme,
        iconButtonTheme = iconButtonTheme,
        topAppBarTheme = topAppBarTheme,
        segmentedButtonTheme = segmentedButtonTheme,
        snackbarTheme = snackbarTheme,
    )
}
