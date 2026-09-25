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

package dev.krtirtho.spotube.core.deeplink

/**
 * Cross-platform receiver for URIs handed to the app by the operating system
 * (deep links). Platform entry points (Android activity intents, desktop command
 * line / open-URI handler, iOS `onOpenURL`) call [onNewUri]; the main composable
 * installs a [listener] once composition starts.
 *
 * Follows the Compose Multiplatform deep linking docs pattern: URIs arriving
 * before a listener is installed are cached and delivered as soon as one is set.
 */
object ExternalUriHandler {
    private var cached: String? = null

    var listener: ((uri: String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let(value::invoke)
                cached = null
            }
        }

    fun onNewUri(uri: String) {
        if (uri.isBlank()) return
        val currentListener = listener
        if (currentListener != null) {
            currentListener(uri)
        } else {
            cached = uri
        }
    }
}