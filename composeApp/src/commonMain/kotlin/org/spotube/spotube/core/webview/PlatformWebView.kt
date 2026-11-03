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

import dev.nucleusframework.webview.web.WebContent
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.cookie.CookieManager
import org.spotube.plugin_interfaces.host_apis.Cookie
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
class WebViewController(val navigationCommands: NavigationCommands): KoinComponent {
    private val logger by injectLogger<WebViewController>()
    private var cookieManager: CookieManager? = null
    private val urlFlow = MutableStateFlow("")
    private val webViewCreated = MutableSharedFlow<Unit>(replay = 1)
    var currentPluginId: String? = null
        private set

    suspend fun getCookies(url: String): List<Cookie> {
        if (cookieManager == null) {
            logger.w { "CookieManager is not initialized. Returning empty cookie list." }
            return emptyList()
        }
        val cookies = cookieManager!!.getCookies(url)
        val cookieList = mutableListOf<Cookie>()
        cookies.forEach {
            cookieList.add(
                Cookie(
                    name = it.name,
                    value = it.value,
                    domain = it.domain ?: "",
                    path = it.path,
                    expiresAt = it.expiresDate,
                    secure = it.isSecure ?: false,
                    httpOnly = it.isHttpOnly ?: false
                )
            )
        }
        return cookieList
    }

    private var content: String? = null
    private var isHtmlContent: Boolean = false
    fun getContent(): String? = content
    fun getWebContent(additionalHttpHeaders: Map<String, String> = emptyMap()): WebContent {
        if (content == null) throw IllegalStateException("Content is null. This should not happen as WebView should only be opened when content is set.")
        if (isHtmlContent) return WebContent.Data(data = content!!, mimeType = "text/html")
        return WebContent.Url(url = content!!, additionalHttpHeaders = additionalHttpHeaders)
    }

    var webViewNavigator: WebViewNavigator? = null

    fun emitUrlChange(url: String) {
        urlFlow.value = url
    }

    fun emitWebViewCreated() {
        webViewCreated.tryEmit(Unit)
    }

    fun setCookieManager(cookieManager: CookieManager) {
        this.cookieManager = cookieManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun closeWebview() {
        cookieManager = null
        content = null
        isHtmlContent = false
        webViewNavigator = null
        navigationCommands.pop(Routes.WebView)
        urlFlow.value = ""
        webViewCreated.resetReplayCache()
        _postMessagesFlow.resetReplayCache()
    }

    fun dispose() {
        cookieManager = null
        content = null
        isHtmlContent = false
        webViewNavigator = null
    }

    fun navigateTo(url: String, pluginId: String) {
        if (this.content != null) {
            throw IllegalStateException("WebView is already open. Please close the current WebView before navigating to a new URL.")
        }
        this.currentPluginId = pluginId
        this.content = url
        this.isHtmlContent = false
        navigationCommands.navigateTo(Routes.WebView)
    }

    fun navigateToHTML(html: String, pluginId: String) {
        if (this.content != null) {
            throw IllegalStateException("WebView is already open. Please close the current WebView before navigating to a new URL.")
        }
        this.currentPluginId = pluginId
        this.content = html
        this.isHtmlContent = true
        navigationCommands.navigateTo(Routes.WebView)
    }

    suspend fun evaluateJavascript(jsCode: String): String? {
        if (webViewNavigator == null) {
            throw IllegalStateException("WebView is not initialized. Cannot evaluate JavaScript.")
        }
        val completer = CompletableDeferred<String?>()
        try {
            webViewNavigator?.evaluateJavaScript(jsCode) { result ->
                completer.complete(result)
            }
        } catch (e: Exception) {
            completer.completeExceptionally(e)
            throw e
        }
        return completer.await()
    }

    suspend fun clearData(pluginId: String? = null) {
        cookieManager?.removeAllCookies()
        val targetPluginId = pluginId ?: currentPluginId
        platformClearWebviewData(targetPluginId)
        cookieManager = null
        content = null
        isHtmlContent = false
        webViewNavigator = null
        currentPluginId = null
    }

    val urlChangedFlow = urlFlow.asStateFlow()
    val webviewCreatedFlow = webViewCreated.asSharedFlow()
    private val _postMessagesFlow = MutableSharedFlow<String>(replay = 1)
    fun emitPostMessage(message: String) {
        _postMessagesFlow.tryEmit(message)
    }

    val postMessagesFlow = _postMessagesFlow.asSharedFlow()
}