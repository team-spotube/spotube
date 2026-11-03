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

package org.spotube.spotube.core.zipline

import app.cash.zipline.loader.ZiplineHttpClient
import org.spotube.spotube.core.di.injectLogger
import io.ktor.http.URLBuilder
import io.ktor.http.decodeURLQueryComponent
import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.core.component.KoinComponent

class FileSystemHTTPClient(private val baseDir: Path) : ZiplineHttpClient(), KoinComponent {
    private val logger by injectLogger<FileSystemHTTPClient>()
    private val okio = FileSystem.SYSTEM

    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        try {
            val parsedUrl = URLBuilder(url)
            var fullPath = parsedUrl.encodedParameters["path"]?.decodeURLQueryComponent()?.toPath()

            if (fullPath == null && parsedUrl.encodedPathSegments.isNotEmpty()) {
                fullPath = baseDir / parsedUrl.encodedPathSegments.last()
                logger.d { "Constructed full path from URL path: $fullPath" }
            }

            if (fullPath == null) {
                throw IllegalArgumentException("[FileSystemHTTPClient] Invalid URL: $url. Expected a 'path' query parameter or a simple path in the URL.")
            }

            logger.d { "Reading: $fullPath" }
            return okio.read(fullPath) {
                val str = readByteString()
                // Print the length of the content being read for debugging
                logger.d { "Read ${str.size / 1024.0} KB from $fullPath" }
                // Print the sha256 hash of the content for verification
                val hash = str.sha256().hex()
                logger.d { "SHA-256 hash of content: $hash" }
                str
            }
        } catch (e: Exception) {
            logger.e(e) { "Error reading file for URL: $url. Exception: ${e.message}" }
            throw e
        }
    }
}