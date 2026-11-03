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

val Iconsax.IconsaxMusicDashboard: ImageVector
    get() {
        if (_IconsaxMusicDashboard != null) {
            return _IconsaxMusicDashboard!!
        }
        _IconsaxMusicDashboard = ImageVector.Builder(
            name = "IconsaxMusicDashboard",
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
                    moveTo(7f, 2.05f)
                    verticalLineTo(21.95f)
                    curveTo(3.85f, 21.66f, 2f, 19.55f, 2f, 16.19f)
                    verticalLineTo(7.81f)
                    curveTo(2f, 4.45f, 3.85f, 2.34f, 7f, 2.05f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(22f, 7.81f)
                    verticalLineTo(16.19f)
                    curveTo(22f, 19.83f, 19.83f, 22f, 16.19f, 22f)
                    horizontalLineTo(7.81f)
                    curveTo(7.53f, 22f, 7.26f, 21.99f, 7f, 21.95f)
                    verticalLineTo(2.05f)
                    curveTo(7.26f, 2.01f, 7.53f, 2f, 7.81f, 2f)
                    horizontalLineTo(16.19f)
                    curveTo(19.83f, 2f, 22f, 4.17f, 22f, 7.81f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(18.49f, 6.8f)
                    curveTo(18.17f, 6.55f, 17.59f, 6.31f, 16.67f, 6.56f)
                    lineTo(13.69f, 7.38f)
                    curveTo(12.8f, 7.61f, 12.22f, 8.36f, 12.22f, 9.3f)
                    verticalLineTo(11.05f)
                    verticalLineTo(13.21f)
                    curveTo(11.99f, 13.13f, 11.74f, 13.07f, 11.48f, 13.07f)
                    curveTo(10.24f, 13.07f, 9.24f, 14.08f, 9.24f, 15.31f)
                    curveTo(9.24f, 16.54f, 10.25f, 17.55f, 11.48f, 17.55f)
                    curveTo(12.7f, 17.55f, 13.7f, 16.56f, 13.72f, 15.35f)
                    curveTo(13.72f, 15.34f, 13.73f, 15.33f, 13.73f, 15.32f)
                    verticalLineTo(11.62f)
                    lineTo(17.7f, 10.54f)
                    verticalLineTo(12.22f)
                    curveTo(17.47f, 12.14f, 17.22f, 12.08f, 16.95f, 12.08f)
                    curveTo(15.71f, 12.08f, 14.71f, 13.09f, 14.71f, 14.32f)
                    curveTo(14.71f, 15.56f, 15.72f, 16.56f, 16.95f, 16.56f)
                    curveTo(18.17f, 16.56f, 19.17f, 15.57f, 19.19f, 14.35f)
                    curveTo(19.19f, 14.34f, 19.2f, 14.33f, 19.2f, 14.31f)
                    verticalLineTo(9.55f)
                    verticalLineTo(8.48f)
                    curveTo(19.18f, 7.72f, 18.95f, 7.16f, 18.49f, 6.8f)
                    close()
                    moveTo(11.47f, 16.05f)
                    curveTo(11.06f, 16.05f, 10.73f, 15.72f, 10.73f, 15.31f)
                    curveTo(10.73f, 14.9f, 11.06f, 14.57f, 11.47f, 14.57f)
                    curveTo(11.88f, 14.57f, 12.21f, 14.9f, 12.21f, 15.31f)
                    curveTo(12.21f, 15.72f, 11.87f, 16.05f, 11.47f, 16.05f)
                    close()
                    moveTo(16.93f, 15.05f)
                    curveTo(16.52f, 15.05f, 16.19f, 14.72f, 16.19f, 14.31f)
                    curveTo(16.19f, 13.9f, 16.52f, 13.57f, 16.93f, 13.57f)
                    curveTo(17.34f, 13.57f, 17.67f, 13.9f, 17.67f, 14.31f)
                    curveTo(17.67f, 14.72f, 17.34f, 15.05f, 16.93f, 15.05f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicDashboard!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicDashboard: ImageVector? = null
