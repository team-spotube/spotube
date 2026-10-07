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

package dev.krtirtho.spotube.core.webview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.X
import dev.krtirtho.spotube.core.tools.user_agents.UserAgents
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.nucleusframework.webview.jsbridge.IJsMessageHandler
import dev.nucleusframework.webview.jsbridge.JsMessage
import dev.nucleusframework.webview.jsbridge.rememberWebViewJsBridge
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.WebViewNavigator
import dev.nucleusframework.webview.web.WebViewState
import dev.nucleusframework.webview.web.rememberWebViewNavigator

class PostMessageHandler(
    private val onMessageReceived: (String) -> Unit = {}
) : IJsMessageHandler {
    override fun methodName(): String {
        return "sendMessage"
    }

    override fun handle(
        message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit
    ) {
        onMessageReceived(message.params)
        callback(message.params)
    }
}

@Composable
fun PlatformWebViewScreen(webViewController: WebViewController) {
    if (webViewController.getContent() == null) {
        // This should never happen, but just in case
        Text("No URL to load")
        return
    }

    val state = remember {
        WebViewState(
            webViewController.getWebContent(
                additionalHttpHeaders = mapOf(
                    "User-Agent" to UserAgents.random()
                )
            )
        )
    }.apply {
        this.content = webViewController.getWebContent()
        platformWebviewConfig(this, webViewController.currentPluginId)
    }

    val navigator = rememberWebViewNavigator()
    val webViewBridge = rememberWebViewJsBridge(navigator)

    val bridgeBootstrapScript = remember {
        """
        (function() {
            if (typeof window.sendMessage !== "function") {
                window.sendMessage = function(message) {
                    if (typeof message !== "string") {
                        throw new TypeError("[window.sendMessage] Message must be a string");
                    }
                    window.kmpJsBridge.callNative("sendMessage", message);
                };
            }

            if (!window.bridgeReady) {
                const event = new CustomEvent("onBridgeReady");
                window.dispatchEvent(event);
                window.bridgeReady = true;
            }
        })();
        """.trimIndent()
    }

    LaunchedEffect(state) {
        snapshotFlow { state.lastLoadedUrl }.collect { url ->
            if (url != null) {
                webViewController.emitUrlChange(url)
                navigator.evaluateJavaScript(bridgeBootstrapScript)
                webViewController.emitWebViewCreated()
            }
        }
    }

    LaunchedEffect(state.cookieManager, navigator) {
        webViewController.setCookieManager(cookieManager = state.cookieManager)
        webViewController.webViewNavigator = navigator
    }

    LaunchedEffect(webViewBridge) {
        webViewBridge.register(PostMessageHandler { message ->
            webViewController.emitPostMessage(message)
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewController.dispose()
        }
    }

    BaseScaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            ApplicationMainBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.height(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    navigator.navigateBack()
                                }, enabled = navigator.canGoBack
                            ) {
                                Icon(
                                    FeatherIcons.ChevronLeft,
                                    contentDescription = "Go back to browser history"
                                )
                            }
                            IconButton(
                                onClick = {
                                    navigator.navigateForward()
                                }, enabled = navigator.canGoForward
                            ) {
                                Icon(
                                    FeatherIcons.ChevronRight,
                                    contentDescription = "Go forward to browser history"
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp).padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                        ) {
                            BasicTextField(
                                value = state.lastLoadedUrl ?: "",
                                onValueChange = {}, // Read-only
                                readOnly = true,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    .wrapContentHeight(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            )
        })
    { innerPadding ->
        WebView(
            state = state,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            navigator = navigator,
            webViewJsBridge = webViewBridge,
            onCreated = { webView ->
                navigator.evaluateJavaScript(bridgeBootstrapScript)
            },
        )
    }
}