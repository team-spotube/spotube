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

package org.spotube.spotube.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The nav back stack is saved when the Android activity stops (a file picker opens, the user switches apps). A route
 * missing from [serializersConfig] crashes that save, so every route must be registered.
 */
class RoutesSerializationTest {
    @Test
    fun everyRouteIsRegisteredForStateSaving() {
        val routes = Routes::class.sealedSubclasses
        assertTrue(routes.size > 10)
        val module = serializersConfig.serializersModule
        for (route in routes) {
            val instance = route.objectInstance as? NavKey ?: continue // data classes need arguments, objects are enough to catch gaps
            runCatching { encodeToSavedState(PolymorphicSerializer(NavKey::class), instance, serializersConfig) }
                .onFailure { throw AssertionError("${route.simpleName} is not registered in serializersConfig", it) }
        }
        // data classes: check the registration through the module
        for (route in routes.filter { it.objectInstance == null }) {
            val registered = module.getPolymorphic(NavKey::class, serializedClassName = route.qualifiedName ?: "") != null
            assertTrue(registered, "${route.simpleName} is not registered in serializersConfig")
        }
    }
}
