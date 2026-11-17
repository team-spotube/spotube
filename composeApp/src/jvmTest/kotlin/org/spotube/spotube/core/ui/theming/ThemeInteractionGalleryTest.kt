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

package org.spotube.spotube.core.ui.theming

import org.spotube.spotube.core.ui.theming.engine.BuiltInThemes
import java.io.File
import kotlin.test.Test

/** Dev tool: renders hovered navigation, menu and dialog states to `build/theme-gallery/interaction-*` (PNG files). */
class ThemeInteractionGalleryTest {
    @Test
    fun rendersHoveredStates() {
        val out = File("build/theme-gallery")
        val modern = BuiltInThemes.loadModern("build/tmp/theme-gallery-cache")
        for ((name, theme) in listOf("stock" to null, "modern" to modern)) {
            for (dark in listOf(true, false)) {
                val mode = if (dark) "dark" else "light"
                fun file(part: String) = File(out, "interaction-$name-$mode-$part.png")
                ThemeInteractionGallery.render(theme, dark, "side0", file("sidebar"), 760, 420) { ThemeInteractionGallery.Navigation() }
                ThemeInteractionGallery.render(theme, dark, "dock0", file("dock"), 760, 420) { ThemeInteractionGallery.Navigation() }
                ThemeInteractionGallery.render(theme, dark, "menu0", file("menu"), 360, 300, hoverOffset = androidx.compose.ui.geometry.Offset(0f, 72f)) { ThemeInteractionGallery.Menu() }
                ThemeInteractionGallery.render(theme, dark, "close", file("dialog"), 560, 360) { ThemeInteractionGallery.DialogView() }
            }
        }
    }
}
