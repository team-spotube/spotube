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

package org.spotube.spotube.core.newpipe

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.spotube.spotube.core.paths.Paths
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Path.Companion.toPath
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class NewPipeDownloader(private val cookieJar: PersistentCookieJar) : Downloader() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)

        headers.forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
            requestBuilder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body.string()
        val latestUrl = response.request.url.toString()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            latestUrl
        )
    }

    companion object {
        private var instance: NewPipeDownloader? = null

            /**
         * Safe to call on every Activity creation: the process (and so this singleton) outlives
         * the Activity, and a second [PersistentCookieJar] would open a second DataStore on the
         * same cookies file, which DataStore rejects.
         */
        @Synchronized
        fun init(paths: Paths) {
            if (instance != null) return
            val cookieJar = PersistentCookieJar(paths)
            instance = NewPipeDownloader(cookieJar).also { NewPipe.init(it) }
        }
    }
}

class PersistentCookieJar(private val paths: Paths) : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { "${paths.getApplicationDataDirPath()}/spotube_cookies.preferences_pb".toPath() }
    )

    init {
        loadCookies()
    }

    private fun loadCookies() {
        runBlocking {
            val prefs = dataStore.data.first()
            val json = prefs[stringPreferencesKey("newpipe_cookies")] ?: return@runBlocking
            try {
                val serialized: Map<String, List<String>> = Json.decodeFromString(json)
                serialized.forEach { (host, cookieStrings) ->
                    cookieStore[host] = cookieStrings.mapNotNull { s ->
                        runCatching {
                            Cookie.parse(
                                HttpUrl.Builder().scheme("https").host(host).build(), s
                            )
                        }.getOrNull()
                    }.toMutableList()
                }
            } catch (e: Exception) {
                cookieStore.clear()
            }
        }
    }

    private fun saveCookies() {
        runBlocking {
            dataStore.edit { prefs ->
                val serialized = cookieStore.mapValues { (_, cookies) ->
                    cookies.map { cookie ->
                        "${cookie.name}=${cookie.value}; domain=${cookie.domain}; path=${cookie.path}; ${if (cookie.secure) "Secure" else ""}; ${if (cookie.httpOnly) "HttpOnly" else ""}"
                    }
                }
                prefs[stringPreferencesKey("newpipe_cookies")] = Json.encodeToString(serialized)
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }

        for (newCookie in cookies) {
            val existingIndex =
                hostCookies.indexOfFirst { it.name == newCookie.name && it.path == newCookie.path }
            if (existingIndex >= 0) {
                hostCookies[existingIndex] = newCookie
            } else {
                hostCookies.add(newCookie)
            }
        }

        hostCookies.removeAll { it.expiresAt < System.currentTimeMillis() }

        saveCookies()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val hostCookies = cookieStore[host] ?: return emptyList()

        val validCookies = hostCookies.filter { cookie ->
            cookie.matches(url) && cookie.expiresAt >= System.currentTimeMillis()
        }

        return validCookies
    }
}
