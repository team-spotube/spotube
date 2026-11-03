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

val Iconsax.IconsaxDirectboxReceive: ImageVector
    get() {
        if (_IconsaxDirectboxReceive != null) {
            return _IconsaxDirectboxReceive!!
        }
        _IconsaxDirectboxReceive = ImageVector.Builder(
            name = "IconsaxDirectboxReceive",
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
                    moveTo(14.79f, 4f)
                    horizontalLineTo(9.21f)
                    curveTo(4.79f, 4f, 4.79f, 6.35f, 4.79f, 8.42f)
                    verticalLineTo(12.21f)
                    curveTo(4.79f, 12.43f, 4.89f, 12.63f, 5.06f, 12.76f)
                    curveTo(5.23f, 12.89f, 5.46f, 12.94f, 5.67f, 12.88f)
                    curveTo(6.12f, 12.76f, 6.68f, 12.7f, 7.35f, 12.7f)
                    curveTo(8.02f, 12.7f, 8.16f, 12.78f, 8.56f, 13.08f)
                    lineTo(9.47f, 14.04f)
                    curveTo(10.12f, 14.74f, 11.05f, 15.14f, 12.01f, 15.14f)
                    curveTo(12.97f, 15.14f, 13.89f, 14.74f, 14.55f, 14.04f)
                    lineTo(15.46f, 13.08f)
                    curveTo(15.86f, 12.78f, 16f, 12.7f, 16.67f, 12.7f)
                    curveTo(17.34f, 12.7f, 17.9f, 12.76f, 18.35f, 12.88f)
                    curveTo(18.56f, 12.94f, 18.78f, 12.89f, 18.96f, 12.76f)
                    curveTo(19.13f, 12.63f, 19.23f, 12.42f, 19.23f, 12.21f)
                    verticalLineTo(8.42f)
                    curveTo(19.21f, 6.35f, 19.21f, 4f, 14.79f, 4f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(14.275f, 6.8f)
                    curveTo(14.015f, 6.54f, 13.585f, 6.54f, 13.325f, 6.8f)
                    lineTo(12.675f, 7.45f)
                    verticalLineTo(2.67f)
                    curveTo(12.675f, 2.3f, 12.365f, 2f, 11.995f, 2f)
                    curveTo(11.625f, 2f, 11.315f, 2.3f, 11.315f, 2.67f)
                    verticalLineTo(7.44f)
                    lineTo(10.675f, 6.8f)
                    curveTo(10.415f, 6.54f, 9.985f, 6.54f, 9.725f, 6.8f)
                    curveTo(9.465f, 7.06f, 9.465f, 7.49f, 9.725f, 7.75f)
                    lineTo(11.525f, 9.55f)
                    curveTo(11.535f, 9.56f, 11.535f, 9.56f, 11.545f, 9.56f)
                    curveTo(11.605f, 9.61f, 11.665f, 9.66f, 11.745f, 9.69f)
                    curveTo(11.825f, 9.72f, 11.915f, 9.74f, 12.005f, 9.74f)
                    curveTo(12.095f, 9.74f, 12.175f, 9.72f, 12.265f, 9.69f)
                    curveTo(12.345f, 9.66f, 12.425f, 9.61f, 12.485f, 9.54f)
                    lineTo(14.285f, 7.74f)
                    curveTo(14.535f, 7.49f, 14.535f, 7.06f, 14.275f, 6.8f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(18.69f, 11.531f)
                    curveTo(18.12f, 11.381f, 17.45f, 11.301f, 16.65f, 11.301f)
                    curveTo(15.54f, 11.301f, 15.13f, 11.571f, 14.56f, 12.001f)
                    curveTo(14.53f, 12.021f, 14.5f, 12.051f, 14.47f, 12.081f)
                    lineTo(13.52f, 13.091f)
                    curveTo(12.72f, 13.931f, 11.28f, 13.941f, 10.48f, 13.081f)
                    lineTo(9.53f, 12.081f)
                    curveTo(9.5f, 12.051f, 9.47f, 12.021f, 9.44f, 12.001f)
                    curveTo(8.87f, 11.571f, 8.46f, 11.301f, 7.35f, 11.301f)
                    curveTo(6.55f, 11.301f, 5.88f, 11.381f, 5.31f, 11.531f)
                    curveTo(2.93f, 12.171f, 2.93f, 14.061f, 2.93f, 15.721f)
                    verticalLineTo(16.651f)
                    curveTo(2.93f, 19.161f, 2.93f, 22.001f, 8.28f, 22.001f)
                    horizontalLineTo(15.72f)
                    curveTo(19.27f, 22.001f, 21.07f, 20.201f, 21.07f, 16.651f)
                    verticalLineTo(15.721f)
                    curveTo(21.07f, 14.061f, 21.07f, 12.171f, 18.69f, 11.531f)
                    close()
                    moveTo(14.33f, 18.401f)
                    horizontalLineTo(9.67f)
                    curveTo(9.29f, 18.401f, 8.98f, 18.091f, 8.98f, 17.701f)
                    curveTo(8.98f, 17.311f, 9.29f, 17.001f, 9.67f, 17.001f)
                    horizontalLineTo(14.33f)
                    curveTo(14.71f, 17.001f, 15.02f, 17.311f, 15.02f, 17.701f)
                    curveTo(15.02f, 18.091f, 14.71f, 18.401f, 14.33f, 18.401f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(15.02f, 17.7f)
                    curveTo(15.02f, 18.09f, 14.71f, 18.4f, 14.33f, 18.4f)
                    horizontalLineTo(9.67f)
                    curveTo(9.29f, 18.4f, 8.98f, 18.09f, 8.98f, 17.7f)
                    curveTo(8.98f, 17.31f, 9.29f, 17f, 9.67f, 17f)
                    horizontalLineTo(14.33f)
                    curveTo(14.71f, 17f, 15.02f, 17.31f, 15.02f, 17.7f)
                    close()
                }
            }
        }.build()

        return _IconsaxDirectboxReceive!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxDirectboxReceive: ImageVector? = null
