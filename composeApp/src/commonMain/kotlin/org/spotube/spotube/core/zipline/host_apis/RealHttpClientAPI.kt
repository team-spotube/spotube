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

package org.spotube.spotube.core.zipline.host_apis

import org.koin.core.component.inject
import org.spotube.spotube.core.network.HttpClientFactory
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod
import org.spotube.plugin_interfaces.host_apis.HttpResponse
import org.spotube.spotube.core.di.injectLogger
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.takeFrom
import org.koin.core.component.KoinComponent

class RealHttpClientAPI : HttpClientAPI, KoinComponent {
    val logger by injectLogger<RealHttpClientAPI>()
    private val httpClientFactory: HttpClientFactory by inject()
    private val httpClient get() = httpClientFactory.default

    override suspend fun request(
        method: HttpMethod,
        url: String,
        requestHeaders: Map<String, String>?,
        body: String?
    ): HttpResponse {
        logger.i {
            buildString {
                append("[${method.name.uppercase()}] $url\n")
                append("Headers:\n")
                requestHeaders?.forEach { (key, value) ->
                    append("  $key: $value\n")
                }
                body?.let {
                    append("Body: $it\n")
                }
            }
        }
        val res = httpClient.request {
            this.method = when (method) {
                HttpMethod.Get -> io.ktor.http.HttpMethod.Get
                HttpMethod.Post -> io.ktor.http.HttpMethod.Post
                HttpMethod.Put -> io.ktor.http.HttpMethod.Put
                HttpMethod.Delete -> io.ktor.http.HttpMethod.Delete
                HttpMethod.Patch -> io.ktor.http.HttpMethod.Patch
                HttpMethod.Head -> io.ktor.http.HttpMethod.Head
                HttpMethod.Options -> io.ktor.http.HttpMethod.Options
            }
            this.url {
                takeFrom(url)
            }
            requestHeaders?.forEach { (key, value) ->
                headers.append(key, value)
            }
            body?.let { setBody(it) }
        }

        return HttpResponse(
            statusCode = res.status.value,
            headers = res.headers.entries().associate { it.key to it.value.joinToString(",") },
            body = res.bodyAsText()
        )
    }
}