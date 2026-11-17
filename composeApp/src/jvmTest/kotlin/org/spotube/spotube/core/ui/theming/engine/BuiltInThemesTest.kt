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

package org.spotube.spotube.core.ui.theming.engine

import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.resources.iconsax.CarbonGithubLogo
import org.spotube.spotube.resources.iconsax.IconsaxHome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BuiltInThemesTest {
    @Test
    fun modernLoadsAndCompilesWithoutWarningsInBothAppearances() {
        val theme = BuiltInThemes.loadModern("build/tmp/builtin-cache")
        assertEquals(emptySet(), theme.problems)
        for (dark in listOf(true, false)) {
            val compiled = compileTheme(
                doc = theme.doc, assets = theme.assets, env = ThemeEnv(dark, compact = false),
                baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, dark),
                baseComponents = MillenniumDarkThemeComponents, parseProblems = theme.problems,
            )
            // A typo in any property name shows up here as "unknown property".
            assertEquals(emptyList(), compiled.problems, "dark=$dark")
            assertNotEquals(MillenniumDarkThemeComponents, compiled.components)
        }
    }

    @Test
    fun idsAreRecognised() {
        assertEquals(true, BuiltInThemes.isBuiltIn(BuiltInThemes.MODERN_ID))
        assertEquals(false, BuiltInThemes.isBuiltIn("builtin:blue"))
        assertEquals(false, BuiltInThemes.isBuiltIn(null))
    }

    @Test
    fun modernSwapsIconsaxForLucideAndTurnsOffTheGloss() {
        val theme = BuiltInThemes.loadModern("build/tmp/builtin-cache")
        val icons = theme.assets.icons
        assertEquals(true, icons.isNotEmpty())
        assertEquals(true, icons.values.all { it is ThemeIcon.Vector })
        assertEquals(true, org.spotube.spotube.resources.iconsax.Iconsax.IconsaxHome.name in icons)
        // Brand artwork has no Lucide counterpart and must keep its own icon.
        assertEquals(false, org.spotube.spotube.resources.iconsax.Iconsax.CarbonGithubLogo.name in icons)
        val compiled = compileTheme(
            doc = theme.doc, assets = theme.assets, env = ThemeEnv(dark = true, compact = false),
            baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, true),
            baseComponents = MillenniumDarkThemeComponents, parseProblems = theme.problems,
        )
        assertEquals(false, compiled.interaction.enabled)
        assertEquals(icons.keys, compiled.icons.keys)
    }
}
