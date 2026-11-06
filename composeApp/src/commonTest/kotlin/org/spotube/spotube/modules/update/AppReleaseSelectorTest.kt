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

package org.spotube.spotube.modules.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppReleaseSelectorTest {
    private fun version(v: String) = assertNotNull(AppReleaseSelector.parseTag(v))
    private fun releases(vararg tags: String) = tags.map { GitHubRelease(tagName = it) }
    private fun select(current: String, vararg tags: String, ignored: String? = null) =
        AppReleaseSelector.select(version(current), releases(*tags), ignored)?.tagName

    @Test
    fun parsesFullSemVerTags() {
        assertEquals("6.0.0-beta.2+build.7", version(" V6.0.0-beta.2+build.7 ").toString())
        assertEquals("1.2.3", version("v1.2.3").toString())
        assertNull(AppReleaseSelector.parseTag("nightly"))
        assertNull(AppReleaseSelector.parseTag("v6.0"))
        assertNull(AppReleaseSelector.parseTag("v01.0.0"))
    }

    @Test
    fun stableInstallIgnoresPreReleases() {
        assertEquals("v5.1.0", select("5.0.0", "v6.0.0-beta.1", "v5.1.0", "nightly", "v5.0.1"))
        assertNull(select("5.0.0", "v6.0.0-beta.1", "nightly"))
    }

    @Test
    fun preReleaseInstallFollowsNewerPreReleases() {
        assertEquals("v6.0.0-beta.10", select("6.0.0-beta.2", "v6.0.0-beta.10", "v6.0.0-beta.9", "v5.9.0"))
        assertEquals("v6.0.0-rc.1", select("6.0.0-beta.2", "v6.0.0-beta.3", "v6.0.0-rc.1"))
        assertEquals("v6.0.0", select("6.0.0-beta.2", "v6.0.0-rc.1", "v6.0.0"))
        assertEquals("v6.0.0-beta.0", select("6.0.0-alpha", "v6.0.0-beta.0", "v6.0.0-alpha.1"))
    }

    @Test
    fun buildMetadataDoesNotCountAsNewer() {
        assertNull(select("6.0.0", "v6.0.0+build.2"))
    }

    @Test
    fun skipsDraftsAndOlderReleases() {
        val list = listOf(GitHubRelease("v7.0.0", draft = true), GitHubRelease("v5.0.0"))
        assertNull(AppReleaseSelector.select(version("6.0.0"), list, ignoredTag = null))
    }

    @Test
    fun ignoredVersionMatchesBySemVerPrecedence() {
        assertNull(select("6.0.0", "v6.1.0+build.3", ignored = "V6.1.0"))
        assertEquals("v6.2.0", select("6.0.0", "v6.1.0", "v6.2.0", ignored = "v6.1.0"))
    }
}
