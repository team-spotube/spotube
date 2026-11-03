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

val Iconsax.SwapHorizontal2: ImageVector
    get() {
        if (_SwapHorizontal2 != null) {
            return _SwapHorizontal2!!
        }
        _SwapHorizontal2 = ImageVector.Builder(
            name = "SwapHorizontal2",
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
                horizontalLineTo(7.81f)
                curveTo(4.17f, 2f, 2f, 4.17f, 2f, 7.81f)
                verticalLineTo(16.18f)
                curveTo(2f, 19.83f, 4.17f, 22f, 7.81f, 22f)
                horizontalLineTo(16.18f)
                curveTo(19.82f, 22f, 21.99f, 19.83f, 21.99f, 16.19f)
                verticalLineTo(7.81f)
                curveTo(22f, 4.17f, 19.83f, 2f, 16.19f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(17.85f, 13.53f)
                curveTo(17.77f, 13.35f, 17.63f, 13.2f, 17.44f, 13.12f)
                curveTo(17.35f, 13.08f, 17.25f, 13.06f, 17.15f, 13.06f)
                horizontalLineTo(6.85f)
                curveTo(6.44f, 13.06f, 6.1f, 13.4f, 6.1f, 13.81f)
                curveTo(6.1f, 14.22f, 6.44f, 14.56f, 6.85f, 14.56f)
                horizontalLineTo(15.35f)
                lineTo(13.59f, 16.32f)
                curveTo(13.3f, 16.61f, 13.3f, 17.09f, 13.59f, 17.38f)
                curveTo(13.74f, 17.53f, 13.93f, 17.6f, 14.12f, 17.6f)
                curveTo(14.31f, 17.6f, 14.5f, 17.53f, 14.65f, 17.38f)
                lineTo(17.69f, 14.34f)
                curveTo(17.76f, 14.27f, 17.81f, 14.19f, 17.85f, 14.1f)
                curveTo(17.92f, 13.92f, 17.92f, 13.71f, 17.85f, 13.53f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(6.15f, 10.47f)
                curveTo(6.23f, 10.65f, 6.37f, 10.8f, 6.56f, 10.88f)
                curveTo(6.65f, 10.92f, 6.75f, 10.94f, 6.85f, 10.94f)
                horizontalLineTo(17.16f)
                curveTo(17.57f, 10.94f, 17.91f, 10.6f, 17.91f, 10.19f)
                curveTo(17.91f, 9.78f, 17.57f, 9.44f, 17.16f, 9.44f)
                horizontalLineTo(8.66f)
                lineTo(10.42f, 7.68f)
                curveTo(10.71f, 7.39f, 10.71f, 6.91f, 10.42f, 6.62f)
                curveTo(10.13f, 6.33f, 9.65f, 6.33f, 9.36f, 6.62f)
                lineTo(6.32f, 9.65f)
                curveTo(6.25f, 9.72f, 6.19f, 9.81f, 6.15f, 9.9f)
                curveTo(6.08f, 10.08f, 6.08f, 10.29f, 6.15f, 10.47f)
                close()
            }
        }.build()

        return _SwapHorizontal2!!
    }

@Suppress("ObjectPropertyName")
private var _SwapHorizontal2: ImageVector? = null
