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

package dev.krtirtho.spotube

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.sun.jna.Library
import com.sun.jna.Native
import dev.krtirtho.spotube.core.di.initKoin
import dev.krtirtho.spotube.core.deeplink.ExternalUriHandler
import dev.krtirtho.spotube.core.newpipe.NewPipeDownloader
import dev.krtirtho.spotube.core.paths.Paths
import dev.krtirtho.spotube.core.systemtray.SystemTray
import dev.krtirtho.spotube.core.systemtray.SystemTrayService
import dev.krtirtho.spotube.core.ui.component.LocalApplicationScope
import dev.krtirtho.spotube.core.ui.component.LocalWindowScope
import dev.krtirtho.spotube.core.ui.component.LocalWindowState
import dev.krtirtho.spotube.modules.settings.SettingsProvider
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object KoinPathsProvider : KoinComponent {
    val paths: Paths get() = get()
}

private object KoinServicesProvider : KoinComponent {
    val systemTrayService: SystemTrayService get() = get()
    val settingsProvider: SettingsProvider get() = get()
}

private interface LibC : Library {
    fun setenv(name: String, value: String, overwrite: Int): Int

    companion object {
        val INSTANCE: LibC = Native.load("c", LibC::class.java)
    }
}

/**
 * WebKitGTK is only used on Linux (Windows = WebView2, macOS = WKWebView).
 * The webview is created *after* the window is already mapped and the Tao
 * render/swap loop is running. WebKitGTK's accelerated-compositing path then
 * initialises its own GL context in-process, racing Tao's swap thread on the
 * same Mesa display, which deterministically segfaults `libgallium` on the
 * next Compose flush. Disabling WebKit's hardware-accelerated compositing
 * (and its DMABUF renderer) removes that GL context entirely — login pages
 * render fine in software. Must run before libwebkit2gtk is loaded.
 */
private fun disableWebKitGpuCompositing() {
    if (!System.getProperty("os.name").lowercase().contains("linux")) return
    LibC.INSTANCE.setenv("WEBKIT_DISABLE_COMPOSITING_MODE", "1", 1)
    LibC.INSTANCE.setenv("WEBKIT_DISABLE_DMABUF_RENDERER", "1", 1)
}

/**
 * Routes `spotube://` deep links into [ExternalUriHandler].
 * macOS delivers them through the open-URI handler; on Linux/Windows they arrive
 * as command line arguments (scheme registration is handled by the distribution
 * packaging, e.g. the `.desktop` file's `Exec %u`).
 */
private fun handleStartupDeepLinks(args: Array<String>) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().setOpenURIHandler { event ->
                ExternalUriHandler.onNewUri(event.uri.toString())
            }
        }
    }
    args.firstOrNull { it.startsWith("spotube:", ignoreCase = true) }
        ?.let(ExternalUriHandler::onNewUri)
}

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    disableWebKitGpuCompositing()
    handleStartupDeepLinks(args)
    FileKit.init(appId = "dev.krtirtho.spotube")
    initKoin()
    NewPipeDownloader.init(KoinPathsProvider.paths)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    nucleusApplication(backend = NucleusBackend.Tao) {
        val windowState = rememberWindowState(
            width = 1080.dp,
            height = 720.dp
        )

        val settingsProvider = KoinServicesProvider.settingsProvider
        val settings by settingsProvider.settingsState.collectAsState(initial = null)
        val minimizeToTray = settings?.minimizeToTray ?: false

        val trayService = KoinServicesProvider.systemTrayService

        var isWindowVisible by remember { mutableStateOf(true) }

        val onToggleWindowVisibility = { isWindowVisible = !isWindowVisible }
        val onExit = {
            trayService.close()
            appScope.cancel()
            exitApplication()
        }

        trayService.setCallbacks(onToggleWindowVisibility)

        if (minimizeToTray) {
            SystemTray(
                isWindowVisible = isWindowVisible,
                onToggleWindowVisibility = onToggleWindowVisibility,
                onExit = onExit,
            )
        }

        DecoratedWindow(
            state = windowState,
            onCloseRequest = {
                if (minimizeToTray) {
                    isWindowVisible = false
                } else {
                    trayService.close()
                    appScope.cancel()
                    exitApplication()
                }
            },
            title = "Spotube",
            visible = isWindowVisible,
        ) {
            CompositionLocalProvider(
                LocalApplicationScope provides this@nucleusApplication,
                LocalWindowScope provides this@DecoratedWindow,
                LocalWindowState provides windowState
            ) {
                App()
            }
        }
    }
}
