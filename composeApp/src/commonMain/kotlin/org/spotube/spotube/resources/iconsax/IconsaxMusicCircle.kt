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

val Iconsax.IconsaxMusicCircle: ImageVector
    get() {
        if (_IconsaxMusicCircle != null) {
            return _IconsaxMusicCircle!!
        }
        _IconsaxMusicCircle = ImageVector.Builder(
            name = "IconsaxMusicCircle",
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
                    moveTo(2.58f, 9.42f)
                    curveTo(2.5f, 9.42f, 2.41f, 9.41f, 2.33f, 9.38f)
                    curveTo(1.94f, 9.24f, 1.74f, 8.81f, 1.87f, 8.42f)
                    curveTo(2.54f, 6.54f, 3.7f, 4.89f, 5.25f, 3.64f)
                    curveTo(5.57f, 3.38f, 6.04f, 3.43f, 6.3f, 3.75f)
                    curveTo(6.56f, 4.07f, 6.51f, 4.54f, 6.19f, 4.81f)
                    curveTo(4.87f, 5.88f, 3.86f, 7.3f, 3.29f, 8.92f)
                    curveTo(3.18f, 9.23f, 2.89f, 9.42f, 2.58f, 9.42f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(2.58f, 16.08f)
                    curveTo(2.27f, 16.08f, 1.98f, 15.89f, 1.87f, 15.58f)
                    curveTo(1.46f, 14.42f, 1.25f, 13.21f, 1.25f, 12f)
                    curveTo(1.25f, 11.59f, 1.59f, 11.25f, 2f, 11.25f)
                    curveTo(2.41f, 11.25f, 2.75f, 11.59f, 2.75f, 12f)
                    curveTo(2.75f, 13.04f, 2.93f, 14.08f, 3.29f, 15.08f)
                    curveTo(3.43f, 15.47f, 3.22f, 15.9f, 2.83f, 16.04f)
                    curveTo(2.75f, 16.07f, 2.66f, 16.08f, 2.58f, 16.08f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 22.75f)
                    curveTo(10.94f, 22.75f, 9.89f, 22.59f, 8.87f, 22.28f)
                    curveTo(8.47f, 22.16f, 8.25f, 21.74f, 8.37f, 21.34f)
                    curveTo(8.49f, 20.94f, 8.91f, 20.72f, 9.31f, 20.84f)
                    curveTo(10.18f, 21.11f, 11.09f, 21.24f, 12f, 21.24f)
                    curveTo(17.1f, 21.24f, 21.25f, 17.09f, 21.25f, 11.99f)
                    curveTo(21.25f, 11.47f, 21.2f, 10.93f, 21.1f, 10.36f)
                    curveTo(21.03f, 9.95f, 21.3f, 9.56f, 21.71f, 9.49f)
                    curveTo(22.11f, 9.42f, 22.51f, 9.69f, 22.58f, 10.1f)
                    curveTo(22.7f, 10.76f, 22.76f, 11.38f, 22.76f, 12f)
                    curveTo(22.75f, 17.93f, 17.93f, 22.75f, 12f, 22.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(5.72f, 20.5f)
                    curveTo(5.55f, 20.5f, 5.39f, 20.45f, 5.25f, 20.33f)
                    curveTo(4.68f, 19.87f, 4.22f, 19.43f, 3.83f, 18.98f)
                    curveTo(3.56f, 18.67f, 3.6f, 18.19f, 3.91f, 17.92f)
                    curveTo(4.23f, 17.65f, 4.7f, 17.69f, 4.97f, 18f)
                    curveTo(5.3f, 18.38f, 5.7f, 18.76f, 6.19f, 19.16f)
                    curveTo(6.51f, 19.42f, 6.56f, 19.89f, 6.3f, 20.21f)
                    curveTo(6.16f, 20.4f, 5.94f, 20.5f, 5.72f, 20.5f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(20.24f, 7.09f)
                    curveTo(20f, 7.09f, 19.77f, 6.98f, 19.62f, 6.76f)
                    curveTo(17.9f, 4.25f, 15.04f, 2.75f, 12f, 2.75f)
                    curveTo(11.09f, 2.75f, 10.18f, 2.88f, 9.31f, 3.15f)
                    curveTo(8.92f, 3.27f, 8.5f, 3.05f, 8.37f, 2.65f)
                    curveTo(8.24f, 2.25f, 8.47f, 1.83f, 8.87f, 1.71f)
                    curveTo(9.89f, 1.41f, 10.94f, 1.25f, 12f, 1.25f)
                    curveTo(15.54f, 1.25f, 18.85f, 3f, 20.86f, 5.92f)
                    curveTo(21.09f, 6.26f, 21.01f, 6.73f, 20.67f, 6.96f)
                    curveTo(20.54f, 7.05f, 20.39f, 7.09f, 20.24f, 7.09f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.03f, 6.5f)
                    curveTo(15.7f, 6.25f, 15.1f, 6f, 14.14f, 6.26f)
                    lineTo(10.95f, 7.12f)
                    curveTo(10.03f, 7.38f, 9.43f, 8.16f, 9.43f, 9.12f)
                    verticalLineTo(10.76f)
                    verticalLineTo(13.34f)
                    curveTo(9.17f, 13.241f, 8.89f, 13.181f, 8.59f, 13.181f)
                    curveTo(7.3f, 13.181f, 6.25f, 14.231f, 6.25f, 15.521f)
                    curveTo(6.25f, 16.81f, 7.3f, 17.861f, 8.59f, 17.861f)
                    curveTo(9.87f, 17.861f, 10.9f, 16.83f, 10.92f, 15.561f)
                    curveTo(10.92f, 15.55f, 10.93f, 15.54f, 10.93f, 15.521f)
                    verticalLineTo(11.33f)
                    lineTo(15.25f, 10.151f)
                    verticalLineTo(12.281f)
                    curveTo(14.99f, 12.181f, 14.71f, 12.12f, 14.41f, 12.12f)
                    curveTo(13.12f, 12.12f, 12.07f, 13.17f, 12.07f, 14.46f)
                    curveTo(12.07f, 15.75f, 13.12f, 16.801f, 14.41f, 16.801f)
                    curveTo(15.7f, 16.801f, 16.75f, 15.75f, 16.75f, 14.46f)
                    verticalLineTo(9.17f)
                    verticalLineTo(8.25f)
                    curveTo(16.75f, 7.45f, 16.51f, 6.86f, 16.03f, 6.5f)
                    close()
                    moveTo(8.59f, 16.361f)
                    curveTo(8.13f, 16.361f, 7.75f, 15.981f, 7.75f, 15.521f)
                    curveTo(7.75f, 15.061f, 8.13f, 14.681f, 8.59f, 14.681f)
                    curveTo(9.05f, 14.681f, 9.43f, 15.061f, 9.43f, 15.521f)
                    curveTo(9.43f, 15.981f, 9.05f, 16.361f, 8.59f, 16.361f)
                    close()
                    moveTo(14.41f, 15.3f)
                    curveTo(13.95f, 15.3f, 13.57f, 14.92f, 13.57f, 14.46f)
                    curveTo(13.57f, 14f, 13.95f, 13.62f, 14.41f, 13.62f)
                    curveTo(14.87f, 13.62f, 15.25f, 14f, 15.25f, 14.46f)
                    curveTo(15.25f, 14.92f, 14.87f, 15.3f, 14.41f, 15.3f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicCircle: ImageVector? = null
