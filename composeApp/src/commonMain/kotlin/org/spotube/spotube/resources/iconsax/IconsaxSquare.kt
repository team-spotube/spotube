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

val Iconsax.IconsaxSquare: ImageVector
    get() {
        if (_IconsaxSquare != null) {
            return _IconsaxSquare!!
        }
        _IconsaxSquare = ImageVector.Builder(
            name = "IconsaxSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 22.75f)
                horizontalLineTo(9f)
                curveTo(3.57f, 22.75f, 1.25f, 20.43f, 1.25f, 15f)
                verticalLineTo(9f)
                curveTo(1.25f, 3.57f, 3.57f, 1.25f, 9f, 1.25f)
                horizontalLineTo(15f)
                curveTo(20.43f, 1.25f, 22.75f, 3.57f, 22.75f, 9f)
                verticalLineTo(15f)
                curveTo(22.75f, 20.43f, 20.43f, 22.75f, 15f, 22.75f)
                close()
                moveTo(9f, 2.75f)
                curveTo(4.39f, 2.75f, 2.75f, 4.39f, 2.75f, 9f)
                verticalLineTo(15f)
                curveTo(2.75f, 19.61f, 4.39f, 21.25f, 9f, 21.25f)
                horizontalLineTo(15f)
                curveTo(19.61f, 21.25f, 21.25f, 19.61f, 21.25f, 15f)
                verticalLineTo(9f)
                curveTo(21.25f, 4.39f, 19.61f, 2.75f, 15f, 2.75f)
                horizontalLineTo(9f)
                close()
            }
        }.build()

        return _IconsaxSquare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSquare: ImageVector? = null
