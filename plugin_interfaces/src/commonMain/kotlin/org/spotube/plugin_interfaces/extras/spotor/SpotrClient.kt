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

import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod

class SpotrClient(
    private val httpClientAPI: HttpClientAPI,
    private val configBlock: (SpotrClientConfig.() -> Unit)? = null
) {
    val config = SpotrClientConfig().apply { configBlock?.invoke(this) }

    var defaultUrl: String
        get() = config.defaultUrl
        set(value) {
            config.defaultUrl = value
        }

    val defaultHeaders: MutableMap<String, String> = config.defaultHeaders
    val interceptors: MutableList<Interceptor> = config.interceptors
    var serializer: ContentSerializer?
        get() = config.serializer
        set(value) {
            config.serializer = value
        }

    suspend fun request(
        method: HttpMethod,
        block: HttpRequestBuilder.() -> Unit = {}
    ): SpotrHttpResponse {
        val builder = HttpRequestBuilder().apply {
            this.method = method
            contentSerializer = config.serializer
            block()
            applyDefaults()
        }

        return executeWithInterceptors(builder)
    }

    suspend fun get(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Get, block)

    suspend fun post(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Post, block)

    suspend fun put(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Put, block)

    suspend fun delete(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Delete, block)

    suspend fun patch(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Patch, block)

    suspend fun head(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Head, block)

    suspend fun options(block: HttpRequestBuilder.() -> Unit = {}): SpotrHttpResponse =
        request(HttpMethod.Options, block)

    private fun HttpRequestBuilder.applyDefaults() {
        if (url.isEmpty() && config.defaultUrl.isNotEmpty()) {
            url = config.defaultUrl
        } else if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = config.defaultUrl.trimEnd('/') + "/" + url.trimStart('/')
        } else if (url.isNotEmpty() && config.defaultUrl.isNotEmpty()) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                val afterProtocol = url.substringAfter("://")
                val hostEndIndex = afterProtocol.indexOfAny(charArrayOf('/', '?'))
                val hostPart = if (hostEndIndex >= 0) afterProtocol.substring(0, hostEndIndex) else afterProtocol

                if (hostPart.isEmpty()) {
                    val remainder = if (hostEndIndex >= 0) afterProtocol.substring(hostEndIndex) else ""
                    url = if (remainder.isEmpty()) {
                        config.defaultUrl
                    } else {
                        config.defaultUrl.trimEnd('/') + remainder
                    }
                }
            }
        }

        if (!parameters.isEmpty()) {
            val queryString = parameters.build()
            url = if (url.contains('?')) "$url&$queryString" else "$url?$queryString"
        }

        config.defaultHeaders.forEach { (key, value) ->
            if (!headers.contains(key)) {
                headers.append(key, value)
            }
        }

        if (contentType != null && !headers.contains("Content-Type")) {
            headers.set("Content-Type", contentType!!)
        }
    }

    private suspend fun executeWithInterceptors(builder: HttpRequestBuilder): SpotrHttpResponse {
        val execute: suspend (HttpRequestBuilder) -> SpotrHttpResponse = { req ->
            val bodyString: String? = when (val b = req.body) {
                is String -> b
                is ByteArray -> b.decodeToString()
                null -> null
                else -> throw IllegalStateException("Body must be String or a ByteArray. Use jsonBody() for automatic serialization.")
            }

            val rawResponse = httpClientAPI.request(
                method = req.method,
                url = req.url,
                requestHeaders = req.headers.build(),
                body = bodyString
            )

            SpotrHttpResponse(
                statusCode = rawResponse.statusCode,
                headers = rawResponse.headers.mapValues { listOf(it.value) },
                rawBody = rawResponse.body ?: "",
                defaultSerializer = config.serializer
            )
        }

        var current: suspend (HttpRequestBuilder) -> SpotrHttpResponse = execute

        for (i in config.interceptors.indices.reversed()) {
            val interceptor = config.interceptors[i]
            val next = current
            current = { req ->
                interceptor.intercept(
                    InterceptorContext(req) { next(it) }
                )
            }
        }

        return current(builder)
    }
}
