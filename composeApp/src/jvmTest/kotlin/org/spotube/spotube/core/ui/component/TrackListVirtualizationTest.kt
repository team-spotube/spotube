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

package org.spotube.spotube.core.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #3200: big track lists must only compose the rows on screen. */
@OptIn(ExperimentalComposeUiApi::class)
class TrackListVirtualizationTest {
    private fun track(index: Int) = MetadataTrack(
        // Every 100th id repeats: playlists can hold the same track twice.
        id = if (index % 100 == 0) "dup" else "t$index",
        title = "Song $index",
        durationMs = 200_000L,
        trackNumber = 1,
        discNumber = 1,
        artists = listOf(MetadataArtist.Basic(id = "a", name = "Artist", thumbnails = emptyList(), externalUri = null)),
        album = null,
        thumbnails = emptyList(),
        explicit = false,
        popularity = 0,
        isrcCode = null,
        externalUri = null)

    private fun allNodes(scene: ImageComposeScene): List<SemanticsNode> {
        fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
        return scene.semanticsOwners.flatMap { flatten(it.rootSemanticsNode) }
    }

    private fun songTitles(scene: ImageComposeScene): Set<String> = allNodes(scene)
        .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
        .map { it.text }
        .filter { it.startsWith("Song ") }
        .toSet()

    @Test
    fun onlyVisibleRowsAreComposedAndTheEndLoadsMore() {
        val tracks = (0 until 2000).map(::track)
        val listState = LazyListState()
        var scope: CoroutineScope? = null
        var nextPageRequests = 0

        ImageComposeScene(1000, 800) {
            scope = rememberCoroutineScope()
            SpotubeTheme(UserSettings(theme = Theme.DARK)) {
                TrackList(
                    tracks = tracks,
                    hasMore = true,
                    state = listState,
                    onLoadNextPage = { nextPageRequests++ })
            }
        }.use { scene ->
            repeat(3) { scene.render().close() }

            val visible = songTitles(scene)
            assertTrue("Song 1" in visible, "first rows render: $visible")
            assertTrue(visible.size in 3..60, "expected only on-screen rows, composed ${visible.size}")
            assertEquals(0, nextPageRequests)

            scope!!.launch { listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1) }
            repeat(3) { scene.render().close() }

            val atEnd = songTitles(scene)
            assertTrue("Song 1999" in atEnd, "last rows render: ${atEnd.sorted().takeLast(5)}")
            assertTrue("Song 1" !in atEnd)
            assertEquals(1, nextPageRequests)
        }
    }
}
