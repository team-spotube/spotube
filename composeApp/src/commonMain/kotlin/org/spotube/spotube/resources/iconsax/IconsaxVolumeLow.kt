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

val Iconsax.IconsaxVolumeLow: ImageVector
    get() {
        if (_IconsaxVolumeLow != null) {
            return _IconsaxVolumeLow!!
        }
        _IconsaxVolumeLow = ImageVector.Builder(
            name = "IconsaxVolumeLow",
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
                    moveTo(19.33f, 16.75f)
                    curveTo(19.17f, 16.75f, 19.02f, 16.7f, 18.88f, 16.6f)
                    curveTo(18.55f, 16.35f, 18.48f, 15.88f, 18.73f, 15.55f)
                    curveTo(20.3f, 13.46f, 20.3f, 10.54f, 18.73f, 8.45f)
                    curveTo(18.48f, 8.12f, 18.55f, 7.65f, 18.88f, 7.4f)
                    curveTo(19.21f, 7.15f, 19.68f, 7.22f, 19.93f, 7.55f)
                    curveTo(21.9f, 10.17f, 21.9f, 13.83f, 19.93f, 16.45f)
                    curveTo(19.79f, 16.65f, 19.56f, 16.75f, 19.33f, 16.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.35f, 3.78f)
                    curveTo(14.23f, 3.16f, 12.8f, 3.32f, 11.34f, 4.23f)
                    lineTo(8.42f, 6.06f)
                    curveTo(8.22f, 6.18f, 7.99f, 6.25f, 7.76f, 6.25f)
                    horizontalLineTo(6.83f)
                    horizontalLineTo(6.33f)
                    curveTo(3.91f, 6.25f, 2.58f, 7.58f, 2.58f, 10f)
                    verticalLineTo(14f)
                    curveTo(2.58f, 16.42f, 3.91f, 17.75f, 6.33f, 17.75f)
                    horizontalLineTo(6.83f)
                    horizontalLineTo(7.76f)
                    curveTo(7.99f, 17.75f, 8.22f, 17.82f, 8.42f, 17.94f)
                    lineTo(11.34f, 19.77f)
                    curveTo(12.22f, 20.32f, 13.08f, 20.59f, 13.88f, 20.59f)
                    curveTo(14.4f, 20.59f, 14.9f, 20.47f, 15.35f, 20.22f)
                    curveTo(16.46f, 19.6f, 17.08f, 18.31f, 17.08f, 16.59f)
                    verticalLineTo(7.41f)
                    curveTo(17.08f, 5.69f, 16.46f, 4.4f, 15.35f, 3.78f)
                    close()
                }
            }
        }.build()

        return _IconsaxVolumeLow!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxVolumeLow: ImageVector? = null
