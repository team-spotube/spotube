package org.spotube.spotube.artwork

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable

/**
 * Configuration for ArtworkCacheService.
 */
@Serializable
data class ArtworkCacheConfig(
    /** Maximum total disk cache size in bytes (default: 200 MB). */
    val maxDiskCacheBytes: Long = 200 * 1024 * 1024,

    /** Maximum download size per image in bytes (default: 20 MB). */
    val maxDownloadBytes: Long = 20 * 1024 * 1024,

    /** Time-to-live for cached entries (default: 7 days). */
    val ttl: kotlin.time.Duration = kotlin.time.Duration.days(7),

    /** Allowed HTTPS hosts for artwork URLs (security allowlist). */
    val allowedHosts: Set<String> = DEFAULT_ALLOWED_HOSTS
)

/**
 * Default allowed hosts for artwork - matches DuranMusic's allowlist.
 * Only these hosts are accepted to prevent SSRF and untrusted image loads.
 */
val DEFAULT_ALLOWED_HOSTS = setOf(
    "coverartarchive.org",
    "archive.org",
    "i.scdn.co",
    "m.media-amazon.com",
    "lastfm.freetls.fastly.net",
    "upload.wikimedia.org",
    "is1-ssl.mzstatic.com",
    "is2-ssl.mzstatic.com",
    "is3-ssl.mzstatic.com",
    "is4-ssl.mzstatic.com",
    "is5-ssl.mzstatic.com"
)

/**
 * Interface for artwork cache service.
 * Provides centralized album artwork loading with:
 * - HTTPS host validation (security)
 * - Disk/memory size limits (performance)
 * - Configurable TTL (freshness)
 * - Graceful fallback to placeholder (UX)
 *
 * Based on DuranMusic's ArtworkCacheService in `artwork_cache_service.dart`.
 */
@ExperimentalCoroutinesApi
interface ArtworkCacheService {

    /** Validates if an artwork URL is safe (HTTPS + allowed host). */
    fun isValidArtworkUrl(url: String?): Boolean

    /** Configuration for this cache service. */
    val config: ArtworkCacheConfig

    /**
     * Preloads an artwork URL into cache.
     * Useful for prefetching next items in a list.
     */
    suspend fun preloadArtwork(url: String?)

    /**
     * Clears expired entries from cache (based on TTL).
     */
    suspend fun cleanupExpired()

    /**
     * Clears all cache entries.
     */
    suspend fun clearCache()
}

/**
 * Extension functions for easy artwork loading from metadata models.
 */
@ExperimentalCoroutinesApi
fun SpotubeSimpleAlbum.artworkUrl(): String? {
    return images.firstOrNull()?.url
}

@ExperimentalCoroutinesApi
fun SpotubeFullTrack.artworkUrl(): String? {
    return album.images.firstOrNull()?.url
}

@ExperimentalCoroutinesApi
fun SpotubePlaylist.artworkUrl(): String? {
    return images.firstOrNull()?.url
}