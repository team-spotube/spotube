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

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named

// Interface name prefixes for container/VM/VPN links other LAN devices can't reach.
private val VirtualInterfacePrefixes = listOf(
    "docker", "br-", "veth", "virbr", "vmnet", "vboxnet", "cni", "flannel", "podman",
    "lxc", "lxd", "tun", "tap", "wg", "zt", "tailscale", "utun", "ham"
)

/**
 * Returns the preferred IPv4 address for mDNS advertisement on Android.
 * Tries to use the Wi-Fi interface IP address since mDNS typically works on LAN.
 */
internal actual fun preferredAdvertiseAddress(): String? = runCatching {
    val context = android.app.ActivityThread.currentApplication().applicationContext
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Get the active network
    val activeNetwork = connectivityManager.activeNetwork ?: return null
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null
    
    // Prefer Wi-Fi for mDNS (local network)
    if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        return null
    }
    
    // Get the link addresses for the active network
    val linkAddresses = connectivityManager.getLinkAddresses(activeNetwork)
    return linkAddresses.firstOrNull { address ->
        val inetAddress = address.address
        inetAddress is java.net.Inet4Address && inetAddress.isSiteLocalAddress
    }?.address?.hostAddress
}.getOrNull() ?: runCatching {
    // Fallback: try to get the first non-loopback, non-virtual interface address
    java.net.NetworkInterface.getNetworkInterfaces().toList()
        .filter { iface ->
            iface.isUp && !iface.isLoopback && !iface.isVirtual && !iface.isPointToPoint &&
                VirtualInterfacePrefixes.none { iface.name.startsWith(it) }
        }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is java.net.Inet4Address && it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()

// Interface name prefixes for container/VM/VPN links other LAN devices can't reach.
private val VirtualInterfacePrefixes = listOf(
    "docker", "br-", "veth", "virbr", "vmnet", "vboxnet", "cni", "flannel", "podman",
    "lxc", "lxd", "tun", "tap", "wg", "zt", "tailscale", "utun", "ham"
)