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

package org.spotube.spotube.modules.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import org.spotube.spotube.core.discovery.DeviceDiscoveryService
import org.spotube.spotube.core.discovery.DiscoveredDevice
import org.spotube.spotube.core.discovery.DiscoveryState
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.ConnectionState
import org.spotube.spotube.core.remote.RemoteControlClient
import org.spotube.spotube.core.remote.RemoteControlService
import org.spotube.spotube.modules.settings.SettingsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.jetbrains.compose.resources.StringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.devices_error_connect_failed
import spotube.composeapp.generated.resources.devices_error_discovery_failed

/**
 * A user-facing error for the devices screen. [rawMessage] (an untranslated
 * cause reported by the transport) takes precedence when present; otherwise the
 * composable resolves [message] with [args].
 */
data class DevicesError(
    val message: StringResource,
    val args: List<String> = emptyList(),
    val rawMessage: String? = null)

data class DeviceItemUi(
    val key: String,
    /** Null when the device advertised a blank name. */
    val name: String?,
    /** `host:port`, or null while the address is still being resolved. */
    val address: String?,
    val isConnecting: Boolean,
    val isConnected: Boolean)

data class ConnectedDeviceUi(
    val name: String?,
    val address: String,
    val isDisconnecting: Boolean = false)

data class DevicesUiState(
    val devices: List<DeviceItemUi> = emptyList(),
    val isDiscovering: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedDevice: ConnectedDeviceUi? = null,
    val error: DevicesError? = null) {
    val showEmptyState: Boolean get() = devices.isEmpty() && !isConnecting
}

class DevicesViewModel(
    private val navigationCommands: NavigationCommands) : ViewModel(), KoinComponent {
    private val logger = Logger.withTag("DevicesViewModel")
    private val discoveryService: DeviceDiscoveryService by inject()
    private val remoteControlClient: RemoteControlClient by inject()
    private val remoteControlService: RemoteControlService by inject()
    private val settingsProvider: SettingsProvider by inject()

    private val _devices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    private val _isDiscovering = MutableStateFlow(false)
    private val _connectingToDevice = MutableStateFlow<DiscoveredDevice?>(null)
    private val _error = MutableStateFlow<DevicesError?>(null)
    private val _isDisconnecting = MutableStateFlow(false)

    val uiState: StateFlow<DevicesUiState> = combine(
        _devices,
        _isDiscovering,
        _connectingToDevice,
        remoteControlClient.connectionState,
        combine(_error, _isDisconnecting, ::Pair)
    ) { devices, isDiscovering, connectingTo, connection, (error, isDisconnecting) ->
        val connected = connection as? ConnectionState.Connected
        DevicesUiState(
            devices = devices.values.map { device ->
                DeviceItemUi(
                    key = device.key,
                    name = device.name.ifBlank { null },
                    address = device.address(),
                    isConnecting = connectingTo?.key == device.key,
                    isConnected = connected != null && device.matches(connected))
            },
            isDiscovering = isDiscovering,
            isConnecting = connectingTo != null,
            connectedDevice = connected?.let {
                ConnectedDeviceUi(
                    name = it.deviceName.ifBlank { null },
                    address = "${it.host}:${it.port}",
                    isDisconnecting = isDisconnecting)
            },
            error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    private var discoveryJob: Job? = null

    init {
        viewModelScope.launch {
            var previous: ConnectionState = remoteControlClient.connectionState.value
            remoteControlClient.connectionState.collect { state ->
                val wasConnecting = previous is ConnectionState.Connecting
                previous = state
                when (state) {
                    is ConnectionState.Error -> {
                        _error.value = DevicesError(message = state.fallback, rawMessage = state.message)
                        _connectingToDevice.value = null
                    }
                    // Only a drop *during* an attempt means it failed. Switching devices
                    // disconnects the old one first, and that must not cancel the new attempt.
                    is ConnectionState.Disconnected -> if (wasConnecting) _connectingToDevice.value = null
                    is ConnectionState.Connected -> {
                        // Only navigate for a connection this screen started. A connection
                        // that already existed when the screen opened must not bounce the
                        // user straight to the remote screen.
                        if (_connectingToDevice.value != null) {
                            _connectingToDevice.value = null
                            navigationCommands.navigateTo(Routes.RemoteControl)
                        }
                    }
                    is ConnectionState.Connecting -> {}
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
                            // Don't hold up the rest of the events while this device resolves.
                            launch { event.resolve() }
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
            } catch (e: CancellationException) {
                // stopDiscovery() (e.g. leaving the screen) is not a failure.
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Discovery failed" }
                _error.value = DevicesError(Res.string.devices_error_discovery_failed, listOf(e.message.toString()))
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

    fun onDeviceClick(key: String) {
        val device = _devices.value[key] ?: return
        val connected = remoteControlClient.connectionState.value as? ConnectionState.Connected
        when {
            // Already controlling it (e.g. came back from the remote screen): just reopen it.
            connected != null && device.matches(connected) -> openRemote()
            connected != null -> viewModelScope.launch {
                remoteControlClient.disconnect()
                connectToDevice(device)
            }
            else -> connectToDevice(device)
        }
    }

    fun openRemote() {
        navigationCommands.navigateTo(Routes.RemoteControl)
    }

    private fun connectToDevice(device: DiscoveredDevice) {
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
                    remoteDeviceName = device.name)
                // connect() returns once the socket is open, but the host may still be
                // asking its user for permission. The connection-state collector clears
                // the connecting flag once the outcome is known.
            } catch (e: Exception) {
                logger.e(e) { "Failed to connect to device" }
                _error.value = DevicesError(Res.string.devices_error_connect_failed, listOf(e.message.toString()))
                _connectingToDevice.value = null
            }
        }
    }

    fun disconnect() {
        if (_isDisconnecting.value) return
        _isDisconnecting.value = true
        viewModelScope.launch {
            try {
                remoteControlClient.disconnect()
            } finally {
                _isDisconnecting.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

private fun DiscoveredDevice.address(): String? =
    if (host.isNotBlank() && port > 0) "$host:$port" else null

private fun DiscoveredDevice.matches(connected: ConnectionState.Connected): Boolean =
    host == connected.host && port == connected.port
