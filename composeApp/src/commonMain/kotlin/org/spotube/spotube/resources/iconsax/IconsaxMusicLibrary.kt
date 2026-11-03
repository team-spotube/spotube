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

val Iconsax.IconsaxMusicLibrary: ImageVector
    get() {
        if (_IconsaxMusicLibrary != null) {
            return _IconsaxMusicLibrary!!
        }
        _IconsaxMusicLibrary = ImageVector.Builder(
            name = "IconsaxMusicLibrary",
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
                    moveTo(19f, 7f)
                    verticalLineTo(8.13f)
                    curveTo(18.68f, 8.04f, 18.35f, 8f, 18f, 8f)
                    horizontalLineTo(6f)
                    curveTo(5.65f, 8f, 5.32f, 8.04f, 5f, 8.13f)
                    verticalLineTo(7f)
                    curveTo(5f, 5.9f, 5.9f, 5f, 7f, 5f)
                    horizontalLineTo(17f)
                    curveTo(18.1f, 5f, 19f, 5.9f, 19f, 7f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(16f, 3.51f)
                    verticalLineTo(5f)
                    horizontalLineTo(8f)
                    verticalLineTo(3.51f)
                    curveTo(8f, 2.68f, 8.68f, 2f, 9.51f, 2f)
                    horizontalLineTo(14.49f)
                    curveTo(15.32f, 2f, 16f, 2.68f, 16f, 3.51f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(22f, 12f)
                    verticalLineTo(18f)
                    curveTo(22f, 20.2f, 20.2f, 22f, 18f, 22f)
                    horizontalLineTo(6f)
                    curveTo(3.8f, 22f, 2f, 20.2f, 2f, 18f)
                    verticalLineTo(12f)
                    curveTo(2f, 10.15f, 3.28f, 8.58f, 5f, 8.13f)
                    curveTo(5.32f, 8.04f, 5.65f, 8f, 6f, 8f)
                    horizontalLineTo(18f)
                    curveTo(18.35f, 8f, 18.68f, 8.04f, 19f, 8.13f)
                    curveTo(20.72f, 8.58f, 22f, 10.15f, 22f, 12f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.35f, 10.51f)
                    curveTo(15.05f, 10.28f, 14.51f, 10.06f, 13.66f, 10.29f)
                    lineTo(11.01f, 11.02f)
                    curveTo(10.18f, 11.24f, 9.65f, 11.94f, 9.65f, 12.8f)
                    verticalLineTo(14.35f)
                    verticalLineTo(16.15f)
                    curveTo(9.47f, 16.1f, 9.27f, 16.06f, 9.07f, 16.06f)
                    curveTo(7.93f, 16.06f, 7f, 16.99f, 7f, 18.13f)
                    curveTo(7f, 19.27f, 7.93f, 20.2f, 9.07f, 20.2f)
                    curveTo(10.21f, 20.2f, 11.13f, 19.28f, 11.14f, 18.15f)
                    curveTo(11.14f, 18.14f, 11.15f, 18.13f, 11.15f, 18.12f)
                    verticalLineTo(14.91f)
                    lineTo(14.5f, 14f)
                    verticalLineTo(15.26f)
                    curveTo(14.32f, 15.21f, 14.13f, 15.17f, 13.93f, 15.17f)
                    curveTo(12.79f, 15.17f, 11.86f, 16.1f, 11.86f, 17.24f)
                    curveTo(11.86f, 18.38f, 12.79f, 19.31f, 13.93f, 19.31f)
                    curveTo(15.07f, 19.31f, 16f, 18.38f, 16f, 17.24f)
                    verticalLineTo(13.02f)
                    verticalLineTo(12.07f)
                    curveTo(16f, 11.2f, 15.64f, 10.74f, 15.35f, 10.51f)
                    close()
                    moveTo(9.07f, 18.71f)
                    curveTo(8.75f, 18.71f, 8.5f, 18.45f, 8.5f, 18.14f)
                    curveTo(8.5f, 17.83f, 8.76f, 17.57f, 9.07f, 17.57f)
                    curveTo(9.38f, 17.57f, 9.64f, 17.83f, 9.64f, 18.14f)
                    curveTo(9.64f, 18.45f, 9.39f, 18.71f, 9.07f, 18.71f)
                    close()
                    moveTo(13.93f, 17.82f)
                    curveTo(13.61f, 17.82f, 13.36f, 17.56f, 13.36f, 17.25f)
                    curveTo(13.36f, 16.94f, 13.62f, 16.68f, 13.93f, 16.68f)
                    curveTo(14.24f, 16.68f, 14.5f, 16.94f, 14.5f, 17.25f)
                    curveTo(14.5f, 17.56f, 14.24f, 17.82f, 13.93f, 17.82f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicLibrary!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicLibrary: ImageVector? = null
