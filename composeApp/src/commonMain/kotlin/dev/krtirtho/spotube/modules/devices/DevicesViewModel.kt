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

package dev.krtirtho.spotube.modules.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.krtirtho.spotube.core.discovery.DeviceDiscoveryService
import dev.krtirtho.spotube.core.discovery.DiscoveredDevice
import dev.krtirtho.spotube.core.discovery.DiscoveryState
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.remote.ConnectionState
import dev.krtirtho.spotube.core.remote.RemoteControlClient
import dev.krtirtho.spotube.core.remote.RemoteControlService
import dev.krtirtho.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DevicesViewModel(
    private val navigationCommands: NavigationCommands,
) : ViewModel(), KoinComponent {
    private val logger = Logger.withTag("DevicesViewModel")
    private val discoveryService: DeviceDiscoveryService by inject()
    private val remoteControlClient: RemoteControlClient by inject()
    private val remoteControlService: RemoteControlService by inject()
    private val settingsProvider: SettingsProvider by inject()

    private val _devices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val devices: StateFlow<Map<String, DiscoveredDevice>> = _devices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectingToDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val connectingToDevice: StateFlow<DiscoveredDevice?> = _connectingToDevice.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        // Observe connection state from the client
        viewModelScope.launch {
            remoteControlClient.connectionState.collect { state ->
                _connectionState.value = state
                if (state is ConnectionState.Error) {
                    _error.value = state.message
                    _connectingToDevice.value = null
                } else if (state is ConnectionState.Disconnected) {
                    _connectingToDevice.value = null
                } else if (state is ConnectionState.Connected) {
                    // Navigate to remote control screen after successful connection
                    _connectingToDevice.value = null
                    navigationCommands.navigateTo(Routes.RemoteControl)
                }
            }
        }
        // Whenever the local device id is resolved, drop any of our own
        // advertisements that may have been picked up before we knew our id.
        viewModelScope.launch {
            remoteControlService.localDeviceId.collect { id ->
                if (id.isNotBlank()) {
                    removeSelf()
                }
            }
        }
    }

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        _isDiscovering.value = true
        _error.value = null
        logger.i { "Starting device discovery" }
        // Advertising may have failed before the local-network permission was
        // granted; give it another chance now that discovery is being used.
        remoteControlService.retryAdvertising()
        discoveryJob = viewModelScope.launch {
            try {
                discoveryService.discover().collect { event ->
                    logger.d { "Discovery event: $event" }
                    when (event) {
                        is DiscoveryState.Discovered -> {
                            event.resolve()
                            if (!isSelf(event.device)) {
                                _devices.update { it + (event.device.key to event.device.copy()) }
                            }
                        }
                        is DiscoveryState.Resolved -> {
                            if (isSelf(event.device)) {
                                // Resolved now carries our deviceId in TXT; drop self.
                                _devices.update { it - event.device.key }
                            } else {
                                _devices.update { it + (event.device.key to event.device) }
                            }
                        }
                        is DiscoveryState.Removed -> {
                            _devices.update { it - event.device.key }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Discovery failed" }
                _error.value = "Discovery failed: ${e.message}"
                _isDiscovering.value = false
            }
        }
    }

    /**
     * True when [device] is this device's own advertisement. On the initial
     * `Discovered` event dns-sd hasn't resolved the TXT record yet (deviceId is
     * empty), so we match by the name we advertise; once resolved we also have
     * the authoritative deviceId.
     */
    private fun isSelf(device: DiscoveredDevice): Boolean {
        val localId = remoteControlService.localDeviceId.value.ifBlank {
            settingsProvider.settingsState.value?.remoteControlDeviceId ?: ""
        }
        if (localId.isNotBlank() && device.deviceId == localId) return true
        // Match by the deterministic advertised name as a fallback for the
        // pre-resolution event where deviceId isn't available yet.
        return device.name.isNotBlank() && device.name == remoteControlService.advertisedName()
    }

    private fun removeSelf() {
        val localId = remoteControlService.localDeviceId.value
        if (localId.isBlank()) return
        _devices.update { map ->
            map.filterNot { (_, device) -> device.deviceId == localId }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _isDiscovering.value = false
    }

    fun connectToDevice(device: DiscoveredDevice) {
        if (_connectingToDevice.value != null) {
            logger.w { "Already connecting to a device" }
            return
        }
        if (isSelf(device)) {
            logger.w { "Refusing to connect to self: ${device.name}" }
            return
        }

        _connectingToDevice.value = device
        _error.value = null
        logger.i { "Connecting to device ${device.name} at ${device.host}:${device.port}" }

        viewModelScope.launch {
            try {
                val settings = settingsProvider.settingsState.value
                val deviceId = settings?.remoteControlDeviceId ?: ""
                val deviceName = settings?.remoteControlDeviceName?.ifBlank { "Spotube Controller" }
                    ?: "Spotube Controller"

                remoteControlClient.connect(
                    host = device.host,
                    port = device.port,
                    deviceId = deviceId,
                    deviceName = deviceName,
                )
                
                // Clear connecting state after connection attempt
                // The connectionState flow will show the actual connection status
                _connectingToDevice.value = null
            } catch (e: Exception) {
                logger.e(e) { "Failed to connect to device" }
                _error.value = "Failed to connect: ${e.message}"
                _connectingToDevice.value = null
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            remoteControlClient.disconnect()
        }
    }

    fun clearError() {
        _error.value = null
    }
}