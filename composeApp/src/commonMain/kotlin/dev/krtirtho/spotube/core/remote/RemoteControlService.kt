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

package dev.krtirtho.spotube.core.remote

import co.touchlab.kermit.Logger
import com.appstractive.dnssd.NetService
import dev.krtirtho.spotube.core.discovery.DeviceDiscoveryService
import dev.krtirtho.spotube.core.server.LocalServer
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Advertises this device on the local network via DNS-SD so that other Spotube
 * instances can discover and control it. Advertises only while the
 * "Allow remote control" setting is enabled and the local playback server is
 * listening on the LAN (0.0.0.0).
 *
 * Registration is retried with backoff: NsdManager is flaky right after a cold
 * start, and a single registration attempt is bounded by a short timeout so a
 * stalled platform callback can't wedge a dispatcher thread for long.
 */
class RemoteControlService(
    private val settingsRepository: SettingsRepository,
    private val discoveryService: DeviceDiscoveryService,
    private val localServer: LocalServer,
) {
    private val log = Logger.withTag("RemoteControlService")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _localDeviceId = MutableStateFlow("")
    val localDeviceId: StateFlow<String> = _localDeviceId.asStateFlow()

    private var advertisedService: NetService? = null

    /** A registration attempt that may have been left pending by the platform. */
    private var pendingService: NetService? = null

    private var registerJob: Job? = null
    private var cleanupJob: Job? = null

    init {
        // Ensure a stable device id exists and is persisted up front, so discovery
        // can reliably filter out this device's own advertisement.
        scope.launch {
            _localDeviceId.value = resolveDeviceId()
        }
        scope.launch {
            combine(
                settingsRepository.userSettings,
                localServer.port,
            ) { settings, port -> settings to port }
                .distinctUntilChanged()
                .collect { (settings, port) ->
                    if (settings.allowRemoteControl && port != null) {
                        if (registerJob?.isActive != true) {
                            registerJob = scope.launch {
                                registerLoop(settings.remoteControlDeviceName, port)
                            }
                        }
                    } else {
                        registerJob?.cancel()
                        registerJob = null
                        stopAdvertising()
                    }
                }
        }
    }

    /**
     * The service name this device advertises under, derived deterministically
     * from settings so discovery can match it against the local advertisement.
     */
    fun advertisedName(): String {
        val deviceId = _localDeviceId.value.ifBlank {
            settingsRepository.userSettings.value.remoteControlDeviceId
        }
        val configured = settingsRepository.userSettings.value.remoteControlDeviceName
        return configured.ifBlank { "Spotube-${deviceId.take(6)}" }
    }

    /**
     * Kicks off (or restarts) the advertising loop. Used when the local-network
     * permission is granted at runtime after earlier attempts failed.
     */
    fun retryAdvertising() {
        val settings = settingsRepository.userSettings.value
        val port = localServer.port.value
        if (!settings.allowRemoteControl || port == null) return
        registerJob?.cancel()
        // Clean up any in-flight registration the cancelled job may have leaked.
        scheduleCleanup(pendingService)
        pendingService = null
        registerJob = scope.launch {
            registerLoop(settings.remoteControlDeviceName, port)
        }
    }

    private suspend fun registerLoop(name: String, port: Int) {
        val deviceId = resolveDeviceId()
        _localDeviceId.value = deviceId
        val serviceName = name.ifBlank { "Spotube-${deviceId.take(6)}" }

        var attempt = 0
        while (advertisedService == null && coroutineContext.isActive) {
            attempt++
            // The user may have toggled the setting off during backoff.
            if (!settingsRepository.userSettings.value.allowRemoteControl) return
            val service = discoveryService.createService(
                name = serviceName,
                port = port,
                deviceId = deviceId,
            )
            pendingService = service
            try {
                service.register(timeoutInMs = REGISTER_TIMEOUT_MS)
                pendingService = null
                advertisedService = service
                log.i { "Advertising remote control service '$serviceName' on port $port (attempt $attempt)" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingService = null
                log.w(e) { "Failed to advertise remote control service (attempt $attempt); retrying in ${retryDelayMs(attempt)}ms" }
                // The library leaks the platform registration on timeout. Once the
                // platform eventually completes it (success), isRegistered flips
                // and unregister() will actually remove it — keep trying until then.
                scheduleCleanup(service)
                delay(retryDelayMs(attempt))
            }
        }
    }

    /**
     * Repeatedly tries to unregister a service whose registration attempt failed.
     * The library's `unregister()` is a no-op while the platform hasn't completed
     * the registration, so poll until it has (or give up after a while).
     */
    private fun scheduleCleanup(service: NetService?) {
        if (service == null) return
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            repeat(REGISTER_CLEANUP_TRIES) {
                delay(1_000)
                runCatching { service.unregister() }
            }
        }
    }

    private suspend fun stopAdvertising() {
        registerJob?.cancel()
        registerJob = null
        // Clean up any in-flight registration the cancelled job may have leaked.
        scheduleCleanup(pendingService)
        pendingService = null
        if (advertisedService != null) {
            runCatching { advertisedService?.unregister() }
            advertisedService = null
            log.i { "Stopped advertising remote control service" }
        }
    }

    private suspend fun resolveDeviceId(): String {
        val settings = settingsRepository.userSettings.first()
        if (settings.remoteControlDeviceId.isNotBlank()) {
            return settings.remoteControlDeviceId
        }
        val generated = buildString(16) {
            val chars = "0123456789abcdef"
            repeat(16) { append(chars[Random.nextInt(chars.length)]) }
        }
        settingsRepository.updateSettings(settings.copy(remoteControlDeviceId = generated))
        return generated
    }

    private fun retryDelayMs(attempt: Int): Long = when {
        attempt >= 6 -> 5 * 60_000L
        attempt >= 3 -> 30_000L
        else -> 5_000L
    }

    companion object {
        // Generous enough that the library's timeout (which leaks the platform
        // registration) rarely fires on a working system — registration callbacks
        // normally arrive within a second.
        private const val REGISTER_TIMEOUT_MS = 10_000L

        // How long to keep polling unregister() on a failed service, in seconds.
        private const val REGISTER_CLEANUP_TRIES = 15
    }
}