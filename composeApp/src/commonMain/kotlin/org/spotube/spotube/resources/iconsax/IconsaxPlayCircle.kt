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

val Iconsax.IconsaxPlayCircle: ImageVector
    get() {
        if (_IconsaxPlayCircle != null) {
            return _IconsaxPlayCircle!!
        }
        _IconsaxPlayCircle = ImageVector.Builder(
            name = "IconsaxPlayCircle",
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
                    moveTo(14.91f, 14.12f)
                    curveTo(16.71f, 13.08f, 16.71f, 11.38f, 14.91f, 10.34f)
                    lineTo(13.46f, 9.5f)
                    lineTo(12.01f, 8.66f)
                    curveTo(10.21f, 7.62f, 8.74f, 8.47f, 8.74f, 10.55f)
                    verticalLineTo(12.22f)
                    verticalLineTo(13.89f)
                    curveTo(8.74f, 15.55f, 9.68f, 16.43f, 10.98f, 16.18f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(4f, 6f)
                    curveTo(2.75f, 7.67f, 2f, 9.75f, 2f, 12f)
                    curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                    curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                    curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                    curveTo(10.57f, 2f, 9.2f, 2.3f, 7.97f, 2.85f)
                }
            }
        }.build()

        return _IconsaxPlayCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxPlayCircle: ImageVector? = null
