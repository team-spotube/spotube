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

package org.spotube.spotube.core.jam

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JamNamesTest {
    @Test
    fun sameSeedSameName() = runTest {
        assertEquals(JamNames.generate("device-a"), JamNames.generate("device-a"))
    }

    @Test
    fun namesAreTwoFriendlyWordsAndVaried() = runTest {
        val names = List(60) { JamNames.generate("device-$it") }
        assertTrue(names.all { Regex("^[A-Z][a-z]+ [A-Z][a-z]+$").matches(it) }, names.toString())
        assertTrue(names.all { it.length <= JamNames.MAX_LENGTH })
        assertTrue(names.toSet().size >= 50, "only ${names.toSet().size} distinct names")
    }

    @Test
    fun typedNamesAreCleaned() {
        assertEquals("Kingkor Roy", JamNames.normalize("  Kingkor \u0000 \t Roy \n"))
        assertEquals("", JamNames.normalize("   "))
    }
}
