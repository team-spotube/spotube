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

package org.spotube.spotube.modules.settings

import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.core.ui.theming.engine.BuiltInThemes
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeSelectionTest {
    @Test
    fun legacyAndUnknownSelectionsMeanMillennium() {
        for (legacy in listOf(null, "builtin:blue", "builtin:orange", "builtin:lime", "gone-theme", "")) {
            assertEquals(BuiltInThemes.MILLENNIUM_ID, normalizeSelectedThemeId(legacy, listOf("mine")), "'$legacy'")
        }
    }

    @Test
    fun knownSelectionsAreKept() {
        assertEquals(BuiltInThemes.MODERN_ID, normalizeSelectedThemeId(BuiltInThemes.MODERN_ID, emptyList()))
        assertEquals(BuiltInThemes.MILLENNIUM_ID, normalizeSelectedThemeId(BuiltInThemes.MILLENNIUM_ID, emptyList()))
        assertEquals("mine", normalizeSelectedThemeId("mine", listOf("mine", "other")))
    }

    @Test
    fun accentSwatchesAreTheLightPaletteAccents() {
        assertEquals("#005CB9", MillenniumPalette.Blue.accentHex())
        assertEquals("#9A3412", MillenniumPalette.Orange.accentHex())
        assertEquals("#3F6212", MillenniumPalette.Lime.accentHex())
    }
}
