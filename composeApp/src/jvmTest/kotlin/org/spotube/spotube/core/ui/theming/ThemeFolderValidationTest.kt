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

import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.core.ui.theming.engine.PrecompiledTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeVarType
import org.spotube.spotube.core.ui.theming.engine.ThemeVarDecl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Validates every theme folder in `composeApp/themes` and, when `THEMES_DIR` is set, in that directory (the separate
 * themes repository): each must load without warnings and declare its metadata, every parameter choice (all enum
 * options, number extremes, both bools) must compile warning-free in light and dark, and light/dark gallery PNGs are
 * written to `build/theme-gallery/external` for review.
 */
class ThemeFolderValidationTest {
    private fun themeDirs(): List<File> = listOfNotNull(File("themes"), System.getenv("THEMES_DIR")?.let(::File))
        .filter { it.isDirectory }
        .flatMap { root -> root.listFiles { f -> f.isDirectory && File(f, "theme.json").exists() }.orEmpty().toList() }
        .sortedBy { it.name }

    /** The values worth testing for [d]: every option, the range ends, both booleans. */
    private fun choices(d: ThemeVarDecl): List<String> = when (d.type) {
        ThemeVarType.Enum -> d.options
        ThemeVarType.Bool -> listOf("true", "false")
        ThemeVarType.Number -> listOfNotNull(d.min, d.max).map { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
        ThemeVarType.Color -> d.presets
        ThemeVarType.String -> emptyList()
    }

    @Test
    fun everyThemeFolderLoadsAndEveryParameterChoiceCompiles() {
        val out = File("build/theme-gallery/external").apply { mkdirs() }
        for (dir in themeDirs()) {
            val problems = LinkedHashSet<String>()
            val theme = ThemeGallery.load(dir, problems)
            assertNotNull(theme, dir.name)
            assertEquals(emptySet(), problems, "${dir.name}: load problems")
            assertEquals(dir.name, theme.id, "${dir.name}: folder name must equal the theme id")
            assertEquals(null, theme.meta.missingRequired(theme.doc.name), "${dir.name} must declare name, version and author")
            assertNotNull(theme.meta.repository, "${dir.name}: repository")
            assertNotNull(theme.meta.license, "${dir.name}: license")
            assertNotNull(theme.logo, "${dir.name}: logo")

            val palette = MillenniumPalette.Blue
            for ((name, decl) in theme.doc.parameters) {
                for (value in choices(decl)) {
                    val pre = PrecompiledTheme.create(theme, palette, mapOf(name to value))
                    for (dark in listOf(false, true)) {
                        assertEquals(emptyList(), pre.forAppearance(dark).problems, "${dir.name}: $name=$value dark=$dark")
                    }
                }
            }
            ThemeGallery.render(theme, dark = false, out = File(out, "${dir.name}-light.png"))
            ThemeGallery.render(theme, dark = true, out = File(out, "${dir.name}-dark.png"))
        }
    }
}
