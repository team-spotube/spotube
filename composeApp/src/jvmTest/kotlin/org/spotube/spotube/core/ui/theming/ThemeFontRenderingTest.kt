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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.use
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.core.ui.theming.engine.ThemeIcon
import org.spotube.spotube.core.ui.theming.engine.ThemePackage
import org.spotube.spotube.core.ui.theming.engine.loadTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Renders a real font + glyph icon through the whole pipeline (Skia included). */
class ThemeFontRenderingTest {
    private fun systemFont(): ByteArray? = File("/usr/share/fonts").takeIf { it.exists() }
        ?.walkTopDown()?.firstOrNull { it.isFile && it.extension.lowercase() == "ttf" && it.length() < 4_000_000 }
        ?.readBytes()

    private fun render(theme: LoadedTheme?, onFrame: (FontFamily?) -> Unit): ByteArray {
        var bytes = ByteArray(0)
        ImageComposeScene(300, 120) {
            SpotubeTheme(UserSettings(theme = Theme.DARK), theme = theme) {
                val family = AppTheme.typography.bodyMedium.fontFamily
                SideEffect { onFrame(family) }
                BasicText("Hamburgefonstiv", style = AppTheme.typography.titleLarge)
                AppIcon(Iconsax.IconsaxHome)
            }
        }.use { scene ->
            repeat(3) { scene.render(it * 16_000_000L) }
            bytes = scene.render(64_000_000L).encodeToData()!!.bytes
        }
        return bytes
    }

    @Test
    fun customFontAndGlyphIconRenderThroughSkia() {
        val font = systemFont() ?: return // no system fonts on this machine
        val problems = LinkedHashSet<String>()
        val theme = loadTheme(
            ThemePackage(
                """{"id":"e2e","assets":{"fonts":{"Real":[{"src":"f.ttf"}]},
                     "icons":{"IconsaxHome":{"glyph":"H","font":"Real"}}},
                    "tokens":{"type":{"family":"Real"}}}""",
                mapOf("f.ttf" to font),
            ),
            "build/tmp/theme-e2e-cache", problems,
        )!!
        assertEquals(emptySet(), problems)
        assertTrue(theme.assets.icons.getValue("IconsaxHome") is ThemeIcon.Glyph)

        var family: FontFamily? = null
        val themed = render(theme) { family = it }
        val stock = render(null) { }
        assertEquals(theme.assets.fontFamilies.getValue("Real"), family)
        assertNotEquals(stock.toList(), themed.toList(), "themed output should differ from stock")
    }
}
