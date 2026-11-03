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

val Iconsax.IconsaxCheckCircle: ImageVector
    get() {
        if (_IconsaxCheckCircle != null) {
            return _IconsaxCheckCircle!!
        }
        _IconsaxCheckCircle = ImageVector.Builder(
            name = "IconsaxCheckCircle",
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
                    moveTo(12f, 22f)
                    curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
                    curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
                    curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                    curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(7.75f, 12f)
                    lineTo(10.58f, 14.83f)
                    lineTo(16.25f, 9.17f)
                }
            }
        }.build()

        return _IconsaxCheckCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxCheckCircle: ImageVector? = null
