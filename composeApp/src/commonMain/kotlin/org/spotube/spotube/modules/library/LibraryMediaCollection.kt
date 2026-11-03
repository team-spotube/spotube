/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library

import org.spotube.spotube.core.ui.misc.itemsWithUniqueKeys
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.misc.SkeletonTree
import org.spotube.spotube.modules.shell.LocalAppShellBottomInset
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only reports viewport geometry. The owning ViewModel decides when to load another page. */
@Composable
internal fun <T> LibraryMediaCollection(
    items: List<T>,
    itemKey: (T) -> String,
    viewMode: LibraryViewMode,
    onViewportChanged: (Int?, Int) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit,
    skeleton: @Composable (Boolean) -> Unit,
    loading: Boolean = false,
    loadingMore: Boolean = false,
    header: (@Composable (Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val reportViewport by rememberUpdatedState(onViewportChanged)
    val padding = PaddingValues(top = 16.dp, bottom = LocalAppShellBottomInset.current + 16.dp)
    LaunchedEffect(viewMode, gridState, listState) {
        val positions = if (viewMode == LibraryViewMode.List) snapshotFlow {
            listState.layoutInfo.let { it.visibleItemsInfo.lastOrNull()?.index to it.totalItemsCount }
        } else snapshotFlow {
            gridState.layoutInfo.let { it.visibleItemsInfo.lastOrNull()?.index to it.totalItemsCount }
        }
        positions.distinctUntilChanged().collect { (last, total) -> reportViewport(last, total) }
    }
    if (viewMode == LibraryViewMode.List) {
        LazyColumn(
            modifier.fillMaxSize(), state = listState, contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) items(8) { SkeletonTree(true) { skeleton(true) } }
            else {
                if (header != null) item(key = "library_header") { header(true) }
                itemsWithUniqueKeys(items, id = itemKey, keyPrefix = "media:") { itemContent(it, true) }
                if (loadingMore) items(4) { SkeletonTree(true) { skeleton(true) } }
            }
        }
    } else {
        LazyVerticalGrid(
            GridCells.Adaptive(160.dp), modifier.fillMaxSize(), state = gridState,
            contentPadding = padding, horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (loading) items(8) { SkeletonTree(true) { skeleton(false) } }
            else {
                if (header != null) item(key = "library_header") { header(false) }
                itemsWithUniqueKeys(items, id = itemKey, keyPrefix = "media:") { itemContent(it, false) }
                if (loadingMore) items(4) { SkeletonTree(true) { skeleton(false) } }
            }
        }
    }
}
