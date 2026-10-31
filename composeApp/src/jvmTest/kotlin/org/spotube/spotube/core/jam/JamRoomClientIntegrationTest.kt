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

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.spotube.spotube.core.network.HttpClientFactory
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the real client against a room server. Skipped unless `JAM_TEST_URL` is set:
 * `cd jam-server && npm run dev`, then `JAM_TEST_URL=http://127.0.0.1:8787 ./gradlew :composeApp:jvmTest`.
 */
class JamRoomClientIntegrationTest {
    private val serverUrl: String? = System.getenv("JAM_TEST_URL")

    @Test
    fun hostAndGuestShareARoom() = runBlocking {
        val url = serverUrl ?: return@runBlocking
        val http = HttpClientFactory()
        val host = JamRoomClient(http)
        val guest = JamRoomClient(http)
        val code = JamRoomCode.generate()
        try {
            host.testConnection(url).getOrThrow()

            val hostWelcome = host.connect(url, code, JamRoomCode.newClientKey(), "Host", create = true).getOrThrow()
            val hostId = hostWelcome.you
            assertEquals(hostId, hostWelcome.host)
            // Same machine: the clock offset is just measurement noise.
            assertTrue(abs(host.clock.offsetMs!!) < 50, "offset ${host.clock.offsetMs}")

            val queue = JamQueue(listOf(JamMediaItem(url = "", trackId = "t1", title = "Song", artist = "A",
                album = "", durationMs = 1_000, coverUrl = "", protocol = "PROGRESSIVE")))
            host.sendQueue(queue)
            val anchor = JamAnchor("t1", 0, positionMs = 1_234, serverTimeMs = host.clock.serverTime(0), playing = true)
            host.sendAnchor(anchor)
            Thread.sleep(300)

            // Late joiners get the retained queue and anchor in the welcome.
            val guestKey = JamRoomCode.newClientKey()
            val guestWelcome = guest.connect(url, code, guestKey, "Guest", create = false).getOrThrow()
            val guestId = guestWelcome.you
            assertEquals(queue, guestWelcome.queue)
            assertEquals(anchor, guestWelcome.anchor)
            assertEquals(listOf(hostId, guestId), guestWelcome.participants.map { it.id })

            val received = async(start = CoroutineStart.UNDISPATCHED) { host.events.filterIsInstance<JamRoomEvent.Command>().first() }
            guest.sendCommand(JamCommand.SeekTo(42_000))
            val command = withTimeout(5_000) { received.await() }
            assertEquals(guestId, command.from)
            assertEquals(JamCommand.SeekTo(42_000), command.command)

            val closed = async(start = CoroutineStart.UNDISPATCHED) { guest.events.filterIsInstance<JamRoomEvent.Closed>().first() }
            host.kick(guestId, ban = false)
            assertEquals("kicked", withTimeout(5_000) { closed.await() }.code)

            val wrongRoom = guest.connect(url, JamRoomCode.generate(), guestKey, "Guest", create = false)
            assertEquals("room_not_found", (wrongRoom.exceptionOrNull() as JamRoomException).code)
        } finally {
            host.disconnect()
            guest.disconnect()
            http.close()
        }
    }
}
