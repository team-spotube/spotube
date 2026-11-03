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

import org.spotube.plugin_interfaces.host_apis.Cookie
import org.spotube.plugin_interfaces.host_apis.WebViewAPI
import org.spotube.spotube.core.webview.WebViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RealWebViewAPI(
    private val scope: CoroutineScope,
    private val webViewController: WebViewController,
    private val pluginId: String) : WebViewAPI {
    override fun navigateTo(url: String) {
        scope.launch(Dispatchers.Main) {
            webViewController.navigateTo(url, pluginId)
        }
    }

    override fun navigateToHTML(html: String) {
        scope.launch {
            webViewController.navigateToHTML(html, pluginId)
        }
    }

    override suspend fun getCookies(url: String): List<Cookie> {
        return withContext(Dispatchers.Main) {
            webViewController.getCookies(url)
        }
    }

    override suspend fun evaluateJavaScript(script: String): String? {
        return withContext(Dispatchers.Main) {
            webViewController.evaluateJavascript(script)
        }
    }

    override fun urlChangeFlow(): Flow<String> {
        return webViewController.urlChangedFlow
    }
    override fun webviewCreatedFlow(): Flow<Unit> {
        return webViewController.webviewCreatedFlow
    }
    override fun postMessagesFlow(): Flow<String> {
        return webViewController.postMessagesFlow
    }

    override fun exitWebView() {
        scope.launch(Dispatchers.Main) {
            webViewController.closeWebview()
        }
    }

}