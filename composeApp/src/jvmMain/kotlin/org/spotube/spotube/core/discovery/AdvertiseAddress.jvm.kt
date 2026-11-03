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

package org.spotube.spotube.core.discovery

import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

// Interface name prefixes for container/VM/VPN links other LAN devices can't reach.
private val VirtualInterfacePrefixes = listOf(
    "docker", "br-", "veth", "virbr", "vmnet", "vboxnet", "cni", "flannel", "podman",
    "lxc", "lxd", "tun", "tap", "wg", "zt", "tailscale", "utun", "ham")

internal actual fun preferredAdvertiseAddress(): String? =
    (routedLanAddress() ?: firstPhysicalLanAddress())?.hostAddress

/**
 * The source address the OS would use to reach the outside world, i.e. the interface that
 * owns the default route. A connected UDP socket sends nothing; it only resolves the route.
 */
private fun routedLanAddress(): InetAddress? = runCatching {
    DatagramSocket().use { socket ->
        socket.connect(InetAddress.getByName("192.0.2.1"), 9) // TEST-NET-1, never routed anywhere
        socket.localAddress
    }
}.getOrNull()?.takeIf { it is Inet4Address && it.isSiteLocalAddress }

/** Fallback for networks without a default route (e.g. an offline LAN). */
private fun firstPhysicalLanAddress(): InetAddress? = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { iface ->
            iface.isUp && !iface.isLoopback && !iface.isVirtual && !iface.isPointToPoint &&
                VirtualInterfacePrefixes.none { iface.name.startsWith(it) }
        }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
}.getOrNull()
