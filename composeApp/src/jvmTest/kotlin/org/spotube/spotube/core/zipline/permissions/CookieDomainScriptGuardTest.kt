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

import app.cash.zipline.QuickJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Runs [CookieDomainPolicy.guardScript] in a real JS engine against a fake page whose `location`
 * and `document` are non-configurable, as they are on a browser's `window`.
 */
class CookieDomainScriptGuardTest {
    private val policy = CookieDomainPolicy(listOf("example.com", "*.allowed.org"), testPublicSuffixes)

    private class Page(val js: QuickJs, val policy: CookieDomainPolicy) {
        fun run(script: String): Any? = js.evaluate(policy.guardScript(script))

        /** What the host makes of [CookieDomainPolicy.pageCheckScript] on this page. */
        fun deniedResource(): String? = policy.deniedResourceFrom(js.evaluate(policy.pageCheckScript()) as String?)

        fun assertDenied(script: String) {
            assertFailsWith<Exception> { run(script) }
            assertNull(js.evaluate("globalThis.leaked"), "script ran on a page it may not script")
        }
    }

    private fun onPage(
        protocol: String,
        hostname: String,
        domain: String = hostname,
        policy: CookieDomainPolicy = this.policy,
        block: Page.() -> Unit) {
        QuickJs.create().use { js ->
            js.evaluate(
                """
                Object.defineProperty(globalThis, "location", {
                    value: { protocol: "$protocol", hostname: "$hostname" },
                    writable: false, configurable: false });
                Object.defineProperty(globalThis, "document", {
                    value: { domain: "$domain", cookie: "secret=1" },
                    writable: false, configurable: false });
                globalThis.leaked = null;
                """.trimIndent())
            Page(js, policy).block()
        }
    }

    @Test
    fun runsOnDeclaredHostsAndKeepsTheResult() {
        onPage("https:", "example.com") { assertEquals("secret=1", run("document.cookie")) }
        onPage("https:", "example.com.") { assertEquals("secret=1", run("document.cookie")) }
        onPage("https:", "a.b.allowed.org") { assertEquals(3, run("var x = 1; x + 2")) }
    }

    @Test
    fun refusesUndeclaredAndLookalikeHosts() {
        for (host in listOf("victim.com", "allowed.org", "example.com.victim.com", "notexample.com")) {
            onPage("https:", host) { assertDenied("globalThis.leaked = document.cookie") }
        }
    }

    @Test
    fun hoistedFunctionsCannotDisableTheCheck() {
        onPage("https:", "victim.com") {
            assertDenied("function ok() { return true; } globalThis.leaked = document.cookie")
            assertDenied("function location() {} globalThis.leaked = document.cookie")
            assertDenied("function document() {} globalThis.leaked = document.cookie")
            assertDenied("var location = { protocol: 'https:', hostname: 'example.com' }; globalThis.leaked = 1")
        }
    }

    @Test
    fun cannotEscapeTheGuardStatement() {
        onPage("https:", "victim.com") {
            assertDenied("else { globalThis.leaked = document.cookie }")
            assertDenied("} globalThis.leaked = document.cookie; {")
            assertDenied("*/ globalThis.leaked = document.cookie")
            assertDenied("\n(globalThis.leaked = document.cookie)")
        }
    }

    @Test
    fun opaqueOriginPagesMayBeScripted() {
        onPage("about:", "", domain = "") { assertEquals(42, run("40 + 2")) }
        onPage("data:", "", domain = "", policy = CookieDomainPolicy(emptyList(), testPublicSuffixes)) {
            assertEquals(42, run("40 + 2"))
        }
    }

    @Test
    fun documentsInheritingAnOriginAreJudgedByIt() {
        onPage("blob:", "", domain = "victim.com") { assertDenied("globalThis.leaked = document.cookie") }
        onPage("blob:", "", domain = "example.com") { assertEquals("secret=1", run("document.cookie")) }
    }

    @Test
    fun noDeclarationsMeansNoSitePages() {
        onPage("https:", "example.com", policy = CookieDomainPolicy(emptyList(), testPublicSuffixes)) {
            assertDenied("globalThis.leaked = document.cookie")
        }
    }

    @Test
    fun pageCheckAgreesWithTheGuard() {
        onPage("https:", "example.com") { assertNull(deniedResource()) }
        onPage("https:", "a.allowed.org") { assertNull(deniedResource()) }
        onPage("about:", "", domain = "") { assertNull(deniedResource()) }
        onPage("https:", "victim.com") { assertEquals("victim.com", deniedResource()) }
        onPage("https:", "allowed.org") { assertEquals("allowed.org", deniedResource()) }
        onPage("blob:", "", domain = "victim.com") { assertEquals("victim.com", deniedResource()) }
        onPage("https:", "example.com", policy = CookieDomainPolicy(emptyList(), testPublicSuffixes)) {
            assertEquals("example.com", deniedResource())
        }
    }

    @Test
    fun pageCheckIgnoresHostsThatLookLikeTheMarker() {
        onPage("https:", "${CookieDomainPolicy.SCRIPT_ALLOWED_MARKER}.victim.com") {
            assertEquals("${CookieDomainPolicy.SCRIPT_ALLOWED_MARKER}.victim.com", deniedResource())
        }
    }
}
