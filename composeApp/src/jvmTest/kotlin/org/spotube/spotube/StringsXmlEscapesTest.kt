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

package org.spotube.spotube

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose Multiplatform resources do not unescape `\'` and `\"` (Android's aapt does), so they show up with the
 * backslash in the UI. Write a plain ' or " in strings.xml. `\n`, `\t` and `\uXXXX` are handled.
 */
class StringsXmlEscapesTest {
    @Test
    fun noEscapedQuotesInStrings() {
        val bad = File("src/commonMain/composeResources").walkTopDown()
            .filter { it.name == "strings.xml" }
            .flatMap { file ->
                file.readLines().withIndex().filter { (_, line) -> Regex("""(?<!\\)\\['"]""").containsMatchIn(line) }
                    .map { "${file.parentFile.name}/strings.xml:${it.index + 1}" }
            }.toList()
        assertTrue(bad.isEmpty(), "escaped quotes show a literal backslash in Compose resources; use ' or \" directly: ${bad.take(10)}")
    }
}
