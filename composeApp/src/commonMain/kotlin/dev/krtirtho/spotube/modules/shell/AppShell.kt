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

package dev.krtirtho.spotube.modules.shell

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.NavigationState
import dev.krtirtho.spotube.core.navigation.Navigator
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.remote.ConnectionRequestDialogHost
import dev.krtirtho.spotube.core.remote.RemotePlaybackController
import dev.krtirtho.spotube.modules.devices.PlayDestinationPickerHost
import dev.krtirtho.spotube.modules.lyrics.LyricsScreen
import dev.krtirtho.spotube.modules.shell.alternative_track.AlternativeTrackContent
import dev.krtirtho.spotube.modules.shell.alternative_track.AlternativeTrackContentViewModel
import dev.krtirtho.spotube.modules.shell.alternative_track.AlternativeTrackSheet
import dev.krtirtho.spotube.modules.shell.player_queue.PlayerQueueContent
import dev.krtirtho.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import dev.krtirtho.spotube.modules.shell.player_queue.QueueSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

val LocalAppShellBottomInset = staticCompositionLocalOf { 0.dp }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppShell(
    navigator: Navigator,
    navigationState: NavigationState,
    viewModel: AppShellViewModel = koinViewModel<AppShellViewModel>(),
    queueViewModel: PlayerQueueContentViewModel = koinViewModel<PlayerQueueContentViewModel>(),
    alternativeViewModel: AlternativeTrackContentViewModel = koinViewModel<AlternativeTrackContentViewModel>(),
    content: @Composable () -> Unit,
) {
    val navigatorCommands: NavigationCommands = koinInject()
    val remotePlaybackController: RemotePlaybackController = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(remotePlaybackController) {
        remotePlaybackController.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    val isQueueVisible by queueViewModel.isQueueVisible.collectAsState()
    val isAlternativeVisible by alternativeViewModel.isAlternativeVisible.collectAsState()
    val isLyricsOverlayVisible by viewModel.isLyricsOverlayVisible.collectAsState()

    LaunchedEffect(navigatorCommands, navigator) {
        launch {
            navigatorCommands.navigationCommandFlow.collect { route ->
                navigator.navigate(route)
            }
        }
        launch {
            navigatorCommands.navigationPopCommandFlow.collect { route ->
                navigationState.backStacks[navigationState.topLevelRoute]?.let { backStack ->
                    backStack.lastOrNull()?.let { currentRoute ->
                        if (route == null || route == currentRoute) {
                            navigator.pop()
                        }
                    }
                }
            }
        }
    }

    ConnectionRequestDialogHost()
    PlayDestinationPickerHost()

    Box(modifier = Modifier.fillMaxSize()) {
        val useSidebar = viewModel.useSidebar()
        val bottomOverlayInset = viewModel.bottomOverlayInset(useSidebar)

        CompositionLocalProvider(LocalAppShellBottomInset provides bottomOverlayInset) {
            if (useSidebar) {
                val hazeState = rememberHazeState()
                val bgHazeState = rememberHazeState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .hazeSource(state = bgHazeState) // <-- Put it here!
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            AppSidebar(
                                hazeState = bgHazeState,
                                navigator = navigator,
                                navigationState = navigationState
                            )
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color = Color.Gray.copy(alpha = 0.2f),
                                thickness = 1.dp,
                            )
                            Box(modifier = Modifier.weight(1f).hazeSource(hazeState)) {
                                content()
                            }
                        }

                        if (isQueueVisible) {
                            Box(
                                modifier = Modifier.fillMaxSize().clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { queueViewModel.setQueueVisibility(false) },
                                )
                            )
                        }

                        if (isAlternativeVisible) {
                            Box(
                                modifier = Modifier.fillMaxSize().clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { alternativeViewModel.setAlternativeVisibility(false) },
                                )
                            )
                        }

                        QueueSheet(
                            isVisible = isQueueVisible,
                            onDismiss = { queueViewModel.setQueueVisibility(false) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            PlayerQueueContent(
                                viewModel = queueViewModel,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        AlternativeTrackSheet(
                            isVisible = isAlternativeVisible,
                            onDismiss = { alternativeViewModel.setAlternativeVisibility(false) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            AlternativeTrackContent(
                                viewModel = alternativeViewModel,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    AppLargePlayer(
                        hazeState = hazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        onQueue = queueViewModel::toggleQueueVisibility,
                        onAlternativeSource = alternativeViewModel::toggleAlternativeVisibility,
                        onLyrics = { navigatorCommands.navigateTo(Routes.Lyrics) },
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    CompactPlayerOverlay(
                        navigator = navigator,
                        navigationState = navigationState,
                        viewModel = viewModel,
                        onQueue = queueViewModel::toggleQueueVisibility,
                        onAlternativeSource = alternativeViewModel::toggleAlternativeVisibility,
                        onExpandLyrics = viewModel::showLyricsOverlay,
                        content = content,
                    )

                    QueueSheet(
                        isVisible = isQueueVisible,
                        onDismiss = { queueViewModel.setQueueVisibility(false) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PlayerQueueContent(
                            viewModel = queueViewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    AlternativeTrackSheet(
                        isVisible = isAlternativeVisible,
                        onDismiss = { alternativeViewModel.setAlternativeVisibility(false) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AlternativeTrackContent(
                            viewModel = alternativeViewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (isLyricsOverlayVisible) {
                        Dialog(
                            onDismissRequest = { viewModel.hideLyricsOverlay() },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true,
                            ),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LyricsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    onClose = { viewModel.hideLyricsOverlay() },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Drawn last so it floats above the players/sheets, just above the
        // bottom overlay (large player or compact player + bottombar).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomOverlayInset + 12.dp),
        )
    }
}

@OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun BoxWithConstraintsScope.CompactPlayerOverlay(
    navigator: Navigator,
    navigationState: NavigationState,
    viewModel: AppShellViewModel,
    onQueue: () -> Unit,
    onAlternativeSource: () -> Unit,
    onExpandLyrics: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val floatingPlayerHeightPx = with(density) { viewModel.floatingPlayerHeight.toPx() }
    val bottomBarHeightPx = with(density) { viewModel.bottomBarHeight.toPx() }
    val navBarInsetPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val peekHeightPx = floatingPlayerHeightPx + bottomBarHeightPx + navBarInsetPx
    val peekHeight = with(density) { peekHeightPx.toDp() }

    val sheetHeightPx = with(density) { maxHeight.toPx() }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )

    val progressState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sheetState, sheetHeightPx, peekHeightPx) {
        snapshotFlow {
            try {
                sheetState.requireOffset()
            } catch (_: IllegalStateException) {
                peekHeightPx
            }
        }.collect { offset ->
            val expandedOffset = 0f
            val collapsedOffset = sheetHeightPx - peekHeightPx
            val range = (collapsedOffset - expandedOffset).coerceAtLeast(1f)
            val rawProgress = (collapsedOffset - offset) / range
            progressState.floatValue = rawProgress.coerceIn(0f, 1f)
        }
    }

    val uiState by remember {
        derivedStateOf {
            viewModel.compactSheetUiState(progressState.floatValue)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = null,
        sheetContainerColor = Color.Transparent,
        sheetShape = RectangleShape,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.showExpandedPlayer) {
                    AppExpandedPlayer(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = uiState.expandedPlayerAlpha },
                        onCollapse = {
                            scope.launch { sheetState.partialExpand() }
                        },
                        onQueue = onQueue,
                        onAlternativeSource = onAlternativeSource,
                        onExpandLyrics = onExpandLyrics,
                    )
                }

                if (uiState.floatingPlayerAlpha > 0.01f) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppFloatingPlayer(
                            modifier = Modifier
                                .offset(y = 12.dp)
                                .graphicsLayer {
                                    alpha = uiState.floatingPlayerAlpha
                                },
                        )
                    }
                }
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
    if (uiState.floatingPlayerAlpha > 0.01f) {
        AppBottombar(
            navigator = navigator,
            navigationState = navigationState,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).graphicsLayer {
                alpha = uiState.floatingPlayerAlpha
            },
        )
    }
}
