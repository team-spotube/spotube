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

val Iconsax.IconsaxSetting2: ImageVector
    get() {
        if (_IconsaxSetting2 != null) {
            return _IconsaxSetting2!!
        }
        _IconsaxSetting2 = ImageVector.Builder(
            name = "IconsaxSetting2",
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
                    moveTo(2f, 12.879f)
                    verticalLineTo(11.119f)
                    curveTo(2f, 10.079f, 2.85f, 9.219f, 3.9f, 9.219f)
                    curveTo(5.71f, 9.219f, 6.45f, 7.939f, 5.54f, 6.369f)
                    curveTo(5.02f, 5.469f, 5.33f, 4.299f, 6.24f, 3.779f)
                    lineTo(7.97f, 2.789f)
                    curveTo(8.76f, 2.319f, 9.78f, 2.599f, 10.25f, 3.389f)
                    lineTo(10.36f, 3.579f)
                    curveTo(11.26f, 5.149f, 12.74f, 5.149f, 13.65f, 3.579f)
                    lineTo(13.76f, 3.389f)
                    curveTo(14.23f, 2.599f, 15.25f, 2.319f, 16.04f, 2.789f)
                    lineTo(17.77f, 3.779f)
                    curveTo(18.68f, 4.299f, 18.99f, 5.469f, 18.47f, 6.369f)
                    curveTo(17.56f, 7.939f, 18.3f, 9.219f, 20.11f, 9.219f)
                    curveTo(21.15f, 9.219f, 22.01f, 10.069f, 22.01f, 11.119f)
                    verticalLineTo(12.879f)
                    curveTo(22.01f, 13.919f, 21.16f, 14.779f, 20.11f, 14.779f)
                    curveTo(18.3f, 14.779f, 17.56f, 16.059f, 18.47f, 17.629f)
                    curveTo(18.99f, 18.539f, 18.68f, 19.699f, 17.77f, 20.219f)
                    lineTo(16.04f, 21.209f)
                    curveTo(15.25f, 21.679f, 14.23f, 21.399f, 13.76f, 20.609f)
                    lineTo(13.65f, 20.419f)
                    curveTo(12.75f, 18.849f, 11.27f, 18.849f, 10.36f, 20.419f)
                    lineTo(10.25f, 20.609f)
                    curveTo(9.78f, 21.399f, 8.76f, 21.679f, 7.97f, 21.209f)
                    lineTo(6.24f, 20.219f)
                    curveTo(5.33f, 19.699f, 5.02f, 18.529f, 5.54f, 17.629f)
                    curveTo(6.45f, 16.059f, 5.71f, 14.779f, 3.9f, 14.779f)
                    curveTo(2.85f, 14.779f, 2f, 13.919f, 2f, 12.879f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 15.25f)
                    curveTo(13.795f, 15.25f, 15.25f, 13.795f, 15.25f, 12f)
                    curveTo(15.25f, 10.205f, 13.795f, 8.75f, 12f, 8.75f)
                    curveTo(10.205f, 8.75f, 8.75f, 10.205f, 8.75f, 12f)
                    curveTo(8.75f, 13.795f, 10.205f, 15.25f, 12f, 15.25f)
                    close()
                }
            }
        }.build()

        return _IconsaxSetting2!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSetting2: ImageVector? = null
