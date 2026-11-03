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

package org.spotube.spotube.modules.webview

import androidx.compose.runtime.Composable
import org.spotube.spotube.core.di.rememberLogger
import org.spotube.spotube.core.webview.PlatformWebViewScreen
import org.spotube.spotube.core.webview.WebViewController

internal object WebViewScreen
@Composable
fun WebViewScreen(controller: WebViewController) {
    val logger = rememberLogger<WebViewScreen>()
    logger.d("WebViewScreen Opened")
    return PlatformWebViewScreen(webViewController = controller)
}