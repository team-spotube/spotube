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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.time.Duration.Companion.seconds
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

    private fun repo(respond: suspend (etag: String?) -> Download) = PublicSuffixListRepository(
        cacheDir = { dir },
        download = { etag -> requests += etag; respond(etag) },
        now = { time })

    private fun advance(by: Duration) { time += by }

    /** [PublicSuffixListRepository.get], then waits for any background refresh it started. */
    private suspend fun PublicSuffixListRepository.getSettled(): PublicSuffixList =
        get().also { awaitRefresh() }

    @Test
    fun downloadsOnceADayAndCachesOnDisk() = runBlocking {
        val repo = repo { Download.Fetched(listText, "\"v1\"") }
        assertTrue(repo.get().isPublicSuffix("github.io"))
        repo.getSettled()
        advance(23.hours)
        repo.getSettled()
        assertEquals(listOf<String?>(null), requests, "fetched more than once within a day")

        // A restart reads the disk copy instead of downloading again.
        val restarted = repo { error("should not download") }
        assertTrue(restarted.getSettled().isPublicSuffix("co.uk"))
        assertEquals(1, requests.size)
    }

    @Test
    fun refreshesWithAConditionalRequestAfterADay() = runBlocking {
        var unchanged = false
        val repo = repo { if (unchanged) Download.NotModified else Download.Fetched(listText, "\"v1\"") }
        repo.get()
        unchanged = true
        advance(25.hours)
        assertTrue(repo.getSettled().isPublicSuffix("github.io"))
        assertEquals(listOf(null, "\"v1\""), requests)
        // The 304 counts as today's check.
        repo.getSettled()
        assertEquals(2, requests.size)
    }

    @Test
    fun aStaleListIsServedWithoutWaitingForTheDownload() = runBlocking {
        val hang = CompletableDeferred<Download>()
        var stalled = false
        val repo = repo { if (stalled) hang.await() else Download.Fetched(listText, null) }
        repo.get()
        stalled = true
        advance(25.hours)
        // Returns the old list straight away while the refresh hangs in the background.
        val list = withTimeout(1.seconds) { repo.get() }
        assertTrue(list.isPublicSuffix("github.io"))
        hang.complete(Download.Fetched(listText, "\"v2\""))
        repo.awaitRefresh()
    }

    @Test
    fun keepsTheLastGoodListWhileOffline() = runBlocking {
        var offline = false
        val repo = repo { if (offline) error("offline") else Download.Fetched(listText, null) }
        repo.get()
        offline = true
        advance(30.hours)
        assertTrue(repo.getSettled().isPublicSuffix("github.io"))
        // Doesn't retry on every call while offline.
        advance(10.minutes)
        repo.getSettled()
        assertEquals(2, requests.size)
        advance(1.hours)
        repo.getSettled()
        assertEquals(3, requests.size)
    }

    @Test
    fun withNoListYetEveryDomainCountsAsASuffix() = runBlocking {
        val repo = repo { error("offline") }
        val list = repo.get()
        assertSame(PublicSuffixList.Unavailable, list)
        assertFalse(list.isAvailable)
        assertTrue(list.isPublicSuffix("example.com"))
        // Later calls don't wait on the network again until the retry delay passes.
        repo.getSettled()
        assertEquals(1, requests.size)
    }

    @Test
    fun failuresNeverEscape() = runBlocking {
        val repo = repo { throw IllegalStateException("boom") }
        repo.warmUp()
        repo.awaitRefresh()
        assertSame(PublicSuffixList.Unavailable, repo.get())
    }

    @Test
    fun rejectsResponsesThatArentTheList() = runBlocking {
        val repo = repo { Download.Fetched("<html>Sign in to the Wi-Fi</html>", null) }
        assertSame(PublicSuffixList.Unavailable, repo.get())
        assertFalse(FileSystem.SYSTEM.exists(dir / "public_suffix_list.dat"))
    }
}
