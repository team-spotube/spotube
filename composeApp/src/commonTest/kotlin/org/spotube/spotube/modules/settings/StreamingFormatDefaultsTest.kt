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

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.spotube.PlatformType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class StreamingFormatDefaultsTest {

    private val opusWebm = AudioFormat(codec = "opus", container = "webm", qualities = emptyList())

    @Test
    fun `ios defaults to aac in mp4`() {
        val format = defaultStreamingMusicFormat(PlatformType.IOS)
        assertEquals("aac" to "mp4", format.codec to format.container)
    }

    @Test
    fun `other platforms keep opus in webm`() {
        listOf(PlatformType.Android, PlatformType.Linux, PlatformType.MacOS, PlatformType.Windows).forEach { platform ->
            val format = defaultStreamingMusicFormat(platform)
            assertEquals("opus" to "webm", format.codec to format.container, platform.name)
        }
    }

    @Test
    fun `saved opus webm is moved to aac on ios`() {
        val migrated = UserSettings(streamingMusicFormat = opusWebm).withPlayableStreamingFormat(PlatformType.IOS)
        assertEquals("mp4", migrated.streamingMusicFormat.container)
    }

    @Test
    fun `saved opus webm is kept off ios`() {
        val settings = UserSettings(streamingMusicFormat = opusWebm)
        assertSame(settings, settings.withPlayableStreamingFormat(PlatformType.Linux))
    }
}
