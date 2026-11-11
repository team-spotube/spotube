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

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.modules.settings.UserSettings

/*
 * The "Feishin" palette family: Feishin's Default Dark / Default Light themes
 * (https://github.com/jeffvli/feishin, src/shared/themes/default.ts and default-light/, GPL-3.0)
 * expressed in Millennium's 15 colour roles. See THEME-SPEC.md at the repository root.
 *
 * [src] = Feishin's own value. [derived] = Feishin has no such role, or its value is too dim
 * for the way Millennium uses the role (accent / danger / success are drawn as text and icons).
 *
 * accentTop == accentBottom on purpose: Millennium's "gel" gradient renders flat when its two
 * stops match, which is the look being imported.
 */

/** Feishin Default Dark. */
val FeishinDarkColors = MillenniumColors(
    background = Color(0xFF0C0C0C),   // [src] background
    surface = Color(0xFF141414),      // [src] surface
    raised = Color(0xFF1C1C1C),       // [derived] surface + one step
    inset = Color(0xFF080808),        // [src] background-alternate
    text = Color(0xFFE1E1E1),         // [src] foreground
    muted = Color(0xFF969696),        // [src] foreground-muted
    border = Color(0xFF2A2A2A),       // [derived] hairline, ~1.3:1 on surfaces
    highlight = Color(0xFF2A2A2A),    // [derived] no bevel sheen; just enough for hover feedback
    accent = Color(0xFF4C84FD),       // [derived] primary lifted so it reads as text/icons (>= 4.5:1)
    accentTop = Color(0xFF3574FC),    // [src] primary
    accentBottom = Color(0xFF3574FC), // [src] primary
    onAccent = Color.White,
    success = Color(0xFF32CC32),      // [src] state-success
    danger = Color(0xFFE05252),       // [derived] state-error (#CC3232) lifted to >= 4.5:1 as text
    scrim = Color(0xA6000000),        // black at 65%
    flat = true,                      // no gel, glow, rim, orb or glossy hover; small radii
)

/** Feishin Default Light. */
val FeishinLightColors = MillenniumColors(
    background = Color(0xFFEBEBEB),   // [src] background
    surface = Color(0xFFE1E1E1),      // [src] surface
    raised = Color.White,             // [derived]
    inset = Color(0xFFF0F0F0),        // [src] background-alternate
    text = Color(0xFF191919),         // [src] foreground
    muted = Color(0xFF505050),        // [src] foreground-muted
    border = Color(0xFFC9C9C9),       // [derived]
    highlight = Color.White,          // [derived]
    accent = Color(0xFF0058B8),       // [derived] primary darkened so it reads as text/icons
    accentTop = Color(0xFF007AFF),    // [src] primary
    accentBottom = Color(0xFF007AFF), // [src] primary
    onAccent = Color.White,
    success = Color(0xFF177332),      // [derived] state-success (#30D158) darkened to read as text
    danger = Color(0xFFB8231A),       // [derived] state-error (#FF3B30) darkened to read as text
    scrim = Color(0xA6000000),
    flat = true,
)

private val MillenniumBuiltInSchemes = MillenniumColorSchemes()

/** Millennium resolves one of its own three families, so Feishin rides in the default (Blue) slots. */
private val FeishinSchemes = MillenniumColorSchemes(light = FeishinLightColors, dark = FeishinDarkColors)

/**
 * The palettes the app offers: Millennium's own three, plus families added by this build.
 * The built-in role sets are never edited; an added family only supplies its own.
 */
@Serializable
enum class AppPalette {
    Blue,
    Orange,
    Lime,
    Feishin;

    /** The Millennium family whose slots this palette is resolved through. */
    val millennium: MillenniumPalette
        get() = when (this) {
            Orange -> MillenniumPalette.Orange
            Lime -> MillenniumPalette.Lime
            Blue, Feishin -> MillenniumPalette.Blue
        }

    val colorSchemes: MillenniumColorSchemes
        get() = if (this == Feishin) FeishinSchemes else MillenniumBuiltInSchemes

    fun colors(dark: Boolean): MillenniumColors = colorSchemes.resolve(millennium, dark = dark)

    companion object {
        fun of(palette: MillenniumPalette): AppPalette = when (palette) {
            MillenniumPalette.Blue -> Blue
            MillenniumPalette.Orange -> Orange
            MillenniumPalette.Lime -> Lime
        }
    }
}

/** The palette in use: an added family when one is chosen, else the stock Millennium palette. */
val UserSettings.activePalette: AppPalette
    get() = appPalette ?: AppPalette.of(palette)
