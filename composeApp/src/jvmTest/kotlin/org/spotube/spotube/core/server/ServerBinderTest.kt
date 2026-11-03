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


package org.spotube.spotube.core.server

import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ServerBinderTest {
    private val host = "127.0.0.1"

    private fun health(port: Int) =
        URI("http://$host:$port/health").toURL().readText()

    @Test
    fun usesPreferredPortWhenFree() = runBlocking {
        val free = ServerSocket(0, 1, InetAddress.getByName(host)).use { it.localPort }
        val bound = startServerWithPortFallback(host, free) {
            routing { get("/health") { call.respondText("ok") } }
        }
        try {
            assertEquals(free, bound.port)
            assertTrue(!bound.usedFallback)
            assertEquals("ok", health(bound.port))
        } finally {
            bound.engine.stop(0, 500)
        }
    }

    @Test
    fun fallsBackToAnotherPortWhenPreferredIsTaken() = runBlocking {
        ServerSocket(0, 1, InetAddress.getByName(host)).use { blocker ->
            val taken = blocker.localPort
            val failures = mutableListOf<Int>()
            val bound = startServerWithPortFallback(host, taken, { port, _ -> failures += port }) {
                routing { get("/health") { call.respondText("ok") } }
            }
            try {
                assertNotEquals(taken, bound.port)
                assertTrue(bound.usedFallback)
                assertEquals(listOf(taken), failures)
                assertEquals("ok", health(bound.port))
            } finally {
                bound.engine.stop(0, 500)
            }
        }
    }
}
