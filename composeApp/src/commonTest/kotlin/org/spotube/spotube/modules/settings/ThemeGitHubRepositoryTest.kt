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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Theme discovery must not burn GitHub's tiny unauthenticated rate limit. */
class ThemeGitHubRepositoryTest {
    private val searchJson = """{"total_count":0,"incomplete_results":false,"items":[]}"""

    private class Recorder { val requests = mutableListOf<HttpRequestData>() }

    private fun repo(rec: Recorder, answer: (HttpRequestData) -> io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.client.request.HttpResponseData): ThemeGitHubRepository =
        ThemeGitHubRepository(HttpClient(MockEngine { req -> rec.requests += req; answer(req)(this) }))

    @Test
    fun repeatedSearchesWithinTheTtlCostOneRequest() = runTest {
        val rec = Recorder()
        val repo = repo(rec) { { respond(searchJson, HttpStatusCode.OK, headersOf(HttpHeaders.ETag, "\"v1\"")) } }
        repeat(5) { repo.searchThemes() }
        assertEquals(1, rec.requests.size)
        // A different page is a different request.
        repo.searchThemes(page = 2)
        assertEquals(2, rec.requests.size)
    }

    @Test
    fun forcedRefreshRevalidatesWithEtagAndKeepsTheCachedBodyOn304() = runTest {
        val rec = Recorder()
        val repo = repo(rec) { req ->
            if (req.headers[HttpHeaders.IfNoneMatch] == "\"v1\"") ({ respond("", HttpStatusCode.NotModified) })
            else ({ respond(searchJson, HttpStatusCode.OK, headersOf(HttpHeaders.ETag, "\"v1\"")) })
        }
        repo.searchThemes()
        val again = repo.searchThemes(force = true)
        assertEquals(0, again.totalCount)
        assertEquals(2, rec.requests.size)
        assertEquals("\"v1\"", rec.requests[1].headers[HttpHeaders.IfNoneMatch])
        assertNull(rec.requests[0].headers[HttpHeaders.IfNoneMatch])
    }

    @Test
    fun aRateLimitPausesAllRequestsAndServesStaleDataWhenItHasSome() = runTest {
        val rec = Recorder()
        var limited = false
        val repo = repo(rec) {
            if (limited) ({ respondError(HttpStatusCode.Forbidden, "", headersOf("X-RateLimit-Remaining" to listOf("0"), HttpHeaders.RetryAfter to listOf("120"))) })
            else ({ respond(searchJson, HttpStatusCode.OK) })
        }
        repo.searchThemes()
        limited = true
        // Stale data is served instead of failing, and the limit is remembered...
        assertEquals(0, repo.searchThemes(force = true).totalCount)
        val sent = rec.requests.size
        // ...so nothing else goes out, not even for other pages or other endpoints, until it resets.
        assertFailsWith<GitHubRateLimitedException> { repo.searchThemes(page = 2) }
        assertFailsWith<GitHubRateLimitedException> { repo.releases("a", "b") }
        assertEquals(sent, rec.requests.size)
    }

    @Test
    fun aPlainForbiddenIsNotTreatedAsARateLimit() = runTest {
        val rec = Recorder()
        val repo = repo(rec) { { respondError(HttpStatusCode.Forbidden, "nope", headersOf("X-RateLimit-Remaining", "30")) } }
        assertFailsWith<IllegalStateException> { repo.searchThemes() }
        assertFailsWith<IllegalStateException> { repo.searchThemes(page = 2) }
        assertEquals(2, rec.requests.size)
    }

    @Test
    fun releaseListsAreCachedPerRepository() = runTest {
        val rec = Recorder()
        val repo = repo(rec) { { respond("[]", HttpStatusCode.OK) } }
        repeat(3) { repo.releases("o", "r") }
        repo.releases("o", "other")
        assertEquals(2, rec.requests.size)
    }
}
