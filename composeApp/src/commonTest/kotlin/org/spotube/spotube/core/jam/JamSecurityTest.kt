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

package org.spotube.spotube.core.jam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JamSecurityTest {
    private fun item(trackId: String = "t1", url: String = "", coverUrl: String = "https://i.example/c.jpg") =
        JamMediaItem(url = url, trackId = trackId, title = "Song", artist = "A", album = "B",
            durationMs = 1_000, coverUrl = coverUrl, protocol = "PROGRESSIVE")

    @Test
    fun peerItemsWithoutATrackIdAreDropped() {
        assertNull(item(trackId = "", url = "http://attacker.example/track.mp3").sanitized())
        assertNull(item(trackId = "", url = "file:///etc/passwd").sanitized())
    }

    @Test
    fun peerItemsLoseUrlsAndNonHttpsCovers() {
        val clean = item(url = "http://attacker.example/x", coverUrl = "http://attacker.example/pixel.png").sanitized()!!
        assertEquals("", clean.url)
        assertEquals("", clean.coverUrl)
        assertEquals("https://i.example/c.jpg", item().sanitized()!!.coverUrl)
    }

    @Test
    fun peerTextIsBounded() {
        val clean = item().copy(title = "x".repeat(10_000), durationMs = -5).sanitized()!!
        assertTrue(clean.title.length <= 300)
        assertEquals(0, clean.durationMs)
    }

    @Test
    fun localTrackKeysDontRevealPaths() {
        val key = JamMediaItem.localTrackKey("/home/alice/Music/song.mp3")
        assertFalse("alice" in key || "Music" in key)
        assertEquals(key, JamMediaItem.localTrackKey("/home/alice/Music/song.mp3"))
        assertNotEquals(key, JamMediaItem.localTrackKey("/home/alice/Music/other.mp3"))
    }

    @Test
    fun clientKeysAreLongRandomAndAcceptedByTheServer() {
        val keys = List(100) { JamRoomCode.newClientKey() }
        assertEquals(keys.size, keys.toSet().size)
        // Mirrors CLIENT_KEY in the jam-server repository's src/protocol.ts.
        val serverPattern = Regex("^[A-Za-z0-9_-]{22,64}$")
        assertTrue(keys.all { serverPattern.matches(it) })
    }

    @Test
    fun participantKeysAreStablePerRoomAndUnlinkableAcrossRooms() {
        val device = JamRoomCode.newClientKey()
        assertEquals(jamParticipantKey(device, "ABCDEF"), jamParticipantKey(device, "ABCDEF"))
        assertNotEquals(jamParticipantKey(device, "ABCDEF"), jamParticipantKey(device, "ABCDEG"))
        assertNotEquals(jamParticipantKey(device, "ABCDEF"), jamParticipantKey(JamRoomCode.newClientKey(), "ABCDEF"))
        assertTrue(Regex("^[A-Za-z0-9_-]{22,64}$").matches(jamParticipantKey(device, "ABCDEF")))
    }

    @Test
    fun deviceDigestsRevealNothingAndDifferPerRoom() {
        val digest = jamDeviceDigest("ABCDEF")!!
        assertEquals(digest, jamDeviceDigest("ABCDEF"))
        assertNotEquals(digest, jamDeviceDigest("ABCDEG"))
        assertTrue(jamDeviceTraits().none { trait -> trait.length > 3 && trait in digest })
        // Mirrors DEVICE_DIGEST in the jam-server repository's src/protocol.ts.
        assertTrue(Regex("^[A-Za-z0-9_-]{16,64}$").matches(digest))
    }

    @Test
    fun roomCodesAreValid() {
        repeat(200) { assertTrue(JamRoomCode.isValid(JamRoomCode.generate())) }
    }
}
