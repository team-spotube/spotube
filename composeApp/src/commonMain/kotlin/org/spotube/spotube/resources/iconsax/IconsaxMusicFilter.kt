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

val Iconsax.IconsaxMusicFilter: ImageVector
    get() {
        if (_IconsaxMusicFilter != null) {
            return _IconsaxMusicFilter!!
        }
        _IconsaxMusicFilter = ImageVector.Builder(
            name = "IconsaxMusicFilter",
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
                    moveTo(22f, 3.75f)
                    horizontalLineTo(2f)
                    curveTo(1.59f, 3.75f, 1.25f, 3.41f, 1.25f, 3f)
                    curveTo(1.25f, 2.59f, 1.59f, 2.25f, 2f, 2.25f)
                    horizontalLineTo(22f)
                    curveTo(22.41f, 2.25f, 22.75f, 2.59f, 22.75f, 3f)
                    curveTo(22.75f, 3.41f, 22.41f, 3.75f, 22f, 3.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(11f, 9.75f)
                    horizontalLineTo(2f)
                    curveTo(1.59f, 9.75f, 1.25f, 9.41f, 1.25f, 9f)
                    curveTo(1.25f, 8.59f, 1.59f, 8.25f, 2f, 8.25f)
                    horizontalLineTo(11f)
                    curveTo(11.41f, 8.25f, 11.75f, 8.59f, 11.75f, 9f)
                    curveTo(11.75f, 9.41f, 11.41f, 9.75f, 11f, 9.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(8f, 15.75f)
                    horizontalLineTo(2f)
                    curveTo(1.59f, 15.75f, 1.25f, 15.41f, 1.25f, 15f)
                    curveTo(1.25f, 14.59f, 1.59f, 14.25f, 2f, 14.25f)
                    horizontalLineTo(8f)
                    curveTo(8.41f, 14.25f, 8.75f, 14.59f, 8.75f, 15f)
                    curveTo(8.75f, 15.41f, 8.41f, 15.75f, 8f, 15.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(6f, 21.75f)
                    horizontalLineTo(2f)
                    curveTo(1.59f, 21.75f, 1.25f, 21.41f, 1.25f, 21f)
                    curveTo(1.25f, 20.59f, 1.59f, 20.25f, 2f, 20.25f)
                    horizontalLineTo(6f)
                    curveTo(6.41f, 20.25f, 6.75f, 20.59f, 6.75f, 21f)
                    curveTo(6.75f, 21.41f, 6.41f, 21.75f, 6f, 21.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21.86f, 7.68f)
                    curveTo(21.27f, 7.23f, 20.46f, 7.14f, 19.51f, 7.4f)
                    lineTo(15.16f, 8.58f)
                    curveTo(13.99f, 8.9f, 13.27f, 9.85f, 13.27f, 11.05f)
                    verticalLineTo(13.6f)
                    verticalLineTo(17.28f)
                    curveTo(12.85f, 17.04f, 12.36f, 16.89f, 11.84f, 16.89f)
                    curveTo(10.23f, 16.89f, 8.91f, 18.2f, 8.91f, 19.82f)
                    curveTo(8.91f, 21.43f, 10.22f, 22.75f, 11.84f, 22.75f)
                    curveTo(13.46f, 22.75f, 14.77f, 21.44f, 14.77f, 19.82f)
                    verticalLineTo(14.17f)
                    lineTo(21.25f, 12.4f)
                    verticalLineTo(15.83f)
                    curveTo(20.83f, 15.59f, 20.34f, 15.44f, 19.82f, 15.44f)
                    curveTo(18.21f, 15.44f, 16.89f, 16.75f, 16.89f, 18.37f)
                    curveTo(16.89f, 19.98f, 18.2f, 21.3f, 19.82f, 21.3f)
                    curveTo(21.44f, 21.3f, 22.75f, 19.99f, 22.75f, 18.37f)
                    verticalLineTo(11.42f)
                    verticalLineTo(9.87f)
                    curveTo(22.75f, 8.86f, 22.45f, 8.12f, 21.86f, 7.68f)
                    close()
                    moveTo(11.84f, 21.25f)
                    curveTo(11.05f, 21.25f, 10.41f, 20.61f, 10.41f, 19.82f)
                    curveTo(10.41f, 19.03f, 11.05f, 18.39f, 11.84f, 18.39f)
                    curveTo(12.63f, 18.39f, 13.27f, 19.03f, 13.27f, 19.82f)
                    curveTo(13.27f, 20.61f, 12.63f, 21.25f, 11.84f, 21.25f)
                    close()
                    moveTo(19.82f, 19.8f)
                    curveTo(19.03f, 19.8f, 18.39f, 19.16f, 18.39f, 18.37f)
                    curveTo(18.39f, 17.58f, 19.03f, 16.94f, 19.82f, 16.94f)
                    curveTo(20.61f, 16.94f, 21.25f, 17.58f, 21.25f, 18.37f)
                    curveTo(21.25f, 19.16f, 20.61f, 19.8f, 19.82f, 19.8f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicFilter!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicFilter: ImageVector? = null
