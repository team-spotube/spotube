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

package org.spotube.spotube.modules.shell.player_queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.composeunstyled.DrawerPlacement
import com.composeunstyled.SheetDetent
import com.composeunstyled.currentWindowWidthBreakpoint
import com.composeunstyled.rememberModalBottomSheetState
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.millennium.composables.MillenniumDrawer
import org.spotube.millennium.composables.MillenniumDrawerValue
import org.spotube.millennium.composables.MillenniumModalBottomSheet
import org.spotube.millennium.composables.rememberMillenniumDrawerState

private val SlidingSheetBreakpoint = 840.dp

@Composable
fun QueueSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isExpanded = currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded

    if (isExpanded) {
        EndDrawerSheet(isVisible, onDismiss, content)
    } else {
        BottomSheetContent(isVisible, onDismiss, content)
    }
}

@Composable
internal fun EndDrawerSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberMillenniumDrawerState()
    LaunchedEffect(isVisible) {
        drawerState.targetValue =
            if (isVisible) MillenniumDrawerValue.Open else MillenniumDrawerValue.Closed
    }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == MillenniumDrawerValue.Closed && isVisible) {
            onDismiss()
        }
    }
    MillenniumDrawer(
        state = drawerState,
        placement = DrawerPlacement.End,
        shape = RectangleShape,
        maximumWidth = 400.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
internal fun BottomSheetContent(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!isVisible) return
    // Show the whole sheet and cap the content instead: a detent that reveals only part of a
    // full-height sheet leaves the bottom of the queue list off-screen and unreachable.
    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded)
    )
    LaunchedEffect(sheetState.bottomSheetState.currentDetent) {
        if (sheetState.bottomSheetState.currentDetent == SheetDetent.Hidden) {
            onDismiss()
        }
    }
    MillenniumModalBottomSheet(
        state = sheetState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        contentSpacing = 8.dp
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            content()
        }
    }
}
