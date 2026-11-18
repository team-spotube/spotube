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

package org.spotube.spotube.modules.settings

import org.spotube.spotube.core.ui.theming.engine.ThemeUpdateSource
import org.spotube.spotube.core.ui.theming.engine.parseThemeDocument
import org.spotube.spotube.modules.plugin.GitHubAsset
import org.spotube.spotube.modules.plugin.GitHubRelease
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeUpdatesTest {
    private val source = ThemeUpdateSource("owner", "repo", "my-theme.zip")

    private fun release(tag: String, asset: String? = "my-theme.zip", prerelease: Boolean = false, draft: Boolean = false) = GitHubRelease(
        tagName = tag,
        assets = listOfNotNull(asset?.let { GitHubAsset(it, "https://github.com/owner/repo/releases/download/$tag/$it") }),
        htmlUrl = "https://github.com/owner/repo/releases/tag/$tag",
        prerelease = prerelease,
        draft = draft,
    )

    @Test
    fun tagsAreSemverWithOrWithoutV() {
        assertEquals("1.2.3", parseReleaseVersion("v1.2.3")?.toString())
        assertEquals("1.2.3", parseReleaseVersion("1.2.3")?.toString())
        assertNull(parseReleaseVersion("release-1"))
        assertNull(parseReleaseVersion("v1.2"))
    }

    @Test
    fun picksTheNewestPublishedReleaseWithTheArchive() {
        val releases = listOf(
            release("v1.0.0"), release("v1.2.0"), release("v1.1.0"),
            release("v9.0.0", prerelease = true), release("v8.0.0", draft = true),
            release("v7.0.0", asset = "other.zip"), release("latest"),
        )
        val update = latestThemeUpdate("1.0.0", releases, source)
        assertEquals("1.2.0", update?.version)
        assertEquals("v1.2.0", update?.tag)
        assertTrue(update!!.assetUrl.endsWith("/v1.2.0/my-theme.zip"))
    }

    @Test
    fun noUpdateWhenInstalledIsCurrentOrNewer() {
        assertNull(latestThemeUpdate("1.2.0", listOf(release("v1.2.0")), source))
        assertNull(latestThemeUpdate("2.0.0", listOf(release("v1.2.0")), source))
        assertEquals("1.0.0", latestThemeUpdate(null, listOf(release("1.0.0")), source)?.version)
    }

    @Test
    fun assetsMustComeFromGitHub() {
        assertTrue(isGitHubAssetUrl("https://github.com/o/r/releases/download/v1/a.zip"))
        assertTrue(!isGitHubAssetUrl("http://github.com/o/r/a.zip"))
        assertTrue(!isGitHubAssetUrl("https://evil.example/github.com/a.zip"))
        assertTrue(!isGitHubAssetUrl("https://github.com/o/r/a.zip\n"))
    }

    @Test
    fun themeDeclaresItsUpdateSource() {
        val problems = linkedSetOf<String>()
        val doc = parseThemeDocument("""{"schema":"1.2.0","updates":{"github":"https://github.com/o/r","asset":"t.zip"}}""", problems)
        assertEquals(ThemeUpdateSource("o", "r", "t.zip"), doc?.meta?.updates)
        assertEquals(emptySet(), problems)

        val bad = linkedSetOf<String>()
        assertNull(parseThemeDocument("""{"updates":{"github":"https://evil.example/o/r","asset":"t.zip"}}""", bad)?.meta?.updates)
        assertTrue(bad.single().contains("updates.github"))
        val badAsset = linkedSetOf<String>()
        assertNull(parseThemeDocument("""{"updates":{"github":"https://github.com/o/r","asset":"../t.exe"}}""", badAsset)?.meta?.updates)
        assertTrue(badAsset.single().contains("updates.asset"))
    }
}
