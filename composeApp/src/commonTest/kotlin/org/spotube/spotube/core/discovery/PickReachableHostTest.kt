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

package org.spotube.spotube.core.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class PickReachableHostTest {
    @Test
    fun prefersIpv4OverLeadingIpv6() {
        assertEquals("192.168.1.20", pickReachableHost(listOf("fe80::1%wlan0", "2001:db8::5", "192.168.1.20"), "phone.local"))
    }

    @Test
    fun skipsLinkLocalWhenAnythingElseExists() {
        assertEquals("2001:db8::5", pickReachableHost(listOf("fe80::1%wlan0", "2001:db8::5"), "phone.local"))
    }

    @Test
    fun fallsBackToHostWhenNoAddresses() {
        assertEquals("phone.local", pickReachableHost(emptyList(), "phone.local"))
    }
}
