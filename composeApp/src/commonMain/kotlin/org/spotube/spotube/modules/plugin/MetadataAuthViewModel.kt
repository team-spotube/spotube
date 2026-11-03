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

package org.spotube.spotube.modules.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.di.injectLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

data class MetadataAuthUiState(
    /** True when the selected metadata plugin needs an account and the user isn't signed in. */
    val loginRequired: Boolean = false,
    val pluginName: String? = null,
    /** True while a login started from the prompt is still running. */
    val isLoggingIn: Boolean = false)

/**
 * Tells screens whether the selected metadata plugin is waiting for a login, so they can show a
 * friendly sign-in prompt instead of surfacing the plugin's raw authorization exception.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MetadataAuthViewModel(
    private val pluginManager: PluginManager) : ViewModel(), KoinComponent {
    private val logger by injectLogger<MetadataAuthViewModel>()
    private val isLoggingIn = MutableStateFlow(false)

    val uiState: StateFlow<MetadataAuthUiState> = combine(
        pluginManager.selectedMetadataPlugin.flatMapLatest { service ->
            if (service == null) flowOf(false) else loginRequiredFlow(service)
        },
        pluginManager.state,
        isLoggingIn) { loginRequired, managerState, loggingIn ->
        val name = (managerState as? PluginManagerStates.Data)
            ?.selectedPlugins?.get(PluginAbility.METADATA)?.name
        MetadataAuthUiState(loginRequired = loginRequired, pluginName = name, isLoggingIn = loggingIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetadataAuthUiState())

    private fun loginRequiredFlow(service: org.spotube.spotube.core.zipline.PluginService): Flow<Boolean> = flow {
        val requiresAuth = try {
            service.use { coreAPI.requiresAuthentication }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
        if (!requiresAuth) {
            emit(false)
        } else {
            service.loggedInFlow.collect { emit(!it) }
        }
    }

    fun login() {
        if (isLoggingIn.value) return
        val service = pluginManager.selectedMetadataPlugin.value ?: return
        // The login runs in the plugin manager's scope so it survives this screen; we only wait on it.
        val task = pluginManager.launchTask {
            runCatching { service.use { coreAPI.login() } }
                .onFailure { logger.e(it) { "Metadata plugin login failed" } }
        }
        isLoggingIn.value = true
        viewModelScope.launch {
            try {
                task.join()
            } finally {
                isLoggingIn.update { false }
            }
        }
    }
}
