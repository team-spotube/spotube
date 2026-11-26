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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumListTile
import org.spotube.millennium.composables.MillenniumSwitch
import org.spotube.spotube.core.ui.theming.engine.BuiltInThemes
import org.spotube.millennium.theme.resolve
import org.spotube.spotube.core.ui.theming.engine.LoadedTheme
import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import kotlin.test.Test

/** Opt-in dev benchmark (set THEME_BENCH=1; prints numbers): frame time of a scrolling track-list-like screen per theme. */
class ThemePerformanceTest {
    private fun frameMillis(theme: LoadedTheme?, frames: Int = 90): Double {
        var total = 0L
        ImageComposeScene(1000, 700) {
            SpotubeTheme(UserSettings(theme = Theme.LIGHT), theme = theme) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items((0 until 400).toList()) { i ->
                        MillenniumListTile(
                            title = { Text("Track number $i") },
                            subtitle = { Text("Some artist") },
                            leading = { AppIcon(Iconsax.IconsaxMusic) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MillenniumSwitch(checked = i % 2 == 0, onCheckedChange = {})
                                    MillenniumIconButton(Iconsax.IconsaxHeart, "like", {}, style = MillenniumIconButtonStyle.Quiet, modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                            selected = i == 3,
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }.use { scene ->
            repeat(20) { scene.render(it * 16_000_000L) }
            var t = 400_000_000L
            repeat(frames) {
                scene.sendPointerEvent(PointerEventType.Scroll, Offset(500f, 300f), scrollDelta = Offset(0f, 3f))
                val start = System.nanoTime()
                scene.render(t)
                total += System.nanoTime() - start
                t += 16_000_000L
            }
        }
        return total / 1e6 / frames
    }

    @Test
    fun printFrameTimes() {
        if (System.getenv("THEME_BENCH") == null) return // opt-in: THEME_BENCH=1 ./gradlew :composeApp:jvmTest --tests '*ThemePerformanceTest*' -i
        val modern = BuiltInThemes.loadModern("build/tmp/perf-cache")
        repeat(2) { frameMillis(null, 20) } // JIT warm-up
        val stock = frameMillis(null)
        val themed = frameMillis(modern)
        val compileMs = run {
            val doc = modern.doc
            val times = (0 until 15).map {
                val s = System.nanoTime()
                org.spotube.spotube.core.ui.theming.engine.compileTheme(
                    doc = doc, assets = modern.assets, env = org.spotube.spotube.core.ui.theming.engine.ThemeEnv(false, false),
                    baseColors = org.spotube.millennium.theme.MillenniumColorSchemes().resolve(org.spotube.millennium.theme.MillenniumPalette.Blue, false),
                    baseComponents = org.spotube.millennium.theme.MillenniumLightThemeComponents,
                )
                (System.nanoTime() - s) / 1e6
            }
            times.drop(5).average()
        }
        println("PERF frame stock=%.1fms modern=%.1fms compileTheme=%.1fms".format(stock, themed, compileMs))
    }
}
