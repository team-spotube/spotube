package org.spotube.spotube.modules.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.navigation.TOP_LEVEL_ROUTES
import org.spotube.spotube.core.navigation.rememberNavigationState
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class CompactPlayerOverlayDragTest {
    @Test
    fun draggingTheSheetUpShowsTheExpandedPlayer() {
        var expandedComposed = false
        var floatingCompositions = 0
        var floatingPlaced = false
        val width = 400
        val height = 800
        ImageComposeScene(width, height) {
            SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                val navigation = rememberNavigationState(Routes.Home, TOP_LEVEL_ROUTES)
                val navigator = remember(navigation) { Navigator(navigation) }
                CompactPlayerOverlay(
                    navigator = navigator,
                    navigationState = navigation,
                    viewModel = remember { AppShellViewModel() },
                    onQueue = {}, onAlternativeSource = {}, onExpandLyrics = {},
                    floatingPlayer = { modifier ->
                        DisposableEffect(Unit) {
                            floatingCompositions++
                            onDispose {}
                        }
                        Box(
                            modifier.fillMaxWidth().height(64.dp).background(Color.Gray)
                                .onGloballyPositioned { floatingPlaced = true })
                    },
                    expandedPlayer = { modifier, _ ->
                        DisposableEffect(Unit) {
                            expandedComposed = true
                            onDispose { expandedComposed = false }
                        }
                        Box(modifier.fillMaxSize().background(Color.Red))
                    },
                    bottomBar = { modifier ->
                        Box(modifier.fillMaxWidth().height(56.dp).background(Color.Blue))
                    }
                ) { Box(Modifier.fillMaxSize()) }
            }
        }.use { scene ->
            var t = 0L
            fun frame() { scene.render(t); t += 16_000_000L }
            repeat(10) { frame() }
            assertTrue(floatingPlaced, "floating player should show while collapsed")
            assertFalse(expandedComposed, "expanded player should not be composed while collapsed")

            // grab the floating player (just above the bottom bar) and drag to the top
            val startY = height - 120f + 20f
            val start = Offset(width / 2f, startY)
            scene.sendPointerEvent(PointerEventType.Press, start, type = PointerType.Touch, timeMillis = t / 1_000_000L)
            repeat(5) { frame() }
            var y = startY
            repeat(30) {
                y -= (startY - 40f) / 30f
                scene.sendPointerEvent(PointerEventType.Move, Offset(width / 2f, y), type = PointerType.Touch, timeMillis = t / 1_000_000L)
                frame()
            }
            scene.sendPointerEvent(PointerEventType.Release, Offset(width / 2f, y), type = PointerType.Touch, timeMillis = t / 1_000_000L)
            repeat(120) { frame() }

            assertTrue(expandedComposed, "expanded player should be composed after dragging up")
            // out of layout while the expanded player covers it, but never re-composed
            floatingPlaced = false
            repeat(5) { frame() }
            assertFalse(floatingPlaced, "floating player should be out of layout when fully expanded")

            // drag back down: the floating player must return without being composed again
            val downStart = Offset(width / 2f, 60f)
            scene.sendPointerEvent(PointerEventType.Press, downStart, type = PointerType.Touch, timeMillis = t / 1_000_000L)
            repeat(5) { frame() }
            var dy = downStart.y
            repeat(30) {
                dy += (height - 160f) / 30f
                scene.sendPointerEvent(PointerEventType.Move, Offset(width / 2f, dy), type = PointerType.Touch, timeMillis = t / 1_000_000L)
                frame()
            }
            scene.sendPointerEvent(PointerEventType.Release, Offset(width / 2f, dy), type = PointerType.Touch, timeMillis = t / 1_000_000L)
            repeat(120) { frame() }
            assertTrue(floatingPlaced, "floating player should be back after collapsing")
            assertTrue(floatingCompositions == 1, "floating player was composed $floatingCompositions times, expected 1")
        }
    }
}
