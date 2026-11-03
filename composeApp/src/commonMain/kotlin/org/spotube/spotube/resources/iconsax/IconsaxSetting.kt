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

val Iconsax.IconsaxSetting: ImageVector
    get() {
        if (_IconsaxSetting != null) {
            return _IconsaxSetting!!
        }
        _IconsaxSetting = ImageVector.Builder(
            name = "IconsaxSetting",
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
                    moveTo(18.51f, 8.251f)
                    curveTo(19.01f, 8.251f, 19.41f, 7.851f, 19.41f, 7.351f)
                    verticalLineTo(2.701f)
                    curveTo(19.41f, 2.201f, 19.01f, 1.801f, 18.51f, 1.801f)
                    curveTo(18.01f, 1.801f, 17.61f, 2.201f, 17.61f, 2.701f)
                    verticalLineTo(7.351f)
                    curveTo(17.61f, 7.841f, 18.02f, 8.251f, 18.51f, 8.251f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 15.75f)
                    curveTo(11.5f, 15.75f, 11.1f, 16.15f, 11.1f, 16.65f)
                    verticalLineTo(21.3f)
                    curveTo(11.1f, 21.8f, 11.5f, 22.2f, 12f, 22.2f)
                    curveTo(12.5f, 22.2f, 12.9f, 21.8f, 12.9f, 21.3f)
                    verticalLineTo(16.65f)
                    curveTo(12.9f, 16.16f, 12.5f, 15.75f, 12f, 15.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(5.49f, 8.251f)
                    curveTo(5.99f, 8.251f, 6.39f, 7.851f, 6.39f, 7.351f)
                    verticalLineTo(2.701f)
                    curveTo(6.39f, 2.201f, 5.99f, 1.801f, 5.49f, 1.801f)
                    curveTo(4.99f, 1.801f, 4.59f, 2.201f, 4.59f, 2.701f)
                    verticalLineTo(7.351f)
                    curveTo(4.59f, 7.841f, 4.99f, 8.251f, 5.49f, 8.251f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(7.35f, 10.17f)
                    horizontalLineTo(3.63f)
                    curveTo(3.13f, 10.17f, 2.73f, 10.57f, 2.73f, 11.07f)
                    curveTo(2.73f, 11.57f, 3.13f, 11.97f, 3.63f, 11.97f)
                    horizontalLineTo(4.59f)
                    verticalLineTo(21.3f)
                    curveTo(4.59f, 21.8f, 4.99f, 22.2f, 5.49f, 22.2f)
                    curveTo(5.99f, 22.2f, 6.39f, 21.8f, 6.39f, 21.3f)
                    verticalLineTo(11.97f)
                    horizontalLineTo(7.35f)
                    curveTo(7.85f, 11.97f, 8.25f, 11.57f, 8.25f, 11.07f)
                    curveTo(8.25f, 10.57f, 7.84f, 10.17f, 7.35f, 10.17f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(20.37f, 10.17f)
                    horizontalLineTo(16.65f)
                    curveTo(16.15f, 10.17f, 15.75f, 10.57f, 15.75f, 11.07f)
                    curveTo(15.75f, 11.57f, 16.15f, 11.97f, 16.65f, 11.97f)
                    horizontalLineTo(17.61f)
                    verticalLineTo(21.3f)
                    curveTo(17.61f, 21.8f, 18.01f, 22.2f, 18.51f, 22.2f)
                    curveTo(19.01f, 22.2f, 19.41f, 21.8f, 19.41f, 21.3f)
                    verticalLineTo(11.97f)
                    horizontalLineTo(20.37f)
                    curveTo(20.87f, 11.97f, 21.27f, 11.57f, 21.27f, 11.07f)
                    curveTo(21.27f, 10.57f, 20.87f, 10.17f, 20.37f, 10.17f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(13.86f, 12.031f)
                    horizontalLineTo(12.9f)
                    verticalLineTo(2.701f)
                    curveTo(12.9f, 2.201f, 12.5f, 1.801f, 12f, 1.801f)
                    curveTo(11.5f, 1.801f, 11.1f, 2.201f, 11.1f, 2.701f)
                    verticalLineTo(12.031f)
                    horizontalLineTo(10.14f)
                    curveTo(9.64f, 12.031f, 9.24f, 12.431f, 9.24f, 12.931f)
                    curveTo(9.24f, 13.431f, 9.64f, 13.831f, 10.14f, 13.831f)
                    horizontalLineTo(13.86f)
                    curveTo(14.36f, 13.831f, 14.76f, 13.431f, 14.76f, 12.931f)
                    curveTo(14.76f, 12.431f, 14.36f, 12.031f, 13.86f, 12.031f)
                    close()
                }
            }
        }.build()

        return _IconsaxSetting!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSetting: ImageVector? = null
