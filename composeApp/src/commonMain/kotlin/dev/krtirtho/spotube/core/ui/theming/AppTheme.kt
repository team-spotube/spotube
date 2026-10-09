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

package dev.krtirtho.spotube.core.ui.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.rememberDynamicColorScheme
import dev.krtirtho.spotube.core.ui.base.BaseUIColorScheme
import dev.krtirtho.spotube.core.ui.base.baseUIColorScheme
import dev.krtirtho.spotube.modules.settings.Theme
import dev.krtirtho.spotube.modules.settings.UserSettings

/**
 * The app's single source of truth for colour.
 *
 * The selected [UserSettings.surfaceTheme] and [UserSettings.accentColor] produce a
 * [BaseUIColorScheme], and every Material role is then overridden from it. That way the
 * whole app — Material components included — renders in the chosen surface theme's colours
 * and accent, instead of Material's dynamic palette drifting away from the base UI.
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }
    val isLight = !isDarkTheme

    val accent = if (isDarkTheme) {
        settings.accentColor.toDarkColor()
    } else {
        settings.accentColor.toLightColor()
    }

    val baseColors = remember(settings.surfaceTheme, isLight, accent) {
        baseUIColorScheme(settings.surfaceTheme, isLight, accent)
    }

    val dynamicScheme = rememberDynamicColorScheme(
        seedColor = accent,
        isDark = isDarkTheme,
    )

    MaterialTheme(
        colorScheme = dynamicScheme.mergeWithBaseColors(baseColors),
    ) {
        Surface(content = content)
    }
}

/**
 * Overrides the accent/surface roles of a dynamically generated [ColorScheme] with the
 * base UI palette, so Material components share the exact same colours as the base
 * components and the surface theme's primary/secondary backgrounds.
 */
private fun ColorScheme.mergeWithBaseColors(base: BaseUIColorScheme): ColorScheme = copy(
    primary = base.accentPrimary,
    onPrimary = base.onAccentPrimary,
    primaryContainer = base.primaryContainer,
    onPrimaryContainer = base.onPrimaryContainer,
    inversePrimary = base.actionInverse,
    secondary = base.accentSecondary,
    onSecondary = base.onAccentSecondary,
    secondaryContainer = base.secondaryContainer,
    onSecondaryContainer = base.onSecondaryContainer,
    background = base.surface,
    onBackground = base.onBackground,
    surface = base.surface,
    onSurface = base.onSurface,
    surfaceVariant = base.surfaceVariant,
    onSurfaceVariant = base.onSurfaceVariant,
    surfaceTint = base.accentPrimary,
    surfaceBright = base.surfaceContainerHigh,
    surfaceDim = base.surfaceContainerHighest,
    surfaceContainerLowest = base.surface,
    surfaceContainerLow = base.surfaceContainer,
    surfaceContainer = base.surfaceContainer,
    surfaceContainerHigh = base.surfaceContainerHigh,
    surfaceContainerHighest = base.surfaceContainerHighest,
    inverseSurface = base.backgroundInverse,
    inverseOnSurface = base.onBackgroundInverse,
    outline = base.outline,
    outlineVariant = base.outlineVariant,
    error = base.error,
    onError = base.onError,
    scrim = base.scrim,
)
