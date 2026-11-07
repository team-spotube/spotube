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

package org.spotube.plugin_interfaces.host_apis

/** A permission the host checks per call, beyond the plugin's coarse capabilities. */
enum class PluginPermission(val key: String) {
    /**
     * Reading WebView cookies for a URL. Granted only for hosts matching the plugin's declared
     * `cookieDomains` in `plugin.json`.
     */
    WEBVIEW_COOKIES("webview.cookies"),

    /**
     * Running JavaScript in the WebView. Granted only on pages whose host matches the plugin's
     * `cookieDomains` (a page's script can read its cookies), and on the plugin's own HTML from
     * [WebViewAPI.navigateToHTML].
     */
    WEBVIEW_SCRIPT("webview.script");

    companion object {
        fun fromKey(key: String): PluginPermission? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Thrown by a host API when the plugin asks for something its `plugin.json` didn't declare,
 * e.g. [WebViewAPI.getCookies] for a host outside its `cookieDomains`.
 *
 * Zipline doesn't carry exception types across the bridge: inside a plugin this arrives as a
 * `ZiplineException` whose message embeds this one's. Use [asInsufficientPermission] to get the
 * typed exception back:
 *
 * ```
 * try {
 *     webView.getCookies("https://accounts.example.com")
 * } catch (e: Exception) {
 *     val denied = e.asInsufficientPermission() ?: throw e
 *     logger.e({ "Not allowed to read cookies for ${denied.resource}" })
 * }
 * ```
 */
class InsufficientPermissionException(
    val permission: PluginPermission,
    /** What was refused: the host of the URL or page, or the rejected URL if it has none. */
    val resource: String,
    detail: String? = null,
) : IllegalStateException(formatMessage(permission, resource, detail)) {
    companion object {
        private const val PREFIX = "Insufficient permission"

        // Matches the first line of the message even when it's embedded in a stack-trace string,
        // which is how it arrives on the plugin side of the Zipline bridge.
        private val MESSAGE_PATTERN = Regex("""$PREFIX '([a-z0-9._-]+)' for '([^'\r\n]*)'""")

        private fun formatMessage(permission: PluginPermission, resource: String, detail: String?): String {
            val safeResource = resource.replace(Regex("""['\r\n]"""), "")
            val base = "$PREFIX '${permission.key}' for '$safeResource'"
            return if (detail.isNullOrBlank()) base else "$base: $detail"
        }

        /**
         * Recovers the typed exception from [throwable], whether it is one already (host side) or a
         * bridged exception whose message carries one (plugin side). Returns `null` otherwise.
         */
        fun from(throwable: Throwable): InsufficientPermissionException? {
            var current: Throwable? = throwable
            while (current != null) {
                if (current is InsufficientPermissionException) return current
                val match = current.message?.let { MESSAGE_PATTERN.find(it) }
                if (match != null) {
                    val permission = PluginPermission.fromKey(match.groupValues[1]) ?: return null
                    return InsufficientPermissionException(permission, match.groupValues[2])
                }
                current = current.cause
            }
            return null
        }
    }
}

/** See [InsufficientPermissionException.from]. */
fun Throwable.asInsufficientPermission(): InsufficientPermissionException? =
    InsufficientPermissionException.from(this)
