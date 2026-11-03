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

package org.spotube.spotube.core.zipline

import app.cash.zipline.ZiplineService
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsAPI
import org.spotube.spotube.core.zipline.plugin_apis.common.RealCoreAPI
import org.spotube.spotube.core.zipline.plugin_apis.lrclib.RealLRCLibLyricsAPI
import org.spotube.spotube.core.zipline.plugin_apis.newpipe_yt.RealNewPipeAudioAPI
import org.spotube.spotube.modules.plugin.LRCLIB_BUILT_IN_PLUGIN
import org.spotube.spotube.modules.plugin.NEWPIPE_YOUTUBE_BUILT_IN_PLUGIN
import org.spotube.spotube.modules.plugin.PluginEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

class BuiltInPluginService(
    private val pluginInfo: PluginEntry) : PluginService, KoinComponent {

    override val pluginId: String = pluginInfo.id
    private val loggedInStateFlow = MutableStateFlow(false)
    override val loggedInFlow = loggedInStateFlow.asStateFlow()
    val servicesRegistry = mutableMapOf<KClass<*>, ZiplineService>()

//    val webViewController: WebViewController by inject()
//    val persistedStorage by lazy { RealPersistedStorageAPI(pluginInfo) }

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun runLogInFlowObservers() = scope.launch {
        val coreAPI = servicesRegistry[CoreAPI::class] as CoreAPI?
        coreAPI?.loggedInFlow?.collect { isLoggedIn ->
            loggedInStateFlow.value = isLoggedIn
        }
    }

    override suspend fun start() {
        when (pluginInfo) {
            NEWPIPE_YOUTUBE_BUILT_IN_PLUGIN -> {
                servicesRegistry[CoreAPI::class] = RealCoreAPI()
                servicesRegistry[AudioAPI::class] = RealNewPipeAudioAPI()
            }

            LRCLIB_BUILT_IN_PLUGIN -> {
                servicesRegistry[CoreAPI::class] = RealCoreAPI()
                servicesRegistry[LyricsAPI::class] = RealLRCLibLyricsAPI()
            }
        }
        runLogInFlowObservers()
    }

    override suspend fun stop() {
        for (service in servicesRegistry.values) {
            service.close()
        }
        servicesRegistry.clear()
    }

    override suspend fun <T> use(block: suspend PluginServiceScope.() -> T): T {
        //Since built-in plugins don't require any special setup, we can just execute the block with an empty scope
        return block(PluginServiceScope(servicesRegistry))
    }

}