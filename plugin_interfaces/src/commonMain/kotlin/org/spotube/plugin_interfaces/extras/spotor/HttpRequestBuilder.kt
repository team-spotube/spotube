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

import org.spotube.plugin_interfaces.host_apis.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class HttpRequestBuilder {
    var method: HttpMethod = HttpMethod.Get
    var url: String = ""
    val headers: HeadersBuilder = HeadersBuilder()
    val parameters: ParametersBuilder = ParametersBuilder()
    var body: Any? = null
    var contentType: String? = null
    var contentSerializer: ContentSerializer? = null

    fun url(value: String) {
        this.url = value
    }

    fun url(block: URLBuilder.() -> Unit) {
        val builder = URLBuilder()
        builder.block()
        this.url = builder.build()
    }

    fun parameter(key: String, value: String) {
        parameters.append(key, value)
    }

    fun headers(block: HeadersBuilder.() -> Unit) {
        headers.block()
    }

    fun body(value: ByteArray) {
        this.body = value
    }

    inline fun <reified T : Any> jsonBody(value: T, serializer: ContentSerializer) {
        val ser = Json.serializersModule.serializer<T>()
        this.body = serializer.serialize(value, ser)
        this.contentType = serializer.contentType
    }

    inline fun <reified T : Any> jsonBody(value: T) {
        val s = contentSerializer ?: throw IllegalStateException("No ContentSerializer configured. Set it via SpotrClient config or pass serializer explicitly.")
        val ser = Json.serializersModule.serializer<T>()
        this.body = s.serialize(value, ser)
        this.contentType = s.contentType
    }

    fun contentType(value: String) {
        this.contentType = value
    }
}

class ParametersBuilder {
    private val params: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun append(key: String, value: String) {
        params.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun set(key: String, value: String) {
        params[key] = mutableListOf(value)
    }

    fun getAll(key: String): List<String>? = params[key]

    fun entries(): Map<String, List<String>> = params.toMap()

    fun isEmpty(): Boolean = params.isEmpty()

    fun build(): String {
        if (params.isEmpty()) return ""
        return params.entries.joinToString("&") { (key, values) ->
            values.joinToString("&") { "$key=$it" }
        }
    }
}
