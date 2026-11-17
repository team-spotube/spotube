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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.composeunstyled.Text
import org.jetbrains.skia.EncodedImageFormat
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumCheckbox
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumProgress
import org.spotube.millennium.composables.MillenniumBottomNavigationBar
import org.spotube.millennium.composables.MillenniumBottomNavigationItem
import org.spotube.millennium.composables.MillenniumSidebar
import org.spotube.millennium.composables.MillenniumSidebarContent
import org.spotube.millennium.composables.MillenniumSidebarItem
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSlider
import org.spotube.millennium.composables.MillenniumSwitch
import org.spotube.millennium.composables.MillenniumTextField
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.millenniumPanelBrush
import org.spotube.millennium.theme.millenniumSurface
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.core.ui.theming.engine.loadTheme
import org.spotube.spotube.core.ui.theming.engine.readThemeArchive
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHome
import org.spotube.spotube.resources.iconsax.IconsaxPlay
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A dev tool + CI check: packs every `themes/<name>/` folder, loads it and renders a gallery PNG. */
internal object ThemeGallery {
    fun pack(dir: File): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            dir.walkTopDown().filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry(f.relativeTo(dir).invariantSeparatorsPath)); z.write(f.readBytes()); z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    fun load(dir: File, problems: MutableSet<String> = LinkedHashSet()): LoadedTheme? =
        loadTheme(readThemeArchive(pack(dir)), "build/tmp/theme-gallery-cache", problems)

    fun render(theme: LoadedTheme?, dark: Boolean, out: File, parameters: Map<String, String> = emptyMap()) {
        out.parentFile.mkdirs()
        ImageComposeScene(1360, 820) {
            val settings = UserSettings(theme = if (dark) Theme.DARK else Theme.LIGHT)
            SpotubeTheme(settings, org.spotube.spotube.core.ui.theming.engine.PrecompiledTheme.create(theme, settings.palette, parameters)) {
                Gallery()
            }
        }.use { scene ->
            repeat(4) { scene.render(it * 16_000_000L) }
            val bytes = scene.render(100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
            out.writeBytes(bytes)
        }
    }

    @Composable
    private fun Gallery() {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize().appBackground()) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { Controls() }
            NavSample(Modifier.width(340.dp).fillMaxSize())
        }
    }

    /** A sidebar over a bottom dock, the way the app lays them out, with the second item selected. */
    @Composable
    private fun NavSample(modifier: Modifier) {
        Column(modifier) {
            MillenniumSidebar(
                modifier = Modifier.weight(1f).fillMaxWidth(), collapsed = false, onCollapsedChange = {},
            ) {
                MillenniumSidebarContent {
                    MillenniumSidebarItem(label = "Home", onClick = {}, icon = Iconsax.IconsaxHome)
                    MillenniumSidebarItem(label = "Search", onClick = {}, selected = true, icon = Iconsax.IconsaxSearch)
                    MillenniumSidebarItem(label = "Playlists", onClick = {}, icon = Iconsax.IconsaxHeart)
                    MillenniumSidebarItem(label = "Settings", onClick = {}, icon = Iconsax.IconsaxSetting2)
                }
            }
            MillenniumBottomNavigationBar(windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)) {
                listOf("Home" to Iconsax.IconsaxHome, "Search" to Iconsax.IconsaxSearch, "Library" to Iconsax.IconsaxHeart).forEachIndexed { i, (label, icon) ->
                    MillenniumBottomNavigationItem(
                        selected = i == 1, onClick = {}, contentDescription = label,
                        icon = { AppIcon(icon) }, label = { Text(label) },
                    )
                }
            }
        }
    }

    @Composable
    private fun Controls() {
        val c = MillenniumTheme.colors
        val field = rememberTextFieldState("Search the library")
        val checked = remember { mutableStateOf(true) }
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MillenniumButton(onClick = {}) { Text("Primary") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Secondary) { Text("Secondary") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Quiet) { Text("Quiet") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Danger) { Text("Danger") }
                MillenniumButton(onClick = {}, enabled = false) { Text("Disabled") }
            }
            // Icons placed inside buttons must follow each variant's foreground (readable on the fill).
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MillenniumButton(onClick = {}) { AppIcon(Iconsax.IconsaxPlay); Text("Play") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Secondary) { AppIcon(Iconsax.IconsaxShuffle); Text("Shuffle") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Quiet) { AppIcon(Iconsax.IconsaxHeart); Text("Like") }
                MillenniumButton(onClick = {}, style = MillenniumButtonStyle.Danger) { AppIcon(Iconsax.IconsaxTrash); Text("Delete") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // The content-lambda form used by the app's player controls: the icon must follow the fill's foreground.
                MillenniumIconButton(onClick = {}, style = MillenniumIconButtonStyle.Primary) { AppIcon(Iconsax.IconsaxPlay) }
                MillenniumIconButton(onClick = {}, style = MillenniumIconButtonStyle.Danger) { AppIcon(Iconsax.IconsaxTrash) }
                MillenniumIconButton(Iconsax.IconsaxHome, "home", {}, style = MillenniumIconButtonStyle.Primary)
                MillenniumIconButton(Iconsax.IconsaxSearch, "search", {}, style = MillenniumIconButtonStyle.Secondary)
                MillenniumIconButton(Iconsax.IconsaxSetting2, "magic", {}, style = MillenniumIconButtonStyle.Quiet)
                MillenniumIconButton(Iconsax.IconsaxHome, "danger", {}, style = MillenniumIconButtonStyle.Danger)
                MillenniumSwitch(checked = true, onCheckedChange = {}, label = "On")
                MillenniumSwitch(checked = false, onCheckedChange = {}, label = "Off")
                MillenniumCheckbox(checked = checked.value, onCheckedChange = { checked.value = it }, label = "Checked")
                MillenniumCheckbox(checked = false, onCheckedChange = {}, label = "Unchecked")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MillenniumSlider(value = 0.6f, onValueChange = {}, label = "Volume", modifier = Modifier.width(300.dp))
                MillenniumProgress(progress = 0.7f, modifier = Modifier.width(300.dp))
            }
            MillenniumTextField(state = field, label = "Search", modifier = Modifier.fillMaxWidth())
            // App-style panel: built directly with millenniumSurface (like the settings cards), no component theme.
            Column(
                Modifier.fillMaxWidth()
                    .millenniumSurface(
                        background = millenniumPanelBrush(c), shape = AppTheme.shapes.medium,
                        borderColor = c.border, borderWidth = 1.dp, shadowElevation = 6.dp,
                    )
                    .padding(14.dp),
            ) { Text("App-built panel (millenniumSurface, elevation 6)", color = c.text) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MillenniumCard(modifier = Modifier.weight(1f)) {
                    Text("Card title", color = c.text)
                    Text("Cards sit on the surface brush.", color = c.muted)
                }
                Column(Modifier.weight(1f)) {
                    MillenniumListTile(title = { Text("Now playing") }, subtitle = { Text("Selected row") }, selected = true, onClick = {})
                    MillenniumSeparator()
                    MillenniumListTile(title = { Text("Next track") }, subtitle = { Text("Normal row") }, onClick = {})
                }
            }
        }
    }
}
