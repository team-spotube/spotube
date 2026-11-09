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

import org.spotube.plugin_interfaces.host_apis.InsufficientPermissionException
import org.spotube.plugin_interfaces.host_apis.PluginPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A few real rules of each kind from the Public Suffix List. */
internal val testPublicSuffixes = PublicSuffixList.parse(
    """
    // ===BEGIN ICANN DOMAINS===
    com
    org
    uk
    co.uk
    *.ck
    !www.ck
    公司.cn
    // ===BEGIN PRIVATE DOMAINS===
    github.io
    blogspot.com
    """.trimIndent())

class CookieDomainPolicyTest {
    private fun policy(vararg domains: String) = CookieDomainPolicy(domains.toList(), testPublicSuffixes)

    private fun parse(raw: String) = CookieDomainPattern.parse(raw, testPublicSuffixes)

    private fun invalid(vararg declared: String) =
        PluginPermissionValidator.invalidCookieDomains(declared.toList(), testPublicSuffixes)

    private fun assertDenied(policy: CookieDomainPolicy, url: String, resource: String? = null) {
        val e = assertFailsWith<InsufficientPermissionException> { policy.requireAllowed(url) }
        assertEquals(PluginPermission.WEBVIEW_COOKIES, e.permission)
        if (resource != null) assertEquals(resource, e.resource)
    }

    @Test
    fun exactDomainMatchesOnlyThatHost() {
        val p = policy("example.com")
        assertEquals("example.com", p.requireAllowed("https://example.com/login?x=1"))
        assertEquals("example.com", p.requireAllowed("HTTP://Example.COM.:8080/"))
        assertDenied(p, "https://www.example.com/", resource = "www.example.com")
        assertDenied(p, "https://example.com.evil.net/", resource = "example.com.evil.net")
        assertDenied(p, "https://notexample.com/", resource = "notexample.com")
    }

    @Test
    fun wildcardMatchesSubdomainsButNotParent() {
        val p = policy("*.example.com")
        assertEquals("a.example.com", p.requireAllowed("https://a.example.com"))
        assertEquals("a.b.example.com", p.requireAllowed("https://a.b.example.com/path"))
        assertDenied(p, "https://example.com/")
        assertDenied(p, "https://badexample.com/")
    }

    @Test
    fun deniesEverythingWithoutDeclarations() {
        assertDenied(policy(), "https://example.com/", resource = "example.com")
    }

    @Test
    fun deniesNonHttpAndRelativeUrls() {
        val p = policy("localhost", "example.com")
        assertDenied(p, "example.com")
        assertDenied(p, "/relative/path")
        assertDenied(p, "file:///etc/passwd")
        assertDenied(p, "javascript:alert(1)")
        assertDenied(p, "")
    }

    @Test
    fun parsesAndRejectsPatterns() {
        assertEquals(CookieDomainPattern.Exact("example.com"), parse(" Example.COM. "))
        assertEquals(CookieDomainPattern.Subdomains("example.com"), parse("*.example.com"))
        assertEquals(CookieDomainPattern.Exact("localhost"), parse("localhost"))
        val bad = arrayOf(
            "*", "*.com", "*.", "", "https://example.com", "example.com/path", "example.com:443",
            "a.*.example.com", "*example.com", "exa mple.com", "-bad.com", "bad-.com", "**.example.com")
        assertEquals(bad.toList(), invalid(*bad))
        assertEquals(emptyList(), invalid("a.com", "*.b.co"))
    }

    @Test
    fun wildcardsOverPublicSuffixesAreRejected() {
        val bad = arrayOf("*.co.uk", "*.github.io", "*.blogspot.com", "*.foo.ck", "*.xn--55qx5d.cn")
        assertEquals(bad.toList(), invalid(*bad))
        // Registrable domains under them are fine, and so is an exception to a wildcard rule.
        assertEquals(emptyList(), invalid("*.bbc.co.uk", "*.someone.github.io", "*.www.ck", "*.a.xn--55qx5d.cn"))
        // An exact host is one site, even when it's a suffix.
        assertEquals(emptyList(), invalid("github.io", "co.uk"))
    }

    @Test
    fun withoutADownloadedListNoWildcardIsAccepted() {
        val none = PublicSuffixList.Unavailable
        assertEquals(listOf("*.example.com"), PluginPermissionValidator.invalidCookieDomains(
            listOf("*.example.com", "example.com"), none))
        val p = CookieDomainPolicy(listOf("*.example.com", "example.com"), none)
        assertEquals(listOf(CookieDomainPattern.Exact("example.com")), p.patterns)
    }

    @Test
    fun publicSuffixRules() {
        val psl = testPublicSuffixes
        for (suffix in listOf("com", "co.uk", "github.io", "foo.ck", "xn--55qx5d.cn", "anytld")) {
            assertTrue(psl.isPublicSuffix(suffix), suffix)
        }
        for (domain in listOf("example.com", "bbc.co.uk", "me.github.io", "www.ck", "a.foo.ck")) {
            assertFalse(psl.isPublicSuffix(domain), domain)
        }
    }

    @Test
    fun punycodeMatchesRfc3492() {
        assertEquals("bcher-kva", Punycode.encode("bücher"))
        assertEquals("mnchen-3ya", Punycode.encode("münchen"))
        assertEquals("55qx5d", Punycode.encode("公司"))
        assertEquals("ls8h", Punycode.encode("\uD83D\uDCA9")) // U+1F4A9, a surrogate pair
    }

    @Test
    fun invalidDeclarationsNeverWidenAccess() {
        val p = policy("*", "*.com", "example.com")
        assertEquals(listOf(CookieDomainPattern.Exact("example.com")), p.patterns)
        assertDenied(p, "https://other.com/")
    }

    @Test
    fun readsThePageCheckResult() {
        val p = policy("example.com")
        assertNull(p.deniedResourceFrom(CookieDomainPolicy.SCRIPT_ALLOWED_MARKER))
        assertNull(p.deniedResourceFrom("\"${CookieDomainPolicy.SCRIPT_ALLOWED_MARKER}\"")) // Android
        assertEquals("victim.com", p.deniedResourceFrom("${CookieDomainPolicy.SCRIPT_DENIED_MARKER}victim.com"))
        assertEquals("victim.com", p.deniedResourceFrom("\"${CookieDomainPolicy.SCRIPT_DENIED_MARKER}victim.com\""))
        // No answer from the page, or one it didn't make, is a refusal.
        for (result in listOf(null, "", "null", "undefined", "x${CookieDomainPolicy.SCRIPT_ALLOWED_MARKER}")) {
            assertEquals("current page", p.deniedResourceFrom(result), result)
        }
        val e = p.scriptDenied("victim.com")
        assertEquals(PluginPermission.WEBVIEW_SCRIPT, e.permission)
        assertEquals("victim.com", e.resource)
    }

    @Test
    fun navigationOnlyToHttpUrls() {
        CookieDomainPolicy.requireNavigable("https://anything.net/login")
        for (url in listOf("javascript:alert(document.cookie)", "data:text/html,x", "file:///x", "about:blank", "intent://x")) {
            assertFailsWith<IllegalArgumentException>(url) { CookieDomainPolicy.requireNavigable(url) }
        }
    }

    @Test
    fun cookiesNamingTheDomainOnlyInTheQueryAreDropped() {
        // The iOS cookie manager matches with url.contains(domain), so this URL would return
        // victim.com's cookies without the host filter.
        val host = policy("example.com").requireAllowed("https://example.com/?next=victim.com")
        assertFalse(CookieDomainPolicy.cookieAppliesTo("victim.com", host, domainlessCookiesAreUrlScoped = false))
        assertFalse(CookieDomainPolicy.cookieAppliesTo(".victim.com", host, domainlessCookiesAreUrlScoped = false))
    }

    @Test
    fun cookieDomainMatching() {
        fun applies(domain: String, host: String) =
            CookieDomainPolicy.cookieAppliesTo(domain, host, domainlessCookiesAreUrlScoped = false)
        assertTrue(applies(".example.com", "a.example.com"))
        assertTrue(applies("example.com", "example.com"))
        assertFalse(applies("other.com", "example.com"))
        assertFalse(applies("a.example.com", "example.com"))
        assertFalse(applies("ample.com", "example.com"))
    }

    @Test
    fun domainlessCookiesPassOnlyWhereTheStoreScopesThemByUrl() {
        assertTrue(CookieDomainPolicy.cookieAppliesTo("", "example.com", domainlessCookiesAreUrlScoped = true))
        assertFalse(CookieDomainPolicy.cookieAppliesTo("", "example.com", domainlessCookiesAreUrlScoped = false))
        assertFalse(CookieDomainPolicy.cookieAppliesTo("  ", "example.com", domainlessCookiesAreUrlScoped = false))
    }
}
