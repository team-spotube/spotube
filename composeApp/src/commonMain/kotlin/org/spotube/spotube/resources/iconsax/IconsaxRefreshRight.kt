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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxRefreshRight: ImageVector
    get() {
        if (_IconsaxRefreshRight != null) {
            return _IconsaxRefreshRight!!
        }
        _IconsaxRefreshRight = ImageVector.Builder(
            name = "IconsaxRefreshRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f
            ) {
                moveTo(16.19f, 2f)
                horizontalLineTo(7.82f)
                curveTo(4.17f, 2f, 2f, 4.17f, 2f, 7.81f)
                verticalLineTo(16.18f)
                curveTo(2f, 19.82f, 4.17f, 21.99f, 7.81f, 21.99f)
                horizontalLineTo(16.18f)
                curveTo(19.82f, 21.99f, 21.99f, 19.82f, 21.99f, 16.18f)
                verticalLineTo(7.81f)
                curveTo(22f, 4.17f, 19.83f, 2f, 16.19f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(16.78f, 9.07f)
                curveTo(16.55f, 8.72f, 16.08f, 8.63f, 15.74f, 8.86f)
                curveTo(15.4f, 9.09f, 15.3f, 9.56f, 15.53f, 9.9f)
                curveTo(16f, 10.6f, 16.24f, 11.42f, 16.24f, 12.26f)
                curveTo(16.24f, 14.6f, 14.33f, 16.51f, 11.99f, 16.51f)
                curveTo(9.65f, 16.51f, 7.74f, 14.6f, 7.74f, 12.26f)
                curveTo(7.74f, 9.92f, 9.65f, 8.01f, 11.99f, 8.01f)
                curveTo(12.18f, 8.01f, 12.36f, 8.03f, 12.55f, 8.05f)
                lineTo(12f, 8.46f)
                curveTo(11.67f, 8.7f, 11.59f, 9.17f, 11.84f, 9.51f)
                curveTo(11.99f, 9.71f, 12.22f, 9.82f, 12.45f, 9.82f)
                curveTo(12.6f, 9.82f, 12.76f, 9.77f, 12.89f, 9.68f)
                lineTo(14.83f, 8.26f)
                curveTo(14.84f, 8.25f, 14.84f, 8.24f, 14.85f, 8.24f)
                curveTo(14.86f, 8.23f, 14.87f, 8.23f, 14.88f, 8.22f)
                curveTo(14.91f, 8.19f, 14.93f, 8.16f, 14.95f, 8.13f)
                curveTo(14.98f, 8.09f, 15.02f, 8.06f, 15.04f, 8.01f)
                curveTo(15.06f, 7.97f, 15.07f, 7.92f, 15.09f, 7.88f)
                curveTo(15.1f, 7.83f, 15.12f, 7.79f, 15.13f, 7.74f)
                curveTo(15.14f, 7.69f, 15.13f, 7.65f, 15.12f, 7.6f)
                curveTo(15.12f, 7.55f, 15.12f, 7.51f, 15.1f, 7.46f)
                curveTo(15.09f, 7.41f, 15.06f, 7.37f, 15.04f, 7.32f)
                curveTo(15.02f, 7.29f, 15.02f, 7.25f, 14.99f, 7.21f)
                curveTo(14.98f, 7.2f, 14.97f, 7.2f, 14.97f, 7.19f)
                curveTo(14.96f, 7.18f, 14.96f, 7.17f, 14.95f, 7.16f)
                lineTo(13.28f, 5.25f)
                curveTo(13.01f, 4.94f, 12.53f, 4.9f, 12.22f, 5.18f)
                curveTo(11.91f, 5.45f, 11.88f, 5.93f, 12.15f, 6.24f)
                lineTo(12.43f, 6.56f)
                curveTo(12.29f, 6.55f, 12.15f, 6.53f, 12f, 6.53f)
                curveTo(8.83f, 6.53f, 6.25f, 9.11f, 6.25f, 12.28f)
                curveTo(6.25f, 15.45f, 8.83f, 18.03f, 12f, 18.03f)
                curveTo(15.17f, 18.03f, 17.75f, 15.45f, 17.75f, 12.28f)
                curveTo(17.75f, 11.12f, 17.42f, 10.02f, 16.78f, 9.07f)
                close()
            }
        }.build()

        return _IconsaxRefreshRight!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxRefreshRight: ImageVector? = null
