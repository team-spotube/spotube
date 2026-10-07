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
 * Material's [Scaffold] paints Material's own surface colour, which hides the selected
 * [SurfaceStyle] (Watery/Icy/Chrome/Acrylic/Glass). [BaseScaffold] instead:
 *
 * - is **transparent by default**, so the app-wide themed backdrop shows through, or
 * - paints a chosen [surface] material locally, or
 * - uses an explicit [containerColor] when one is given.
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
    surface: SurfaceStyle? = null,
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
        val backgroundModifier = when {
            containerColor != null -> Modifier.background(containerColor)
            surface != null -> {
                val isLight = LocalBaseUIColors.current.isLight
                Modifier.baseSurfaceBackdrop(surface, isLight)
            }

            else -> Modifier
        }
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
        val style = LocalBaseUITheme.current.surfaceTheme
        BaseScaffold(
            surface = style,
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
