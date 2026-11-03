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

val Iconsax.IconsaxShare: ImageVector
    get() {
        if (_IconsaxShare != null) {
            return _IconsaxShare!!
        }
        _IconsaxShare = ImageVector.Builder(
            name = "IconsaxShare",
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
                    moveTo(20.36f, 12.73f)
                    curveTo(19.99f, 12.73f, 19.68f, 12.45f, 19.64f, 12.08f)
                    curveTo(19.4f, 9.88f, 18.22f, 7.9f, 16.4f, 6.64f)
                    curveTo(16.07f, 6.41f, 15.99f, 5.96f, 16.22f, 5.63f)
                    curveTo(16.45f, 5.3f, 16.9f, 5.22f, 17.23f, 5.45f)
                    curveTo(19.4f, 6.96f, 20.8f, 9.32f, 21.09f, 11.93f)
                    curveTo(21.13f, 12.33f, 20.84f, 12.69f, 20.44f, 12.73f)
                    curveTo(20.41f, 12.73f, 20.39f, 12.73f, 20.36f, 12.73f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(3.74f, 12.779f)
                    curveTo(3.72f, 12.779f, 3.69f, 12.779f, 3.67f, 12.779f)
                    curveTo(3.27f, 12.739f, 2.98f, 12.379f, 3.02f, 11.979f)
                    curveTo(3.29f, 9.369f, 4.67f, 7.009f, 6.82f, 5.489f)
                    curveTo(7.14f, 5.259f, 7.6f, 5.339f, 7.83f, 5.659f)
                    curveTo(8.06f, 5.989f, 7.98f, 6.439f, 7.66f, 6.669f)
                    curveTo(5.86f, 7.949f, 4.69f, 9.929f, 4.47f, 12.119f)
                    curveTo(4.43f, 12.499f, 4.11f, 12.779f, 3.74f, 12.779f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(15.99f, 21.1f)
                    curveTo(14.76f, 21.69f, 13.44f, 21.99f, 12.06f, 21.99f)
                    curveTo(10.62f, 21.99f, 9.25f, 21.67f, 7.97f, 21.02f)
                    curveTo(7.61f, 20.85f, 7.47f, 20.41f, 7.65f, 20.05f)
                    curveTo(7.82f, 19.69f, 8.26f, 19.55f, 8.62f, 19.72f)
                    curveTo(9.25f, 20.04f, 9.92f, 20.26f, 10.6f, 20.39f)
                    curveTo(11.52f, 20.57f, 12.46f, 20.58f, 13.38f, 20.42f)
                    curveTo(14.06f, 20.3f, 14.73f, 20.09f, 15.35f, 19.79f)
                    curveTo(15.72f, 19.62f, 16.16f, 19.76f, 16.32f, 20.13f)
                    curveTo(16.5f, 20.49f, 16.36f, 20.93f, 15.99f, 21.1f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12.05f, 2.01f)
                    curveTo(10.5f, 2.01f, 9.23f, 3.27f, 9.23f, 4.83f)
                    curveTo(9.23f, 6.39f, 10.49f, 7.65f, 12.05f, 7.65f)
                    curveTo(13.61f, 7.65f, 14.87f, 6.39f, 14.87f, 4.83f)
                    curveTo(14.87f, 3.27f, 13.61f, 2.01f, 12.05f, 2.01f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(5.05f, 13.869f)
                    curveTo(3.5f, 13.869f, 2.23f, 15.129f, 2.23f, 16.689f)
                    curveTo(2.23f, 18.249f, 3.49f, 19.509f, 5.05f, 19.509f)
                    curveTo(6.61f, 19.509f, 7.87f, 18.249f, 7.87f, 16.689f)
                    curveTo(7.87f, 15.129f, 6.6f, 13.869f, 5.05f, 13.869f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(18.95f, 13.869f)
                    curveTo(17.4f, 13.869f, 16.13f, 15.129f, 16.13f, 16.689f)
                    curveTo(16.13f, 18.249f, 17.39f, 19.509f, 18.95f, 19.509f)
                    curveTo(20.51f, 19.509f, 21.77f, 18.249f, 21.77f, 16.689f)
                    curveTo(21.77f, 15.129f, 20.51f, 13.869f, 18.95f, 13.869f)
                    close()
                }
            }
        }.build()

        return _IconsaxShare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxShare: ImageVector? = null
