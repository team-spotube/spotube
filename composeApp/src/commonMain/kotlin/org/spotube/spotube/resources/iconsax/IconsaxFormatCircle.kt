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

val Iconsax.IconsaxFormatCircle: ImageVector
    get() {
        if (_IconsaxFormatCircle != null) {
            return _IconsaxFormatCircle!!
        }
        _IconsaxFormatCircle = ImageVector.Builder(
            name = "IconsaxFormatCircle",
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
                    moveTo(21.5f, 5.35f)
                    curveTo(21.5f, 6.26f, 21.07f, 7.07f, 20.41f, 7.59f)
                    curveTo(19.93f, 7.97f, 19.32f, 8.2f, 18.65f, 8.2f)
                    curveTo(17.07f, 8.2f, 15.8f, 6.93f, 15.8f, 5.35f)
                    curveTo(15.8f, 4.68f, 16.03f, 4.08f, 16.41f, 3.59f)
                    horizontalLineTo(16.42f)
                    curveTo(16.93f, 2.93f, 17.74f, 2.5f, 18.65f, 2.5f)
                    curveTo(20.23f, 2.5f, 21.5f, 3.77f, 21.5f, 5.35f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8.2f, 5.35f)
                    curveTo(8.2f, 6.93f, 6.93f, 8.2f, 5.35f, 8.2f)
                    curveTo(4.68f, 8.2f, 4.08f, 7.97f, 3.59f, 7.59f)
                    curveTo(2.93f, 7.07f, 2.5f, 6.26f, 2.5f, 5.35f)
                    curveTo(2.5f, 3.77f, 3.77f, 2.5f, 5.35f, 2.5f)
                    curveTo(6.26f, 2.5f, 7.07f, 2.93f, 7.59f, 3.59f)
                    curveTo(7.97f, 4.08f, 8.2f, 4.68f, 8.2f, 5.35f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(21.5f, 18.651f)
                    curveTo(21.5f, 20.231f, 20.23f, 21.501f, 18.65f, 21.501f)
                    curveTo(17.74f, 21.501f, 16.93f, 21.071f, 16.42f, 20.411f)
                    horizontalLineTo(16.41f)
                    curveTo(16.03f, 19.931f, 15.8f, 19.321f, 15.8f, 18.651f)
                    curveTo(15.8f, 17.071f, 17.07f, 15.801f, 18.65f, 15.801f)
                    curveTo(19.32f, 15.801f, 19.92f, 16.031f, 20.41f, 16.411f)
                    verticalLineTo(16.421f)
                    curveTo(21.07f, 16.931f, 21.5f, 17.741f, 21.5f, 18.651f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8.2f, 18.651f)
                    curveTo(8.2f, 19.321f, 7.97f, 19.921f, 7.59f, 20.411f)
                    curveTo(7.07f, 21.081f, 6.26f, 21.501f, 5.35f, 21.501f)
                    curveTo(3.77f, 21.501f, 2.5f, 20.231f, 2.5f, 18.651f)
                    curveTo(2.5f, 17.741f, 2.93f, 16.931f, 3.59f, 16.421f)
                    verticalLineTo(16.411f)
                    curveTo(4.07f, 16.031f, 4.68f, 15.801f, 5.35f, 15.801f)
                    curveTo(6.93f, 15.801f, 8.2f, 17.071f, 8.2f, 18.651f)
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
                    moveTo(21.5f, 12f)
                    curveTo(21.5f, 13.6f, 21.11f, 15.09f, 20.41f, 16.41f)
                    curveTo(19.93f, 16.03f, 19.32f, 15.8f, 18.65f, 15.8f)
                    curveTo(17.07f, 15.8f, 15.8f, 17.07f, 15.8f, 18.65f)
                    curveTo(15.8f, 19.32f, 16.03f, 19.92f, 16.41f, 20.41f)
                    curveTo(15.09f, 21.11f, 13.6f, 21.5f, 12f, 21.5f)
                    curveTo(10.41f, 21.5f, 8.91f, 21.11f, 7.59f, 20.41f)
                    curveTo(7.97f, 19.93f, 8.2f, 19.32f, 8.2f, 18.65f)
                    curveTo(8.2f, 17.07f, 6.93f, 15.8f, 5.35f, 15.8f)
                    curveTo(4.68f, 15.8f, 4.08f, 16.03f, 3.59f, 16.41f)
                    curveTo(2.89f, 15.09f, 2.5f, 13.6f, 2.5f, 12f)
                    curveTo(2.5f, 10.41f, 2.89f, 8.91f, 3.59f, 7.59f)
                    curveTo(4.08f, 7.97f, 4.68f, 8.2f, 5.35f, 8.2f)
                    curveTo(6.93f, 8.2f, 8.2f, 6.93f, 8.2f, 5.35f)
                    curveTo(8.2f, 4.68f, 7.97f, 4.08f, 7.59f, 3.59f)
                    curveTo(8.91f, 2.89f, 10.41f, 2.5f, 12f, 2.5f)
                    curveTo(13.6f, 2.5f, 15.09f, 2.89f, 16.41f, 3.59f)
                    curveTo(16.03f, 4.07f, 15.8f, 4.68f, 15.8f, 5.35f)
                    curveTo(15.8f, 6.93f, 17.07f, 8.2f, 18.65f, 8.2f)
                    curveTo(19.32f, 8.2f, 19.92f, 7.97f, 20.41f, 7.59f)
                    curveTo(21.11f, 8.91f, 21.5f, 10.41f, 21.5f, 12f)
                    close()
                }
            }
        }.build()

        return _IconsaxFormatCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxFormatCircle: ImageVector? = null
