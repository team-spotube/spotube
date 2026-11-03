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

package org.spotube.spotube.core.paths

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSDownloadsDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual class Paths {
    actual fun getApplicationCacheDirPath(): String {
        // Return ios cache dir
        val cacheDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null)

        return requireNotNull(cacheDirectory?.path)
    }

    actual fun getApplicationDataDirPath(): String {
        // Return ios document dir
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null)

        return requireNotNull(documentDirectory?.path)
    }

    actual fun getUserDownloadsDirPath(): String {
        val downloadsDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDownloadsDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null)

        return (downloadsDirectory?.path ?: getApplicationDataDirPath()) + "/Spotube"
    }

    actual fun getMusicCacheDirPath(): String {
        return getApplicationCacheDirPath() + "/music_cache"
    }
}