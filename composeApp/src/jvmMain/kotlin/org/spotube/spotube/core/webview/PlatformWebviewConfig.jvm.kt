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

package org.spotube.spotube.core.webview

import org.spotube.spotube.core.paths.Paths
import dev.nucleusframework.webview.web.WebViewState
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.context.GlobalContext

actual fun platformWebviewConfig(webView: WebViewState, pluginId: String?) {
    val paths = GlobalContext.get().get<Paths>()

    val baseDir = "${paths.getApplicationCacheDirPath()}/webview_data".toPath()
    val dataDir = if (pluginId != null) baseDir / pluginId else baseDir
    webView.webSettings.desktopWebSettings.dataDirectory = dataDir.toString()
}

actual suspend fun platformClearWebviewData(pluginId: String?) {
    if (pluginId == null) return
    val paths = GlobalContext.get().get<Paths>()
    val dataDirStr = "${paths.getApplicationCacheDirPath()}/webview_data/$pluginId"
    val dataDir = dataDirStr.toPath()
    if (FileSystem.SYSTEM.exists(dataDir)) {
        FileSystem.SYSTEM.deleteRecursively(dataDir)
    }
}