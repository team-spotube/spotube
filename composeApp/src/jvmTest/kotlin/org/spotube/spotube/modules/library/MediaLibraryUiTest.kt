/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import org.spotube.spotube.core.ui.component.cards.AvatarCard
import org.spotube.spotube.core.ui.component.cards.PlayableCard
import org.spotube.spotube.core.ui.component.cards.PlayableListTile
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.downloads.DownloadItem
import org.spotube.spotube.modules.downloads.DownloadRowAction
import org.spotube.spotube.modules.downloads.DownloadStatus
import org.spotube.spotube.modules.downloads.DownloadsContent
import org.spotube.spotube.modules.downloads.DownloadsToolbarTitle
import org.spotube.spotube.modules.downloads.DownloadsToolbarSummary
import org.spotube.spotube.modules.downloads.DownloadsToolbarActions
import org.spotube.spotube.modules.downloads.downloadsUiState
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksState
import org.spotube.spotube.modules.library.local_tracks.LocalTracksContent
import org.spotube.spotube.modules.library.local_tracks.LocalTracksBreadcrumb
import org.spotube.spotube.modules.library.local_tracks.localTracksUiState
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaFolder
import org.spotube.spotube.modules.library.local_tracks.media.LocalMediaTrack
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import java.io.File
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.composables.MillenniumTopAppBar
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.media_artist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class MediaLibraryUiTest {
    private val tracks = listOf(
        LocalMediaTrack("/music/one.mp3", "Nishongota", listOf("Aurora"), 185_000, "Clancy"),
        LocalMediaTrack(
            "/music/two.mp3",
            "Amay Prashna Kare Neel Dhrubatara",
            listOf("Hemanta Mukherjee"),
            195_000
        ),
        LocalMediaTrack("/downloads/three.mp3", "Whatever It Takes", emptyList(), 0)
    )
    private val folders = listOf(
        LocalMediaFolder("/music", "Music", tracks.take(2)),
        LocalMediaFolder("/downloads", "Downloads", tracks.takeLast(1))
    )
    private val artists = listOf("Aurora", "Hemanta Mukherjee", "Imagine Dragons")
    private val downloadItems = listOf(
        DownloadItem("1", tracks[0].name, "Aurora", null, DownloadStatus.Downloading, 0.48f),
        DownloadItem("2", tracks[1].name, "Hemanta Mukherjee", null, DownloadStatus.Completed),
        DownloadItem(
            "3",
            tracks[2].name,
            "Imagine Dragons",
            null,
            DownloadStatus.Failed("Connection lost")
        ),
        DownloadItem("4", "Clancy", "Twenty One Pilots", null, DownloadStatus.Queued)
    )

    @Test
    fun downloadRowsHaveReadyProgressAndCorrectActions() {
        val state = downloadsUiState(downloadItems)
        assertEquals(2, state.activeCount)
        assertEquals(1, state.completedCount)
        assertTrue(state.canClearCompleted)
        assertEquals(48, state.items[0].percent)
        assertEquals(DownloadRowAction.Cancel, state.items[0].action)
        assertEquals(DownloadRowAction.Remove, state.items[1].action)
        assertEquals(DownloadRowAction.Retry, state.items[2].action)
        assertEquals("Connection lost", state.items[2].error)
        assertEquals(
            0f,
            downloadsUiState(listOf(downloadItems[0].copy(progress = Float.NaN))).items.single().progress
        )
    }

    @Test
    fun localMediaFilteringAndMetadataAreComputedBeforeRendering() {
        val base = LibraryLocalTracksState(folders = folders)
        val root = localTracksUiState(base, "aurora")
        assertEquals(listOf("Music"), root.visibleFolders.map { it.name })
        assertEquals(3, root.totalTrackCount)
        val inFolder = localTracksUiState(base.copy(currentFolderPath = "/music"), "clancy")
        assertEquals(listOf(tracks[0].name), inFolder.visibleTracks.map { it.title })
        assertEquals("3:05", inFolder.visibleTracks.single().durationLabel)
        assertEquals("Aurora • Clancy", inFolder.visibleTracks.single().subtitle)
        assertTrue(inFolder.canPlayFolder)
        assertFalse(
            localTracksUiState(
                base.copy(
                    currentFolderPath = "/music",
                    canAccessMedia = false
                ), ""
            ).canPlayFolder
        )
    }

    @Test
    fun expandedDownloadsHasOneHeaderAndOneClearAction() {
        var cleared = 0
        val state = downloadsUiState(downloadItems)
        ImageComposeScene(960, 660) {
            SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                Column(Modifier.fillMaxSize()) {
                    MillenniumTopAppBar(
                        title = { DownloadsToolbarTitle() },
                        subtitle = { DownloadsToolbarSummary(state) },
                        actions = { DownloadsToolbarActions(state) { cleared++ } }
                    )
                    DownloadsContent(state, {}, {}, {}, {}, Modifier.weight(1f), showHeader = false)
                }
            }
        }.use { scene ->
            repeat(6) { scene.render().close() }
            val nodes = allNodes(scene)
            val texts = nodes.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
                .map { it.text }
            assertEquals(1, texts.count { it == "Downloads" })
            assertEquals(1, texts.count { it == "Clear completed" })
            val action = nodes.first {
                it.config.getOrNull(SemanticsActions.OnClick) != null &&
                        it.config.getOrNull(SemanticsProperties.Text)
                            ?.any { text -> text.text == "Clear completed" } == true
            }
            action.config[SemanticsActions.OnClick].action?.invoke()
            assertEquals(1, cleared)
        }
    }

    @Test
    fun breadcrumbIsCompactAndItsParentLinkNavigatesUp() {
        var height = 0
        var navigatedUp = 0
        val state = localTracksUiState(
            LibraryLocalTracksState(
                folders = folders,
                currentFolderPath = "/music"
            ), ""
        )
        ImageComposeScene(360, 48) {
            SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                LocalTracksBreadcrumb(
                    state,
                    { navigatedUp++ },
                    {},
                    Modifier.onGloballyPositioned { height = it.size.height })
            }
        }.use { scene ->
            repeat(6) { scene.render().close() }
            assertEquals(40, height)
            val parent = allNodes(scene).first {
                it.config.getOrNull(SemanticsActions.OnClick) != null &&
                        it.config.getOrNull(SemanticsProperties.ContentDescription)
                            ?.contains("Back to folders") == true
            }
            parent.config[SemanticsActions.OnClick].action?.invoke()
            assertEquals(1, navigatedUp)
        }
    }

    @Test
    fun renderScreensAndCollectionModesInLightAndDark() {
        val output = File("build/reports/media-library-ui").apply { mkdirs() }
        for (theme in listOf(Theme.DARK, Theme.LIGHT)) {
            for (width in listOf(360, 960)) {
                render(output, "downloads-$theme-$width", width, theme) {
                    val state = downloadsUiState(downloadItems)
                    if (width >= 840) Column(Modifier.fillMaxSize()) {
                        MillenniumTopAppBar(
                            title = { DownloadsToolbarTitle() },
                            subtitle = { DownloadsToolbarSummary(state) },
                            actions = { DownloadsToolbarActions(state, {}) })
                        DownloadsContent(
                            state,
                            {},
                            {},
                            {},
                            {},
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                            showHeader = false
                        )
                    } else DownloadsContent(
                        state,
                        {},
                        {},
                        {},
                        {},
                        Modifier.padding(horizontal = 12.dp)
                    )
                }
                render(output, "folders-$theme-$width", width, theme) {
                    LocalTracksContent(
                        localTracksUiState(
                            LibraryLocalTracksState(folders = folders),
                            ""
                        ), {}, {}, {}, {}, {}, Modifier.padding(horizontal = 12.dp)
                    )
                }
                render(output, "tracks-$theme-$width", width, theme) {
                    LocalTracksContent(
                        localTracksUiState(
                            LibraryLocalTracksState(
                                folders = folders,
                                currentFolderPath = "/music"
                            ), ""
                        ), {}, {}, {}, {}, {}, Modifier.padding(horizontal = 12.dp)
                    )
                }
                for (mode in LibraryViewMode.entries) render(
                    output,
                    "collection-$mode-$theme-$width",
                    width,
                    theme
                ) {
                    LibraryMediaCollection(
                        items = tracks,
                        itemKey = { it.path },
                        viewMode = mode,
                        onViewportChanged = { _, _ -> },
                        itemContent = { item, list ->
                            PlayableCard(
                                item.name,
                                item.artists.joinToString(),
                                onClick = {},
                                onPlay = {},
                                onAddToQueue = {},
                                listMode = list
                            )
                        },
                        skeleton = {},
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                for (mode in LibraryViewMode.entries) render(
                    output,
                    "artists-$mode-$theme-$width",
                    width,
                    theme
                ) {
                    LibraryMediaCollection(
                        items = artists,
                        itemKey = { it },
                        viewMode = mode,
                        onViewportChanged = { _, _ -> },
                        itemContent = { artist, list ->
                            AvatarCard(
                                artist,
                                stringResource(Res.string.media_artist),
                                onClick = {},
                                listMode = list
                            )
                        },
                        skeleton = {},
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }

    @Test
    fun bothCardTypesRespondToHoverAndClick() {
        val output = File("build/reports/media-library-ui").apply { mkdirs() }
        for (avatar in listOf(false, true)) {
            var clicks = 0
            ImageComposeScene(192, 260) {
                SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                    Box(
                        Modifier.fillMaxSize().background(MillenniumTheme.colors.background)
                            .padding(16.dp)
                    ) {
                        if (avatar) AvatarCard(
                            tracks[0].artists.first(),
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("card")
                        )
                        else PlayableCard(
                            tracks[0].name,
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("card")
                        )
                    }
                }
            }.use { scene ->
                repeat(4) { scene.render().close() }
                val before = bytes(scene, 0)
                scene.sendPointerEvent(PointerEventType.Enter, Offset(80f, 80f), timeMillis = 0)
                scene.sendPointerEvent(PointerEventType.Move, Offset(80f, 80f), timeMillis = 1)
                repeat(12) { scene.render((it + 1) * 16_000_000L).close() }
                val hovered = bytes(scene, 250_000_000)
                assertFalse(
                    before.contentEquals(hovered),
                    "Hover should visibly change the ${if (avatar) "avatar" else "playable"} card"
                )
                File(output, "card-${if (avatar) "avatar" else "playable"}-hover.png").writeBytes(
                    hovered
                )
                scene.sendPointerEvent(
                    PointerEventType.Press,
                    Offset(80f, 80f),
                    timeMillis = 260,
                    buttons = PointerButtons(isPrimaryPressed = true),
                    button = PointerButton.Primary
                )
                scene.render(300_000_000).close()
                scene.sendPointerEvent(
                    PointerEventType.Release, Offset(80f, 80f), timeMillis = 310,
                    buttons = PointerButtons(), button = PointerButton.Primary
                )
                scene.render(350_000_000).close()
                assertEquals(1, clicks)
            }
        }
    }

    @Test
    fun listPlaybackActionsDoNotTriggerNavigation() {
        var navigations = 0
        var plays = 0
        ImageComposeScene(360, 100) {
            SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                PlayableListTile(tracks[0].name, onClick = { navigations++ }, onPlay = { plays++ })
            }
        }.use { scene ->
            repeat(4) { scene.render().close() }
            val play = allNodes(scene).first {
                it.config.getOrNull(SemanticsActions.OnClick) != null &&
                        it.config.getOrNull(SemanticsProperties.ContentDescription)
                            ?.contains("Play") == true
            }
            val center = play.boundsInRoot.center
            scene.sendPointerEvent(
                PointerEventType.Press,
                center,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.sendPointerEvent(
                PointerEventType.Release,
                center,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render().close()
            assertEquals(1, plays)
            assertEquals(0, navigations)
        }
    }

    private fun render(
        output: File,
        name: String,
        width: Int,
        theme: Theme,
        content: @Composable () -> Unit
    ) {
        ImageComposeScene(width, 660) {
            SpotubeTheme(UserSettings(theme = theme)) {
                Box(
                    Modifier.fillMaxSize().background(MillenniumTheme.colors.background)
                ) { content() }
            }
        }.use { scene ->
            repeat(6) { scene.render().close() }
            if (name.startsWith("downloads")) {
                val texts = allNodes(scene).flatMap {
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty()
                }.map { it.text }
                assertTrue("Downloading 48%" in texts)
            }
            File(output, "$name.png").writeBytes(bytes(scene, 0))
        }
    }

    private fun bytes(scene: ImageComposeScene, time: Long): ByteArray =
        scene.render(time).use { image ->
            assertNotNull(image.encodeToData()).use { it.bytes }
        }

    private fun allNodes(scene: ImageComposeScene): List<SemanticsNode> {
        fun flatten(node: SemanticsNode): List<SemanticsNode> =
            listOf(node) + node.children.flatMap(::flatten)
        return scene.semanticsOwners.flatMap { flatten(it.rootSemanticsNode) }
    }
}
