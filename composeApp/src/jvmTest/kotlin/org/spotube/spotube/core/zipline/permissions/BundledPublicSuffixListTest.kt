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

package org.spotube.spotube.core.zipline.permissions

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Loads the Public Suffix List the app ships, through the same resource path as the app. */
class BundledPublicSuffixListTest {
    private val psl = runBlocking { PublicSuffixList.bundled() }

    @Test
    fun knowsCommonSharedSuffixes() {
        // ICANN, private, wildcard (*.ck) and IDN (公司.cn) rules.
        for (suffix in listOf("com", "co.uk", "github.io", "blogspot.com", "appspot.com", "foo.ck", "xn--55qx5d.cn")) {
            assertTrue(psl.isPublicSuffix(suffix), suffix)
        }
        for (domain in listOf("example.com", "bbc.co.uk", "someone.github.io", "www.ck", "google.com")) {
            assertFalse(psl.isPublicSuffix(domain), domain)
        }
    }

    @Test
    fun rejectsWildcardsOverSharedSuffixes() {
        val declared = listOf("*.co.uk", "*.github.io", "*.appspot.com", "*.bbc.co.uk", "*.example.com")
        assertEquals(
            listOf("*.co.uk", "*.github.io", "*.appspot.com"),
            PluginPermissionValidator.invalidCookieDomains(declared, psl))
    }
}
