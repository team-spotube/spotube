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

package org.spotube.spotube.modules.library.local_tracks.media

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoverThumbnailTest {
    private fun png(width: Int, height: Int): ByteArray = Surface.makeRasterN32Premul(width, height).use { surface ->
        // Noise-free but non-uniform so the PNG isn't trivially small.
        for (x in 0 until width step 7) {
            surface.canvas.drawRect(Rect.makeXYWH(x.toFloat(), 0f, 3f, height.toFloat()), Paint().apply { color = (0xFF000000.toInt() or (x * 2654435761L).toInt()) })
        }
        surface.makeImageSnapshot().use { it.encodeToData(EncodedImageFormat.PNG)!!.bytes }
    }

    @Test
    fun largeCoverIsShrunkToASmallJpeg() {
        val original = png(1600, 1200)
        val thumbnail = assertNotNull(coverThumbnail(original))

        Image.makeFromEncoded(thumbnail).use { image ->
            assertEquals(160, minOf(image.width, image.height))
            assertEquals(213, image.width)
        }
        assertTrue(thumbnail.size < 32 * 1024, "thumbnail is ${thumbnail.size} bytes")
    }

    @Test
    fun unreadableCoverIsDropped() {
        assertNull(coverThumbnail(byteArrayOf(1, 2, 3, 4)))
    }
}
