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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

interface CoverflowScope {
    fun <T> items(
        items: List<T>,
        onSelectHandler: (item: T, index: Int) -> Unit = { _: T, _: Int -> },
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable ((item: T) -> Unit))

    fun items(
        count: Int,
        onSelectHandler: (index: Int) -> Unit = {},
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable ((index: Int) -> Unit))
}

internal class CoverflowScopeImpl(
    private val geometry: Geometry,
    private val lazyListScope: LazyListScope,
    private val coverflowState: CoverflowState) : CoverflowScope {
    override fun <T> items(
        items: List<T>,
        onSelectHandler: (item: T, index: Int) -> Unit,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable (item: T) -> Unit) = items(
        count = items.size,
        onSelectHandler = {
            onSelectHandler(items[it], it)
        },
        key = key,
        contentType = contentType) {
        itemContent(items[it])
    }

    override fun items(
        count: Int,
        onSelectHandler: (index: Int) -> Unit,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable (index: Int) -> Unit) = lazyListScope.items(
        count,
        key,
        contentType) {
        CoverflowItem(
            onClickHandler = { coverflowState.scrollToItem(it) },
            onSelectedHandler = { isSelected: Boolean ->
                if (selectHandler(it, isSelected)) {
                    onSelectHandler(it)
                }
            },
            geometry = geometry) {
            itemContent(it)
        }
    }

    private fun selectHandler(
        index: Int,
        isSelected: Boolean): Boolean {
        if (!isSelected && index == coverflowState.selectedIndex) {
            coverflowState.selectedIndex = -1
        } else if (isSelected && index != coverflowState.selectedIndex) {
            coverflowState.selectedIndex = index
            return true
        }
        return false
    }
}
