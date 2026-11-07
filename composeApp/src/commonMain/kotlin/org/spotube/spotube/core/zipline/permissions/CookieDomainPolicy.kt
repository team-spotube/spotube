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

package org.spotube.spotube.core.zipline.permissions

import io.ktor.http.Url
import kotlinx.serialization.json.Json
import org.spotube.plugin_interfaces.host_apis.InsufficientPermissionException
import org.spotube.plugin_interfaces.host_apis.PluginPermission

/**
 * One entry of a plugin's `cookieDomains`:
 *  - `example.com` grants exactly that host.
 *  - `*.example.com` grants every subdomain at any depth, but not `example.com` itself (declare
 *    both if both are needed). The part after `*.` can't be a public suffix (see
 *    [PublicSuffixList]), so `*.com`, `*.co.uk` and `*.github.io` are rejected.
 *
 * Hosts are ASCII (punycode for IDNs), compared case-insensitively, without scheme, port or path.
 */
sealed interface CookieDomainPattern {
    val declared: String

    fun matches(host: String): Boolean

    data class Exact(val host: String) : CookieDomainPattern {
        override val declared get() = host
        override fun matches(host: String) = host == this.host
    }

    data class Subdomains(val parent: String) : CookieDomainPattern {
        override val declared get() = "*.$parent"
        override fun matches(host: String) = host.endsWith(".$parent")
    }

    companion object {
        private val LABEL = Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$")

        /** The canonical spelling of a declared entry: trimmed, lowercase, no trailing dot. */
        fun normalize(raw: String): String = raw.trim().lowercase().removeSuffix(".")

        /** Parses one declared entry, or returns `null` if it isn't a valid pattern. */
        fun parse(raw: String, publicSuffixes: PublicSuffixList): CookieDomainPattern? {
            val value = normalize(raw)
            if (value.startsWith("*.")) {
                val parent = value.removePrefix("*.")
                val valid = isHostname(parent) && '.' in parent && !publicSuffixes.isPublicSuffix(parent)
                return if (valid) Subdomains(parent) else null
            }
            return if (isHostname(value)) Exact(value) else null
        }

        private fun isHostname(value: String): Boolean =
            value.isNotEmpty() && value.length <= 253 && value.split('.').all { LABEL.matches(it) }
    }
}

/**
 * Decides which WebView cookies a plugin may read, from the `cookieDomains` it declared in
 * `plugin.json`. Construct it with already-validated entries (see [PluginPermissionValidator]);
 * anything unparseable is ignored here, which only ever narrows access.
 *
 * Covers every way a plugin can reach cookies: [requireAllowed] for `getCookies`, and
 * [pageCheckScript] plus [guardScript] for `evaluateJavaScript`, since a page's script can read
 * `document.cookie`.
 */
class CookieDomainPolicy(declaredDomains: List<String>, publicSuffixes: PublicSuffixList) {
    val patterns: List<CookieDomainPattern> =
        declaredDomains.mapNotNull { CookieDomainPattern.parse(it, publicSuffixes) }

    /**
     * Returns the normalized host of [url] if the plugin may read its cookies.
     *
     * @throws InsufficientPermissionException if [url] isn't an http(s) URL with a declared host.
     */
    fun requireAllowed(url: String): String {
        val host = hostOf(url) ?: throw InsufficientPermissionException(
            PluginPermission.WEBVIEW_COOKIES,
            resource = url.take(200),
            detail = "cookies can only be read for absolute http(s) URLs")
        if (patterns.none { it.matches(host) }) {
            throw InsufficientPermissionException(
                PluginPermission.WEBVIEW_COOKIES,
                resource = host,
                detail = if (patterns.isEmpty()) {
                    "the plugin declares no cookieDomains in plugin.json"
                } else {
                    "not covered by the plugin's cookieDomains ${patterns.map { it.declared }}"
                })
        }
        return host
    }

    fun allowsHost(host: String): Boolean = patterns.any { it.matches(host) }

    /**
     * Prefixes [script] with a statement that throws, before any of [script] runs, unless the
     * document it runs in is one the plugin may script: an http(s) page on a declared host, or a
     * document with an opaque origin (the plugin's own HTML). Checking inside the page closes the
     * race where the page navigates between the host's check and the script running.
     *
     * Why the plugin's code can't get around it, although it shares the program:
     *  - The guard is a complete `if (...) { throw }` statement. Whatever follows can only add an
     *    `else`, which runs only when access was granted; it can't reach back into the condition.
     *  - Function declarations in [script] are hoisted above the guard, so the guard only touches
     *    its own locals plus `location` and `document`. Those are non-configurable on `window`, so
     *    redeclaring them fails the whole program before anything runs. Prototype methods can't
     *    be replaced by declarations alone.
     *  - `document.domain` is the origin's host ("" when opaque), so a `blob:` URL or an `about:blank`
     *    inheriting a site's origin is judged by that site.
     */
    fun guardScript(script: String): String =
        "if (" + pageCheckFunction() + "() !== null) { throw \"" + SCRIPT_DENIED + "\"; }\n" + script

    /**
     * A script on its own that evaluates to [SCRIPT_ALLOWED_MARKER] if [guardScript] would let a
     * script run on the current page, or [SCRIPT_DENIED_MARKER] followed by the refused host
     * otherwise. Platforms report a thrown error differently (or never), so the host asks the page
     * with this instead of reading meaning into a failed [guardScript] run.
     */
    fun pageCheckScript(): String =
        "(function () { var denied = " + pageCheckFunction() + "();" +
            "return denied === null ? \"" + SCRIPT_ALLOWED_MARKER + "\" : \"" + SCRIPT_DENIED_MARKER + "\" + denied; })()"

    /**
     * A JS function expression returning `null` when the current document may be scripted, or a
     * description of it (its host, or its scheme) when not.
     */
    private fun pageCheckFunction(): String {
        val exact = patterns.filterIsInstance<CookieDomainPattern.Exact>().map { it.host }
        val parents = patterns.filterIsInstance<CookieDomainPattern.Subdomains>().map { ".${it.parent}" }
        return buildString {
            append("(function () {")
            append("var exact = ").append(Json.encodeToString(exact)).append(";")
            append("var parents = ").append(Json.encodeToString(parents)).append(";")
            append("function ok(h) {")
            append("if (typeof h !== \"string\") return false;")
            append("if (h.charAt(h.length - 1) === \".\") h = h.slice(0, -1);")
            append("if (h === \"\") return false;")
            append("for (var i = 0; i < exact.length; i++) if (h === exact[i]) return true;")
            append("for (var j = 0; j < parents.length; j++) {")
            append("var p = parents[j];")
            append("if (h.length > p.length && h.slice(h.length - p.length) === p) return true;")
            append("}")
            append("return false;")
            append("}")
            append("var protocol = location.protocol;")
            append("if (protocol === \"http:\" || protocol === \"https:\") {")
            append("return ok(location.hostname) ? null : String(location.hostname);")
            append("}")
            append("var domain = document.domain;")
            append("return domain === \"\" || ok(domain) ? null : String(domain || protocol);")
            append("})")
        }
    }

    /**
     * Reads the result of [pageCheckScript]: `null` if the page may be scripted, otherwise the
     * refused host. A result the page didn't produce (no marker at all) counts as refused.
     */
    fun deniedResourceFrom(pageCheckResult: String?): String? {
        // Android hands back the value JSON-encoded, the other platforms as is.
        val result = pageCheckResult.orEmpty().trim().removeSurrounding("\"")
        if (result == SCRIPT_ALLOWED_MARKER) return null
        return result.takeIf { it.startsWith(SCRIPT_DENIED_MARKER) }
            ?.removePrefix(SCRIPT_DENIED_MARKER)
            ?.take(253)
            ?.ifEmpty { null }
            ?: "current page"
    }

    /** The error for a page that [deniedResourceFrom] refused. */
    fun scriptDenied(resource: String) = InsufficientPermissionException(
        PluginPermission.WEBVIEW_SCRIPT,
        resource = resource,
        detail = if (patterns.isEmpty()) {
            "the plugin declares no cookieDomains in plugin.json"
        } else {
            "page is not covered by the plugin's cookieDomains ${patterns.map { it.declared }}"
        })

    companion object {
        /** Thrown inside the page when [guardScript] refuses to run a script. */
        const val SCRIPT_DENIED = "Insufficient permission 'webview.script'"

        const val SCRIPT_ALLOWED_MARKER = "spotube-script-allowed"
        const val SCRIPT_DENIED_MARKER = "spotube-script-denied:"

        /**
         * Rejects anything but absolute http(s) URLs as a navigation target. `javascript:` would run
         * code in the current page, and `data:`/`file:`/`blob:` aren't plugin pages.
         */
        fun requireNavigable(url: String) {
            require(hostOf(url) != null) { "navigateTo only accepts absolute http(s) URLs" }
        }

        /** Host of an absolute http(s) URL, lowercased and without a trailing dot. */
        fun hostOf(url: String): String? {
            val trimmed = url.trim()
            // Ktor fills in "localhost" for a URL without a host, so require the scheme up front.
            if (!trimmed.startsWith("https://", ignoreCase = true) &&
                !trimmed.startsWith("http://", ignoreCase = true)) return null
            val host = runCatching { Url(trimmed).host }.getOrNull()
                ?.lowercase()
                ?.removeSuffix(".")
            return host?.takeIf { it.isNotEmpty() }
        }

        /**
         * Whether a cookie scoped to [cookieDomain] is one the browser would send to [host] (RFC 6265
         * domain matching).
         *
         * A cookie without a domain passes only when [domainlessCookiesAreUrlScoped]: the platform's
         * cookie store never reports domains and only returns the cookies it sends to the URL asked
         * for (Android). Elsewhere every cookie carries its domain, so one without is dropped.
         */
        fun cookieAppliesTo(cookieDomain: String, host: String, domainlessCookiesAreUrlScoped: Boolean): Boolean {
            val domain = cookieDomain.trim().lowercase().removePrefix(".").removeSuffix(".")
            if (domain.isEmpty()) return domainlessCookiesAreUrlScoped
            return host == domain || host.endsWith(".$domain")
        }
    }
}

/** Checks the permission declarations in a plugin's `plugin.json` before it is installed. */
object PluginPermissionValidator {
    /** The declared `cookieDomains` entries that aren't valid patterns. */
    fun invalidCookieDomains(declared: List<String>, publicSuffixes: PublicSuffixList): List<String> =
        declared.filter { CookieDomainPattern.parse(it, publicSuffixes) == null }
}
