package dev.krtirtho.spotube.core.server

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioFormat
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioQuality
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioStream
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for the stream preference selection used by [StreamingUrlRepository].
 *
 * Ordering contract (best first):
 * 1. Fewest codec/container mismatches against the preferred format.
 * 2. Matching quality family (lossy vs lossless).
 * 3. Closest bitrate / sample rate.
 * 4. Closest channel count (lossless only).
 * 5. First occurrence wins remaining ties.
 */
class StreamingUrlRepositoryTest {

    // ---------- helpers ----------

    private fun opusWebm() = AudioFormat(codec = "opus", container = "webm", qualities = emptyList())

    private fun flac() = AudioFormat(codec = "flac", container = "flac", qualities = emptyList())

    private fun lossy(
        url: String,
        bitrate: Int,
        codec: String = "opus",
        container: String = "webm",
        protocol: StreamProtocol = StreamProtocol.PROGRESSIVE,
    ) = AudioStream.Lossy(
        url = url,
        codec = codec,
        container = container,
        protocol = protocol,
        bitrate = bitrate,
    )

    private fun lossless(
        url: String,
        sampleRate: Int,
        channels: Int,
        codec: String = "flac",
        container: String = "flac",
    ) = AudioStream.Lossless(
        url = url,
        codec = codec,
        container = container,
        sampleRate = sampleRate,
        channels = channels,
    )

    private fun select(
        streams: List<AudioStream>,
        format: AudioFormat = opusWebm(),
        quality: AudioQuality = AudioQuality.Lossy(256_000),
    ) = selectPreferredAudioStream(streams, format, quality)

    // ---------- empty / single ----------

    @Test
    fun `returns null when no streams are available`() {
        assertNull(select(emptyList()))
    }

    @Test
    fun `returns the only stream when exactly one is available`() {
        val only = lossy("opus-128", 128_000)
        assertSame(only, select(listOf(only)))
    }

    @Test
    fun `does not mutate the input list`() {
        val streams = mutableListOf(
            lossy("opus-44", 44_000),
            lossy("opus-256", 256_000),
        )
        val snapshot = streams.toList()

        select(streams)

        assertEquals(snapshot, streams)
    }

    // ---------- format priority ----------

    @Test
    fun `prefers an exact codec and container match over a closer quality in another format`() {
        val otherFormat = lossy("aac-256", 256_000, codec = "aac", container = "mp4")
        val preferredFormat = lossy("opus-128", 128_000)

        val result = select(
            streams = listOf(otherFormat, preferredFormat),
            quality = AudioQuality.Lossy(256_000),
        )

        assertSame(preferredFormat, result)
    }

    @Test
    fun `format matching ignores codec and container case`() {
        val exact = lossy("opus-128", 128_000, codec = "OpUs", container = "WeBm")
        val mismatch = lossy("aac-128", 128_000, codec = "aac", container = "mp4")

        val result = select(
            streams = listOf(mismatch, exact),
            format = AudioFormat(codec = "OPUS", container = "WEBM", qualities = emptyList()),
        )

        assertSame(exact, result)
    }

    @Test
    fun `a partial format match beats a full format mismatch regardless of bitrate`() {
        val partial = lossy("opus-mp4-320", 320_000, container = "mp4")
        val fullMismatch = lossy("aac-mp4-44", 44_000, codec = "aac", container = "mp4")

        val result = select(
            streams = listOf(fullMismatch, partial),
            quality = AudioQuality.Lossy(44_000),
        )

        assertSame(partial, result)
    }

    @Test
    fun `a one-field match beats a two-field mismatch`() {
        val oneField = lossy("opus-ogg", 128_000, container = "ogg")
        val twoFields = lossy("aac-mp4", 128_000, codec = "aac", container = "mp4")

        val result = select(streams = listOf(twoFields, oneField))

        assertSame(oneField, result)
    }

    @Test
    fun `codec-only and container-only matches tie on format and are resolved by quality`() {
        val codecOnly = lossy("opus-mp4-320", 320_000, container = "mp4")
        val containerOnly = lossy("aac-webm-44", 44_000, codec = "aac", container = "webm")

        val result = select(
            streams = listOf(codecOnly, containerOnly),
            quality = AudioQuality.Lossy(44_000),
        )

        assertSame(containerOnly, result)
    }

    @Test
    fun `when nothing matches the format the closest quality is chosen`() {
        val low = lossy("aac-128", 128_000, codec = "aac", container = "mp4")
        val target = lossy("aac-256", 256_000, codec = "aac", container = "mp4")
        val high = lossy("aac-320", 320_000, codec = "aac", container = "mp4")

        val result = select(
            streams = listOf(low, target, high),
            quality = AudioQuality.Lossy(256_000),
        )

        assertSame(target, result)
    }

    @Test
    fun `format match is prioritized over matching the quality family`() {
        // Same format but wrong family vs. wrong format but correct family.
        val sameFormatWrongFamily = lossless("opus-webm-flac", 48_000, 2, codec = "opus", container = "webm")
        val wrongFormatRightFamily = lossy("aac-44", 44_000, codec = "aac", container = "mp4")

        val result = select(
            streams = listOf(wrongFormatRightFamily, sameFormatWrongFamily),
            quality = AudioQuality.Lossy(44_000),
        )

        assertSame(sameFormatWrongFamily, result)
    }

    // ---------- lossy quality ----------

    @Test
    fun `exact lossy bitrate wins`() {
        val low = lossy("opus-44", 44_000)
        val exact = lossy("opus-128", 128_000)
        val high = lossy("opus-256", 256_000)

        val result = select(
            streams = listOf(low, exact, high),
            quality = AudioQuality.Lossy(128_000),
        )

        assertSame(exact, result)
    }

    @Test
    fun `closest lower lossy bitrate wins`() {
        val lower = lossy("opus-128", 128_000)
        val higher = lossy("opus-320", 320_000)

        val result = select(
            streams = listOf(higher, lower),
            quality = AudioQuality.Lossy(200_000),
        )

        assertSame(lower, result)
    }

    @Test
    fun `closest higher lossy bitrate wins`() {
        val lower = lossy("opus-44", 44_000)
        val higher = lossy("opus-128", 128_000)

        val result = select(
            streams = listOf(lower, higher),
            quality = AudioQuality.Lossy(100_000),
        )

        assertSame(higher, result)
    }

    @Test
    fun `equidistant lossy bitrates pick the first occurrence`() {
        val below = lossy("opus-96", 96_000)
        val above = lossy("opus-104", 104_000)

        assertSame(below, select(listOf(below, above), quality = AudioQuality.Lossy(100_000)))
        assertSame(above, select(listOf(above, below), quality = AudioQuality.Lossy(100_000)))
    }

    @Test
    fun `lossy preference prefers a lossy stream over lossless in the same format`() {
        val losslessStream = lossless("opus-webm-lossless", 48_000, 2, codec = "opus", container = "webm")
        val lossyStream = lossy("opus-256", 256_000)

        val result = select(
            streams = listOf(losslessStream, lossyStream),
            quality = AudioQuality.Lossy(128_000),
        )

        assertSame(lossyStream, result)
    }

    @Test
    fun `lossy distance does not overflow for extreme bitrates`() {
        val min = lossy("opus-min", Int.MIN_VALUE)
        val max = lossy("opus-max", Int.MAX_VALUE)

        val result = select(
            streams = listOf(min, max),
            quality = AudioQuality.Lossy(Int.MAX_VALUE),
        )

        assertSame(max, result)
    }

    // ---------- lossless quality ----------

    @Test
    fun `exact lossless sample rate and channels wins`() {
        val otherRate = lossless("flac-44", 44_100, 2)
        val mono = lossless("flac-48-mono", 48_000, 1)
        val exact = lossless("flac-48-stereo", 48_000, 2)

        val result = select(
            streams = listOf(otherRate, mono, exact),
            format = flac(),
            quality = AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        )

        assertSame(exact, result)
    }

    @Test
    fun `closest lossless sample rate wins`() {
        val near = lossless("flac-44", 44_100, 2)
        val far = lossless("flac-96", 96_000, 2)

        val result = select(
            streams = listOf(far, near),
            format = flac(),
            quality = AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        )

        assertSame(near, result)
    }

    @Test
    fun `lossless sample rate tie is broken by channel count`() {
        val mono = lossless("flac-44-mono", 44_100, 1)
        val stereo = lossless("flac-44-stereo", 44_100, 2)

        val result = select(
            streams = listOf(mono, stereo),
            format = flac(),
            quality = AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        )

        assertSame(stereo, result)
    }

    @Test
    fun `lossless preference prefers a lossless stream over lossy in the same format`() {
        val lossyStream = lossy("flac-flac-lossy", 128_000, codec = "flac", container = "flac")
        val losslessStream = lossless("flac-44", 44_100, 2)

        val result = select(
            streams = listOf(lossyStream, losslessStream),
            format = flac(),
            quality = AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        )

        assertSame(losslessStream, result)
    }

    // ---------- quality-family fallbacks ----------

    @Test
    fun `lossy preference with only lossless streams still returns the first stream`() {
        val first = lossless("flac-44", 44_100, 2)
        val second = lossless("flac-48", 48_000, 2)

        val result = select(
            streams = listOf(first, second),
            format = flac(),
            quality = AudioQuality.Lossy(256_000),
        )

        assertSame(first, result)
    }

    @Test
    fun `lossless preference with only lossy streams still returns the first stream`() {
        val first = lossy("opus-128", 128_000)
        val second = lossy("opus-320", 320_000)

        val result = select(
            streams = listOf(first, second),
            quality = AudioQuality.Lossless(sampleRate = 48_000, channels = 2),
        )

        assertSame(first, result)
    }

    // ---------- degenerate / orthogonal inputs ----------

    @Test
    fun `empty preferred codec and container fall back to quality`() {
        val low = lossy("opus-44", 44_000)
        val target = lossy("opus-256", 256_000)

        val result = select(
            streams = listOf(low, target),
            format = AudioFormat(codec = "", container = "", qualities = emptyList()),
            quality = AudioQuality.Lossy(256_000),
        )

        assertSame(target, result)
    }

    @Test
    fun `protocol does not influence selection`() {
        val progressive = lossy("a", 128_000, protocol = StreamProtocol.PROGRESSIVE)
        val dash = lossy("b", 128_000, protocol = StreamProtocol.DASH)
        val hls = lossy("c", 128_000, protocol = StreamProtocol.HLS)

        val result = select(streams = listOf(progressive, dash, hls))

        assertSame(progressive, result)
    }

    @Test
    fun `selection is stable across repeated calls`() {
        val streams = listOf(
            lossy("opus-44", 44_000),
            lossy("opus-128", 128_000),
            lossy("opus-256", 256_000),
        )

        val first = select(streams, quality = AudioQuality.Lossy(100_000))
        val second = select(streams, quality = AudioQuality.Lossy(100_000))

        assertSame(first, second)
    }

    // ---------- normalizeManifestUrl ----------

    @Test
    fun `absolute http urls are returned unchanged`() {
        val url = "http://example.com/audio.mp3"
        assertEquals(url, normalizeManifestUrl(url, StreamProtocol.PROGRESSIVE))
    }

    @Test
    fun `absolute https urls are returned unchanged for every protocol`() {
        val url = "https://example.com/manifest.m3u8"
        assertEquals(url, normalizeManifestUrl(url, StreamProtocol.HLS))
        assertEquals(url, normalizeManifestUrl(url, StreamProtocol.DASH))
        assertEquals(url, normalizeManifestUrl(url, StreamProtocol.PROGRESSIVE))
    }

    @Test
    fun `relative hls manifest paths are resolved against the youtube origin`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            normalizeManifestUrl("/watch?v=abc", StreamProtocol.HLS),
        )
    }

    @Test
    fun `relative dash manifest paths are resolved against the youtube origin`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc",
            normalizeManifestUrl("/watch?v=abc", StreamProtocol.DASH),
        )
    }

    @Test
    fun `relative progressive urls are returned unchanged`() {
        assertEquals(
            "/watch?v=abc",
            normalizeManifestUrl("/watch?v=abc", StreamProtocol.PROGRESSIVE),
        )
    }

    @Test
    fun `empty url is handled without throwing`() {
        assertEquals("", normalizeManifestUrl("", StreamProtocol.PROGRESSIVE))
        assertEquals("https://www.youtube.com", normalizeManifestUrl("", StreamProtocol.DASH))
        assertEquals("https://www.youtube.com", normalizeManifestUrl("", StreamProtocol.HLS))
    }
}
