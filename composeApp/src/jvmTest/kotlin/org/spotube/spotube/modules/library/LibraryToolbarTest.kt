/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.spotube.millennium.composables.MillenniumTopAppBar
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryToolbarTest {
    private lateinit var library: LibraryState

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        library = LibraryState()
    }

    @AfterTest
    fun cleanup() {
        library.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun closingSearchClearsTheActiveFilter() = runTest {
        library.openSearch()
        library.uiState.value.searchState.setTextAndPlaceCursorAtEnd("Muse")
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertTrue(library.uiState.value.searchMode)
        assertEquals("Muse", library.queryFlow(LibraryTab.Playlists).first())

        library.closeSearch()
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertFalse(library.uiState.value.searchMode)
        assertEquals("", library.uiState.value.searchState.text.toString())
    }

    @Test
    fun eachTabRetainsItsOwnFilterAndActions() = runTest {
        library.setQuery(LibraryTab.Playlists, "Rock")
        library.onTabSelected(LibraryTab.Albums)
        library.setQuery(LibraryTab.Albums, "Jazz")
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals("Jazz", library.uiState.value.searchState.text.toString())
        assertFalse(library.uiState.value.canCreatePlaylist)

        library.onTabSelected(LibraryTab.Playlists)
        runCurrent()
        assertEquals("Rock", library.uiState.value.searchState.text.toString())
        assertTrue(library.uiState.value.canCreatePlaylist)
        library.showCreatePlaylist()
        runCurrent()
        assertTrue(library.uiState.value.showCreatePlaylist)

        library.onTabSelected(LibraryTab.LocalTracks)
        runCurrent()
        assertTrue(library.uiState.value.canRefresh)
        assertFalse(library.uiState.value.showCreatePlaylist)
        library.onTabSelected(LibraryTab.Downloads)
        runCurrent()
        assertFalse(library.uiState.value.canFilter)
        assertFalse(library.uiState.value.searchMode)
    }

    @Test
    fun anInlineDesktopQueryRemainsVisibleWhenTheLayoutBecomesCompact() = runTest {
        library.setQuery(LibraryTab.Artists, "Radiohead")
        library.onTabSelected(LibraryTab.Artists)
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertTrue(library.uiState.value.searchMode)
        assertEquals("Radiohead", library.uiState.value.searchState.text.toString())
    }

    @Test
    fun viewModeDefaultsAndOverridesArePerTab() = runTest {
        assertEquals(LibraryViewMode.List, defaultLibraryViewMode(true))
        assertEquals(LibraryViewMode.Grid, defaultLibraryViewMode(false))
        library.toggleViewMode(isCompact = true)
        runCurrent()
        assertEquals(LibraryViewMode.Grid, library.uiState.value.viewModeOverride)
        library.onTabSelected(LibraryTab.Albums)
        runCurrent()
        assertNull(library.uiState.value.viewModeOverride)
        library.toggleViewMode(isCompact = false)
        runCurrent()
        assertEquals(LibraryViewMode.List, library.uiState.value.viewModeOverride)
        library.onTabSelected(LibraryTab.Playlists)
        runCurrent()
        assertEquals(LibraryViewMode.Grid, library.uiState.value.viewModeOverride)
        library.onTabSelected(LibraryTab.LocalTracks)
        library.toggleViewMode(isCompact = true)
        runCurrent()
        assertFalse(library.uiState.value.canChangeView)
        assertNull(library.uiState.value.viewModeOverride)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun renderExpandedCompactAndSearchToolbars() {
        val output = File("build/reports/library-toolbar").apply { mkdirs() }
        for (tab in listOf(LibraryTab.Playlists, LibraryTab.Albums, LibraryTab.LocalTracks)) {
            for ((mode, width) in listOf(
                "desktop" to 575,
                "mobile" to 360,
                "search" to 360,
                "compact-desktop" to 392,
                "compact-desktop-search" to 392
            )) {
                val expandedDesktop = mode == "desktop"
                val searching = mode.contains("search")
                val state = LibraryToolbarUiState(
                    currentTab = tab,
                    searchMode = searching,
                    searchState = TextFieldState(if (searching) "Muse" else ""),
                    canCreatePlaylist = tab == LibraryTab.Playlists,
                    canRefresh = tab == LibraryTab.LocalTracks,
                    canChangeView = tab != LibraryTab.LocalTracks
                )
                ImageComposeScene(width, 88) {
                    SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                        MillenniumTopAppBar(
                            title = {
                                LibraryToolbarTitle(
                                    state,
                                    expandedDesktop,
                                    mode.startsWith("compact-desktop")
                                )
                            },
                            actions = {
                                LibraryToolbarActions(
                                    state,
                                    expandedDesktop,
                                    {},
                                    {},
                                    {},
                                    {},
                                    compactDesktop = mode.startsWith("compact-desktop")
                                )
                                if (expandedDesktop || mode.startsWith("compact-desktop")) Spacer(
                                    Modifier.width(132.dp)
                                )
                            }
                        )
                    }
                }.use { scene ->
                    repeat(8) { scene.render().close() }
                    fun flatten(node: SemanticsNode): List<SemanticsNode> =
                        listOf(node) + node.children.flatMap(::flatten)

                    val nodes =
                        scene.semanticsOwners.flatMap { flatten(it.unmergedRootSemanticsNode) }
                    val descriptions = nodes.flatMap {
                        it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
                    }
                    val textFields =
                        nodes.filter { it.config.getOrNull(SemanticsProperties.EditableText) != null }
                    assertEquals(if (expandedDesktop || searching) 1 else 0, textFields.size)
                    if (searching) {
                        assertTrue("Close filter" in descriptions)
                        assertFalse("Create playlist" in descriptions)
                        assertFalse("Refresh local tracks" in descriptions)
                        assertEquals(
                            "Muse",
                            textFields.single().config[SemanticsProperties.EditableText].text
                        )
                    } else {
                        assertEquals(tab == LibraryTab.Playlists, "Create playlist" in descriptions)
                        assertEquals(
                            tab == LibraryTab.LocalTracks,
                            "Refresh local tracks" in descriptions
                        )
                    }
                    textFields.forEach { node ->
                        assertTrue(
                            node.boundsInRoot.left >= 0f && node.boundsInRoot.right <= width,
                            "Filter must fit beside desktop window controls"
                        )
                    }
                    scene.render().use { image ->
                        assertNotNull(image.encodeToData()).use {
                            File(
                                output,
                                "${tab.name}-$mode.png"
                            ).writeBytes(it.bytes)
                        }
                    }
                }
            }
        }
    }
}
