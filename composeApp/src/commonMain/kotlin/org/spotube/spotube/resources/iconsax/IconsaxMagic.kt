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

val Iconsax.IconsaxMagic: ImageVector
    get() {
        if (_IconsaxMagic != null) {
            return _IconsaxMagic!!
        }
        _IconsaxMagic = ImageVector.Builder(
            name = "IconsaxMagic",
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
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(7.999f, 16.11f)
                    lineTo(2.109f, 22f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(13.83f, 4.08f)
                    lineTo(15.55f, 4.77f)
                    curveTo(16f, 4.95f, 16f, 5.24f, 15.55f, 5.43f)
                    lineTo(13.83f, 6.12f)
                    lineTo(13.141f, 7.85f)
                    curveTo(12.96f, 8.28f, 12.66f, 8.28f, 12.481f, 7.85f)
                    lineTo(11.79f, 6.12f)
                    lineTo(10.071f, 5.43f)
                    curveTo(9.641f, 5.25f, 9.641f, 4.96f, 10.071f, 4.77f)
                    lineTo(11.79f, 4.08f)
                    lineTo(12.481f, 2.35f)
                    curveTo(12.66f, 1.89f, 12.96f, 1.89f, 13.141f, 2.35f)
                    lineTo(13.83f, 4.08f)
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
                    moveTo(20.34f, 9.15f)
                    lineTo(21.89f, 9.77f)
                    curveTo(22.291f, 9.93f, 22.291f, 10.2f, 21.89f, 10.36f)
                    lineTo(20.34f, 10.98f)
                    lineTo(19.721f, 12.53f)
                    curveTo(19.56f, 12.92f, 19.291f, 12.92f, 19.131f, 12.53f)
                    lineTo(18.51f, 10.98f)
                    lineTo(16.961f, 10.36f)
                    curveTo(16.57f, 10.2f, 16.57f, 9.93f, 16.961f, 9.77f)
                    lineTo(18.51f, 9.15f)
                    lineTo(19.131f, 7.6f)
                    curveTo(19.291f, 7.19f, 19.56f, 7.19f, 19.721f, 7.6f)
                    lineTo(20.34f, 9.15f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12.38f, 11.85f)
                    lineTo(13.71f, 12.38f)
                    curveTo(14.06f, 12.52f, 14.06f, 12.75f, 13.71f, 12.89f)
                    lineTo(12.38f, 13.42f)
                    lineTo(11.85f, 14.75f)
                    curveTo(11.71f, 15.09f, 11.48f, 15.09f, 11.34f, 14.75f)
                    lineTo(10.81f, 13.42f)
                    lineTo(9.48f, 12.89f)
                    curveTo(9.15f, 12.75f, 9.15f, 12.52f, 9.48f, 12.38f)
                    lineTo(10.81f, 11.85f)
                    lineTo(11.34f, 10.52f)
                    curveTo(11.48f, 10.17f, 11.71f, 10.17f, 11.85f, 10.52f)
                    lineTo(12.38f, 11.85f)
                    close()
                }
            }
        }.build()

        return _IconsaxMagic!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMagic: ImageVector? = null
