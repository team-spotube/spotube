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

package org.spotube.spotube.modules.shell.alternative_track



import androidx.compose.foundation.layout.Box
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.spotube.spotube.modules.shell.player_queue.BottomSheetContent
import org.spotube.spotube.modules.shell.player_queue.EndDrawerSheet

private val SlidingSheetBreakpoint = 840.dp

@Composable
fun AlternativeTrackSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        if (currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded) {
            EndDrawerSheet(isVisible, onDismiss, content)
        } else {
            BottomSheetContent(isVisible, onDismiss, content)
        }
    }
}
