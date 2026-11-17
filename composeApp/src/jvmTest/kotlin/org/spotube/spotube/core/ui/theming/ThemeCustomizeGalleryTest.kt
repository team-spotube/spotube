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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.engine.BuiltInThemes
import org.spotube.spotube.core.ui.theming.engine.PrecompiledTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.ThemeCardUi
import org.spotube.spotube.modules.settings.ThemeParamOptionUi
import org.spotube.spotube.modules.settings.ThemeParamUi
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.sections.ThemeCustomize
import org.spotube.spotube.core.ui.theming.engine.ThemeVarType
import java.io.File
import kotlin.test.Test

/** Dev tool: renders the "Customize" section of a theme's details (Modern's real parameters) for review. */
class ThemeCustomizeGalleryTest {
    private fun card(accent: String) = ThemeCardUi(
        id = "builtin:modern", name = "", version = null, author = "Spotube Team", description = null, logo = null,
        isModern = true, selected = true, repository = null, bugs = null, homepage = null, contact = null,
        license = null, licenseText = null, fontFiles = 0, icons = 0, problems = emptyList(), hasCustomParameters = true,
        parameters = listOf(
            ThemeParamUi(
                "accent", "Accent color", null, ThemeVarType.Enum, accent, "zinc", 0f, 100f, 1f,
                listOf("zinc" to "#18181b", "blue" to "#2563eb", "green" to "#16a34a", "orange" to "#ea580c", "rose" to "#e11d48", "violet" to "#7c3aed")
                    .map { (v, c) -> ThemeParamOptionUi(v, v.replaceFirstChar(Char::uppercase), c) },
                emptyList(),
            ),
            ThemeParamUi("radius", "Corner radius", null, ThemeVarType.Number, "10", "6", 0f, 16f, 1f, emptyList(), emptyList()),
            ThemeParamUi(
                "density", "Density", "Spacing of buttons and rows.", ThemeVarType.Enum, "comfortable", "comfortable", 0f, 100f, 1f,
                listOf("Compact", "Comfortable", "Spacious").map { ThemeParamOptionUi(it.lowercase(), it, null) }, emptyList(),
            ),
            ThemeParamUi("round", "Rounded avatars", null, ThemeVarType.Bool, "true", "false", 0f, 100f, 1f, emptyList(), emptyList()),
        ),
    )

    @Test
    fun rendersCustomizeSection() {
        val out = File("build/theme-gallery").apply { mkdirs() }
        for (dark in listOf(false, true)) {
            val settings = UserSettings(theme = if (dark) Theme.DARK else Theme.LIGHT)
            val modern = PrecompiledTheme.create(BuiltInThemes.loadModern("build/tmp/theme-gallery-cache"), settings.palette)
            ImageComposeScene(460, 420) {
                SpotubeTheme(settings, modern) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(MillenniumTheme.colors.background).padding(16.dp)) {
                        ThemeCustomize(card("blue"), onSet = { _, _ -> }, onReset = {})
                    }
                }
            }.use { scene ->
                repeat(4) { scene.render(it * 16_000_000L) }
                File(out, "customize-${if (dark) "dark" else "light"}.png").writeBytes(scene.render(100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
