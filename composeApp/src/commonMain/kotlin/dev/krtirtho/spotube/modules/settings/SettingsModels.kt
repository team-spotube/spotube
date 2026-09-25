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

package dev.krtirtho.spotube.modules.settings

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioFormat
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioQuality
import kotlinx.serialization.Serializable

@Serializable
enum class Theme {
    LIGHT, DARK, SYSTEM
}

@Serializable
data class UserSettings(
    // Language and Region
    val language: SupportedLanguages = SupportedLanguages.EN,
    val country: CountryCode = CountryCode.BD,

    // Appearance
    val theme: Theme = Theme.SYSTEM,
    val accentColor: AccentColors = AccentColors.GREEN_GOBLIN,

    // Playback
    val streamingMusicFormat: AudioFormat = AudioFormat(
        codec = "opus",
        container = "webm",
        qualities = listOf(
            AudioQuality.Lossy(bitrate = 44_000),
            AudioQuality.Lossy(bitrate = 96_000),
            AudioQuality.Lossy(bitrate = 128_000),
            AudioQuality.Lossy(bitrate = 256_000),
        )
    ),
    val streamingMusicQuality: AudioQuality = AudioQuality.Lossy(bitrate = 256_000),
    val enableMusicCaching: Boolean = true,
    val cacheFolder: String? = null,
    val cacheSizeLimitMB: Long = -1L,
    val enableEndlessPlayback: Boolean = true,
    val enableConnect: Boolean = false,
    val playbackProxyServerPort: Int = 14769,

    // Remote Control (LAN)
    val allowRemoteControl: Boolean = false,
    val allowedRemoteDevices: List<String> = emptyList(),
    val remoteControlDeviceName: String = "",
    val remoteControlDeviceId: String = "",

    // Group Jam (MQTT)
    val jamParticipantName: String = "",
    val jamBroker: JamBroker = JamBroker(),
    val lastJamCode: String = "",

    // Downloads
    val overloadedDownloadFolder: String? = null, // When null, uses default music folder
    val localMediaFolders: List<String> = emptyList(),
    val downloadMusicFormat: AudioFormat = AudioFormat(
        codec = "aac",
        container = "mp4",
        qualities = listOf(
            AudioQuality.Lossy(bitrate = 44_000),
            AudioQuality.Lossy(bitrate = 96_000),
            AudioQuality.Lossy(bitrate = 128_000),
            AudioQuality.Lossy(bitrate = 256_000),
        )
    ),
    val downloadMusicQuality: AudioQuality = AudioQuality.Lossy(bitrate = 256_000),

    // Desktop
    val minimizeToTray: Boolean = false,
    val discordRichPresence: Boolean = true,

    // Updates
    val autoCheckForUpdates: Boolean = true,
)

/**
 * Configuration for the MQTT broker used by Group Jam. The host is a placeholder
 * until a real broker is configured; users can self-host and point the app at it.
 */
@Serializable
data class JamBroker(
    val name: String = "",
    val host: String = "test.mosquitto.org",
    val port: Int = 1883,
    val useTls: Boolean = false,
    val username: String? = null,
    val password: String? = null,
    val clientIdPrefix: String = "spotube",
    val keepAliveSeconds: Int = 30,
    val connectionTimeoutSeconds: Int = 10,
)
