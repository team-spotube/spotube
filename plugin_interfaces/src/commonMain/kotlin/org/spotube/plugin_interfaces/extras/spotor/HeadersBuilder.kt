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

package org.spotube.plugin_interfaces.extras.spotor

class HeadersBuilder {
    private val headers: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun append(key: String, value: String) {
        headers.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun set(key: String, value: String) {
        headers[key] = mutableListOf(value)
    }

    fun get(key: String): List<String>? = headers[key]

    fun contains(key: String): Boolean = headers.containsKey(key)

    fun entries(): Map<String, List<String>> = headers.toMap()

    fun clear() {
        headers.clear()
    }

    fun build(): Map<String, String> = headers.mapValues { it.value.joinToString(",") }
}
