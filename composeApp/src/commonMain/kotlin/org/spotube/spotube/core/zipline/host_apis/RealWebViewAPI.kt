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
import org.spotube.spotube.core.webview.webViewCookiesWithoutDomainAreUrlScoped
import org.spotube.spotube.core.zipline.permissions.CookieDomainPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * The WebView host API one plugin sees. The WebView is shared, so every call that touches its
 * page, cookies or events works only while this plugin is the one that opened it. Cookie reads and
 * scripts are further limited to the plugin's declared `cookieDomains` ([cookiePolicy]).
 */
class RealWebViewAPI(
    private val scope: CoroutineScope,
    private val webViewController: WebViewController,
    private val pluginId: String,
    private val cookiePolicy: suspend () -> CookieDomainPolicy) : WebViewAPI {
    private val ownsWebView: Boolean get() = webViewController.currentPluginId == pluginId

    private fun requireOwner() {
        check(ownsWebView) { "The WebView is not open for this plugin" }
    }

    override fun navigateTo(url: String) {
        CookieDomainPolicy.requireNavigable(url)
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
        val host = cookiePolicy().requireAllowed(url)
        val cookies = withContext(Dispatchers.Main) {
            requireOwner()
            webViewController.getCookies(url)
        }
        // Don't trust every platform's cookie store to scope results to the URL.
        return cookies.filter {
            CookieDomainPolicy.cookieAppliesTo(it.domain, host, webViewCookiesWithoutDomainAreUrlScoped)
        }
    }

    override suspend fun evaluateJavaScript(script: String): String? {
        val policy = cookiePolicy()
        return withContext(Dispatchers.Main) {
            requireOwner()
            requirePageScriptable(policy)
            val result = withTimeoutOrNull(SCRIPT_TIMEOUT) {
                webViewController.evaluateJavascript(policy.guardScript(script))
            }
            // The in-page guard throws when the page moved to a site the plugin may not script
            // after the check above. Platforms report that as an empty result, or never call back
            // (hence the timeout). Ask the page again so the plugin gets the typed error.
            if (result.isNullOrBlank() || result == "null") requirePageScriptable(policy)
            result
        }
    }

    /** Asks the page itself (not the last reported URL, which can lag) whether it may be scripted. */
    private suspend fun requirePageScriptable(policy: CookieDomainPolicy) {
        val check = withTimeoutOrNull(SCRIPT_TIMEOUT) {
            webViewController.evaluateJavascript(policy.pageCheckScript())
        }
        policy.deniedResourceFrom(check)?.let { throw policy.scriptDenied(it) }
    }

    override fun urlChangeFlow(): Flow<String> {
        return webViewController.urlChangedFlow.filter { ownsWebView }
    }
    override fun webviewCreatedFlow(): Flow<Unit> {
        return webViewController.webviewCreatedFlow.filter { ownsWebView }
    }
    override fun postMessagesFlow(): Flow<String> {
        return webViewController.postMessagesFlow.filter { ownsWebView }
    }

    /** Closes the WebView if this plugin opened it; does nothing if it's already closed or not ours. */
    override fun exitWebView() {
        scope.launch(Dispatchers.Main) {
            if (ownsWebView) webViewController.closeWebview()
        }
    }

    private companion object {
        /** Scripts run synchronously, so a callback this late means the platform dropped it. */
        val SCRIPT_TIMEOUT = 15.seconds
    }
}
