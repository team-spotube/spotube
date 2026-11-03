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

package org.spotube.spotube.core.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowStateRepositoryTest {
    private fun repository(): WindowStateRepository {
        val dir = Files.createTempDirectory("window-state-test").toFile()
        val file = File(dir, "window.preferences_pb")
        return WindowStateRepository(
            PreferenceDataStoreFactory.createWithPath(produceFile = { file.absolutePath.toPath() }))
    }

    @Test
    fun missingValueLoadsNull() = runBlocking {
        assertNull(repository().load())
    }

    @Test
    fun roundTripsNormalState() = runBlocking {
        val store = repository()
        store.save(PersistedWindowState.of(DpSize(1200.dp, 750.dp), WindowPlacement.Floating))

        val loaded = store.load()!!
        assertEquals(DpSize(1200.dp, 750.dp), loaded.size)
        assertEquals(WindowPlacement.Floating, loaded.placement)
    }

    @Test
    fun roundTripsMaximizedStateKeepingNormalSize() = runBlocking {
        val store = repository()
        store.save(PersistedWindowState.of(DpSize(1100.dp, 700.dp), WindowPlacement.Maximized))

        val loaded = store.load()!!
        assertEquals(WindowPlacement.Maximized, loaded.placement)
        assertEquals(DpSize(1100.dp, 700.dp), loaded.size)
    }

    @Test
    fun oversizedSavedSizeIsClampedToScreen() {
        val clamped = DpSize(3000.dp, 2000.dp)
            .coerceForScreen(DpSize(800.dp, 500.dp), maxWidth = 1800.dp, maxHeight = 1000.dp)
        assertEquals(DpSize(1800.dp, 1000.dp), clamped)
    }

    @Test
    fun tinySavedSizeIsRaisedToMinimum() {
        val clamped = DpSize(100.dp, 100.dp)
            .coerceForScreen(DpSize(800.dp, 500.dp), maxWidth = 1800.dp, maxHeight = 1000.dp)
        assertEquals(DpSize(800.dp, 500.dp), clamped)
    }

    @Test
    fun unknownScreenOnlyEnforcesMinimum() {
        val clamped = DpSize(3000.dp, 2000.dp)
            .coerceForScreen(DpSize(800.dp, 500.dp), maxWidth = null, maxHeight = null)
        assertEquals(DpSize(3000.dp, 2000.dp), clamped)
    }
}
