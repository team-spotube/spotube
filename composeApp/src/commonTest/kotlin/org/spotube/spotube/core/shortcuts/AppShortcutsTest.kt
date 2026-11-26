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

package org.spotube.spotube.core.shortcuts

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppShortcutsTest {
    private fun resolve(key: Key, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false) =
        resolveShortcut(key, ctrl, shift, alt)

    @Test
    fun spaceTogglesPlayback() {
        assertEquals(ShortcutAction.PlayPause, resolve(Key.Spacebar))
    }

    @Test
    fun ctrlCommaOpensSettings() {
        assertEquals(ShortcutAction.OpenSettings, resolve(Key.Comma, ctrl = true))
    }

    @Test
    fun ctrlArrowsSkipAndAdjustVolume() {
        assertEquals(ShortcutAction.Next, resolve(Key.DirectionRight, ctrl = true))
        assertEquals(ShortcutAction.Previous, resolve(Key.DirectionLeft, ctrl = true))
        assertEquals(ShortcutAction.VolumeUp, resolve(Key.DirectionUp, ctrl = true))
        assertEquals(ShortcutAction.VolumeDown, resolve(Key.DirectionDown, ctrl = true))
    }

    @Test
    fun plainArrowsSeekAndAdjustVolume() {
        assertEquals(ShortcutAction.SeekForward, resolve(Key.DirectionRight))
        assertEquals(ShortcutAction.SeekBackward, resolve(Key.DirectionLeft))
        assertEquals(ShortcutAction.VolumeUp, resolve(Key.DirectionUp))
        assertEquals(ShortcutAction.VolumeDown, resolve(Key.DirectionDown))
    }

    @Test
    fun altLeftGoesBack() {
        assertEquals(ShortcutAction.Back, resolve(Key.DirectionLeft, alt = true))
    }

    @Test
    fun plainLettersAndModifiedSpaceAreNotShortcuts() {
        assertNull(resolve(Key.S))
        assertNull(resolve(Key.Spacebar, ctrl = true))
        assertNull(resolve(Key.Comma))
    }
}
