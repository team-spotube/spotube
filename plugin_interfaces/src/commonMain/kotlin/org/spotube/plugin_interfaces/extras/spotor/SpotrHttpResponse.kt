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

import kotlinx.serialization.serializer

class SpotrHttpResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val rawBody: String,
    val defaultSerializer: ContentSerializer? = null
) {
    fun bodyAsText(): String = rawBody

    fun bodyAsBytes(): ByteArray = rawBody.encodeToByteArray()

    inline fun <reified T : Any> body(serializer: ContentSerializer? = null): T {
        val s = serializer ?: defaultSerializer ?: throw IllegalStateException("No serializer configured")
        val deserializer = kotlinx.serialization.json.Json.serializersModule.serializer<T>()
        return s.deserialize(rawBody, deserializer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SpotrHttpResponse
        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false
        if (!rawBody.contentEquals(other.rawBody)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + rawBody.hashCode()
        return result
    }
}

