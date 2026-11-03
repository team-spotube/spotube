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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxRepeatArrow: ImageVector
    get() {
        if (_IconsaxRepeatArrow != null) {
            return _IconsaxRepeatArrow!!
        }
        _IconsaxRepeatArrow = ImageVector.Builder(
            name = "IconsaxRepeatArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fillAlpha = 0.4f,
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.4f,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.58f, 5.16f)
                horizontalLineTo(17.42f)
                curveTo(19.08f, 5.16f, 20.42f, 6.5f, 20.42f, 8.16f)
                verticalLineTo(11.48f)
            }
            path(
                fillAlpha = 0.4f,
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.4f,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.74f, 2f)
                lineTo(3.58f, 5.16f)
                lineTo(6.74f, 8.32f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.42f, 18.84f)
                horizontalLineTo(6.58f)
                curveTo(4.92f, 18.84f, 3.58f, 17.5f, 3.58f, 15.84f)
                verticalLineTo(12.519f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.26f, 22f)
                lineTo(20.42f, 18.84f)
                lineTo(17.26f, 15.68f)
            }
        }.build()

        return _IconsaxRepeatArrow!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxRepeatArrow: ImageVector? = null
