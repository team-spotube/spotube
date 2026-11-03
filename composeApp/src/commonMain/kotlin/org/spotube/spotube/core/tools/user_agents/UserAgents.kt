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

package org.spotube.spotube.core.tools.user_agents

import org.koin.core.component.inject
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.network.HttpClientFactory
import co.touchlab.kermit.Logger
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.getPlatform
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Clock

/**
 * Provides browser User-Agent strings that match the platform the app is running on
 * (Android UAs only on Android, iOS only on iOS, and desktop UAs only on the matching
 * Windows / macOS / Linux host).
 *
 * The primary source is the `top-user-agents` list maintained by Microlink (the 100 most
 * used UAs, kept current), cached on disk for [CACHE_TTL_MS]. The hardcoded [FALLBACKS]
 * are used whenever nothing has been fetched yet or the fetch fails.
 *
 * [random] is intentionally synchronous and never blocks on the network: it serves
 * whatever is cached (even stale) and refreshes in the background when needed.
 */
object UserAgents : KoinComponent {
    private val httpClientFactory: HttpClientFactory by inject()
    private const val SOURCE_URL =
        "https://cdn.jsdelivr.net/npm/top-user-agents@latest/src/index.json"
    private const val CACHE_FILE_NAME = "user_agents_cache.json"
    private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // 1 day

    private val logger = Logger.withTag("UserAgents")
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()

    private val platformType: PlatformType = getPlatform().type

    @Serializable
    private data class CacheEntry(val fetchedAtMs: Long, val agents: List<String>)
    private var remoteAgents: List<String> = emptyList()
    private var fetchedAtMs: Long = 0

    // Loads the disk cache once and kicks off a background refresh if it is missing/stale.
    private val bootstrap by lazy {
        loadFromDisk()
        if (isStale()) scope.launch { refresh() }
    }

    fun random(): String {
        bootstrap
        return pool().random()
    }

    /**
     * Fetches the latest list from the network and caches it. No-op while the cache is
     * still fresh unless [force] is set. Failures keep whatever data is already cached.
     */
    suspend fun refresh(force: Boolean = false) = refreshMutex.withLock {
        if (!force && !isStale()) return@withLock
        try {
            val agents = fetchRemote().filter(::isValidAgent)
            if (agents.isEmpty()) return@withLock
            remoteAgents = agents
            fetchedAtMs = now()
            saveToDisk(CacheEntry(fetchedAtMs, agents))
        } catch (e: Exception) {
            logger.w(e) { "Failed to refresh user agents, using cached/fallback ones" }
        }
    }

    private fun pool(): List<String> {
        val matching = remoteAgents.filter { matchesPlatform(it, platformType) }
        return matching.ifEmpty { FALLBACKS.getValue(platformType) }
    }

    private fun isStale() = now() - fetchedAtMs >= CACHE_TTL_MS

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private fun isValidAgent(ua: String) = ua.startsWith("Mozilla/")

    private suspend fun fetchRemote(): List<String> {
        val res = httpClientFactory.default.get(SOURCE_URL)
        if (!res.status.isSuccess()) error("Unexpected status ${res.status}")
        return json.decodeFromString(ListSerializer(String.serializer()), res.bodyAsText())
    }

    private fun cachePath() = (Paths().getApplicationCacheDirPath() + "/" + CACHE_FILE_NAME).toPath()

    private fun loadFromDisk() {
        try {
            val path = cachePath()
            val fs = FileSystem.SYSTEM
            if (!fs.exists(path)) return
            val entry = json.decodeFromString<CacheEntry>(fs.read(path) { readUtf8() })
            val agents = entry.agents.filter(::isValidAgent)
            if (agents.isEmpty()) return
            remoteAgents = agents
            fetchedAtMs = entry.fetchedAtMs
        } catch (e: Exception) {
            logger.w(e) { "Failed to read user agents cache" }
        }
    }

    private fun saveToDisk(entry: CacheEntry) {
        try {
            val path = cachePath()
            val tmp = "$path.tmp".toPath()
            val fs = FileSystem.SYSTEM
            path.parent?.let { fs.createDirectories(it) }
            fs.write(tmp) { writeUtf8(json.encodeToString(CacheEntry.serializer(), entry)) }
            fs.atomicMove(tmp, path)
        } catch (e: Exception) {
            logger.w(e) { "Failed to write user agents cache" }
        }
    }

    internal fun matchesPlatform(ua: String, type: PlatformType): Boolean {
        val isIos = "iPhone" in ua || "iPad" in ua || "iPod" in ua
        val isAndroid = "Android" in ua
        return when (type) {
            PlatformType.IOS -> isIos
            PlatformType.Android -> isAndroid
            PlatformType.Windows -> "Windows NT" in ua && !isAndroid
            PlatformType.MacOS -> "Macintosh" in ua && !isIos
            PlatformType.Linux -> ("X11" in ua || "Linux" in ua) && !isAndroid && "CrOS" !in ua
            // Unknown host: any desktop UA is a safe choice.
            PlatformType.Unknown -> !isIos && !isAndroid
        }
    }

    private val FALLBACKS: Map<PlatformType, List<String>> = mapOf(
        PlatformType.Windows to listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Vivaldi/7.6.3797.63",
        ),
        PlatformType.MacOS to listOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Safari/605.1.15",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Vivaldi/7.5.3735.47",
        ),
        PlatformType.Linux to listOf(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0",
        ),
        PlatformType.Android to listOf(
            "Mozilla/5.0 (Linux; Android 15; ALI-NX1 Build/HONORALI-N21) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.92 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 14; SM-A528B Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.108 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36",
        ),
        PlatformType.IOS to listOf(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/146.0.0.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPad; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
        ),
        PlatformType.Unknown to listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
        ),
    )
}
