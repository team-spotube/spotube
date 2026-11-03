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

package org.spotube.spotube.core.ui.coverflow

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberCoverflowState(
    initialIndex: Int = 0,
    onSelectHandler: (Int) -> Unit = {}): CoverflowState {
    val lazyListState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex + 1)
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    return remember {
        CoverflowState(
            lazyListState,
            coroutineScope,
            onSelectHandler,
            initialIndex)
    }
}

class CoverflowState internal constructor(
    internal val lazyListState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val onSelectHandler: (Int) -> Unit = {},
    initialIndex: Int = 0) {
    internal var geometry: Geometry? = null
    internal var initialScrollDone: Boolean = false

    var selectedIndex: Int = initialIndex
        internal set(it) {
            field = it
            onSelectHandler(it)
        }

    fun scrollToItem(index: Int) {
        val geometryCopy = geometry
        if (geometryCopy != null) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(
                    index + 1,
                    -geometryCopy.spacerWidth)
            }
        }
    }
}
