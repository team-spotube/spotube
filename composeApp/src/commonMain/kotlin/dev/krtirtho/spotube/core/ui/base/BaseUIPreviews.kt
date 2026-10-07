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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Shared scaffold for every preview of a base UI component.
 *
 * It mirrors the runtime environment the components expect:
 * - a [MaterialTheme] whose light/dark mode follows the preview's `uiMode`, so the
 *   `@PreviewLightDark`-style annotations actually change the rendered colors,
 * - a [LocalBaseUITheme] built from that color scheme via [rememberBaseUITheme],
 * - a [Surface] matching the real app background, so layered highlights and shadows
 *   render against the color they will actually sit on.
 *
 * Always render base components inside [BaseUIPreview]; several of them read
 * [LocalBaseUITheme] directly and would otherwise throw at composition time.
 */
@Composable
internal fun BaseUIPreview(
    modifier: Modifier = Modifier,
    surfaceTheme: SurfaceStyle = SurfaceStyle.Glass,
    content: @Composable BoxScope.() -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        val theme = rememberBaseUITheme(surfaceTheme = surfaceTheme)
        CompositionLocalProvider(
            LocalBaseUITheme provides theme,
            LocalBaseUIColors provides theme.colors,
        ) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = theme.colors.surface,
            ) {
                Box(modifier = Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

/**
 * Renders a base UI component on a phone-sized canvas in both light and dark themes.
 */
@Preview(name = "Phone · Light", group = "Base UI devices", device = Devices.PHONE)
@Preview(
    name = "Phone · Dark",
    group = "Base UI devices",
    device = Devices.PHONE,
    uiMode = UI_MODE_NIGHT_YES,
)
internal annotation class BaseUIPhonePreview

/**
 * Renders a base UI component on a desktop-sized canvas in both light and dark themes.
 */
@Preview(name = "Desktop · Light", group = "Base UI devices", device = Devices.DESKTOP)
@Preview(
    name = "Desktop · Dark",
    group = "Base UI devices",
    device = Devices.DESKTOP,
    uiMode = UI_MODE_NIGHT_YES,
)
internal annotation class BaseUIDesktopPreview
