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

package org.spotube.spotube.core.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The library's non-composable getters query the system theme over D-Bus on Linux on every
 * call, which crashed the desktop app (#3183). App code must use the ones in Strings.kt.
 */
class NonComposableStringLookupTest {
    @Test
    fun appCodeUsesTheAppStringLookups() {
        val forbidden = Regex("""^import org\.jetbrains\.compose\.resources\.get(String|PluralString|StringArray)\s*$""")
        val offenders = File("src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> forbidden.matches(line) }
                    .map { (index, _) -> "${file.path}:${index + 1}" }
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "Import getString/getPluralString/getStringArray from org.spotube.spotube.core.i18n instead:\n" +
                offenders.joinToString("\n"))
    }
}
