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

package dev.krtirtho.spotube.core.paths

import android.content.Context
import android.os.Environment

actual class Paths {
    private val context: Context
        get() = requireNotNull(appContext) { "Paths.init(context) must be called before use" }

    actual fun getApplicationCacheDirPath(): String {
        return context.cacheDir.absolutePath
    }

    actual fun getApplicationDataDirPath(): String {
        return context.filesDir.absolutePath
    }

    actual fun getUserDownloadsDirPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + java.io.File.separator + "Spotube"
    }

    actual fun getMusicCacheDirPath(): String {
        return context.cacheDir.absolutePath + "/music_cache"
    }

    companion object {
        @Volatile
        private var appContext: Context? = null

        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}