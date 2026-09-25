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

package dev.krtirtho.spotube.core.discovery

import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.NetService
import com.appstractive.dnssd.createNetService
import com.appstractive.dnssd.discoverServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.text.decodeToString

data class DiscoveredDevice(
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val deviceId: String,
) {
    val key: String get() = "$name$type".replace(".", "")
}

sealed interface DiscoveryState {
    data class Discovered(val device: DiscoveredDevice, val resolve: () -> Unit) : DiscoveryState
    data class Resolved(val device: DiscoveredDevice) : DiscoveryState
    data class Removed(val device: DiscoveredDevice) : DiscoveryState
}

class DeviceDiscoveryService {
    companion object {
        const val SERVICE_TYPE = "_spotube-ctrl._tcp"
        const val TXT_DEVICE_ID = "deviceId"
    }

    fun discover(): Flow<DiscoveryState> = discoverServices(SERVICE_TYPE).map { event ->
        when (event) {
            is DiscoveryEvent.Discovered -> {
                val device = DiscoveredDevice(
                    name = event.service.name,
                    type = event.service.type,
                    host = event.service.host,
                    port = event.service.port,
                    deviceId = event.service.txt[TXT_DEVICE_ID]?.decodeToString().orEmpty(),
                )
                DiscoveryState.Discovered(device = device, resolve = event.resolve)
            }

            is DiscoveryEvent.Resolved -> {
                val device = DiscoveredDevice(
                    name = event.service.name,
                    type = event.service.type,
                    host = event.service.host,
                    port = event.service.port,
                    deviceId = event.service.txt[TXT_DEVICE_ID]?.decodeToString().orEmpty(),
                )
                DiscoveryState.Resolved(device = device)
            }

            is DiscoveryEvent.Removed -> {
                val device = DiscoveredDevice(
                    name = event.service.name,
                    type = event.service.type,
                    host = event.service.host,
                    port = event.service.port,
                    deviceId = "",
                )
                DiscoveryState.Removed(device = device)
            }
        }
    }

    suspend fun advertise(
        name: String,
        port: Int,
        deviceId: String,
        registerTimeoutMs: Long = 5_000,
    ): NetService {
        val service = createService(name, port, deviceId)
        service.register(timeoutInMs = registerTimeoutMs)
        return service
    }

    fun createService(
        name: String,
        port: Int,
        deviceId: String,
    ): NetService = createNetService(
        type = SERVICE_TYPE,
        name = name,
        port = port,
        txt = mapOf(TXT_DEVICE_ID to deviceId),
    )
}