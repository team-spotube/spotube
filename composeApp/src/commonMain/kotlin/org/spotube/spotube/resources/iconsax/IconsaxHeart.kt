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

val Iconsax.IconsaxHeart: ImageVector
    get() {
        if (_IconsaxHeart != null) {
            return _IconsaxHeart!!
        }
        _IconsaxHeart = ImageVector.Builder(
            name = "IconsaxHeart",
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
                    moveTo(20.59f, 4.97f)
                    curveTo(21.47f, 5.96f, 22f, 7.26f, 22f, 8.69f)
                    curveTo(22f, 15.69f, 15.52f, 19.82f, 12.62f, 20.82f)
                    curveTo(12.28f, 20.94f, 11.72f, 20.94f, 11.38f, 20.82f)
                    curveTo(8.48f, 19.82f, 2f, 15.69f, 2f, 8.69f)
                    curveTo(2f, 5.6f, 4.49f, 3.1f, 7.56f, 3.1f)
                    curveTo(9.38f, 3.1f, 10.99f, 3.98f, 12f, 5.34f)
                    curveTo(13.01f, 3.98f, 14.63f, 3.1f, 16.44f, 3.1f)
                }
            }
        }.build()

        return _IconsaxHeart!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxHeart: ImageVector? = null
