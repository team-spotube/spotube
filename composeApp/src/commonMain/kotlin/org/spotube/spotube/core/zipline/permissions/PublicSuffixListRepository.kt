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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.IO
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.spotube.core.paths.Paths
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
 * Until a list has ever been fetched, [get] returns [PublicSuffixList.Unavailable], under which
 * no wildcard cookie domain is accepted. That only ever narrows access.
 */
class PublicSuffixListRepository(
    private val cacheDir: Path,
    private val download: suspend (etag: String?) -> Download,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val now: () -> Instant = { Clock.System.now() },
    private val logger: Logger = Logger.withTag("PublicSuffixListRepository")) {

    sealed interface Download {
        data class Fetched(val text: String, val etag: String?) : Download
        data object NotModified : Download
    }

    private class Cached(val list: PublicSuffixList, val checkedAt: Instant, val etag: String?)

    private val lock = Mutex()
    private var cached: Cached? = null
    private var cacheRead = false
    private var lastFailureAt: Instant? = null

    private val listFile get() = cacheDir / "public_suffix_list.dat"
    private val metaFile get() = cacheDir / "public_suffix_list.meta"

    /** The current list, refreshing it first if the last check is more than a day old. */
    suspend fun get(): PublicSuffixList = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!cacheRead) {
                cached = readCache()
                cacheRead = true
            }
            val current = cached
            val time = now()
            val isFresh = current != null && time - current.checkedAt < MAX_AGE
            val recentlyFailed = lastFailureAt?.let { time - it < RETRY_AFTER_FAILURE } == true
            if (!isFresh && !recentlyFailed) refresh(current, time)
            cached?.list ?: PublicSuffixList.Unavailable
        }
    }

    private suspend fun refresh(current: Cached?, time: Instant) {
        try {
            when (val result = withTimeout(DOWNLOAD_TIMEOUT) { download(current?.etag) }) {
                is Download.Fetched -> {
                    val list = PublicSuffixList.parse(result.text)
                    check(PublicSuffixList.looksComplete(result.text, list)) {
                        "Downloaded public suffix list looks incomplete"
                    }
                    fileSystem.createDirectories(cacheDir)
                    fileSystem.write(listFile) { writeUtf8(result.text) }
                    writeMeta(time, result.etag)
                    cached = Cached(list, time, result.etag)
                }
                Download.NotModified -> {
                    // Only sent with the cached copy's ETag, so there's a copy to keep.
                    val unchanged = checkNotNull(current) { "304 without a cached list" }
                    writeMeta(time, unchanged.etag)
                    cached = Cached(unchanged.list, time, unchanged.etag)
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

    private fun writeMeta(checkedAt: Instant, etag: String?) {
        fileSystem.write(metaFile) {
            writeUtf8(checkedAt.toEpochMilliseconds().toString())
            writeUtf8("\n")
            writeUtf8(etag.orEmpty())
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

    /** Starts the daily check right away, so the list is ready before a plugin needs it. */
    fun warmUp(scope: CoroutineScope) {
        scope.launch { get() }
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
                cacheDir = "${paths.getApplicationDataDirPath()}/public-suffix-list".toPath(),
                download = { etag ->
                    val response = httpClientFactory.default.get(LIST_URL) {
                        if (etag != null) header(HttpHeaders.IfNoneMatch, etag)
                    }
                    when (response.status) {
                        HttpStatusCode.NotModified -> Download.NotModified
                        HttpStatusCode.OK -> Download.Fetched(response.bodyAsText(), response.headers[HttpHeaders.ETag])
                        else -> error("HTTP ${response.status.value} from $LIST_URL")
                    }
                }).also { it.warmUp(CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
    }
}
