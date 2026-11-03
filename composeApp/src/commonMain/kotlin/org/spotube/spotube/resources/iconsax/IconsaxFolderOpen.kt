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

val Iconsax.IconsaxFolderOpen: ImageVector
    get() {
        if (_IconsaxFolderOpen != null) {
            return _IconsaxFolderOpen!!
        }
        _IconsaxFolderOpen = ImageVector.Builder(
            name = "IconsaxFolderOpen",
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
                    strokeLineWidth = 1.5f
                ) {
                    moveTo(21.67f, 14.3f)
                    lineTo(21.27f, 19.3f)
                    curveTo(21.12f, 20.83f, 21f, 22f, 18.29f, 22f)
                    horizontalLineTo(5.71f)
                    curveTo(3f, 22f, 2.88f, 20.83f, 2.73f, 19.3f)
                    lineTo(2.33f, 14.3f)
                    curveTo(2.25f, 13.47f, 2.51f, 12.7f, 2.98f, 12.11f)
                    curveTo(2.99f, 12.1f, 2.99f, 12.1f, 3f, 12.09f)
                    curveTo(3.55f, 11.42f, 4.38f, 11f, 5.31f, 11f)
                    horizontalLineTo(18.69f)
                    curveTo(19.62f, 11f, 20.44f, 11.42f, 20.98f, 12.07f)
                    curveTo(20.99f, 12.08f, 21f, 12.09f, 21f, 12.1f)
                    curveTo(21.49f, 12.69f, 21.76f, 13.46f, 21.67f, 14.3f)
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
                    moveTo(3.5f, 11.43f)
                    verticalLineTo(6.28f)
                    curveTo(3.5f, 2.88f, 4.35f, 2.03f, 7.75f, 2.03f)
                    horizontalLineTo(9.02f)
                    curveTo(10.29f, 2.03f, 10.58f, 2.41f, 11.06f, 3.05f)
                    lineTo(12.33f, 4.75f)
                    curveTo(12.65f, 5.17f, 12.84f, 5.43f, 13.69f, 5.43f)
                    horizontalLineTo(16.24f)
                    curveTo(19.64f, 5.43f, 20.49f, 6.28f, 20.49f, 9.68f)
                    verticalLineTo(11.47f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(9.43f, 17f)
                    horizontalLineTo(14.57f)
                }
            }
        }.build()

        return _IconsaxFolderOpen!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxFolderOpen: ImageVector? = null
