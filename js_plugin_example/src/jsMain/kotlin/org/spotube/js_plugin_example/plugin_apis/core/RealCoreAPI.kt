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

package org.spotube.js_plugin_example.plugin_apis.core

import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.PluginUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RealCoreAPI: CoreAPI {
    override suspend fun checkPluginUpdates(currentVersion: net.swiftzer.semver.SemVer): PluginUpdateInfo? {
        return null
    }

    override fun supportMarkdownText(currentVersion: net.swiftzer.semver.SemVer): String {
        return "Support us please!"
    }

    override val requiresAuthentication = true

    private val stateFlow = MutableStateFlow(false)
    override val loggedInFlow: StateFlow<Boolean> = stateFlow.asStateFlow()

    override suspend fun login() {
        stateFlow.value = true
    }

    override suspend fun logout() {
        stateFlow.value = false
    }

}