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

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueLazyKeysTest {
    @Test
    fun blankAndRepeatedIdsGetDistinctKeys() {
        val ids = listOf("a", "", "b", "a", "", "a")
        val keys = uniqueLazyKeys(ids) { it }

        assertEquals(ids.size, keys.toSet().size)
        // First occurrences keep their id so scroll position stays stable.
        assertEquals("a", keys[0])
        assertEquals("b", keys[2])
    }

    @Test
    fun suffixesCannotCollideWithRealIds() {
        val ids = listOf("a", "a", "a#1", "a1", "a_1")
        assertEquals(ids.size, uniqueLazyKeys(ids) { it }.toSet().size)
    }
}
