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

package org.spotube.spotube.core.newpipe

import org.spotube.plugin_interfaces.host_apis.NewPipeSearchKind
import org.spotube.plugin_interfaces.host_apis.NewPipeVideoInfo
import org.spotube.plugin_interfaces.host_apis.NewPipeVideoSearchResult

typealias SearchKind = NewPipeSearchKind
typealias VideoSearchResult = NewPipeVideoSearchResult
typealias VideoInfo = NewPipeVideoInfo

/** Maps NewPipe's codec names onto the plugin's AudioFormat names (mp4a.40.2 → aac). */
internal fun normalizeCodec(codec: String?): String {
    val value = codec.orEmpty()
    return if (value.startsWith("mp4a", ignoreCase = true)) "aac" else value
}

/** Maps NewPipe's file suffixes onto the plugin's AudioFormat containers (m4a → mp4). */
internal fun normalizeContainer(suffix: String): String =
    if (suffix.equals("m4a", ignoreCase = true)) "mp4" else suffix
