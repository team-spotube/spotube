package org.spotube.spotube.artwork

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ArtworkCacheServiceTest {

    private val testConfig = ArtworkCacheConfig(
        allowedHosts = setOf(
            "coverartarchive.org",
            "i.scdn.co",
            "example.com"
        )
    )

    private val service = TestArtworkCacheService(testConfig)

    @Test
    fun `allows HTTPS URLs from allowed hosts`() = runTest {
        assertTrue(service.isValidArtworkUrl("https://coverartarchive.org/cover.jpg"))
        assertTrue(service.isValidArtworkUrl("https://i.scdn.co/image/abc"))
        assertTrue(service.isValidArtworkUrl("https://example.com/image.png"))
    }

    @Test
    fun `rejects HTTP URLs`() = runTest {
        assertFalse(service.isValidArtworkUrl("http://example.com/image.jpg"))
        assertFalse(service.isValidArtworkUrl("http://coverartarchive.org/image.jpg"))
    }

    @Test
    fun `rejects non-allowed HTTPS hosts`() = runTest {
        assertFalse(service.isValidArtworkUrl("https://evil.com/image.jpg"))
        assertFalse(service.isValidArtworkUrl("https://malicious-site.org/image.jpg"))
    }

    @Test
    fun `allows Android content URIs`() = runTest {
        assertTrue(service.isValidArtworkUrl("content://media/external/images/123"))
        assertTrue(service.isValidArtworkUrl("content://com.android.providers.media.documents/document/image%3A456"))
    }

    @Test
    fun `rejects empty or null URLs`() = runTest {
        assertFalse(service.isValidArtworkUrl(null))
        assertFalse(service.isValidArtworkUrl(""))
        assertFalse(service.isValidArtworkUrl("   "))
    }

    @Test
    fun `rejects URLs without scheme`() = runTest {
        assertFalse(service.isValidArtworkUrl("example.com/image.jpg"))
        assertFalse(service.isValidArtworkUrl("coverartarchive.org/image.jpg"))
    }

    @Test
    fun `rejects malformed URLs`() = runTest {
        assertFalse(service.isValidArtworkUrl("not-a-url"))
        assertFalse(service.isValidArtworkUrl("ftp://example.com/image.jpg"))
        assertFalse(service.isValidArtworkUrl("file:///path/to/image.jpg"))
    }
}

/**
 * Test implementation of ArtworkCacheService for unit testing.
 */
@ExperimentalCoroutinesApi
class TestArtworkCacheService(
    override val config: ArtworkCacheConfig
) : ArtworkCacheService {

    override fun isValidArtworkUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return try {
            val uri = java.net.URI.create(url)
            if (uri.scheme == "content") return true
            if (uri.scheme != "https") return false
            config.allowedHosts.contains(uri.host)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun preloadArtwork(url: String?) { }

    override suspend fun cleanupExpired() { }

    override suspend fun clearCache() { }
}