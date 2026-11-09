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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class InsufficientPermissionExceptionTest {
    @Test
    fun recoversFromBridgedStackTraceString() {
        val original = InsufficientPermissionException(
            PluginPermission.WEBVIEW_COOKIES, "accounts.example.com", "not declared")
        // Zipline hands the plugin a ZiplineException whose message is the host's stack trace.
        val bridged = Exception(original.stackTraceToString())
        val recovered = assertNotNull(bridged.asInsufficientPermission())
        assertEquals(PluginPermission.WEBVIEW_COOKIES, recovered.permission)
        assertEquals("accounts.example.com", recovered.resource)
    }

    @Test
    fun returnsSameInstanceAndFollowsCauses() {
        val original = InsufficientPermissionException(PluginPermission.WEBVIEW_COOKIES, "x.com")
        assertSame(original, original.asInsufficientPermission())
        assertSame(original, RuntimeException("wrapped", original).asInsufficientPermission())
    }

    @Test
    fun resourceCannotBreakTheFormat() {
        val e = InsufficientPermissionException(PluginPermission.WEBVIEW_COOKIES, "evil'\ninjected")
        assertEquals("evilinjected", Exception(e.message).asInsufficientPermission()?.resource)
    }

    @Test
    fun ignoresUnrelatedErrors() {
        assertNull(IllegalStateException("boom").asInsufficientPermission())
        assertNull(Exception("Insufficient permission 'unknown.perm' for 'x'").asInsufficientPermission())
    }
}
