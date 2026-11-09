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

package org.spotube.spotube.core.ui.misc

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable

/*
 * Lazy lists crash when two items share a key ("Key ... was already used", #3196). Ids that come
 * from plugins can be blank or repeat (the same playlist twice in a library page, a track added
 * twice), so lists keyed on them use these instead of `items(list, key = { it.id })`.
 */

private const val DUPLICATE_SEPARATOR = '\u0000'

/**
 * One key per item: an item keeps its id the first time it appears, so scroll position and
 * animations stay stable; blank ids and repeats get an occurrence suffix that can't collide with
 * a real id.
 */
fun <T> uniqueLazyKeys(items: List<T>, id: (T) -> String): List<String> {
    val occurrences = HashMap<String, Int>(items.size)
    return items.map { item ->
        val base = id(item)
        val occurrence = occurrences[base] ?: 0
        occurrences[base] = occurrence + 1
        if (occurrence == 0 && base.isNotEmpty()) base else "$base$DUPLICATE_SEPARATOR$occurrence"
    }
}

fun <T> LazyListScope.itemsWithUniqueKeys(
    items: List<T>,
    id: (T) -> String,
    keyPrefix: String = "",
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val keys = uniqueLazyKeys(items, id)
    items(items.size, key = { keyPrefix + keys[it] }) { itemContent(items[it]) }
}

fun <T> LazyGridScope.itemsWithUniqueKeys(
    items: List<T>,
    id: (T) -> String,
    keyPrefix: String = "",
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    val keys = uniqueLazyKeys(items, id)
    items(items.size, key = { keyPrefix + keys[it] }) { itemContent(items[it]) }
}
