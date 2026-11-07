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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import spotube.composeapp.generated.resources.Res

/**
 * The Public Suffix List (https://publicsuffix.org): domains under which anyone can register a
 * name, like `com`, `co.uk` or `github.io`. A plugin declaring `*.co.uk` would get every UK
 * company's cookies, so wildcards whose parent is a public suffix are refused.
 *
 * Both the ICANN and the private section count. Rules are stored in ASCII (punycode for IDNs), to
 * match the hosts the WebView reports.
 */
class PublicSuffixList private constructor(
    private val exact: Set<String>,
    /** Parents of `*.parent` rules: every direct child of `parent` is a public suffix. */
    private val wildcardParents: Set<String>,
    /** `!domain` rules: not a public suffix, even though a wildcard covers it. */
    private val exceptions: Set<String>) {

    /** Whether [domain] (lowercase ASCII, no trailing dot) is itself a public suffix. */
    fun isPublicSuffix(domain: String): Boolean {
        if (domain in exceptions) return false
        if (domain in exact) return true
        if ('.' !in domain) return true // The list's implicit "*" rule: every TLD.
        return domain.substringAfter('.') in wildcardParents
    }

    companion object {
        /** Parses the list's text format: one rule per line, `//` comments, `*.` and `!` prefixes. */
        fun parse(text: String): PublicSuffixList {
            val exact = HashSet<String>()
            val wildcardParents = HashSet<String>()
            val exceptions = HashSet<String>()
            text.lineSequence().forEach { line ->
                val rule = line.trim().takeUnless { it.isEmpty() || it.startsWith("//") }
                    ?.split(' ', '\t')?.first()
                    ?: return@forEach
                when {
                    rule.startsWith("!") -> exceptions += toAscii(rule.substring(1))
                    rule.startsWith("*.") -> wildcardParents += toAscii(rule.substring(2))
                    else -> exact += toAscii(rule)
                }
            }
            return PublicSuffixList(exact, wildcardParents, exceptions)
        }

        private const val RESOURCE_PATH = "files/public_suffix_list.dat"
        private val loadLock = Mutex()
        private var loaded: PublicSuffixList? = null

        /** The bundled list, read and parsed once. */
        suspend fun bundled(): PublicSuffixList = loaded ?: loadLock.withLock {
            loaded ?: parse(Res.readBytes(RESOURCE_PATH).decodeToString()).also { loaded = it }
        }

        private fun toAscii(domain: String): String =
            domain.lowercase().split('.').joinToString(".") { label ->
                if (label.all { it.code < 0x80 }) label else "xn--" + Punycode.encode(label)
            }
    }
}

/** RFC 3492 Punycode encoding of one label, without the `xn--` prefix. */
internal object Punycode {
    private const val BASE = 36
    private const val T_MIN = 1
    private const val T_MAX = 26
    private const val SKEW = 38
    private const val DAMP = 700
    private const val INITIAL_BIAS = 72
    private const val INITIAL_N = 128

    fun encode(label: String): String {
        val input = codePoints(label)
        val output = StringBuilder()
        input.filter { it < 0x80 }.forEach { output.append(it.toChar()) }
        val basicCount = output.length
        var handled = basicCount
        if (basicCount > 0) output.append('-')

        var n = INITIAL_N
        var delta = 0
        var bias = INITIAL_BIAS
        while (handled < input.size) {
            val m = input.filter { it >= n }.min()
            delta += (m - n) * (handled + 1)
            n = m
            for (c in input) {
                if (c < n) delta++
                if (c == n) {
                    var q = delta
                    var k = BASE
                    while (true) {
                        val t = when {
                            k <= bias -> T_MIN
                            k >= bias + T_MAX -> T_MAX
                            else -> k - bias
                        }
                        if (q < t) break
                        output.append(digit(t + (q - t) % (BASE - t)))
                        q = (q - t) / (BASE - t)
                        k += BASE
                    }
                    output.append(digit(q))
                    bias = adapt(delta, handled + 1, handled == basicCount)
                    delta = 0
                    handled++
                }
            }
            delta++
            n++
        }
        return output.toString()
    }

    private fun adapt(delta: Int, numPoints: Int, first: Boolean): Int {
        var d = if (first) delta / DAMP else delta / 2
        d += d / numPoints
        var k = 0
        while (d > ((BASE - T_MIN) * T_MAX) / 2) {
            d /= BASE - T_MIN
            k += BASE
        }
        return k + (BASE - T_MIN + 1) * d / (d + SKEW)
    }

    private fun digit(d: Int): Char = if (d < 26) 'a' + d else '0' + (d - 26)

    private fun codePoints(s: String): List<Int> {
        val points = ArrayList<Int>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) {
                points += ((c.code - 0xD800) shl 10) + (s[i + 1].code - 0xDC00) + 0x10000
                i += 2
            } else {
                points += c.code
                i++
            }
        }
        return points
    }
}
