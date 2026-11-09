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

import okio.ByteString.Companion.encodeUtf8

/**
 * Stable traits of this device (model, OS version, locale, …) for jam bans. They never
 * leave the device as they are: [jamDeviceDigest] hashes them with the room code, and the
 * server hashes that again with the network, so a room only ever holds an opaque value.
 */
internal expect fun jamDeviceTraits(): List<String>

/** Per-room digest of [jamDeviceTraits]: unlinkable across rooms. */
internal fun jamDeviceDigest(roomCode: String): String? = runCatching {
    val traits = jamDeviceTraits().filter { it.isNotBlank() }
    if (traits.isEmpty()) return null
    "spotube-jam-device:$roomCode:${traits.joinToString("|")}".encodeUtf8().sha256().hex()
}.getOrNull()

/**
 * This install's participant secret for room [roomCode], derived from the install's
 * [deviceKey] (random, kept in settings). The same install gets the same identity in a
 * room every time, so a ban sticks across rejoins; different rooms get unrelated keys.
 */
internal fun jamParticipantKey(deviceKey: String, roomCode: String): String =
    "spotube-jam:$roomCode".encodeUtf8().hmacSha256(deviceKey.encodeUtf8()).hex()
