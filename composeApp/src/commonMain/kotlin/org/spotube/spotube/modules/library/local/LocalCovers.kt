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

package org.spotube.spotube.modules.library.local

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Longest edge of a local playlist cover after downscaling. Picked images from galleries are
 * multi-megabyte photos and they end up inside the persisted JSON store, so — like the local
 * media index — they are shrunk to a small JPEG first (#3193).
 */
const val LOCAL_PLAYLIST_COVER_SIZE = 480

/** Downscale [bytes] to a small JPEG of at most [LOCAL_PLAYLIST_COVER_SIZE]px. Null when unreadable. */
expect fun encodeLocalCoverThumbnail(bytes: ByteArray): ByteArray?

/** Downscale the given base64 image and return it base64-encoded again; null when unreadable. */
fun reencodeLocalCoverBase64(base64: String): String? {
    val bytes = base64.decodeBase64() ?: return null
    val thumbnail = encodeLocalCoverThumbnail(bytes.toByteArray()) ?: return null
    return thumbnail.toByteString().base64()
}

/** Decode a stored playlist cover to raw bytes, or null when absent/unreadable. */
fun decodeLocalCoverBase64(base64: String?): ByteArray? =
    base64?.takeIf { it.isNotBlank() }?.decodeBase64()?.toByteArray()
