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

val Iconsax.IconsaxSidebarLeftBroken: ImageVector
    get() {
        if (_IconsaxSidebarLeftBroken != null) {
            return _IconsaxSidebarLeftBroken!!
        }
        _IconsaxSidebarLeftBroken = ImageVector.Builder(
            name = "IconsaxSidebarLeftBroken",
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
                    moveTo(7.97f, 2f)
                    verticalLineTo(22f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(14.97f, 9.439f)
                    lineTo(12.41f, 12f)
                    lineTo(14.97f, 14.559f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(2f, 13f)
                    verticalLineTo(15f)
                    curveTo(2f, 20f, 4f, 22f, 9f, 22f)
                    horizontalLineTo(15f)
                    curveTo(20f, 22f, 22f, 20f, 22f, 15f)
                    verticalLineTo(9f)
                    curveTo(22f, 4f, 20f, 2f, 15f, 2f)
                    horizontalLineTo(9f)
                    curveTo(4f, 2f, 2f, 4f, 2f, 9f)
                }
            }
        }.build()

        return _IconsaxSidebarLeftBroken!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSidebarLeftBroken: ImageVector? = null
