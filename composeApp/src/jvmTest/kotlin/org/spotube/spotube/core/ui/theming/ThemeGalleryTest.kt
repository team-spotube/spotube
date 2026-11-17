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

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Renders the stock look and every sample theme in `composeApp/themes/<name>/` (light and dark) to
 * `build/theme-gallery` (PNG files), and fails if a sample theme has warnings.
 */
class ThemeGalleryTest {
    @Test
    fun rendersStockAndSampleThemes() {
        val out = File("build/theme-gallery")
        ThemeGallery.render(null, dark = true, out = File(out, "stock-dark.png"))
        ThemeGallery.render(null, dark = false, out = File(out, "stock-light.png"))
        val modern = org.spotube.spotube.core.ui.theming.engine.BuiltInThemes.loadModern("build/tmp/theme-gallery-cache")
        ThemeGallery.render(modern, dark = true, out = File(out, "modern-dark.png"))
        ThemeGallery.render(modern, dark = false, out = File(out, "modern-light.png"))
        File("themes").listFiles { f -> f.isDirectory && File(f, "theme.json").exists() }.orEmpty().sortedBy { it.name }.forEach { dir ->
            val problems = LinkedHashSet<String>()
            val theme = ThemeGallery.load(dir, problems)
            assertNotNull(theme, dir.name)
            assertEquals(emptySet(), problems, dir.name)
            assertEquals(null, theme.meta.missingRequired(theme.doc.name), "${dir.name} must declare name, version and author")
            assertNotNull(theme.meta.repository, dir.name)
            ThemeGallery.render(theme, dark = true, out = File(out, "${dir.name}-dark.png"))
            ThemeGallery.render(theme, dark = false, out = File(out, "${dir.name}-light.png"))
        }
    }
}
