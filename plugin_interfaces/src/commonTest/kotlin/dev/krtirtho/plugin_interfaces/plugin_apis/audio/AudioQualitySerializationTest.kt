package dev.krtirtho.plugin_interfaces.plugin_apis.audio

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioQualitySerializationTest {
    @Test
    fun `audio format serializes sealed quality variants`() {
        val formats = listOf(
            AudioFormat(
                codec = "opus",
                container = "webm",
                qualities = listOf(AudioQuality.Lossy(bitrate = 256_000)),
            ),
            AudioFormat(
                codec = "flac",
                container = "flac",
                qualities = listOf(AudioQuality.Lossless(sampleRate = 48_000, channels = 2)),
            ),
        )

        val encoded = Json.encodeToString(formats)
        val decoded = Json.decodeFromString<List<AudioFormat>>(encoded)

        assertEquals(formats, decoded)
    }
}
