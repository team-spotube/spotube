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

class SpotrClientConfig(
    var defaultUrl: String = "",
    val defaultHeaders: MutableMap<String, String> = mutableMapOf(),
    val interceptors: MutableList<Interceptor> = mutableListOf(),
    var serializer: ContentSerializer? = null
) {
    fun defaultHeaders(block: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder()
        builder.block()
        defaultHeaders.putAll(builder.build())
    }

    fun interceptor(interceptor: Interceptor) {
        interceptors.add(interceptor)
    }

    fun interceptor(block: suspend (context: InterceptorContext) -> SpotrHttpResponse) {
        interceptors.add(object : Interceptor {
            override suspend fun intercept(context: InterceptorContext): SpotrHttpResponse = block(context)
        })
    }
}
