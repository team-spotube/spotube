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

val Iconsax.IconsaxPause: ImageVector
    get() {
        if (_IconsaxPause != null) {
            return _IconsaxPause!!
        }
        _IconsaxPause = ImageVector.Builder(
            name = "IconsaxPause",
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
                    moveTo(10.65f, 19.11f)
                    verticalLineTo(4.89f)
                    curveTo(10.65f, 3.54f, 10.08f, 3f, 8.64f, 3f)
                    horizontalLineTo(5.01f)
                    curveTo(3.57f, 3f, 3f, 3.54f, 3f, 4.89f)
                    verticalLineTo(19.11f)
                    curveTo(3f, 20.46f, 3.57f, 21f, 5.01f, 21f)
                    horizontalLineTo(8.64f)
                    curveTo(10.08f, 21f, 10.65f, 20.46f, 10.65f, 19.11f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(21f, 19.11f)
                    verticalLineTo(4.89f)
                    curveTo(21f, 3.54f, 20.43f, 3f, 18.99f, 3f)
                    horizontalLineTo(15.36f)
                    curveTo(13.93f, 3f, 13.35f, 3.54f, 13.35f, 4.89f)
                    verticalLineTo(19.11f)
                    curveTo(13.35f, 20.46f, 13.92f, 21f, 15.36f, 21f)
                    horizontalLineTo(18.99f)
                    curveTo(20.43f, 21f, 21f, 20.46f, 21f, 19.11f)
                    close()
                }
            }
        }.build()

        return _IconsaxPause!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxPause: ImageVector? = null
