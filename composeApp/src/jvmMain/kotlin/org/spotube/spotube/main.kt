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

package org.spotube.spotube

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.spotube_logo
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.graalvm.GraalVmInitializer
import dev.nucleusframework.window.tao.TaoScreenGeometry
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.spotube.spotube.core.appimage.AppImageEnvironment
import org.spotube.spotube.core.di.initKoin
import org.spotube.spotube.core.i18n.AppLocaleProvider
import org.spotube.spotube.core.newpipe.NewPipeDownloader
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.core.systemtray.SystemTray
import org.spotube.spotube.core.systemtray.SystemTrayService
import org.spotube.spotube.core.ui.component.LocalApplicationScope
import org.spotube.spotube.core.ui.component.LocalWindowScope
import org.spotube.spotube.core.ui.component.LocalWindowState
import org.spotube.spotube.core.window.PersistedWindowState
import org.spotube.spotube.core.window.WindowStateRepository
import org.spotube.spotube.core.window.coerceForScreen
import org.spotube.spotube.modules.settings.SettingsProvider
import org.spotube.spotube.modules.settings.SupportedLanguages

object KoinPathsProvider : KoinComponent {
    val paths: Paths get() = get()
}

private object KoinServicesProvider : KoinComponent {
    val systemTrayService: SystemTrayService get() = get()
    val settingsProvider: SettingsProvider get() = get()
    val windowStateRepository: WindowStateRepository get() = get()
}

private const val WINDOW_SCREEN_FRACTION = 0.85f
private const val WINDOW_ASPECT_RATIO = 16f / 10f
private val MAX_WINDOW_WIDTH = 1440.dp
private val MAX_WINDOW_HEIGHT = 960.dp
private val MIN_WINDOW_SIZE = DpSize(800.dp, 500.dp)
private const val WINDOW_STATE_SAVE_DEBOUNCE_MS = 500L

/** Largest [WINDOW_ASPECT_RATIO] size that fits inside [maxWidth] x [maxHeight]. */
private fun fitAspectRatio(maxWidth: Dp, maxHeight: Dp): DpSize {
    val width = minOf(maxWidth, maxHeight * WINDOW_ASPECT_RATIO)
    return DpSize(width, width / WINDOW_ASPECT_RATIO)
}

/**
 * A 16:10 window that fits in 85% of the primary monitor's work area (screen minus
 * taskbar/dock), capped at 1440x960.
 *
 * Uses Nucleus' Tao screen geometry rather than AWT, which doesn't exist on the Tao runtime.
 * The work area is in physical pixels, so it's divided by the monitor scale factor to get dp.
 * Falls back to the cap box when the platform bridge can't report a monitor.
 */
private fun initialWindowSize(): DpSize {
    val workArea = availableScreenSize()
        ?: return fitAspectRatio(MAX_WINDOW_WIDTH, MAX_WINDOW_HEIGHT)

    return fitAspectRatio(
        maxWidth = (workArea.width * WINDOW_SCREEN_FRACTION).coerceAtMost(MAX_WINDOW_WIDTH),
        maxHeight = (workArea.height * WINDOW_SCREEN_FRACTION).coerceAtMost(MAX_WINDOW_HEIGHT))
}

/** The primary monitor's work area in dp, or null when the platform bridge can't report it. */
private fun availableScreenSize(): DpSize? {
    val workArea = runCatching { TaoScreenGeometry.primaryMonitorWorkAreaPx() }.getOrNull()
    if (workArea == null || workArea.size < 4 || workArea[2] <= 0 || workArea[3] <= 0) return null

    val scale = runCatching { TaoScreenGeometry.primaryMonitorScaleFactor() }
        .getOrDefault(1f)
        .takeIf { it > 0f } ?: 1f

    return DpSize((workArea[2] / scale).dp, (workArea[3] / scale).dp)
}

/** Saved size if there is one (clamped to this screen), otherwise the default for a first launch. */
private fun restoreWindowState(saved: PersistedWindowState?): PersistedWindowState {
    if (saved == null || saved.widthDp <= 0f || saved.heightDp <= 0f) {
        return PersistedWindowState.of(initialWindowSize(), WindowPlacement.Floating)
    }
    val screen = availableScreenSize()
    val size = saved.size.coerceForScreen(
        min = MIN_WINDOW_SIZE,
        maxWidth = screen?.width,
        maxHeight = screen?.height)
    return PersistedWindowState.of(size, saved.placement)
}

/** Runs [block] on a fresh thread and waits for it, rethrowing whatever it threw. */
private fun runOnWorkerThread(name: String, block: () -> Unit) {
    var failure: Throwable? = null
    val thread = Thread({ runCatching(block).onFailure { failure = it } }, name)
    thread.start()
    thread.join()
    failure?.let { throw it }
}

@OptIn(ExperimentalComposeUiApi::class, FlowPreview::class)
fun main() {
    // Sets compose.application.resources.dir in the native image. initKoin() creates the audio
    // player, whose VLC loader reads it, so it can't wait for nucleusApplication() (which calls
    // this again, harmlessly). Without it the bundled VLC is skipped for the system one.
    GraalVmInitializer.initialize()
    AppImageEnvironment.apply()
    FileKit.init(appId = "org.spotube.spotube")
    val initApp = {
        initKoin()
        NewPipeDownloader.init(KoinPathsProvider.paths)
    }
    // libvlc and the system media controls initialize COM as multithreaded on the calling thread. In the
    // native image the main thread also creates the Tao window, whose OleInitialize needs a single-threaded
    // apartment and panics with RPC_E_CHANGED_MODE, so on Windows Koin's eager singletons are created
    // elsewhere. COM doesn't exist on macOS/Linux, where this stays on the main thread as before.
    if (getPlatform().type == PlatformType.Windows) {
        runOnWorkerThread("spotube-init", initApp)
    } else {
        initApp()
    }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    nucleusApplication(backend = NucleusBackend.Tao) {
        val windowStateRepository = remember { KoinServicesProvider.windowStateRepository }
        // Needed before the window is created, so the one-time read at startup is blocking
        val restored = remember { restoreWindowState(runBlocking { windowStateRepository.load() }) }
        val windowState = rememberWindowState(placement = restored.placement, size = restored.size)

        // The size to restore into when un-maximizing; only floating windows update it
        var normalSize by remember { mutableStateOf(restored.size) }
        val currentWindowState = { PersistedWindowState.of(normalSize, windowState.placement) }
        // Exit paths can't wait on a coroutine that is about to be cancelled
        val persistWindowStateBlocking = { runBlocking { windowStateRepository.save(currentWindowState()) } }

        LaunchedEffect(windowState) {
            snapshotFlow { windowState.placement to windowState.size }
                .onEach { (placement, size) ->
                    if (placement == WindowPlacement.Floating && size.isSpecified &&
                        size.width > 0.dp && size.height > 0.dp) {
                        normalSize = size
                    }
                }
                .debounce(WINDOW_STATE_SAVE_DEBOUNCE_MS)
                .collect { windowStateRepository.save(currentWindowState()) }
        }

        val settingsProvider = KoinServicesProvider.settingsProvider
        val settings by settingsProvider.settingsState.collectAsState(initial = null)
        val minimizeToTray = settings?.minimizeToTray ?: false

        val trayService = KoinServicesProvider.systemTrayService

        var isWindowVisible by remember { mutableStateOf(true) }

        val onToggleWindowVisibility = { isWindowVisible = !isWindowVisible }
        val onExit = {
            persistWindowStateBlocking()
            trayService.close()
            appScope.cancel()
            exitApplication()
        }

        trayService.setCallbacks(onToggleWindowVisibility)

        if (minimizeToTray) {
            // The tray lives outside App(), so it needs its own provider to follow the app language.
            AppLocaleProvider(settings?.language ?: SupportedLanguages.SYSTEM) {
                SystemTray(
                    isWindowVisible = isWindowVisible,
                    onToggleWindowVisibility = onToggleWindowVisibility,
                    onExit = onExit
                )
            }
        }

        DecoratedWindow(
            state = windowState,
            onCloseRequest = {
                if (minimizeToTray) {
                    isWindowVisible = false
                } else {
                    persistWindowStateBlocking()
                    trayService.close()
                    appScope.cancel()
                    exitApplication()
                }
            },
            title = "Spotube",
            icon = painterResource(Res.drawable.spotube_logo),
            visible = isWindowVisible
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
