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

package dev.krtirtho.spotube.core.ui.component

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.krtirtho.spotube.core.ui.base.LocalBaseUIColors

@Composable
actual fun VerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    val colors = LocalBaseUIColors.current
    CompositionLocalProvider(
        LocalScrollbarStyle provides ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = colors.onSurface.copy(alpha = 0.18f),
            hoverColor = colors.onSurface.copy(alpha = 0.4f),
        ),
    ) {
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = modifier.fillMaxHeight()
                .padding(end = 2.dp)
                .padding(vertical = 16.dp)
                .width(12.dp),
        )
    }
}