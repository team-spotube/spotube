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

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/**
 * Mobile Safari's exact User-Agent on this device.
 *
 * WKWebView's default UA ends in `Mobile/15E148` without Safari's `Version/x … Safari/604.1`,
 * which sites like accounts.greedygreencorp.com treat as an embedded browser ("Oops, something went
 * wrong"). Hand-building the string isn't safe either: Safari freezes the OS part (iOS 26
 * reports `iPhone OS 18_7`). So WebKit builds it: a probe WKWebView gets Safari's application
 * name and reports `navigator.userAgent`, which is cached for every web view.
 */
internal object SafariUserAgent {
    var value: String? = null
        private set

    // Kept alive until its async evaluation calls back.
    private var probe: WKWebView? = null

    /** Must run on the main thread (WebKit); call once at startup. */
    @OptIn(ExperimentalForeignApi::class)
    fun prime() {
        if (value != null || probe != null) return
        val config = WKWebViewConfiguration().apply { applicationNameForUserAgent = safariApplicationName() }
        probe = WKWebView(frame = CGRectZero.readValue(), configuration = config).also { webView ->
            webView.evaluateJavaScript("navigator.userAgent") { result, _ ->
                value = result as? String
                probe = null
                Logger.withTag("SafariUserAgent").i { "WebView User-Agent: $value" }
            }
        }
    }

    // What Safari appends after WebKit's base UA; iPad Safari sends the desktop-class form.
    private fun safariApplicationName(): String {
        val version = UIDevice.currentDevice.systemVersion.split('.').take(2).joinToString(".")
        return if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
            "Version/$version Safari/605.1.15"
        } else {
            "Version/$version Mobile/15E148 Safari/604.1"
        }
    }
}
