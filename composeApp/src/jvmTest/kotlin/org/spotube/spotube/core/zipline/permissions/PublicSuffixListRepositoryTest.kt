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
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.spotube.core.zipline.permissions.PublicSuffixListRepository.Download
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PublicSuffixListRepositoryTest {
    private val dir: Path = (System.getProperty("java.io.tmpdir").toPath() / "psl-test-${System.nanoTime()}")
    private var time = Instant.fromEpochMilliseconds(1_700_000_000_000)
    private val requests = mutableListOf<String?>()

    /** Enough of a list to pass the completeness check, plus a few real rules. */
    private val listText = buildString {
        appendLine("// ===BEGIN ICANN DOMAINS===")
        appendLine("com"); appendLine("uk"); appendLine("co.uk")
        repeat(1200) { appendLine("tld$it") }
        appendLine("// ===BEGIN PRIVATE DOMAINS===")
        appendLine("github.io")
    }

    @AfterTest
    fun cleanUp() = FileSystem.SYSTEM.deleteRecursively(dir)

    private fun repo(respond: (etag: String?) -> Download) = PublicSuffixListRepository(
        cacheDir = dir,
        download = { etag -> requests += etag; respond(etag) },
        now = { time })

    private fun advance(by: Duration) { time += by }

    @Test
    fun downloadsOnceADayAndCachesOnDisk() = runBlocking {
        val repo = repo { Download.Fetched(listText, "\"v1\"") }
        assertTrue(repo.get().isPublicSuffix("github.io"))
        repo.get()
        advance(23.hours)
        repo.get()
        assertEquals(listOf<String?>(null), requests, "fetched more than once within a day")

        // A restart reads the disk copy instead of downloading again.
        val restarted = repo { error("should not download") }
        assertTrue(restarted.get().isPublicSuffix("co.uk"))
    }

    @Test
    fun refreshesWithAConditionalRequestAfterADay() = runBlocking {
        var unchanged = false
        val repo = repo { if (unchanged) Download.NotModified else Download.Fetched(listText, "\"v1\"") }
        repo.get()
        unchanged = true
        advance(25.hours)
        assertTrue(repo.get().isPublicSuffix("github.io"))
        assertEquals(listOf(null, "\"v1\""), requests)
        // The 304 counts as today's check.
        repo.get()
        assertEquals(2, requests.size)
    }

    @Test
    fun keepsTheLastGoodListWhileOffline() = runBlocking {
        var offline = false
        val repo = repo { if (offline) error("offline") else Download.Fetched(listText, null) }
        repo.get()
        offline = true
        advance(30.hours)
        assertTrue(repo.get().isPublicSuffix("github.io"))
        // Doesn't retry on every call while offline.
        advance(10.minutes)
        repo.get()
        assertEquals(2, requests.size)
        advance(1.hours)
        repo.get()
        assertEquals(3, requests.size)
    }

    @Test
    fun withNoListYetEveryDomainCountsAsASuffix() = runBlocking {
        val list = repo { error("offline") }.get()
        assertSame(PublicSuffixList.Unavailable, list)
        assertFalse(list.isAvailable)
        assertTrue(list.isPublicSuffix("example.com"))
    }

    @Test
    fun rejectsResponsesThatArentTheList() = runBlocking {
        val repo = repo { Download.Fetched("<html>Sign in to the Wi-Fi</html>", null) }
        assertSame(PublicSuffixList.Unavailable, repo.get())
        assertFalse(FileSystem.SYSTEM.exists(dir / "public_suffix_list.dat"))
    }
}
