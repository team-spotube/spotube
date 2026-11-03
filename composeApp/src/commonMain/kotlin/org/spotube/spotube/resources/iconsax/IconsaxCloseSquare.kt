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

val Iconsax.IconsaxCloseSquare: ImageVector
    get() {
        if (_IconsaxCloseSquare != null) {
            return _IconsaxCloseSquare!!
        }
        _IconsaxCloseSquare = ImageVector.Builder(
            name = "IconsaxCloseSquare",
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
                    moveTo(16.19f, 2f)
                    horizontalLineTo(7.81f)
                    curveTo(4.17f, 2f, 2f, 4.17f, 2f, 7.81f)
                    verticalLineTo(16.18f)
                    curveTo(2f, 19.83f, 4.17f, 22f, 7.81f, 22f)
                    horizontalLineTo(16.18f)
                    curveTo(19.82f, 22f, 21.99f, 19.83f, 21.99f, 16.19f)
                    verticalLineTo(7.81f)
                    curveTo(22f, 4.17f, 19.83f, 2f, 16.19f, 2f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(13.06f, 11.999f)
                    lineTo(15.36f, 9.699f)
                    curveTo(15.65f, 9.409f, 15.65f, 8.929f, 15.36f, 8.639f)
                    curveTo(15.07f, 8.349f, 14.59f, 8.349f, 14.3f, 8.639f)
                    lineTo(12f, 10.939f)
                    lineTo(9.7f, 8.639f)
                    curveTo(9.41f, 8.349f, 8.93f, 8.349f, 8.64f, 8.639f)
                    curveTo(8.35f, 8.929f, 8.35f, 9.409f, 8.64f, 9.699f)
                    lineTo(10.94f, 11.999f)
                    lineTo(8.64f, 14.299f)
                    curveTo(8.35f, 14.589f, 8.35f, 15.069f, 8.64f, 15.359f)
                    curveTo(8.79f, 15.509f, 8.98f, 15.579f, 9.17f, 15.579f)
                    curveTo(9.36f, 15.579f, 9.55f, 15.509f, 9.7f, 15.359f)
                    lineTo(12f, 13.059f)
                    lineTo(14.3f, 15.359f)
                    curveTo(14.45f, 15.509f, 14.64f, 15.579f, 14.83f, 15.579f)
                    curveTo(15.02f, 15.579f, 15.21f, 15.509f, 15.36f, 15.359f)
                    curveTo(15.65f, 15.069f, 15.65f, 14.589f, 15.36f, 14.299f)
                    lineTo(13.06f, 11.999f)
                    close()
                }
            }
        }.build()

        return _IconsaxCloseSquare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxCloseSquare: ImageVector? = null
