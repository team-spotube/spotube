/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.use
import org.spotube.spotube.core.ui.theming.SpotubeTheme
import org.spotube.spotube.core.ui.theming.appBackground
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.User
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Reviewable local detail layouts in both desktop and narrow-window widths. */
@OptIn(ExperimentalComposeUiApi::class)
class LocalCollectionLayoutTest {
    private data class Track(
        override val path: String,
        override val title: String,
        override val subtitle: String?,
        override val durationLabel: String?,
        override val artwork: ByteArray? = null
    ) : LocalCollectionTrack

    @Test
    fun renderLocalCollectionLayouts() {
        val output = File("build/reports/local-collection-layout").apply { mkdirs() }
        for (width in listOf(390, 1080)) {
            for (type in listOf("playlist", "album", "artist")) {
                ImageComposeScene(width, 720) {
                    SpotubeTheme(UserSettings(theme = Theme.LIGHT)) {
                        Box(Modifier.fillMaxSize().appBackground()) {
                            LocalCollectionScreen(
                                title = when (type) {
                                    "playlist" -> "My favorite local tracks"
                                    "album" -> "Made in the A.M."
                                    else -> "Jonas Blue"
                                },
                                description = if (type == "album") "One Direction • 6 tracks" else "6 tracks",
                                artwork = null,
                                placeholderIcon = when (type) {
                                    "playlist" -> Iconsax.IconsaxMusic
                                    "album" -> Iconsax.IconsaxCd
                                    else -> Iconsax.User
                                },
                                tracks = listOf(
                                    Track("a", "First song", "One Direction", "3:26"),
                                    Track("b", "Second song", "One Direction", "3:10")
                                ),
                                onPlay = {}, onShuffle = {}, onAddToQueue = {}, onTrackClick = {},
                                onEdit = if (type == "playlist") ({}) else null,
                                onRemoveTrack = if (type == "playlist") ({}) else null,
                                artworkShape = if (type == "artist") CircleShape else null,
                                showTopBar = false
                            )
                        }
                    }
                }.use { scene ->
                    repeat(5) { scene.render().close() }
                    File(output, "$type-$width.png").writeBytes(
                        scene.render().use { image -> assertNotNull(image.encodeToData()).bytes }
                    )
                }
            }
        }
    }
}
