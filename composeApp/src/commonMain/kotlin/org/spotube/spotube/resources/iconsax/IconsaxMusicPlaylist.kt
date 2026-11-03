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

val Iconsax.IconsaxMusicPlaylist: ImageVector
    get() {
        if (_IconsaxMusicPlaylist != null) {
            return _IconsaxMusicPlaylist!!
        }
        _IconsaxMusicPlaylist = ImageVector.Builder(
            name = "IconsaxMusicPlaylist",
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
                    moveTo(18f, 5.25f)
                    horizontalLineTo(6f)
                    curveTo(5.59f, 5.25f, 5.25f, 4.91f, 5.25f, 4.5f)
                    curveTo(5.25f, 4.09f, 5.59f, 3.75f, 6f, 3.75f)
                    horizontalLineTo(18f)
                    curveTo(18.41f, 3.75f, 18.75f, 4.09f, 18.75f, 4.5f)
                    curveTo(18.75f, 4.91f, 18.41f, 5.25f, 18f, 5.25f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15f, 2.75f)
                    horizontalLineTo(9f)
                    curveTo(8.59f, 2.75f, 8.25f, 2.41f, 8.25f, 2f)
                    curveTo(8.25f, 1.59f, 8.59f, 1.25f, 9f, 1.25f)
                    horizontalLineTo(15f)
                    curveTo(15.41f, 1.25f, 15.75f, 1.59f, 15.75f, 2f)
                    curveTo(15.75f, 2.41f, 15.41f, 2.75f, 15f, 2.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(18f, 7f)
                    horizontalLineTo(6f)
                    curveTo(3.8f, 7f, 2f, 8.8f, 2f, 11f)
                    verticalLineTo(18f)
                    curveTo(2f, 20.2f, 3.8f, 22f, 6f, 22f)
                    horizontalLineTo(18f)
                    curveTo(20.2f, 22f, 22f, 20.2f, 22f, 18f)
                    verticalLineTo(11f)
                    curveTo(22f, 8.8f, 20.2f, 7f, 18f, 7f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.37f, 9.89f)
                    curveTo(15.07f, 9.65f, 14.52f, 9.42f, 13.64f, 9.65f)
                    lineTo(10.91f, 10.4f)
                    curveTo(10.06f, 10.62f, 9.51f, 11.34f, 9.51f, 12.23f)
                    verticalLineTo(13.83f)
                    verticalLineTo(15.73f)
                    curveTo(9.31f, 15.67f, 9.11f, 15.63f, 8.89f, 15.63f)
                    curveTo(7.72f, 15.63f, 6.77f, 16.58f, 6.77f, 17.75f)
                    curveTo(6.77f, 18.92f, 7.72f, 19.87f, 8.89f, 19.87f)
                    curveTo(10.06f, 19.87f, 11.01f, 18.92f, 11.01f, 17.75f)
                    verticalLineTo(17.74f)
                    verticalLineTo(14.41f)
                    lineTo(14.53f, 13.45f)
                    verticalLineTo(14.82f)
                    curveTo(14.33f, 14.76f, 14.13f, 14.72f, 13.91f, 14.72f)
                    curveTo(12.74f, 14.72f, 11.79f, 15.67f, 11.79f, 16.84f)
                    curveTo(11.79f, 18.01f, 12.74f, 18.96f, 13.91f, 18.96f)
                    curveTo(15.06f, 18.96f, 16f, 18.04f, 16.02f, 16.89f)
                    curveTo(16.02f, 16.87f, 16.03f, 16.86f, 16.03f, 16.84f)
                    verticalLineTo(12.47f)
                    verticalLineTo(11.49f)
                    curveTo(16.03f, 10.59f, 15.67f, 10.12f, 15.37f, 9.89f)
                    close()
                    moveTo(8.89f, 18.36f)
                    curveTo(8.55f, 18.36f, 8.27f, 18.08f, 8.27f, 17.74f)
                    curveTo(8.27f, 17.4f, 8.55f, 17.12f, 8.89f, 17.12f)
                    curveTo(9.23f, 17.12f, 9.5f, 17.39f, 9.51f, 17.73f)
                    curveTo(9.51f, 18.08f, 9.23f, 18.36f, 8.89f, 18.36f)
                    close()
                    moveTo(13.91f, 17.45f)
                    curveTo(13.57f, 17.45f, 13.29f, 17.17f, 13.29f, 16.83f)
                    curveTo(13.29f, 16.49f, 13.57f, 16.21f, 13.91f, 16.21f)
                    curveTo(14.25f, 16.21f, 14.53f, 16.49f, 14.53f, 16.83f)
                    curveTo(14.53f, 17.17f, 14.25f, 17.45f, 13.91f, 17.45f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicPlaylist!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicPlaylist: ImageVector? = null
