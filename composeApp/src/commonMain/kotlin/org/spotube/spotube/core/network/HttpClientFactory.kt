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

package org.spotube.spotube.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The single place the app builds Ktor [HttpClient]s.
 *
 * It owns one client on the platform's default engine (Darwin on iOS, OkHttp on
 * Android/desktop), so every client shares that engine's connection pool and
 * no code path can accidentally pick an engine that doesn't work on a platform.
 *
 * - [default] and [json] are shared: use them directly and never close them.
 * - [create] derives a client with extra configuration (timeouts, WebSockets,
 *   default request, …). It reuses the shared engine; the caller owns the
 *   returned client and may close it without affecting anyone else.
 */
class HttpClientFactory : AutoCloseable {
    private val root = HttpClient()

    /** Plain client with no plugins installed. */
    val default: HttpClient get() = root

    /** Client that (de)serializes JSON bodies via [HttpJson]. */
    val json: HttpClient by lazy {
        create { install(ContentNegotiation) { json(HttpJson) } }
    }

    fun create(block: HttpClientConfig<*>.() -> Unit): HttpClient = root.config(block)

    override fun close() {
        root.close()
    }
}

/** Lenient JSON used for HTTP bodies: remote APIs add fields over time. */
val HttpJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
