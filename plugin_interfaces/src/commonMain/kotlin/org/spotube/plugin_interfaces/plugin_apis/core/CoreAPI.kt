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

package org.spotube.plugin_interfaces.plugin_apis.core

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.StateFlow
import net.swiftzer.semver.SemVer

const val CoreAPI_SERVICE_NAME = "CoreAPI"

interface CoreAPI: ZiplineService {
    suspend fun checkPluginUpdates(currentVersion: SemVer): PluginUpdateInfo?
    // Show a dialog to the user with the given title and message,
    // and return true if they click "OK" or false if they click "Cancel".
    fun supportMarkdownText(currentVersion: SemVer): String

    val requiresAuthentication: Boolean
    val loggedInFlow: StateFlow<Boolean>
    suspend fun login()
    suspend fun logout()
}