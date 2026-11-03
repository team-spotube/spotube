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

package org.spotube.spotube.modules.shell


import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.millennium.adaptive.WindowBreakpoints
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.min

data class CompactSheetUiState(
    val progress: Float,
    val showExpandedPlayer: Boolean,
    val expandedPlayerAlpha: Float,
    val floatingPlayerAlpha: Float,
    val bottomBarAlpha: Float
)

data class LyricsOverlayState(
    val isVisible: Boolean = false
)

data class CompactChromeUiState(val measuredBottomBarHeight: Dp? = null)

class AppShellViewModel : ViewModel() {
    val sidebarMinWidth = 840.dp
    val bottomBarHeight = 80.dp
    // The background bleed overlaps the bottom bar and is not part of the peek height.
    val floatingPlayerHeight = FloatingPlayerContentHeight
    val largePlayerInset = 100.dp
    val floatingPlayerDismissProgress = 0.7f
    val expandedPlayerVisibilityThreshold = 0.001f

    private val lyricsOverlayVisible = MutableStateFlow(false)
    private val compactChrome = MutableStateFlow(CompactChromeUiState())
    val compactChromeUiState = compactChrome.asStateFlow()

    fun onBottomBarMeasured(height: Dp) {
        if (height > 0.dp) compactChrome.value = CompactChromeUiState(height)
    }

    val isLyricsOverlayVisible: StateFlow<Boolean> = lyricsOverlayVisible.asStateFlow()

    @Composable
    fun useSidebar(maxWidth: Dp? = null): Boolean {
        if (maxWidth != null) {
            return maxWidth >= sidebarMinWidth
        }
        return currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded
    }

    fun bottomOverlayInset(
        useSidebar: Boolean,
        navigationBarInset: Dp,
        chrome: CompactChromeUiState
    ): Dp =
        if (useSidebar) largePlayerInset + navigationBarInset
        else (chrome.measuredBottomBarHeight ?: (bottomBarHeight + navigationBarInset)) +
                floatingPlayerHeight

    fun sheetProgress(sheetOffsetPx: Float, sheetHeightPx: Float): Float =
        if (sheetHeightPx > 0f) {
            (sheetOffsetPx / sheetHeightPx).coerceIn(0f, 1f)
        } else {
            0f
        }

    fun floatingPlayerAlpha(progress: Float): Float {
        val normalized = min(progress / floatingPlayerDismissProgress, 1f)
        return 1f - normalized
    }

    fun compactSheetUiState(progress: Float): CompactSheetUiState {
        return CompactSheetUiState(
            progress = progress,
            showExpandedPlayer = progress > expandedPlayerVisibilityThreshold,
            expandedPlayerAlpha = progress,
            floatingPlayerAlpha = floatingPlayerAlpha(progress),
            bottomBarAlpha = 1f - progress
        )
    }

    fun showLyricsOverlay() {
        lyricsOverlayVisible.value = true
    }

    fun hideLyricsOverlay() {
        lyricsOverlayVisible.value = false
    }

    fun toggleLyricsOverlay() {
        lyricsOverlayVisible.update { !it }
    }
}
