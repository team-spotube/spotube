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

val Iconsax.IconsaxLocation: ImageVector
    get() {
        if (_IconsaxLocation != null) {
            return _IconsaxLocation!!
        }
        _IconsaxLocation = ImageVector.Builder(
            name = "IconsaxLocation",
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
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(20.62f, 8.45f)
                    curveTo(19.57f, 3.83f, 15.54f, 1.75f, 12f, 1.75f)
                    curveTo(12f, 1.75f, 12f, 1.75f, 11.99f, 1.75f)
                    curveTo(8.46f, 1.75f, 4.42f, 3.82f, 3.37f, 8.44f)
                    curveTo(2.2f, 13.6f, 5.36f, 17.97f, 8.22f, 20.72f)
                    curveTo(9.28f, 21.74f, 10.64f, 22.25f, 12f, 22.25f)
                    curveTo(13.36f, 22.25f, 14.72f, 21.74f, 15.77f, 20.72f)
                    curveTo(18.63f, 17.97f, 21.79f, 13.61f, 20.62f, 8.45f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 13.46f)
                    curveTo(13.74f, 13.46f, 15.15f, 12.05f, 15.15f, 10.31f)
                    curveTo(15.15f, 8.57f, 13.74f, 7.16f, 12f, 7.16f)
                    curveTo(10.26f, 7.16f, 8.85f, 8.57f, 8.85f, 10.31f)
                    curveTo(8.85f, 12.05f, 10.26f, 13.46f, 12f, 13.46f)
                    close()
                }
            }
        }.build()

        return _IconsaxLocation!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxLocation: ImageVector? = null
