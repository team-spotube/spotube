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

package org.spotube.js_plugin_example.plugin_apis.lyrics

import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricType
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import org.spotube.plugin_interfaces.plugin_apis.lyrics.SyncLyricsLine

class RealLyricsAPI : LyricsAPI {
    override val supportedLyricTypes: List<LyricType> = listOf(LyricType.STATIC, LyricType.SYNCED)

    override suspend fun getStaticLyrics(trackId: String): String {
        return """
            Dummy lyrics for track $trackId
            
            Verse 1:
            This is a placeholder line
            Singing through the test design
            
            Chorus:
            La la la, plugin sample song
            Everything compiles all along
        """.trimIndent()
    }

    override suspend fun getSyncedLyrics(trackId: String): List<SyncLyricsLine> {
        return listOf(
            SyncLyricsLine(time = 0L, text = "[$trackId] Intro"),
            SyncLyricsLine(time = 10_000L, text = "This is a synced dummy lyric line"),
            SyncLyricsLine(time = 20_000L, text = "Another line appears right on time"),
            SyncLyricsLine(time = 30_000L, text = "Chorus: La la la, plugin sample song"),
            SyncLyricsLine(time = 40_000L, text = "Outro: End of demo lyrics")
        )
    }
}