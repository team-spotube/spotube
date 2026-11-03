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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxRepeateOne: ImageVector
    get() {
        if (_IconsaxRepeateOne != null) {
            return _IconsaxRepeateOne!!
        }
        _IconsaxRepeateOne = ImageVector.Builder(
            name = "IconsaxRepeateOne",
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
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(14f, 3f)
                    lineTo(16.44f, 5.34f)
                    lineTo(8.49f, 5.32f)
                    curveTo(4.92f, 5.32f, 1.99f, 8.25f, 1.99f, 11.84f)
                    curveTo(1.99f, 13.63f, 2.72f, 15.26f, 3.9f, 16.44f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(10f, 21f)
                    lineTo(7.56f, 18.66f)
                    lineTo(15.51f, 18.68f)
                    curveTo(19.08f, 18.68f, 22.01f, 15.75f, 22.01f, 12.16f)
                    curveTo(22.01f, 10.37f, 21.28f, 8.74f, 20.1f, 7.56f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12.25f, 14.67f)
                    verticalLineTo(9.33f)
                    lineTo(10.75f, 11f)
                }
            }
        }.build()

        return _IconsaxRepeateOne!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxRepeateOne: ImageVector? = null
