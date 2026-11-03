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

val Iconsax.IconsaxVolumeHigh: ImageVector
    get() {
        if (_IconsaxVolumeHigh != null) {
            return _IconsaxVolumeHigh!!
        }
        _IconsaxVolumeHigh = ImageVector.Builder(
            name = "IconsaxVolumeHigh",
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
                    moveTo(18f, 16.75f)
                    curveTo(17.84f, 16.75f, 17.69f, 16.7f, 17.55f, 16.6f)
                    curveTo(17.22f, 16.35f, 17.15f, 15.88f, 17.4f, 15.55f)
                    curveTo(18.97f, 13.46f, 18.97f, 10.54f, 17.4f, 8.45f)
                    curveTo(17.15f, 8.12f, 17.22f, 7.65f, 17.55f, 7.4f)
                    curveTo(17.88f, 7.15f, 18.35f, 7.22f, 18.6f, 7.55f)
                    curveTo(20.56f, 10.17f, 20.56f, 13.83f, 18.6f, 16.45f)
                    curveTo(18.45f, 16.65f, 18.23f, 16.75f, 18f, 16.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(19.83f, 19.25f)
                    curveTo(19.67f, 19.25f, 19.52f, 19.2f, 19.38f, 19.1f)
                    curveTo(19.05f, 18.85f, 18.98f, 18.38f, 19.23f, 18.05f)
                    curveTo(21.9f, 14.49f, 21.9f, 9.51f, 19.23f, 5.95f)
                    curveTo(18.98f, 5.62f, 19.05f, 5.15f, 19.38f, 4.9f)
                    curveTo(19.71f, 4.65f, 20.18f, 4.72f, 20.43f, 5.05f)
                    curveTo(23.5f, 9.14f, 23.5f, 14.86f, 20.43f, 18.95f)
                    curveTo(20.29f, 19.15f, 20.06f, 19.25f, 19.83f, 19.25f)
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

        return _IconsaxVolumeHigh!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxVolumeHigh: ImageVector? = null
