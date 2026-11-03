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

val Iconsax.IconsaxShuffle: ImageVector
    get() {
        if (_IconsaxShuffle != null) {
            return _IconsaxShuffle!!
        }
        _IconsaxShuffle = ImageVector.Builder(
            name = "IconsaxShuffle",
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
                    moveTo(21.75f, 17.98f)
                    curveTo(21.75f, 17.96f, 21.74f, 17.94f, 21.74f, 17.92f)
                    curveTo(21.73f, 17.84f, 21.72f, 17.76f, 21.69f, 17.69f)
                    curveTo(21.65f, 17.6f, 21.6f, 17.53f, 21.54f, 17.46f)
                    curveTo(21.54f, 17.46f, 21.54f, 17.45f, 21.53f, 17.45f)
                    curveTo(21.46f, 17.38f, 21.38f, 17.33f, 21.29f, 17.29f)
                    curveTo(21.2f, 17.25f, 21.1f, 17.23f, 21f, 17.23f)
                    lineTo(16.33f, 17.25f)
                    curveTo(16.33f, 17.25f, 16.33f, 17.25f, 16.32f, 17.25f)
                    curveTo(15.72f, 17.25f, 15.14f, 16.97f, 14.78f, 16.49f)
                    lineTo(13.56f, 14.92f)
                    curveTo(13.31f, 14.59f, 12.84f, 14.53f, 12.51f, 14.79f)
                    curveTo(12.18f, 15.05f, 12.12f, 15.51f, 12.38f, 15.84f)
                    lineTo(13.6f, 17.41f)
                    curveTo(14.25f, 18.25f, 15.27f, 18.75f, 16.33f, 18.75f)
                    horizontalLineTo(16.34f)
                    lineTo(19.19f, 18.74f)
                    lineTo(18.48f, 19.45f)
                    curveTo(18.19f, 19.74f, 18.19f, 20.22f, 18.48f, 20.51f)
                    curveTo(18.63f, 20.66f, 18.82f, 20.73f, 19.01f, 20.73f)
                    curveTo(19.2f, 20.73f, 19.39f, 20.66f, 19.54f, 20.51f)
                    lineTo(21.54f, 18.51f)
                    curveTo(21.61f, 18.44f, 21.66f, 18.36f, 21.7f, 18.27f)
                    curveTo(21.73f, 18.17f, 21.75f, 18.07f, 21.75f, 17.98f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(8.42f, 6.69f)
                    curveTo(7.77f, 5.79f, 6.73f, 5.26f, 5.62f, 5.26f)
                    curveTo(5.61f, 5.26f, 5.61f, 5.26f, 5.6f, 5.26f)
                    lineTo(2.99f, 5.27f)
                    curveTo(2.58f, 5.27f, 2.24f, 5.61f, 2.24f, 6.02f)
                    curveTo(2.24f, 6.43f, 2.58f, 6.77f, 2.99f, 6.77f)
                    lineTo(5.6f, 6.76f)
                    horizontalLineTo(5.61f)
                    curveTo(6.24f, 6.76f, 6.83f, 7.06f, 7.19f, 7.57f)
                    lineTo(8.27f, 9.07f)
                    curveTo(8.42f, 9.27f, 8.65f, 9.38f, 8.88f, 9.38f)
                    curveTo(9.03f, 9.38f, 9.19f, 9.33f, 9.32f, 9.24f)
                    curveTo(9.66f, 9f, 9.73f, 8.53f, 9.49f, 8.19f)
                    lineTo(8.42f, 6.69f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21.74f, 6.08f)
                    curveTo(21.74f, 6.06f, 21.75f, 6.04f, 21.75f, 6.03f)
                    curveTo(21.75f, 5.93f, 21.73f, 5.83f, 21.69f, 5.74f)
                    curveTo(21.65f, 5.65f, 21.6f, 5.57f, 21.53f, 5.5f)
                    lineTo(19.53f, 3.5f)
                    curveTo(19.24f, 3.21f, 18.76f, 3.21f, 18.47f, 3.5f)
                    curveTo(18.18f, 3.79f, 18.18f, 4.27f, 18.47f, 4.56f)
                    lineTo(19.18f, 5.27f)
                    lineTo(16.45f, 5.26f)
                    curveTo(16.44f, 5.26f, 16.44f, 5.26f, 16.43f, 5.26f)
                    curveTo(15.28f, 5.26f, 14.2f, 5.83f, 13.56f, 6.8f)
                    lineTo(7.17f, 16.38f)
                    curveTo(6.81f, 16.92f, 6.2f, 17.25f, 5.55f, 17.25f)
                    horizontalLineTo(5.54f)
                    lineTo(2.99f, 17.24f)
                    curveTo(2.58f, 17.24f, 2.24f, 17.57f, 2.24f, 17.99f)
                    curveTo(2.24f, 18.4f, 2.57f, 18.74f, 2.99f, 18.74f)
                    lineTo(5.54f, 18.75f)
                    curveTo(5.55f, 18.75f, 5.55f, 18.75f, 5.56f, 18.75f)
                    curveTo(6.72f, 18.75f, 7.79f, 18.18f, 8.43f, 17.21f)
                    lineTo(14.82f, 7.63f)
                    curveTo(15.18f, 7.09f, 15.79f, 6.76f, 16.44f, 6.76f)
                    horizontalLineTo(16.45f)
                    lineTo(21f, 6.78f)
                    curveTo(21.1f, 6.78f, 21.19f, 6.76f, 21.29f, 6.72f)
                    curveTo(21.38f, 6.68f, 21.46f, 6.63f, 21.53f, 6.56f)
                    curveTo(21.53f, 6.56f, 21.53f, 6.55f, 21.54f, 6.55f)
                    curveTo(21.6f, 6.48f, 21.66f, 6.41f, 21.69f, 6.32f)
                    curveTo(21.72f, 6.24f, 21.73f, 6.16f, 21.74f, 6.08f)
                    close()
                }
            }
        }.build()

        return _IconsaxShuffle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxShuffle: ImageVector? = null
