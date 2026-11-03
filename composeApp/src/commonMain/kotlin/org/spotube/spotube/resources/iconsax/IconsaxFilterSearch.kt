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

val Iconsax.IconsaxFilterSearch: ImageVector
    get() {
        if (_IconsaxFilterSearch != null) {
            return _IconsaxFilterSearch!!
        }
        _IconsaxFilterSearch = ImageVector.Builder(
            name = "IconsaxFilterSearch",
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
                    moveTo(19.75f, 15.41f)
                    lineTo(18.9f, 14.56f)
                    curveTo(19.34f, 13.89f, 19.6f, 13.1f, 19.6f, 12.24f)
                    curveTo(19.6f, 9.9f, 17.7f, 8f, 15.36f, 8f)
                    curveTo(13.02f, 8f, 11.12f, 9.9f, 11.12f, 12.24f)
                    curveTo(11.12f, 14.58f, 13.02f, 16.48f, 15.36f, 16.48f)
                    curveTo(16.22f, 16.48f, 17.02f, 16.22f, 17.68f, 15.78f)
                    lineTo(18.53f, 16.63f)
                    curveTo(18.7f, 16.8f, 18.92f, 16.88f, 19.14f, 16.88f)
                    curveTo(19.36f, 16.88f, 19.58f, 16.8f, 19.75f, 16.63f)
                    curveTo(20.08f, 16.29f, 20.08f, 15.74f, 19.75f, 15.41f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(5.41f, 2f)
                    horizontalLineTo(18.58f)
                    curveTo(19.68f, 2f, 20.58f, 2.91f, 20.58f, 4.02f)
                    verticalLineTo(6.24f)
                    curveTo(20.58f, 7.05f, 20.08f, 8.06f, 19.58f, 8.56f)
                    lineTo(15.29f, 12.4f)
                    curveTo(14.69f, 12.91f, 14.29f, 13.92f, 14.29f, 14.72f)
                    verticalLineTo(19.06f)
                    curveTo(14.29f, 19.67f, 13.89f, 20.47f, 13.39f, 20.78f)
                    lineTo(11.99f, 21.69f)
                    curveTo(10.69f, 22.5f, 8.9f, 21.59f, 8.9f, 19.97f)
                    verticalLineTo(14.62f)
                    curveTo(8.9f, 13.91f, 8.5f, 13f, 8.1f, 12.5f)
                    lineTo(4.31f, 8.46f)
                    curveTo(3.81f, 7.95f, 3.41f, 7.05f, 3.41f, 6.44f)
                    verticalLineTo(4.12f)
                    curveTo(3.42f, 2.91f, 4.32f, 2f, 5.41f, 2f)
                    close()
                }
            }
        }.build()

        return _IconsaxFilterSearch!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxFilterSearch: ImageVector? = null
