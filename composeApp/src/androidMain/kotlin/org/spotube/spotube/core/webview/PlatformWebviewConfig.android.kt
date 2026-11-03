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

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import dev.nucleusframework.webview.web.WebViewState

actual fun platformWebviewConfig(webView: WebViewState, pluginId: String?) {
    val nativeWebView = webView.webView?.nativeWebView as? WebView ?: return
    nativeWebView.settings.domStorageEnabled = true
}

actual suspend fun platformClearWebviewData(pluginId: String?) {
    if (pluginId == null) return
    
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    WebStorage.getInstance().deleteAllData()
}