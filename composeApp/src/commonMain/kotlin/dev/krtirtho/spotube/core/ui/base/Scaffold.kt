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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A theme-aware replacement for [Scaffold].
 *
 * A scaffold is always the current theme's **Primary Background** — there is no option to
 * switch its surface. Material's [Scaffold] would paint Material's own surface colour and
 * hide the selected [SurfaceStyle] (Watery/Icy/Chrome/Acrylic/Glass), so [BaseScaffold]
 * paints the theme's primary background material instead.
 *
 * [containerColor] is a narrow escape hatch for the rare screen that paints its own
 * backdrop (e.g. onboarding) and needs the scaffold to stay out of the way; it is not a
 * surface-style switch.
 *
 * It keeps the same slot structure as [Scaffold] (top/bottom bars, snackbar host,
 * floating action button) and hands the resolved [PaddingValues] to [content].
 */
@Composable
fun BaseScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color? = null,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        containerColor = Color.Transparent,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        val colors = LocalBaseUIColors.current
        val backgroundModifier = containerColor?.let { Modifier.background(it) }
            ?: Modifier.baseSurfaceBackdrop(colors.surfaceTheme, colors.isLight)
        Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
            content(innerPadding)
        }
    }
}

@BaseUIPhonePreview
@BaseUIDesktopPreview
@Composable
private fun BaseScaffoldPreview() {
    BaseUIPreview {
        BaseScaffold(
            topBar = {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            },
            bottomBar = {
                Text(
                    text = "Bottom bar",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Content")
                Text("Sits on the selected surface theme.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
