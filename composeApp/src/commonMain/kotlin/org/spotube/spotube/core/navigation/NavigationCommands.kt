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

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// For providing navigation commands to the NavViewModel from other non-viewmodel classes like WebViewController
class NavigationCommands {
    private val _navigationCommand = Channel<Routes>(capacity = Channel.BUFFERED)
    val navigationCommandFlow = _navigationCommand.receiveAsFlow()

    private val _navigationPopCommand = Channel<Routes?>(capacity = Channel.BUFFERED)
    val navigationPopCommandFlow = _navigationPopCommand.receiveAsFlow()

    fun navigateTo(route: Routes) {
        _navigationCommand.trySend(route)
    }

    // Pops the last route. if route is provided, pops if the current route is the provided route, else pops unconditionally
    fun pop(route: Routes? = null) {
        _navigationPopCommand.trySend(route)
    }
}