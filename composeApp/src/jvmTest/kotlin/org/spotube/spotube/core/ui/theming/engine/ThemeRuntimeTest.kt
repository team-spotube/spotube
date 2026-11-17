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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.UserSettingsSource
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The theme is compiled once per selection, off the main thread, and nothing else re-triggers it. */
class ThemeRuntimeTest {
    private val base = "build/tmp/theme-runtime-test".toPath()

    @AfterTest
    fun cleanup() {
        if (FileSystem.SYSTEM.exists(base)) FileSystem.SYSTEM.deleteRecursively(base)
    }

    private class FakeSettings(initial: UserSettings) : UserSettingsSource {
        override val userSettings: MutableStateFlow<UserSettings> = MutableStateFlow(initial)
    }

    private fun runtime(settings: FakeSettings): ThemeRuntime {
        val repo = ThemePackageRepository("$base/data", "$base/cache")
        runBlocking { withTimeout(10_000) { repo.ready.first { it } } }
        return ThemeRuntime(settings, repo)
    }

    @Test
    fun selectingATheme_compilesOnce_andUnrelatedSettingsDoNotRecompile() = runBlocking {
        val settings = FakeSettings(UserSettings())
        val rt = runtime(settings)
        val stock = rt.precompiled.value
        assertEquals(null, stock.loaded)

        settings.userSettings.value = settings.userSettings.value.copy(selectedThemeId = BuiltInThemes.MODERN_ID)
        val modern = withTimeout(10_000) { rt.precompiled.first { it.loaded != null } }
        assertEquals("Modern", modern.loaded?.name)

        // Changing something the theme doesn't depend on must not produce a new compiled theme.
        settings.userSettings.value = settings.userSettings.value.copy(jamParticipantName = "x")
        settings.userSettings.value = settings.userSettings.value.copy(jamParticipantName = "y")
        delay(300)
        assertSame(modern, rt.precompiled.value)

        // Changing the palette does (colours derive from it).
        settings.userSettings.value = settings.userSettings.value.copy(palette = MillenniumPalette.Orange)
        val orange = withTimeout(10_000) { rt.precompiled.first { it.palette == MillenniumPalette.Orange } }
        assertNotSame(modern, orange)
    }

    @Test
    fun bothAppearancesArePrecompiled_andPicksAreStable() {
        val modern = PrecompiledTheme.create(BuiltInThemes.loadModern("$base/cache"), MillenniumPalette.Blue)
        assertSame(modern.forAppearance(true), modern.forAppearance(true))
        assertNotSame(modern.forAppearance(true), modern.forAppearance(false))
        assertTrue(modern.forAppearance(true).colors.background != modern.forAppearance(false).colors.background)
        assertEquals(false, modern.needsLiveContext)
        assertEquals(false, modern.usesItemContext)
    }

    @Test
    fun itemVariantsAreMemoized() {
        val doc = """{"components":{"listTile":{"rules":[{"when":{"var":"ctx.item.current"},"set":{"borderWidth":2}}]}}}"""
        val loaded = loadTheme(ThemePackage(doc, emptyMap()), "$base/cache", LinkedHashSet())!!
        val pre = PrecompiledTheme.create(loaded, MillenniumPalette.Blue)
        assertEquals(true, pre.usesItemContext)
        val a = pre.forItem(true, mapOf("current" to true), emptyMap(), false)
        val b = pre.forItem(true, mapOf("current" to true), emptyMap(), false)
        val c = pre.forItem(true, mapOf("current" to false), emptyMap(), false)
        assertSame(a, b)
        assertNotSame(a, c)
    }

    @Test
    fun liveContextThemesAreFlagged() {
        fun flagged(json: String) = loadTheme(ThemePackage(json, emptyMap()), "$base/cache", LinkedHashSet())!!.usesLiveContext
        assertEquals(true, flagged("""{"components":{"card":{"rules":[{"when":{"var":"ctx.player.isPlaying"},"set":{"borderWidth":2}}]}}}"""))
        assertEquals(true, flagged("""{"components":{"card":{"rules":[{"when":{"var":"ctx.env.compact"},"set":{"borderWidth":2}}]}}}"""))
        assertEquals(false, flagged("""{"components":{"card":{"base":{"borderWidth":2}}}}"""))
    }

    @Test
    fun parameterChangesRecompileOnlyTheSelectedTheme() = runBlocking {
        val settings = FakeSettings(UserSettings(selectedThemeId = BuiltInThemes.MODERN_ID))
        val rt = runtime(settings)
        val first = withTimeout(10_000) { rt.precompiled.first { it.loaded != null } }
        assertEquals(emptyMap(), first.parameters)

        // Another theme's parameters are none of this theme's business: no recompile.
        settings.userSettings.value = settings.userSettings.value.copy(themeParameters = mapOf("someone-else.accent" to "blue"))
        delay(300)
        assertSame(first, rt.precompiled.value)

        // The selected theme's parameter recompiles it with the value, stripped of the theme-id prefix.
        settings.userSettings.value = settings.userSettings.value.copy(
            themeParameters = mapOf("someone-else.accent" to "blue", "${BuiltInThemes.MODERN_ID}.accent" to "blue"),
        )
        val blue = withTimeout(10_000) { rt.precompiled.first { it.parameters.isNotEmpty() } }
        assertEquals(mapOf("accent" to "blue"), blue.parameters)
        assertNotSame(first, blue)
        assertTrue(blue.forAppearance(false).colors.accent != first.forAppearance(false).colors.accent)
    }

    @Test
    fun parametersForStripsThePrefixAndIgnoresOthers() {
        val all = mapOf("a.x" to "1", "a.y" to "2", "ab.x" to "3", "b.x" to "4")
        assertEquals(mapOf("x" to "1", "y" to "2"), parametersFor("a", all))
        assertEquals(emptyMap(), parametersFor(null, all))
        assertEquals(emptyMap(), parametersFor("nope", all))
    }
}
