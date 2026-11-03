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

class URLBuilder {
    var protocol: String = "https"
    var host: String = ""
    var port: Int? = null
    var encodedPath: String = ""
    val parameters: MutableMap<String, String> = mutableMapOf()

    fun takeFrom(url: String) {
        val withoutProtocol = if (url.contains("://")) {
            url.substringAfter("://")
        } else {
            url
        }

        val (hostPart, pathPart) = withoutProtocol.split('/', limit = 2).let {
            it[0] to (if (it.size > 1) "/${it[1]}" else "")
        }

        val (hostOnly, portStr) = hostPart.split(':', limit = 2).let {
            it[0] to (if (it.size > 1) it[1] else null)
        }

        host = hostOnly
        port = portStr?.toIntOrNull()

        if (pathPart.contains('?')) {
            val (path, query) = pathPart.split('?', limit = 2)
            encodedPath = path
            query.split('&').forEach { param ->
                val (key, value) = param.split('=', limit = 2).let {
                    it[0] to (if (it.size > 1) it[1] else "")
                }
                if (key.isNotEmpty()) parameters[key] = value
            }
        } else {
            encodedPath = pathPart
        }
    }

    fun parameter(key: String, value: String) {
        parameters[key] = value
    }

    fun build(): String {
        val baseUrl = buildString {
            append("$protocol://$host")
            port?.let { append(":$it") }
            append(encodedPath)
        }

        return if (parameters.isNotEmpty()) {
            val queryString = parameters.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$baseUrl?$queryString"
        } else {
            baseUrl
        }
    }
}
