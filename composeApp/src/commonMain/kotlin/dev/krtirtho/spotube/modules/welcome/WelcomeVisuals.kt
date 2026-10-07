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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.modules.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import dev.krtirtho.spotube.core.ui.base.LocalBaseUIColors
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.rememberBaseUITheme
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.*

/** App-native controls and surfaces, in neutral charcoal/ivory instead of the user's accent. */
@Composable
fun WelcomeTheme(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF8BA8DD), onPrimary = Color(0xFF10213E),
        primaryContainer = Color(0xFF263754), onPrimaryContainer = Color(0xFFD9E4F7),
        secondary = Color(0xFFBDBDBD), secondaryContainer = Color(0xFF343434),
        onSecondaryContainer = Color(0xFFF0F0F0),
        surface = Color(0xFF181818), background = Color(0xFF181818),
        surfaceContainer = Color(0xFF222222), surfaceContainerHigh = Color(0xFF292929),
        surfaceContainerHighest = Color(0xFF323232),
        onSurface = Color(0xFFEDEBE7), onSurfaceVariant = Color(0xFFABA9A5),
        outlineVariant = Color(0xFF414141),
    ) else lightColorScheme(
        primary = Color(0xFF244879), onPrimary = Color.White,
        primaryContainer = Color(0xFFDDE5F1), onPrimaryContainer = Color(0xFF173252),
        secondaryContainer = Color(0xFFE5E3DF), onSecondaryContainer = Color(0xFF292929),
        surface = Color(0xFFF4F3F0), background = Color(0xFFF4F3F0),
        surfaceContainer = Color(0xFFEDEBE7), surfaceContainerHigh = Color(0xFFFDFCFA),
        surfaceContainerHighest = Color(0xFFE1DFDB),
        onSurface = Color(0xFF252525), onSurfaceVariant = Color(0xFF696762),
        outlineVariant = Color(0xFFD2D0CC),
    )
    MaterialTheme(colorScheme = scheme) {
        val baseTheme = rememberBaseUITheme()
        CompositionLocalProvider(
            LocalBaseUITheme provides baseTheme,
            LocalBaseUIColors provides baseTheme.colors,
        ) {
            content()
        }
    }
}

@Composable
fun WelcomeBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(scheme.surfaceContainer, scheme.surface)),
        )
    )
}

@Composable
fun WelcomeWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.displaySmall.fontSize
) {
    Text(
        stringResource(Res.string.welcome_app_name),
        modifier = modifier,
        fontFamily = FontFamily(Font(Res.font.cookie_regular)),
        fontSize = fontSize,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
