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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** In-house component theming: slots resolve per state at compile time, defaults keep today's look. */
class AppComponentsTest {
    private fun compile(json: String?, dark: Boolean = true): CompiledTheme {
        val problems = LinkedHashSet<String>()
        return compileTheme(
            doc = json?.let { parseThemeDocument(it, problems) }, env = ThemeEnv(dark, false),
            baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, dark),
            baseComponents = MillenniumDarkThemeComponents, parseProblems = problems,
        )
    }

    private fun theme(components: String, extra: String = "") =
        """{"id":"t","name":"t","version":"1.0.0","author":"a","schema":"1.0.0"$extra,"components":{$components}}"""

    @Test
    fun everySlotIsRegisteredOnceAndResolvesForEveryState() {
        assertEquals(AppSlots.all.size, AppSlots.all.toSet().size)
        val t = compile(null).appComponents
        for (slot in AppSlots.all) for (st in AppState.entries) t[slot][st]
    }

    @Test
    fun noThemeAndAThemeThatIgnoresTheComponentGiveTheStockLook() {
        val stock = compile(null).appComponents[AppSlots.MediaCardRoot]
        val other = compile(theme("\"button\":{\"base\":{\"shape\":4}}")).appComponents[AppSlots.MediaCardRoot]
        for (st in AppState.entries) assertEquals(stock[st].shape, other[st].shape)
        assertEquals(160.dp, stock[AppState.Default].width)
        assertEquals(0.985f, stock[AppState.Pressed].scale)
        assertEquals(4.dp, stock[AppState.Hovered].shadowElevation)
    }

    @Test
    fun statesAndVariantsCascadeLikeMillenniumComponents() {
        val t = compile(
            theme(
                """"mediaCard":{"variants":{"root":{"base":{"width":200,"shape":6},
                    "states":{"hovered":{"shadowElevation":9,"borderColor":"#FF0000"}}},
                    "info":{"base":{"spacing":2}}}}""",
            ),
        )
        val root = t.appComponents[AppSlots.MediaCardRoot]
        assertEquals(200.dp, root[AppState.Default].width)
        assertEquals(200.dp, root[AppState.Hovered].width)
        assertEquals(9.dp, root[AppState.Hovered].shadowElevation)
        assertEquals(0.dp, root[AppState.Default].shadowElevation)
        assertEquals(Color(0xFFFF0000), root[AppState.Hovered].borderColor)
        assertEquals(RoundedCornerShape(6.dp), root[AppState.Pressed].shape)
        assertEquals(2.dp, t.appComponents[AppSlots.MediaCardInfo][AppState.Default].spacing)
        // A sibling slot of the same component the theme did not touch keeps its defaults.
        assertEquals(34.dp, t.appComponents[AppSlots.MediaCardArtwork][AppState.Default].actionSize)
        assertTrue(t.problems.isEmpty(), t.problems.toString())
    }

    @Test
    fun rulesVarsAndPaletteReferencesWork() {
        val t = compile(
            theme(
                """"floatingPlayer":{"vars":{"gap":{"type":"number","default":3}},
                    "variants":{"artwork":{"base":{"size":"{vars.gap}","background":["alpha","{palette.accent}",0.5]}}}}""",
            ),
        )
        val art = t.appComponents[AppSlots.FloatingPlayerArtwork][AppState.Default]
        assertEquals(3.dp, art.size)
        assertEquals(t.colors.accent.copy(alpha = 0.5f).value, art.background.value)
    }

    @Test
    fun themeDefaultsDoNotRestyleInHouseComponents() {
        val t = compile(theme("\"card\":{\"base\":{\"shape\":2}}", ",\"defaults\":{\"shape\":99,\"borderWidth\":7}"))
        val stock = compile(null).appComponents[AppSlots.MediaCardRoot][AppState.Default]
        val root = t.appComponents[AppSlots.MediaCardRoot][AppState.Default]
        assertEquals(stock.shape, root.shape)
        assertEquals(stock.borderWidth, root.borderWidth)
    }

    @Test
    fun typosAndWrongTypesAreReportedAndKeepTheDefault() {
        val t = compile(theme("\"mediaCard\":{\"variants\":{\"root\":{\"base\":{\"widht\":1,\"shadowElevation\":\"big\"}}}}"))
        assertTrue(t.problems.any { "components.mediaCard.root" in it && "widht" in it }, t.problems.toString())
        assertTrue(t.problems.any { "shadowElevation" in it && "expected" in it }, t.problems.toString())
        assertEquals(0.dp, t.appComponents[AppSlots.MediaCardRoot][AppState.Default].shadowElevation)
    }

    @Test
    fun lightAndDarkResolveTheirOwnColours() {
        val light = compile(null, dark = false).appComponents[AppSlots.FloatingPlayerRoot][AppState.Default].borderColor
        val dark = compile(null, dark = true).appComponents[AppSlots.FloatingPlayerRoot][AppState.Default].borderColor
        assertNotEquals(light, dark)
    }

    @Test
    fun lookupIsAllocationFreeAndStable() {
        val t = compile(null).appComponents
        assertSame(t[AppSlots.MediaCardRoot][AppState.Hovered], t[AppSlots.MediaCardRoot][AppState.Hovered])
    }
}
