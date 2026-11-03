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

package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxSearch: ImageVector
    get() {
        if (_IconsaxSearch != null) {
            return _IconsaxSearch!!
        }
        _IconsaxSearch = ImageVector.Builder(
            name = "IconsaxSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f
            ) {
                moveTo(11.5f, 21f)
                curveTo(16.747f, 21f, 21f, 16.747f, 21f, 11.5f)
                curveTo(21f, 6.253f, 16.747f, 2f, 11.5f, 2f)
                curveTo(6.253f, 2f, 2f, 6.253f, 2f, 11.5f)
                curveTo(2f, 16.747f, 6.253f, 21f, 11.5f, 21f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(21.3f, 22f)
                curveTo(21.12f, 22f, 20.94f, 21.93f, 20.81f, 21.8f)
                lineTo(18.95f, 19.94f)
                curveTo(18.68f, 19.67f, 18.68f, 19.23f, 18.95f, 18.95f)
                curveTo(19.22f, 18.68f, 19.66f, 18.68f, 19.94f, 18.95f)
                lineTo(21.8f, 20.81f)
                curveTo(22.07f, 21.08f, 22.07f, 21.52f, 21.8f, 21.8f)
                curveTo(21.66f, 21.93f, 21.48f, 22f, 21.3f, 22f)
                close()
            }
        }.build()

        return _IconsaxSearch!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSearch: ImageVector? = null
