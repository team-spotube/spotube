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

val Iconsax.IconsaxPauseCircle: ImageVector
    get() {
        if (_IconsaxPauseCircle != null) {
            return _IconsaxPauseCircle!!
        }
        _IconsaxPauseCircle = ImageVector.Builder(
            name = "IconsaxPauseCircle",
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
                    moveTo(11.97f, 22f)
                    curveTo(17.493f, 22f, 21.97f, 17.523f, 21.97f, 12f)
                    curveTo(21.97f, 6.477f, 17.493f, 2f, 11.97f, 2f)
                    curveTo(6.447f, 2f, 1.97f, 6.477f, 1.97f, 12f)
                    curveTo(1.97f, 17.523f, 6.447f, 22f, 11.97f, 22f)
                    close()
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(10.72f, 14.53f)
                    verticalLineTo(9.47f)
                    curveTo(10.72f, 8.99f, 10.52f, 8.8f, 10.01f, 8.8f)
                    horizontalLineTo(8.71f)
                    curveTo(8.2f, 8.8f, 8f, 8.99f, 8f, 9.47f)
                    verticalLineTo(14.53f)
                    curveTo(8f, 15.01f, 8.2f, 15.2f, 8.71f, 15.2f)
                    horizontalLineTo(10f)
                    curveTo(10.52f, 15.2f, 10.72f, 15.01f, 10.72f, 14.53f)
                    close()
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 14.53f)
                    verticalLineTo(9.47f)
                    curveTo(16f, 8.99f, 15.8f, 8.8f, 15.29f, 8.8f)
                    horizontalLineTo(14f)
                    curveTo(13.49f, 8.8f, 13.29f, 8.99f, 13.29f, 9.47f)
                    verticalLineTo(14.53f)
                    curveTo(13.29f, 15.01f, 13.49f, 15.2f, 14f, 15.2f)
                    horizontalLineTo(15.29f)
                    curveTo(15.8f, 15.2f, 16f, 15.01f, 16f, 14.53f)
                    close()
                }
            }
        }.build()

        return _IconsaxPauseCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxPauseCircle: ImageVector? = null
