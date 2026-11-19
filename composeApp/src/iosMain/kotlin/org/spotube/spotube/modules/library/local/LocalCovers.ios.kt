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

/**
 * The local media index is a stub on iOS today, so there's no shared image pipeline to reuse.
 * Keep already-reasonable images as-is and refuse oversized ones instead of silently bloating
 * the persisted JSON store (see #3193).
 */
actual fun encodeLocalCoverThumbnail(bytes: ByteArray): ByteArray? =
    if (bytes.size <= MAX_PASSTHROUGH_COVER_BYTES) bytes else null

private const val MAX_PASSTHROUGH_COVER_BYTES = 512 * 1024
