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

val Iconsax.IconsaxArrowSquareUp: ImageVector
    get() {
        if (_IconsaxArrowSquareUp != null) {
            return _IconsaxArrowSquareUp!!
        }
        _IconsaxArrowSquareUp = ImageVector.Builder(
            name = "IconsaxArrowSquareUp",
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
            path(fill = SolidColor(Color.White)) {
                moveTo(15.53f, 14.21f)
                curveTo(15.34f, 14.21f, 15.15f, 14.14f, 15f, 13.99f)
                lineTo(12f, 10.99f)
                lineTo(9f, 13.99f)
                curveTo(8.71f, 14.28f, 8.23f, 14.28f, 7.94f, 13.99f)
                curveTo(7.65f, 13.7f, 7.65f, 13.22f, 7.94f, 12.93f)
                lineTo(11.47f, 9.4f)
                curveTo(11.76f, 9.11f, 12.24f, 9.11f, 12.53f, 9.4f)
                lineTo(16.06f, 12.93f)
                curveTo(16.35f, 13.22f, 16.35f, 13.7f, 16.06f, 13.99f)
                curveTo(15.91f, 14.14f, 15.72f, 14.21f, 15.53f, 14.21f)
                close()
            }
        }.build()

        return _IconsaxArrowSquareUp!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxArrowSquareUp: ImageVector? = null
