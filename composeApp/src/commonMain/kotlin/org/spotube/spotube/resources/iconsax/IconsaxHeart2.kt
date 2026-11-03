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
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxHeart2: ImageVector
    get() {
        if (_IconsaxHeart2 != null) {
            return _IconsaxHeart2!!
        }
        _IconsaxHeart2 = ImageVector.Builder(
            name = "IconsaxHeart2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.44f, 3.1f)
                    curveTo(14.63f, 3.1f, 13.01f, 3.98f, 12f, 5.33f)
                    curveTo(10.99f, 3.98f, 9.37f, 3.1f, 7.56f, 3.1f)
                    curveTo(4.49f, 3.1f, 2f, 5.6f, 2f, 8.69f)
                    curveTo(2f, 9.88f, 2.19f, 10.98f, 2.52f, 12f)
                    curveTo(4.1f, 17f, 8.97f, 19.99f, 11.38f, 20.81f)
                    curveTo(11.72f, 20.93f, 12.28f, 20.93f, 12.62f, 20.81f)
                    curveTo(15.03f, 19.99f, 19.9f, 17f, 21.48f, 12f)
                    curveTo(21.81f, 10.98f, 22f, 9.88f, 22f, 8.69f)
                    curveTo(22f, 5.6f, 19.51f, 3.1f, 16.44f, 3.1f)
                    close()
                }
            }
        }.build()

        return _IconsaxHeart2!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxHeart2: ImageVector? = null
