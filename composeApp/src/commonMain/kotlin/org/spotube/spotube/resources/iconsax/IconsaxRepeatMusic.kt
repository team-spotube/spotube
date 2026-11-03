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

val Iconsax.IconsaxRepeatMusic: ImageVector
    get() {
        if (_IconsaxRepeatMusic != null) {
            return _IconsaxRepeatMusic!!
        }
        _IconsaxRepeatMusic = ImageVector.Builder(
            name = "IconsaxRepeatMusic",
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
                    moveTo(3.66f, 16.931f)
                    curveTo(3.47f, 16.931f, 3.28f, 16.861f, 3.13f, 16.711f)
                    curveTo(1.76f, 15.331f, 1f, 13.511f, 1f, 11.581f)
                    curveTo(1f, 7.571f, 4.25f, 4.311f, 8.25f, 4.311f)
                    lineTo(14.32f, 4.331f)
                    lineTo(13.23f, 3.291f)
                    curveTo(12.93f, 3.001f, 12.92f, 2.531f, 13.21f, 2.231f)
                    curveTo(13.5f, 1.931f, 13.97f, 1.921f, 14.27f, 2.211f)
                    lineTo(16.71f, 4.551f)
                    curveTo(16.93f, 4.761f, 17f, 5.091f, 16.89f, 5.371f)
                    curveTo(16.78f, 5.651f, 16.5f, 5.841f, 16.19f, 5.841f)
                    lineTo(8.24f, 5.821f)
                    curveTo(5.07f, 5.821f, 2.49f, 8.411f, 2.49f, 11.591f)
                    curveTo(2.49f, 13.121f, 3.09f, 14.571f, 4.18f, 15.661f)
                    curveTo(4.47f, 15.951f, 4.47f, 16.431f, 4.18f, 16.721f)
                    curveTo(4.04f, 16.861f, 3.85f, 16.931f, 3.66f, 16.931f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(9.75f, 21.501f)
                    curveTo(9.56f, 21.501f, 9.38f, 21.431f, 9.23f, 21.291f)
                    lineTo(6.79f, 18.951f)
                    curveTo(6.57f, 18.741f, 6.5f, 18.411f, 6.61f, 18.131f)
                    curveTo(6.72f, 17.851f, 7f, 17.661f, 7.31f, 17.661f)
                    lineTo(15.26f, 17.681f)
                    curveTo(18.43f, 17.681f, 21.01f, 15.091f, 21.01f, 11.911f)
                    curveTo(21.01f, 10.381f, 20.41f, 8.931f, 19.32f, 7.841f)
                    curveTo(19.03f, 7.551f, 19.03f, 7.071f, 19.32f, 6.781f)
                    curveTo(19.61f, 6.491f, 20.09f, 6.491f, 20.38f, 6.781f)
                    curveTo(21.75f, 8.161f, 22.51f, 9.981f, 22.51f, 11.911f)
                    curveTo(22.51f, 15.921f, 19.26f, 19.181f, 15.26f, 19.181f)
                    lineTo(9.19f, 19.161f)
                    lineTo(10.28f, 20.201f)
                    curveTo(10.58f, 20.491f, 10.59f, 20.961f, 10.3f, 21.261f)
                    curveTo(10.14f, 21.421f, 9.95f, 21.501f, 9.75f, 21.501f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(9f, 15.5f)
                    horizontalLineTo(15f)
                    curveTo(16.93f, 15.5f, 18.5f, 13.92f, 18.5f, 12f)
                    curveTo(18.5f, 10.08f, 16.93f, 8.5f, 15f, 8.5f)
                    horizontalLineTo(9f)
                    curveTo(7.07f, 8.5f, 5.5f, 10.08f, 5.5f, 12f)
                    curveTo(5.5f, 13.92f, 7.07f, 15.5f, 9f, 15.5f)
                    close()
                }
            }
        }.build()

        return _IconsaxRepeatMusic!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxRepeatMusic: ImageVector? = null
