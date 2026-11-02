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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_name_adjectives
import spotube.composeapp.generated.resources.jam_name_animals
import spotube.composeapp.generated.resources.jam_name_format

/** Friendly default names for jam participants ("Mellow Otter"), like npm or Docker use. */
object JamNames {
    /** The server's limit (`MAX_NAME_LENGTH` in the jam-server repository's `src/protocol.ts`). */
    const val MAX_LENGTH = 40

    /**
     * The generated name for [seed]. The same seed always gets the same name, so with this
     * install's key a device keeps its name across sessions until the user picks their own.
     */
    suspend fun generate(seed: String): String {
        val adjectives = getStringArray(Res.array.jam_name_adjectives)
        val animals = getStringArray(Res.array.jam_name_animals)
        val hash = "spotube-jam-name:$seed".encodeUtf8().sha256()
        val adjective = adjectives[pick(hash[0], hash[1], adjectives.size)]
        val animal = animals[pick(hash[2], hash[3], animals.size)]
        return getString(Res.string.jam_name_format, adjective, animal).take(MAX_LENGTH)
    }

    /** Cleans a name the user typed; blank means "use the generated one". */
    fun normalize(input: String): String =
        input.filterNot { it.isISOControl() }.replace(Regex("\\s+"), " ").trim()

    private fun pick(high: Byte, low: Byte, size: Int): Int =
        (((high.toInt() and 0xFF) shl 8) or (low.toInt() and 0xFF)) % size
}
