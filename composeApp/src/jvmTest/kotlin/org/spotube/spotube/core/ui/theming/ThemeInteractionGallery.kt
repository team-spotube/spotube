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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.composeunstyled.Text
import org.jetbrains.skia.EncodedImageFormat
import org.spotube.millennium.composables.MillenniumBottomNavigationBar
import org.spotube.millennium.composables.MillenniumBottomNavigationItem
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumDialog
import org.spotube.millennium.composables.MillenniumDialogBody
import org.spotube.millennium.composables.MillenniumDialogClose
import org.spotube.millennium.composables.MillenniumDialogFooter
import org.spotube.millennium.composables.MillenniumDialogHeader
import org.spotube.millennium.composables.MillenniumDialogTitle
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuContent
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumDropdownMenuItemIcon
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSidebar
import org.spotube.millennium.composables.MillenniumSidebarContent
import org.spotube.millennium.composables.MillenniumSidebarItem
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHome
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import org.spotube.spotube.resources.iconsax.IconsaxShare
import org.spotube.spotube.resources.iconsax.IconsaxTrash
import java.io.File

/** Renders navigation surfaces with the pointer resting on one element, to review hover/selected looks. */
internal object ThemeInteractionGallery {
    private val bounds = HashMap<String, Rect>()

    private fun Modifier.track(key: String) = onGloballyPositioned { bounds[key] = it.boundsInWindow() }

    fun render(
        theme: LoadedTheme?, dark: Boolean, hover: String?, out: File, width: Int, height: Int,
        /** Popup content reports bounds relative to its own origin; this shifts them into the scene. */
        hoverOffset: Offset = Offset.Zero,
        content: @Composable () -> Unit,
    ) {
        out.parentFile.mkdirs()
        bounds.clear()
        ImageComposeScene(width, height) {
            SpotubeTheme(UserSettings(theme = if (dark) Theme.DARK else Theme.LIGHT), theme = theme) {
                Box(Modifier.fillMaxSize().appBackground()) { content() }
            }
        }.use { scene ->
            repeat(3) { scene.render(it * 16_000_000L) }
            hover?.let { key ->
                val r = bounds[key] ?: error("no element '$key'; have ${bounds.keys}")
                val c = r.center + hoverOffset
                scene.sendPointerEvent(PointerEventType.Move, Offset(c.x - 8f, c.y))
                scene.sendPointerEvent(PointerEventType.Move, c)
            }
            repeat(12) { scene.render(100_000_000L + it * 16_000_000L) }
            out.writeBytes(scene.render(400_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    /** Sidebar (left) and dock (bottom), the second sidebar item selected. */
    @Composable
    fun Navigation() {
        Row(Modifier.fillMaxSize()) {
            MillenniumSidebar(modifier = Modifier.width(240.dp).fillMaxHeight(), collapsed = false, onCollapsedChange = {}) {
                MillenniumSidebarContent {
                    MillenniumSidebarItem(label = "Home", onClick = {}, icon = Iconsax.IconsaxHome, modifier = Modifier.track("side0"))
                    MillenniumSidebarItem(label = "Search", onClick = {}, selected = true, icon = Iconsax.IconsaxSearch, modifier = Modifier.track("side1"))
                    MillenniumSidebarItem(label = "Library", onClick = {}, icon = Iconsax.IconsaxMusicLibrary, modifier = Modifier.track("side2"))
                    MillenniumSidebarItem(label = "Settings", onClick = {}, icon = Iconsax.IconsaxSetting2, modifier = Modifier.track("side3"))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                MillenniumBottomNavigationBar(windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)) {
                    dockItem("Home", Iconsax.IconsaxHome, false, "dock0")
                    dockItem("Search", Iconsax.IconsaxSearch, true, "dock1")
                    dockItem("Library", Iconsax.IconsaxMusicLibrary, false, "dock2")
                    dockItem("Settings", Iconsax.IconsaxSetting2, false, "dock3")
                }
            }
        }
    }

    @Composable
    private fun RowScope.dockItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, key: String) {
        MillenniumBottomNavigationItem(
            selected = selected, onClick = {}, modifier = Modifier.track(key), contentDescription = label,
            icon = { MillenniumIcon(icon) }, label = { Text(label) },
        )
    }

    @Composable
    fun Menu() {
        Box(Modifier.padding(24.dp)) {
            MillenniumDropdownMenu(expanded = true, onExpandedChange = {}, anchor = {
                MillenniumButton(onClick = {}) { Text("Open menu") }
            }) {
                MillenniumDropdownMenuContent {
                    MillenniumDropdownMenuItem(onClick = {}, leading = { MillenniumDropdownMenuItemIcon(Iconsax.IconsaxHeart) }, modifier = Modifier.track("menu0")) { Text("Favorite") }
                    MillenniumDropdownMenuItem(onClick = {}, selected = true, leading = { MillenniumDropdownMenuItemIcon(Iconsax.IconsaxShare) }, modifier = Modifier.track("menu1")) { Text("Share") }
                    MillenniumDropdownMenuItem(onClick = {}, leading = { MillenniumDropdownMenuItemIcon(Iconsax.IconsaxTrash) }, modifier = Modifier.track("menu2")) { Text("Delete") }
                }
            }
        }
    }

    @Composable
    fun DialogView() {
        MillenniumDialog(visible = true, onDismissRequest = {}) {
            MillenniumDialogHeader {
                MillenniumDialogTitle("Install theme")
                Box(Modifier.track("close")) { MillenniumDialogClose() }
            }
            MillenniumDialogBody { Text("Only install themes from people you trust.") }
            MillenniumDialogFooter { MillenniumButton(onClick = {}) { Text("Install") } }
        }
    }

    private fun themeCard(id: String, palette: org.spotube.millennium.theme.MillenniumPalette, selected: Boolean) =
        org.spotube.spotube.modules.settings.ThemeCardUi(
            id = id, name = "", version = null, author = "Spotube Team", description = null, logo = null,
            isMillennium = true, swatchColor = if (palette == org.spotube.millennium.theme.MillenniumPalette.Orange) "#9A3412" else if (palette == org.spotube.millennium.theme.MillenniumPalette.Lime) "#3F6212" else "#005CB9", selected = selected, repository = null, bugs = null, homepage = null, contact = null,
            license = "AGPL-3.0", licenseText = null, fontFiles = 0, icons = 0, problems = emptyList(),
        )

    /** The Themes rows: first selected, hover on the second. */
    @Composable
    fun ThemeRows() {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            org.spotube.spotube.modules.settings.sections.ThemeRow(
                themeCard("a", org.spotube.millennium.theme.MillenniumPalette.Blue, true), onOpen = {}, onSelect = {},
            )
            Box(Modifier.track("row1")) {
                org.spotube.spotube.modules.settings.sections.ThemeRow(
                    themeCard("b", org.spotube.millennium.theme.MillenniumPalette.Orange, false), onOpen = {}, onSelect = {},
                )
            }
            org.spotube.spotube.modules.settings.sections.ThemeRow(
                themeCard("c", org.spotube.millennium.theme.MillenniumPalette.Lime, false), onOpen = {}, onSelect = {},
            )
        }
    }
}
