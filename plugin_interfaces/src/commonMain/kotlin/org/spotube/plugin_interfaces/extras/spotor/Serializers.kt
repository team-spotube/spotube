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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

interface ContentSerializer {
    val contentType: String
    fun serialize(value: Any, serializer: KSerializer<*>): String
    fun <T : Any> deserialize(text: String, deserializer: KSerializer<T>): T
}

class JsonContentSerializer(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ContentSerializer {
    override val contentType: String = "application/json"

    override fun serialize(value: Any, serializer: KSerializer<*>): String {
        @Suppress("UNCHECKED_CAST")
        return json.encodeToString(serializer as KSerializer<Any>, value)
    }

    override fun <T : Any> deserialize(text: String, deserializer: KSerializer<T>): T {
        return json.decodeFromString(deserializer, text)
    }
}
