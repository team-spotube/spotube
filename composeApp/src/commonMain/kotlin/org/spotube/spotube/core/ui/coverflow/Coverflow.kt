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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Coverflow(
    modifier: Modifier = Modifier,
    state: CoverflowState = rememberCoverflowState(),
    params: CoverflowParams = CoverflowParams(),
    content: CoverflowScope.() -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var geometry by remember { mutableStateOf<Geometry?>(null) }

    LaunchedEffect(geometry) {
        if (state.initialScrollDone) return@LaunchedEffect
        val g = geometry ?: return@LaunchedEffect
        state.lazyListState.scrollToItem(
            state.selectedIndex + 1,
            -g.spacerWidth)
        state.initialScrollDone = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    size = coordinates.size
                },
            verticalAlignment = Alignment.CenterVertically,
            state = state.lazyListState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state.lazyListState)) {
            if (size != IntSize.Zero) {
                val g = Geometry(params, size)
                geometry = g
                state.geometry = g

                val coverflowScope = CoverflowScopeImpl(
                    geometry = g,
                    lazyListScope = this,
                    coverflowState = state)

                item {
                    Spacer(modifier = Modifier.width(with(LocalDensity.current) { g.spacerWidth.toDp() }))
                }

                coverflowScope.apply(content)

                item {
                    Spacer(modifier = Modifier.width(with(LocalDensity.current) { g.spacerWidth.toDp() }))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clickable {
                    if (state.selectedIndex > 0) {
                        state.scrollToItem(state.selectedIndex - 1)
                    }
                }) {}
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .clickable {
                    state.scrollToItem(state.selectedIndex + 1)
                }) {}
    }
}
