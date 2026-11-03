/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.jam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.navigation.TOP_LEVEL_ROUTES
import org.spotube.spotube.core.navigation.rememberNavigationState
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.shell.AppShellViewModel
import org.spotube.spotube.modules.shell.FloatingPlayerContent
import org.spotube.spotube.modules.shell.FloatingPlayerBottomBleed
import org.spotube.spotube.modules.shell.PlayerUiState
import org.spotube.spotube.modules.shell.AppBottombar
import org.spotube.spotube.modules.shell.CompactPlayerOverlay
import org.spotube.millennium.composables.MillenniumTopAppBar
import org.jetbrains.compose.resources.stringResource
import com.composeunstyled.Text
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.jam_screen_title
import org.spotube.millennium.theme.MillenniumTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JamLayoutTest {
    @Test
    fun measuredDockHeightIncludesSystemInsetExactlyOnce() {
        val viewModel = AppShellViewModel()
        viewModel.onBottomBarMeasured(76.dp)
        assertEquals(
            140.dp,
            viewModel.bottomOverlayInset(false, 0.dp, viewModel.compactChromeUiState.value)
        )
        viewModel.onBottomBarMeasured(100.dp)
        assertEquals(
            164.dp,
            viewModel.bottomOverlayInset(false, 24.dp, viewModel.compactChromeUiState.value)
        )
        viewModel.onBottomBarMeasured(0.dp)
        assertEquals(100.dp, viewModel.compactChromeUiState.value.measuredBottomBarHeight)
    }

    /** Actual Compose controls rendered without native-window capture or a room server connection. */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun renderRoomControlsAtNarrowAndWideSizes() {
        val output = File("build/reports/jam-ui").apply { mkdirs() }
        val states = mapOf(
            "create" to JamUiState(
                serverConfigured = true,
                serverHost = "jam.example.com",
                canCreate = true
            ),
            "join" to JamUiState(
                serverConfigured = true,
                serverHost = "jam.example.com",
                setupMode = JamSetupMode.Join
            ),
            "host" to JamUiState(
                isActive = true,
                isConnected = true,
                isHost = true,
                roomCode = "RC5FJ8",
                serverConfigured = true,
                serverHost = "jam.example.com",
                participants = listOf(
                    JamParticipantUi("host", "Host-8316", "H", true, false),
                    JamParticipantUi("guest", "Guest-6684", "G", false, true)
                )
            ),
            "guest" to JamUiState(
                isActive = true,
                isConnected = true,
                roomCode = "RC5FJ8",
                serverConfigured = true,
                serverHost = "jam.example.com",
                participants = listOf(JamParticipantUi("host", "Host-8316", "H", true, false))
            )
        )
        for (width in listOf(360, 920)) {
            for ((name, state) in states) {
                var playerBottom = Float.NaN
                var navigationTop = Float.NaN
                var navigationBottom = Float.NaN
                ImageComposeScene(width, 760) {
                    SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                        Column(
                            Modifier.fillMaxSize().background(MillenniumTheme.colors.background),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MillenniumTopAppBar(title = { Text(stringResource(Res.string.jam_screen_title)) })
                            val navigation = rememberNavigationState(Routes.Home, TOP_LEVEL_ROUTES)
                            val navigator = remember(navigation) { Navigator(navigation) }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                CompactPlayerOverlay(
                                    navigator = navigator,
                                    navigationState = navigation,
                                    viewModel = remember { AppShellViewModel() },
                                    onQueue = {}, onAlternativeSource = {}, onExpandLyrics = {},
                                    floatingPlayer = { modifier ->
                                        FloatingPlayerContent(
                                            PlayerUiState(),
                                            modifier = modifier.onGloballyPositioned {
                                                playerBottom = it.boundsInRoot().bottom
                                            },
                                            onPlayPause = {},
                                            heartButton = {})
                                    },
                                    bottomBar = { modifier ->
                                        AppBottombar(
                                            navigator,
                                            navigation,
                                            modifier = modifier.onGloballyPositioned {
                                                navigationTop = it.boundsInRoot().top
                                                navigationBottom = it.boundsInRoot().bottom
                                            },
                                            libraryBadge = {})
                                    }
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Column(
                                            Modifier.widthIn(max = 520.dp).fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (state.isActive) SessionPanel(
                                                state,
                                                {},
                                                {},
                                                {},
                                                {},
                                                {},
                                                {})
                                            else SetupPanel(state, {}, {}, {})
                                            ServerStatus(state, {})
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.use { scene ->
                    repeat(8) { scene.render().close() }
                    assertTrue(playerBottom.isFinite())
                    assertEquals(
                        playerBottom - FloatingPlayerBottomBleed.value,
                        navigationTop,
                        "Only the player's bottom surface bleed should overlap navigation"
                    )
                    assertEquals(
                        760f,
                        navigationBottom,
                        "Navigation must reach the window's bottom edge"
                    )
                    scene.render().use { image ->
                        val data = assertNotNull(image.encodeToData())
                        data.use { File(output, "$name-$width.png").writeBytes(it.bytes) }
                    }
                }
            }
        }
    }
}
