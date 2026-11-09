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

import co.touchlab.kermit.Logger
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.paths.Paths
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Keeps the Public Suffix List current: downloads it from publicsuffix.org at most once a day (a
 * conditional request, so an unchanged list costs a 304), keeps the last good copy on disk, and
 * falls back to that copy while offline.
 *
 * None of this may slow the app down or crash it:
 *  - Downloads and disk reads run on [scope], in the background. Constructing this does no work.
 *  - [get] answers from the copy it has, even a stale one, and refreshes in the background. It
 *    waits only when it has never had a list, and then for at most [DOWNLOAD_TIMEOUT].
 *  - Every failure is logged and leaves the last good list in place; [scope] also catches
 *    anything unexpected, so nothing escapes to the app.
 *
 * Until a list has ever been fetched, [get] returns [PublicSuffixList.Unavailable], under which
 * no wildcard cookie domain is accepted. That only ever narrows access.
 */
class PublicSuffixListRepository(
    /** Resolved in the background, on first use. */
    private val cacheDir: () -> Path,
    private val download: suspend (etag: String?) -> Download,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val now: () -> Instant = { Clock.System.now() },
    private val logger: Logger = Logger.withTag("PublicSuffixListRepository"),
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            logger.e(e) { "Public suffix list work failed" }
        })) {

    sealed interface Download {
        data class Fetched(val text: String, val etag: String?) : Download
        data object NotModified : Download
    }

    private class Cached(val list: PublicSuffixList, val checkedAt: Instant, val etag: String?)

    private val lock = Mutex()
    @Volatile
    private var cached: Cached? = null
    @Volatile
    private var cacheRead = false
    @Volatile
    private var lastFailureAt: Instant? = null
    private var refreshJob: Job? = null

    private val listFile get() = cacheDir() / "public_suffix_list.dat"
    private val metaFile get() = cacheDir() / "public_suffix_list.meta"

    /**
     * The newest list available, starting a background refresh when the last check is more than a
     * day old. Waits for that refresh only when there's no list at all yet.
     */
    suspend fun get(): PublicSuffixList {
        cached?.let { current ->
            if (isDue(current)) startRefresh()
            return current.list
        }
        startRefresh()?.join()
        return cached?.list ?: PublicSuffixList.Unavailable
    }

    /** Reads the disk copy and refreshes it if due, in the background. Returns immediately. */
    fun warmUp() {
        scope.launch { get() }
    }

    private fun isDue(current: Cached?): Boolean {
        val time = now()
        if (current != null && time - current.checkedAt < MAX_AGE) return false
        return lastFailureAt?.let { time - it >= RETRY_AFTER_FAILURE } ?: true
    }

    /** The running refresh, a new one if the list is due, or `null` if it's current. */
    private suspend fun startRefresh(): Job? = lock.withLock {
        refreshJob?.takeIf { it.isActive }?.let { return@withLock it }
        if (cacheRead && !isDue(cached)) return@withLock null
        scope.launch { refresh() }.also { refreshJob = it }
    }

    private suspend fun refresh() {
        if (!cacheRead) {
            cached = readCache()
            cacheRead = true
            if (!isDue(cached)) return
        }
        val current = cached
        val time = now()
        try {
            when (val result = withTimeout(DOWNLOAD_TIMEOUT) { download(current?.etag) }) {
                is Download.Fetched -> {
                    val list = PublicSuffixList.parse(result.text)
                    check(PublicSuffixList.looksComplete(result.text, list)) {
                        "Downloaded public suffix list looks incomplete"
                    }
                    cached = Cached(list, time, result.etag)
                    saveCache(result.text, time, result.etag)
                }
                Download.NotModified -> {
                    // Only sent with the cached copy's ETag, so there's a copy to keep.
                    val unchanged = checkNotNull(current) { "304 without a cached list" }
                    cached = Cached(unchanged.list, time, unchanged.etag)
                    saveCache(null, time, unchanged.etag)
                }
            }
            lastFailureAt = null
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            refreshFailed(e, current, time)
        } catch (e: Exception) {
            refreshFailed(e, current, time)
        }
    }

    private fun refreshFailed(e: Exception, current: Cached?, time: Instant) {
        lastFailureAt = time
        logger.w(e) { "Couldn't refresh the public suffix list; using ${if (current != null) "the cached copy" else "none"}" }
    }

    /** Writes the list (unless [text] is null: unchanged) and when it was checked. Best effort. */
    private fun saveCache(text: String?, checkedAt: Instant, etag: String?) {
        try {
            fileSystem.createDirectories(cacheDir())
            if (text != null) fileSystem.write(listFile) { writeUtf8(text) }
            fileSystem.write(metaFile) {
                writeUtf8(checkedAt.toEpochMilliseconds().toString())
                writeUtf8("\n")
                writeUtf8(etag.orEmpty())
            }
        } catch (e: Exception) {
            logger.w(e) { "Couldn't save the public suffix list; it stays in memory" }
        }
    }

    private fun readCache(): Cached? = try {
        if (!fileSystem.exists(listFile) || !fileSystem.exists(metaFile)) {
            null
        } else {
            val text = fileSystem.read(listFile) { readUtf8() }
            val meta = fileSystem.read(metaFile) { readUtf8() }.lines()
            val list = PublicSuffixList.parse(text)
            if (!PublicSuffixList.looksComplete(text, list)) {
                null
            } else {
                Cached(
                    list = list,
                    checkedAt = Instant.fromEpochMilliseconds(meta.first().trim().toLong()),
                    etag = meta.getOrNull(1)?.trim()?.ifEmpty { null })
            }
        }
    } catch (e: Exception) {
        logger.w(e) { "Ignoring an unreadable cached public suffix list" }
        null
    }

    /** Waits for a refresh in progress. For tests. */
    internal suspend fun awaitRefresh() {
        lock.withLock { refreshJob }?.join()
    }

    companion object {
        /** The list's only official location; its maintainers ask for at most one fetch a day. */
        const val LIST_URL = "https://publicsuffix.org/list/public_suffix_list.dat"

        val MAX_AGE: Duration = 24.hours
        /** Wait this long after a failed download before trying again, so offline calls stay fast. */
        val RETRY_AFTER_FAILURE: Duration = 1.hours
        val DOWNLOAD_TIMEOUT: Duration = 20.seconds

        fun create(paths: Paths, httpClientFactory: HttpClientFactory): PublicSuffixListRepository =
            PublicSuffixListRepository(
                cacheDir = { "${paths.getApplicationDataDirPath()}/public-suffix-list".toPath() },
                download = { etag ->
                    val response = httpClientFactory.default.get(LIST_URL) {
                        if (etag != null) header(HttpHeaders.IfNoneMatch, etag)
                    }
                    when (response.status) {
                        HttpStatusCode.NotModified -> Download.NotModified
                        HttpStatusCode.OK -> Download.Fetched(response.bodyAsText(), response.headers[HttpHeaders.ETag])
                        else -> error("HTTP ${response.status.value} from $LIST_URL")
                    }
                }).also { it.warmUp() }
    }
}
