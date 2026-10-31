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

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * A TCP proxy in front of the room server that a test can cut, like a phone losing its
 * network: [goOffline] resets every open connection and refuses new ones until [goOnline].
 */
class TcpProxy(target: String) : AutoCloseable {
    private val targetHost: String
    private val targetPort: Int
    private val server = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress("127.0.0.1", 0))
    }
    private val sockets = CopyOnWriteArrayList<Socket>()

    @Volatile
    private var online = true

    init {
        val uri = URI(target)
        targetHost = uri.host
        targetPort = if (uri.port > 0) uri.port else 80
        thread(isDaemon = true, name = "tcp-proxy") {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                if (!online) {
                    reset(client)
                    continue
                }
                val upstream = runCatching { Socket(targetHost, targetPort) }.getOrElse {
                    reset(client)
                    continue
                }
                sockets += client
                sockets += upstream
                pipe(client, upstream)
                pipe(upstream, client)
            }
        }
    }

    val url: String get() = "http://127.0.0.1:${server.localPort}"

    fun goOffline() {
        online = false
        sockets.forEach(::reset)
        sockets.clear()
    }

    fun goOnline() {
        online = true
    }

    override fun close() {
        goOffline()
        server.close()
    }

    private fun pipe(from: Socket, to: Socket) = thread(isDaemon = true, name = "tcp-proxy-pipe") {
        runCatching { from.getInputStream().copyTo(to.getOutputStream()) }
        reset(from)
        reset(to)
    }

    /** Closes with a TCP reset, as an abrupt network loss looks to the other side. */
    private fun reset(socket: Socket) {
        runCatching {
            socket.setSoLinger(true, 0)
            socket.close()
        }
    }
}
