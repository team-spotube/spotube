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

val Iconsax.IconsaxBoxAdd: ImageVector
    get() {
        if (_IconsaxBoxAdd != null) {
            return _IconsaxBoxAdd!!
        }
        _IconsaxBoxAdd = ImageVector.Builder(
            name = "IconsaxBoxAdd",
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
                    moveTo(22f, 15.7f)
                    curveTo(22f, 15.69f, 21.99f, 15.68f, 21.98f, 15.67f)
                    curveTo(21.94f, 15.61f, 21.89f, 15.55f, 21.84f, 15.5f)
                    curveTo(21.83f, 15.49f, 21.82f, 15.47f, 21.81f, 15.46f)
                    curveTo(21f, 14.56f, 19.81f, 14f, 18.5f, 14f)
                    curveTo(17.24f, 14f, 16.09f, 14.52f, 15.27f, 15.36f)
                    curveTo(14.48f, 16.17f, 14f, 17.28f, 14f, 18.5f)
                    curveTo(14f, 19.34f, 14.24f, 20.14f, 14.65f, 20.82f)
                    curveTo(14.87f, 21.19f, 15.15f, 21.53f, 15.47f, 21.81f)
                    curveTo(15.49f, 21.82f, 15.5f, 21.83f, 15.51f, 21.84f)
                    curveTo(15.56f, 21.89f, 15.61f, 21.93f, 15.67f, 21.98f)
                    curveTo(15.67f, 21.98f, 15.67f, 21.98f, 15.68f, 21.98f)
                    curveTo(15.69f, 21.99f, 15.7f, 22f, 15.71f, 22f)
                    curveTo(16.46f, 22.63f, 17.43f, 23f, 18.5f, 23f)
                    curveTo(20.14f, 23f, 21.57f, 22.12f, 22.35f, 20.82f)
                    curveTo(22.58f, 20.43f, 22.76f, 20f, 22.87f, 19.55f)
                    curveTo(22.96f, 19.21f, 23f, 18.86f, 23f, 18.5f)
                    curveTo(23f, 17.44f, 22.63f, 16.46f, 22f, 15.7f)
                    close()
                    moveTo(20.18f, 19.23f)
                    horizontalLineTo(19.25f)
                    verticalLineTo(20.2f)
                    curveTo(19.25f, 20.61f, 18.91f, 20.95f, 18.5f, 20.95f)
                    curveTo(18.09f, 20.95f, 17.75f, 20.61f, 17.75f, 20.2f)
                    verticalLineTo(19.23f)
                    horizontalLineTo(16.82f)
                    curveTo(16.41f, 19.23f, 16.07f, 18.89f, 16.07f, 18.48f)
                    curveTo(16.07f, 18.07f, 16.41f, 17.73f, 16.82f, 17.73f)
                    horizontalLineTo(17.75f)
                    verticalLineTo(16.84f)
                    curveTo(17.75f, 16.43f, 18.09f, 16.09f, 18.5f, 16.09f)
                    curveTo(18.91f, 16.09f, 19.25f, 16.43f, 19.25f, 16.84f)
                    verticalLineTo(17.73f)
                    horizontalLineTo(20.18f)
                    curveTo(20.59f, 17.73f, 20.93f, 18.07f, 20.93f, 18.48f)
                    curveTo(20.93f, 18.89f, 20.6f, 19.23f, 20.18f, 19.23f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(19.35f, 5.66f)
                    lineTo(13.06f, 2.27f)
                    curveTo(12.4f, 1.91f, 11.6f, 1.91f, 10.93f, 2.27f)
                    lineTo(4.64f, 5.66f)
                    curveTo(4.18f, 5.91f, 3.9f, 6.4f, 3.9f, 6.94f)
                    curveTo(3.9f, 7.48f, 4.18f, 7.97f, 4.64f, 8.22f)
                    lineTo(10.93f, 11.61f)
                    curveTo(11.26f, 11.79f, 11.63f, 11.88f, 11.99f, 11.88f)
                    curveTo(12.35f, 11.88f, 12.72f, 11.79f, 13.05f, 11.61f)
                    lineTo(19.34f, 8.22f)
                    curveTo(19.8f, 7.97f, 20.08f, 7.48f, 20.08f, 6.94f)
                    curveTo(20.1f, 6.4f, 19.81f, 5.91f, 19.35f, 5.66f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(9.9f, 12.79f)
                    lineTo(4.05f, 9.86f)
                    curveTo(3.6f, 9.63f, 3.08f, 9.66f, 2.65f, 9.92f)
                    curveTo(2.22f, 10.18f, 1.97f, 10.64f, 1.97f, 11.14f)
                    verticalLineTo(16.67f)
                    curveTo(1.97f, 17.63f, 2.5f, 18.49f, 3.36f, 18.92f)
                    lineTo(9.21f, 21.84f)
                    curveTo(9.41f, 21.94f, 9.63f, 21.99f, 9.85f, 21.99f)
                    curveTo(10.11f, 21.99f, 10.37f, 21.92f, 10.6f, 21.77f)
                    curveTo(11.03f, 21.51f, 11.28f, 21.05f, 11.28f, 20.55f)
                    verticalLineTo(15.02f)
                    curveTo(11.29f, 14.08f, 10.76f, 13.22f, 9.9f, 12.79f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(22.03f, 11.15f)
                    verticalLineTo(15.74f)
                    curveTo(22.02f, 15.73f, 22.01f, 15.71f, 22f, 15.7f)
                    curveTo(22f, 15.69f, 21.99f, 15.68f, 21.98f, 15.67f)
                    curveTo(21.94f, 15.61f, 21.89f, 15.55f, 21.84f, 15.5f)
                    curveTo(21.83f, 15.49f, 21.82f, 15.47f, 21.81f, 15.46f)
                    curveTo(21f, 14.56f, 19.81f, 14f, 18.5f, 14f)
                    curveTo(17.24f, 14f, 16.09f, 14.52f, 15.27f, 15.36f)
                    curveTo(14.48f, 16.17f, 14f, 17.28f, 14f, 18.5f)
                    curveTo(14f, 19.34f, 14.24f, 20.14f, 14.65f, 20.82f)
                    curveTo(14.82f, 21.11f, 15.03f, 21.37f, 15.26f, 21.61f)
                    lineTo(14.79f, 21.85f)
                    curveTo(14.59f, 21.95f, 14.37f, 22f, 14.15f, 22f)
                    curveTo(13.89f, 22f, 13.63f, 21.93f, 13.39f, 21.78f)
                    curveTo(12.97f, 21.52f, 12.71f, 21.06f, 12.71f, 20.56f)
                    verticalLineTo(15.04f)
                    curveTo(12.71f, 14.08f, 13.24f, 13.22f, 14.1f, 12.79f)
                    lineTo(19.95f, 9.87f)
                    curveTo(20.4f, 9.64f, 20.92f, 9.66f, 21.35f, 9.93f)
                    curveTo(21.77f, 10.19f, 22.03f, 10.65f, 22.03f, 11.15f)
                    close()
                }
            }
        }.build()

        return _IconsaxBoxAdd!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxBoxAdd: ImageVector? = null
