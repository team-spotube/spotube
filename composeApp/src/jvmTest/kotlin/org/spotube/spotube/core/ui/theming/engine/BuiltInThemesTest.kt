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
import androidx.compose.ui.unit.dp
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

    private fun modern(params: Map<String, String>, dark: Boolean = false): CompiledTheme {
        val theme = BuiltInThemes.loadModern("build/tmp/builtin-cache")
        return compileTheme(
            doc = theme.doc, assets = theme.assets, parameters = params, env = ThemeEnv(dark, compact = false),
            baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, dark),
            baseComponents = MillenniumDarkThemeComponents, parseProblems = theme.problems,
        )
    }

    @Test
    fun modernDeclaresItsUserParameters() {
        val doc = BuiltInThemes.loadModern("build/tmp/builtin-cache").doc
        assertEquals(setOf("accent", "radius", "density"), doc.parameters.keys)
        assertEquals("zinc", (doc.parameters.getValue("accent").default as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(6, doc.parameters.getValue("radius").default?.let { (it as kotlinx.serialization.json.JsonPrimitive).content.toInt() })
        // Every accent choice carries a swatch for the UI.
        assertEquals(doc.parameters.getValue("accent").options.toSet(), doc.parameters.getValue("accent").optionColors.keys)
    }

    @Test
    fun accentParameterRecoloursThePrimaryAction() {
        assertEquals(androidx.compose.ui.graphics.Color(0xFF18181B), modern(emptyMap()).colors.accent)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFAFAFA), modern(emptyMap(), dark = true).colors.accent)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF2563EB), modern(mapOf("accent" to "blue")).colors.accent)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF3B82F6), modern(mapOf("accent" to "blue"), dark = true).colors.accent)
        assertEquals(androidx.compose.ui.graphics.Color.White, modern(mapOf("accent" to "blue")).colors.onAccent)
    }

    @Test
    fun radiusParameterDrivesEveryShape() {
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(6.dp), modern(emptyMap()).shapes.medium)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modern(mapOf("radius" to "12")).shapes.medium)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modern(mapOf("radius" to "12")).components.cardTheme.states.default.shape)
        // Never negative: small = max(0, radius - 2).
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(0.dp), modern(mapOf("radius" to "0")).shapes.small)
        assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modern(mapOf("radius" to "99")).shapes.medium) // clamped to max
    }

    @Test
    fun densityParameterDrivesPadding() {
        fun hPad(density: String) = modern(mapOf("density" to density)).components.buttonTheme.primary.default.padding
            .calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
        assertEquals(12.dp, hPad("compact"))
        assertEquals(16.dp, hPad("comfortable"))
        assertEquals(20.dp, hPad("spacious"))
        assertEquals(44.dp, modern(mapOf("density" to "compact")).components.listTileTheme.states.default.minimumHeight)
        assertEquals(68.dp, modern(mapOf("density" to "spacious")).components.listTileTheme.states.default.minimumHeight)
    }

    @Test
    fun everyParameterChoiceCompilesWithoutWarnings() {
        val doc = BuiltInThemes.loadModern("build/tmp/builtin-cache").doc
        for (accent in doc.parameters.getValue("accent").options) for (density in doc.parameters.getValue("density").options) {
            for (dark in listOf(true, false)) {
                assertEquals(emptyList(), modern(mapOf("accent" to accent, "density" to density, "radius" to "10"), dark).problems, "$accent/$density/dark=$dark")
            }
        }
    }
}
