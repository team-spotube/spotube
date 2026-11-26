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

package org.spotube.spotube.core.tools.user_agents

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UserAgentsTest {

    // Platform matches
    @Test
    fun `iOS matches iPhone UA`() {
        val ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.IOS))
    }

    @Test
    fun `iOS matches iPad UA`() {
        val ua = "Mozilla/5.0 (iPad; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.IOS))
    }

    @Test
    fun `iOS matches iPod UA`() {
        val ua = "Mozilla/5.0 (iPod touch; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.IOS))
    }

    @Test
    fun `Android matches Android UA`() {
        val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Android))
    }

    @Test
    fun `Windows matches Windows NT UA`() {
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Windows))
    }

    @Test
    fun `MacOS matches Macintosh UA`() {
        val ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.MacOS))
    }

    @Test
    fun `Linux matches X11 UA`() {
        val ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Linux))
    }

    @Test
    fun `Linux matches Linux UA`() {
        val ua = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Linux))
    }

    // CrOS excluded from Linux
    @Test
    fun `Linux excludes Chrome OS UA`() {
        val ua = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Linux))
    }

    // Case sensitivity (the `in` operator is case-sensitive)
    @Test
    fun `Linux is case-sensitive for "linux"`() {
        val ua = "Mozilla/5.0 (X11; linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Linux))
    }

    @Test
    fun `Android is case-sensitive for "android"`() {
        val ua = "Mozilla/5.0 (linux; android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Android))
    }

    // Multi-keyword strings - platform priority
    @Test
    fun `Android UA with Windows NT still matches Android`() {
        // An Android UA that also contains "Windows NT" should match Android when type is Android
        val ua = "Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Mobile Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Android))
        // But it should NOT match Windows because it contains "Android"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Windows))
    }

    @Test
    fun `iOS UA with Mac OS X still matches iOS`() {
        // iOS UAs contain "Mac OS X" but should match iOS, not MacOS
        val ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.IOS))
        // But it should NOT match MacOS because it contains "iPhone"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.MacOS))
    }

    // Unknown platform fallback
    @Test
    fun `Unknown matches any non-mobile UA`() {
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        assertTrue(UserAgents.matchesPlatform(ua, PlatformType.Unknown))
    }

    @Test
    fun `Unknown rejects iOS UA`() {
        val ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Unknown))
    }

    @Test
    fun `Unknown rejects Android UA`() {
        val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Mobile Safari/537.36"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Unknown))
    }

    // Empty/unknown UAs
    @Test
    fun `empty UA matches nothing`() {
        val ua = ""
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.IOS))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Android))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Windows))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.MacOS))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Linux))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Unknown))
    }

    @Test
    fun `random string UA matches nothing`() {
        val ua = "not-a-user-agent"
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.IOS))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Android))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Windows))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.MacOS))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Linux))
        assertFalse(UserAgents.matchesPlatform(ua, PlatformType.Unknown))
    }
}
