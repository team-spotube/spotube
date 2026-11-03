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

val Iconsax.IconsaxPlayCircle2: ImageVector
    get() {
        if (_IconsaxPlayCircle2 != null) {
            return _IconsaxPlayCircle2!!
        }
        _IconsaxPlayCircle2 = ImageVector.Builder(
            name = "IconsaxPlayCircle2",
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
                    moveTo(19.07f, 19.82f)
                    curveTo(18.88f, 19.82f, 18.69f, 19.75f, 18.54f, 19.6f)
                    curveTo(18.25f, 19.31f, 18.25f, 18.83f, 18.54f, 18.54f)
                    curveTo(22.15f, 14.93f, 22.15f, 9.06f, 18.54f, 5.46f)
                    curveTo(18.25f, 5.17f, 18.25f, 4.69f, 18.54f, 4.4f)
                    curveTo(18.83f, 4.11f, 19.31f, 4.11f, 19.6f, 4.4f)
                    curveTo(23.79f, 8.59f, 23.79f, 15.41f, 19.6f, 19.6f)
                    curveTo(19.45f, 19.75f, 19.26f, 19.82f, 19.07f, 19.82f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(4.93f, 19.82f)
                    curveTo(4.74f, 19.82f, 4.55f, 19.75f, 4.4f, 19.6f)
                    curveTo(0.21f, 15.41f, 0.21f, 8.59f, 4.4f, 4.4f)
                    curveTo(4.69f, 4.11f, 5.17f, 4.11f, 5.46f, 4.4f)
                    curveTo(5.75f, 4.69f, 5.75f, 5.17f, 5.46f, 5.46f)
                    curveTo(1.85f, 9.07f, 1.85f, 14.94f, 5.46f, 18.54f)
                    curveTo(5.75f, 18.83f, 5.75f, 19.31f, 5.46f, 19.6f)
                    curveTo(5.31f, 19.75f, 5.12f, 19.82f, 4.93f, 19.82f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(12f, 22.71f)
                    curveTo(10.75f, 22.7f, 9.56f, 22.5f, 8.45f, 22.11f)
                    curveTo(8.06f, 21.97f, 7.85f, 21.54f, 7.99f, 21.15f)
                    curveTo(8.13f, 20.76f, 8.55f, 20.55f, 8.95f, 20.69f)
                    curveTo(9.91f, 21.02f, 10.93f, 21.2f, 12.01f, 21.2f)
                    curveTo(13.08f, 21.2f, 14.11f, 21.02f, 15.06f, 20.69f)
                    curveTo(15.45f, 20.56f, 15.88f, 20.76f, 16.02f, 21.15f)
                    curveTo(16.16f, 21.54f, 15.95f, 21.97f, 15.56f, 22.11f)
                    curveTo(14.44f, 22.5f, 13.25f, 22.71f, 12f, 22.71f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(15.3f, 3.34f)
                    curveTo(15.22f, 3.34f, 15.13f, 3.33f, 15.05f, 3.3f)
                    curveTo(14.09f, 2.97f, 13.06f, 2.79f, 11.99f, 2.79f)
                    curveTo(10.92f, 2.79f, 9.9f, 2.97f, 8.94f, 3.3f)
                    curveTo(8.55f, 3.43f, 8.12f, 3.23f, 7.98f, 2.84f)
                    curveTo(7.84f, 2.45f, 8.05f, 2.02f, 8.44f, 1.88f)
                    curveTo(9.55f, 1.49f, 10.75f, 1.29f, 11.99f, 1.29f)
                    curveTo(13.23f, 1.29f, 14.43f, 1.49f, 15.54f, 1.88f)
                    curveTo(15.93f, 2.02f, 16.14f, 2.45f, 16f, 2.84f)
                    curveTo(15.9f, 3.15f, 15.61f, 3.34f, 15.3f, 3.34f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(8.74f, 12f)
                    verticalLineTo(10.33f)
                    curveTo(8.74f, 8.25f, 10.21f, 7.4f, 12.01f, 8.44f)
                    lineTo(13.46f, 9.28f)
                    lineTo(14.91f, 10.12f)
                    curveTo(16.71f, 11.16f, 16.71f, 12.86f, 14.91f, 13.9f)
                    lineTo(13.46f, 14.74f)
                    lineTo(12.01f, 15.58f)
                    curveTo(10.21f, 16.62f, 8.74f, 15.77f, 8.74f, 13.69f)
                    verticalLineTo(12f)
                    close()
                }
            }
        }.build()

        return _IconsaxPlayCircle2!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxPlayCircle2: ImageVector? = null
