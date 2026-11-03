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

package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop

@Composable
expect fun VerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier)

fun Modifier.dragScrollable(rowState: LazyListState): Modifier =
    if (getPlatform().isDesktop()) pointerInput(rowState) {
        detectDragGestures { change, dragAmount ->
            // Invert drag direction so content follows direct-manipulation behavior.
            rowState.dispatchRawDelta(-dragAmount.x)
        }
    } else Modifier
