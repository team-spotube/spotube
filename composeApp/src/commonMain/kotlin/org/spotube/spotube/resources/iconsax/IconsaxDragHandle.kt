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

val Iconsax.IconsaxDragHandle: ImageVector
    get() {
        if (_IconsaxDragHandle != null) {
            return _IconsaxDragHandle!!
        }
        _IconsaxDragHandle = ImageVector.Builder(
            name = "IconsaxDragHandle",
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
                    moveTo(8f, 4f)
                    curveTo(6.9f, 4f, 6f, 4.9f, 6f, 6f)
                    curveTo(6f, 7.1f, 6.9f, 8f, 8f, 8f)
                    curveTo(9.1f, 8f, 10f, 7.1f, 10f, 6f)
                    curveTo(10f, 4.9f, 9.1f, 4f, 8f, 4f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(16f, 4f)
                    curveTo(14.9f, 4f, 14f, 4.9f, 14f, 6f)
                    curveTo(14f, 7.1f, 14.9f, 8f, 16f, 8f)
                    curveTo(17.1f, 8f, 18f, 7.1f, 18f, 6f)
                    curveTo(18f, 4.9f, 17.1f, 4f, 16f, 4f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(8f, 10f)
                    curveTo(6.9f, 10f, 6f, 10.9f, 6f, 12f)
                    curveTo(6f, 13.1f, 6.9f, 14f, 8f, 14f)
                    curveTo(9.1f, 14f, 10f, 13.1f, 10f, 12f)
                    curveTo(10f, 10.9f, 9.1f, 10f, 8f, 10f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(16f, 10f)
                    curveTo(14.9f, 10f, 14f, 10.9f, 14f, 12f)
                    curveTo(14f, 13.1f, 14.9f, 14f, 16f, 14f)
                    curveTo(17.1f, 14f, 18f, 13.1f, 18f, 12f)
                    curveTo(18f, 10.9f, 17.1f, 10f, 16f, 10f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(8f, 16f)
                    curveTo(6.9f, 16f, 6f, 16.9f, 6f, 18f)
                    curveTo(6f, 19.1f, 6.9f, 20f, 8f, 20f)
                    curveTo(9.1f, 20f, 10f, 19.1f, 10f, 18f)
                    curveTo(10f, 16.9f, 9.1f, 16f, 8f, 16f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(16f, 16f)
                    curveTo(14.9f, 16f, 14f, 16.9f, 14f, 18f)
                    curveTo(14f, 19.1f, 14.9f, 20f, 16f, 20f)
                    curveTo(17.1f, 20f, 18f, 19.1f, 18f, 18f)
                    curveTo(18f, 16.9f, 17.1f, 16f, 16f, 16f)
                    close()
                }
            }
        }.build()

        return _IconsaxDragHandle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxDragHandle: ImageVector? = null
