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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * The app's own colour system, independent of [MaterialTheme].
 *
 * Each [SurfaceStyle] is a complete visual theme with its own palette (backgrounds,
 * surfaces, text and outlines) rather than Material's role colours. The user's chosen
 * accent is threaded through as `primary`, but everything else comes from the surface
 * theme, so the app never has to rely on Material's colour scheme.
 */
@Immutable
data class BaseUIColorScheme(
    val surfaceTheme: SurfaceStyle,
    val isLight: Boolean,
    /** Full-bleed page background for the current surface theme. */
    val background: Brush,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val backgroundPrimary: Color,
    val onBackgroundPrimary: Color,
    val backgroundSecondary: Color,
    val onBackgroundSecondary: Color,
    val accentPrimary: Color,
    val onAccentPrimary: Color,
    val accentSecondary: Color,
    val onAccentSecondary: Color,
    val backgroundInverse: Color,
    val onBackgroundInverse: Color,
    val actionInverse: Color,
    val onActionInverse: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val scrim: Color,
)

val LocalBaseUIColors = staticCompositionLocalOf<BaseUIColorScheme> {
    error("No BaseUIColorScheme provided. Wrap your UI in a BaseUITheme provider.")
}

/**
 * Builds the [BaseUIColorScheme] for the selected [surfaceTheme] and accent.
 *
 * Light/dark is read from the surrounding [MaterialTheme] (which the app already drives
 * from the user's theme setting), and the accent comes from the user's chosen accent
 * colour. Everything else is derived from the surface theme palette.
 */
@Composable
fun rememberBaseUIColorScheme(
    surfaceTheme: SurfaceStyle = SurfaceStyle.Glass,
    accentColor: Color? = null,
): BaseUIColorScheme {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val accent = accentColor ?: surfaceTheme.accentColor(isLight)
    return remember(surfaceTheme, isLight, accent) {
        baseUIColorScheme(surfaceTheme, isLight, accent)
    }
}

/**
 * The default accent colour that characterises a surface theme. Every theme has its own,
 * so a Watery app is aqua and a Chrome app is steel, unless an accent is passed explicitly.
 */
fun SurfaceStyle.accentColor(isLight: Boolean): Color = when (this) {
    SurfaceStyle.Watery -> if (isLight) Color(0xFF0E8FD6) else Color(0xFF4FC3F7)
    SurfaceStyle.Icy -> if (isLight) Color(0xFF3FA9E0) else Color(0xFF7FD8FF)
    SurfaceStyle.Chrome -> if (isLight) Color(0xFF5B6B7C) else Color(0xFF9AA5B4)
    SurfaceStyle.Acrylic -> if (isLight) Color(0xFF5C6BC0) else Color(0xFF8C9EFF)
    SurfaceStyle.Glass -> if (isLight) Color(0xFF7C4DFF) else Color(0xFFB388FF)
}

/**
 * The secondary accent is derived from the primary accent as a complementary hue, so it always
 * shifts with — and harmonises with — the chosen accent instead of being fixed per style.
 */
private fun Color.complementaryAccent(hueShift: Float = 180f): Color {
    val (hue, saturation, value) = toHsv()
    return Color.hsv(
        (hue + hueShift + 360f) % 360f,
        saturation.coerceIn(0.45f, 0.9f),
        value.coerceIn(0.45f, 0.95f),
    )
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val rawHue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    val hue = if (rawHue < 0f) rawHue + 360f else rawHue
    val saturation = if (max == 0f) 0f else delta / max
    return Triple(hue, saturation, max)
}

fun baseUIColorScheme(
    surfaceTheme: SurfaceStyle,
    isLight: Boolean,
    accent: Color,
): BaseUIColorScheme {
    val palette = surfacePalette(surfaceTheme, isLight)
    val onPrimary = if (accent.luminance() > 0.5f) Color(0xFF10161C) else Color.White
    val primaryContainer = lerp(palette.surface, accent, if (isLight) 0.16f else 0.32f)
    val onPrimaryContainer = if (isLight) {
        lerp(accent, Color.Black, 0.55f)
    } else {
        lerp(accent, Color.White, 0.7f)
    }
    val secondaryContainer = lerp(palette.surface, accent, if (isLight) 0.22f else 0.34f)
    val inversePalette = surfacePalette(surfaceTheme, !isLight)
    val secondaryAccent = accent.complementaryAccent()
    val onSecondaryAccent = if (secondaryAccent.luminance() > 0.5f) Color(0xFF10161C) else Color.White
    val secondaryBackground = if (isLight) {
        lerp(palette.surface, secondaryAccent, 0.14f)
    } else {
        lerp(palette.surface, secondaryAccent, 0.22f)
    }
    val onSecondaryBackground = if (secondaryBackground.luminance() > 0.5f) {
        lerp(secondaryAccent, Color.Black, 0.6f)
    } else {
        lerp(secondaryAccent, Color.White, 0.82f)
    }
    val actionInverse = if (isLight) lerp(accent, Color.Black, 0.45f) else lerp(accent, Color.White, 0.5f)
    val onActionInverse = if (actionInverse.luminance() > 0.5f) Color(0xFF10161C) else Color.White

    return BaseUIColorScheme(
        surfaceTheme = surfaceTheme,
        isLight = isLight,
        background = Brush.verticalGradient(
            listOf(palette.backgroundTop, palette.backgroundBottom),
        ),
        surface = palette.surface,
        surfaceVariant = palette.surfaceVariant,
        surfaceContainer = palette.surfaceContainer,
        surfaceContainerHigh = palette.surfaceContainerHigh,
        surfaceContainerHighest = palette.surfaceContainerHighest,
        onBackground = palette.onBackground,
        onSurface = palette.onSurface,
        onSurfaceVariant = palette.onSurfaceVariant,
        primary = accent,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = palette.onSurface,
        backgroundPrimary = palette.surface,
        onBackgroundPrimary = palette.onSurface,
        backgroundSecondary = secondaryBackground,
        onBackgroundSecondary = onSecondaryBackground,
        accentPrimary = accent,
        onAccentPrimary = onPrimary,
        accentSecondary = secondaryAccent,
        onAccentSecondary = onSecondaryAccent,
        backgroundInverse = inversePalette.surface,
        onBackgroundInverse = inversePalette.onSurface,
        actionInverse = actionInverse,
        onActionInverse = onActionInverse,
        outline = palette.outline,
        outlineVariant = palette.outlineVariant,
        error = if (isLight) Color(0xFFC0392B) else Color(0xFFE57373),
        onError = Color.White,
        scrim = Color.Black.copy(alpha = 0.42f),
    )
}

internal class SurfacePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)

internal fun surfacePalette(style: SurfaceStyle, isLight: Boolean): SurfacePalette {
    return if (isLight) {
        when (style) {
            SurfaceStyle.Glass -> SurfacePalette(
                backgroundTop = Color(0xFFEAF0F7),
                backgroundBottom = Color(0xFFD8E3F1),
                surface = Color(0xFFF8FBFF),
                surfaceVariant = Color(0xFFE4ECF5),
                surfaceContainer = Color(0xFFEEF3F9),
                surfaceContainerHigh = Color(0xFFF3F7FC),
                surfaceContainerHighest = Color(0xFFE1E9F3),
                onBackground = Color(0xFF1B2733),
                onSurface = Color(0xFF1B2733),
                onSurfaceVariant = Color(0xFF56677A),
                outline = Color(0xFFA9B8C8),
                outlineVariant = Color(0xFFD3DDE8),
            )

            SurfaceStyle.Acrylic -> SurfacePalette(
                backgroundTop = Color(0xFFE6EAF0),
                backgroundBottom = Color(0xFFD6DCE5),
                surface = Color(0xFFF4F7FA),
                surfaceVariant = Color(0xFFE1E7EE),
                surfaceContainer = Color(0xFFEAEEF4),
                surfaceContainerHigh = Color(0xFFF1F4F8),
                surfaceContainerHighest = Color(0xFFDCE2E9),
                onBackground = Color(0xFF1C222B),
                onSurface = Color(0xFF1C222B),
                onSurfaceVariant = Color(0xFF545E6C),
                outline = Color(0xFFA6AEBC),
                outlineVariant = Color(0xFFD2D8E0),
            )

            SurfaceStyle.Watery -> SurfacePalette(
                backgroundTop = Color(0xFFEAF7FE),
                backgroundBottom = Color(0xFFD8EFFB),
                surface = Color(0xFFF0FAFF),
                surfaceVariant = Color(0xFFE2F2FB),
                surfaceContainer = Color(0xFFEAF6FD),
                surfaceContainerHigh = Color(0xFFF2FAFE),
                surfaceContainerHighest = Color(0xFFD8ECF8),
                onBackground = Color(0xFF08324A),
                onSurface = Color(0xFF08324A),
                onSurfaceVariant = Color(0xFF3A6884),
                outline = Color(0xFF9EC4DA),
                outlineVariant = Color(0xFFD0E7F4),
            )

            SurfaceStyle.Icy -> SurfacePalette(
                backgroundTop = Color(0xFFE8F7FF),
                backgroundBottom = Color(0xFFC9E8FB),
                surface = Color(0xFFF6FCFF),
                surfaceVariant = Color(0xFFE0F1FC),
                surfaceContainer = Color(0xFFEBF7FE),
                surfaceContainerHigh = Color(0xFFF3FBFF),
                surfaceContainerHighest = Color(0xFFD4EBFA),
                onBackground = Color(0xFF0B3A55),
                onSurface = Color(0xFF0B3A55),
                onSurfaceVariant = Color(0xFF3D6C88),
                outline = Color(0xFF93C0D9),
                outlineVariant = Color(0xFFC9E5F4),
            )

            SurfaceStyle.Chrome -> SurfacePalette(
                backgroundTop = Color(0xFFE4E8ED),
                backgroundBottom = Color(0xFFD0D6DE),
                surface = Color(0xFFF4F7FA),
                surfaceVariant = Color(0xFFE0E5EB),
                surfaceContainer = Color(0xFFE9EDF2),
                surfaceContainerHigh = Color(0xFFF1F4F7),
                surfaceContainerHighest = Color(0xFFDBE0E7),
                onBackground = Color(0xFF1B2733),
                onSurface = Color(0xFF1B2733),
                onSurfaceVariant = Color(0xFF525C6A),
                outline = Color(0xFFA2AAB6),
                outlineVariant = Color(0xFFD0D5DC),
            )
        }
    } else {
        when (style) {
            SurfaceStyle.Glass -> SurfacePalette(
                backgroundTop = Color(0xFF0E1217),
                backgroundBottom = Color(0xFF161C24),
                surface = Color(0xFF1E242C),
                surfaceVariant = Color(0xFF262E38),
                surfaceContainer = Color(0xFF1A2027),
                surfaceContainerHigh = Color(0xFF232A33),
                surfaceContainerHighest = Color(0xFF2C343E),
                onBackground = Color(0xFFE6EDF5),
                onSurface = Color(0xFFE6EDF5),
                onSurfaceVariant = Color(0xFF9AA7B6),
                outline = Color(0xFF46525F),
                outlineVariant = Color(0xFF2C343E),
            )

            SurfaceStyle.Acrylic -> SurfacePalette(
                backgroundTop = Color(0xFF101317),
                backgroundBottom = Color(0xFF181C22),
                surface = Color(0xFF20262E),
                surfaceVariant = Color(0xFF282F39),
                surfaceContainer = Color(0xFF1C2229),
                surfaceContainerHigh = Color(0xFF252C35),
                surfaceContainerHighest = Color(0xFF2E3640),
                onBackground = Color(0xFFE4EAF1),
                onSurface = Color(0xFFE4EAF1),
                onSurfaceVariant = Color(0xFF98A2B0),
                outline = Color(0xFF48525E),
                outlineVariant = Color(0xFF2E3640),
            )

            SurfaceStyle.Watery -> SurfacePalette(
                backgroundTop = Color(0xFF0A1622),
                backgroundBottom = Color(0xFF0E1E2D),
                surface = Color(0xFF0E2233),
                surfaceVariant = Color(0xFF153148),
                surfaceContainer = Color(0xFF0B1D2C),
                surfaceContainerHigh = Color(0xFF12293C),
                surfaceContainerHighest = Color(0xFF183449),
                onBackground = Color(0xFFD6EAF7),
                onSurface = Color(0xFFD6EAF7),
                onSurfaceVariant = Color(0xFF8FB0C6),
                outline = Color(0xFF3C5A72),
                outlineVariant = Color(0xFF183449),
            )

            SurfaceStyle.Icy -> SurfacePalette(
                backgroundTop = Color(0xFF0C1620),
                backgroundBottom = Color(0xFF132331),
                surface = Color(0xFF16283A),
                surfaceVariant = Color(0xFF1E3448),
                surfaceContainer = Color(0xFF122130),
                surfaceContainerHigh = Color(0xFF1A2E42),
                surfaceContainerHighest = Color(0xFF213A50),
                onBackground = Color(0xFFDCEBF7),
                onSurface = Color(0xFFDCEBF7),
                onSurfaceVariant = Color(0xFF92B3CA),
                outline = Color(0xFF40607A),
                outlineVariant = Color(0xFF213A50),
            )

            SurfaceStyle.Chrome -> SurfacePalette(
                backgroundTop = Color(0xFF101317),
                backgroundBottom = Color(0xFF181C22),
                surface = Color(0xFF232830),
                surfaceVariant = Color(0xFF2B313A),
                surfaceContainer = Color(0xFF1E232A),
                surfaceContainerHigh = Color(0xFF272D36),
                surfaceContainerHighest = Color(0xFF303742),
                onBackground = Color(0xFFE7ECF2),
                onSurface = Color(0xFFE7ECF2),
                onSurfaceVariant = Color(0xFF9BA5B2),
                outline = Color(0xFF4B5561),
                outlineVariant = Color(0xFF303742),
            )
        }
    }
}
