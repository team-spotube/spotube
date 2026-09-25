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

package dev.krtirtho.spotube.core.jam

import kotlin.random.Random

/**
 * Six-character room codes shared verbally / by text. Codes are opaque keys used
 * to namespace the MQTT topics of a jam room — they carry no connection details.
 *
 * The alphabet excludes look-alike characters (I, O, 0, 1) so codes are easy to
 * read aloud and retype.
 */
object JamRoomCode {
    const val LENGTH = 6
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) {
            append(ALPHABET[Random.nextInt(ALPHABET.length)])
        }
    }

    /** Uppercases, strips separators/whitespace and truncates to [LENGTH]. */
    fun normalize(input: String): String = input
        .uppercase()
        .filter { it.isLetterOrDigit() }
        .take(LENGTH)

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}