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

val Iconsax.IconsaxVolumeCross: ImageVector
    get() {
        if (_IconsaxVolumeCross != null) {
            return _IconsaxVolumeCross!!
        }
        _IconsaxVolumeCross = ImageVector.Builder(
            name = "IconsaxVolumeCross",
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
                    moveTo(22.53f, 13.42f)
                    lineTo(21.08f, 11.97f)
                    lineTo(22.48f, 10.57f)
                    curveTo(22.77f, 10.28f, 22.77f, 9.8f, 22.48f, 9.51f)
                    curveTo(22.19f, 9.22f, 21.71f, 9.22f, 21.42f, 9.51f)
                    lineTo(20.02f, 10.91f)
                    lineTo(18.57f, 9.46f)
                    curveTo(18.28f, 9.17f, 17.8f, 9.17f, 17.51f, 9.46f)
                    curveTo(17.22f, 9.75f, 17.22f, 10.23f, 17.51f, 10.52f)
                    lineTo(18.96f, 11.97f)
                    lineTo(17.47f, 13.46f)
                    curveTo(17.18f, 13.75f, 17.18f, 14.23f, 17.47f, 14.52f)
                    curveTo(17.62f, 14.67f, 17.81f, 14.74f, 18f, 14.74f)
                    curveTo(18.19f, 14.74f, 18.38f, 14.67f, 18.53f, 14.52f)
                    lineTo(20.02f, 13.03f)
                    lineTo(21.47f, 14.48f)
                    curveTo(21.62f, 14.63f, 21.81f, 14.7f, 22f, 14.7f)
                    curveTo(22.19f, 14.7f, 22.38f, 14.63f, 22.53f, 14.48f)
                    curveTo(22.82f, 14.19f, 22.82f, 13.72f, 22.53f, 13.42f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(14.02f, 3.78f)
                    curveTo(12.9f, 3.16f, 11.47f, 3.32f, 10.01f, 4.23f)
                    lineTo(7.09f, 6.06f)
                    curveTo(6.89f, 6.18f, 6.66f, 6.25f, 6.43f, 6.25f)
                    horizontalLineTo(5.5f)
                    horizontalLineTo(5f)
                    curveTo(2.58f, 6.25f, 1.25f, 7.58f, 1.25f, 10f)
                    verticalLineTo(14f)
                    curveTo(1.25f, 16.42f, 2.58f, 17.75f, 5f, 17.75f)
                    horizontalLineTo(5.5f)
                    horizontalLineTo(6.43f)
                    curveTo(6.66f, 17.75f, 6.89f, 17.82f, 7.09f, 17.94f)
                    lineTo(10.01f, 19.77f)
                    curveTo(10.89f, 20.32f, 11.75f, 20.59f, 12.55f, 20.59f)
                    curveTo(13.07f, 20.59f, 13.57f, 20.47f, 14.02f, 20.22f)
                    curveTo(15.13f, 19.6f, 15.75f, 18.31f, 15.75f, 16.59f)
                    verticalLineTo(7.41f)
                    curveTo(15.75f, 5.69f, 15.13f, 4.4f, 14.02f, 3.78f)
                    close()
                }
            }
        }.build()

        return _IconsaxVolumeCross!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxVolumeCross: ImageVector? = null
