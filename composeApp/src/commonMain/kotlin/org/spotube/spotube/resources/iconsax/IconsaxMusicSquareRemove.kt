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

val Iconsax.IconsaxMusicSquareRemove: ImageVector
    get() {
        if (_IconsaxMusicSquareRemove != null) {
            return _IconsaxMusicSquareRemove!!
        }
        _IconsaxMusicSquareRemove = ImageVector.Builder(
            name = "IconsaxMusicSquareRemove",
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
                    moveTo(21f, 8.65f)
                    verticalLineTo(14.35f)
                    curveTo(21f, 14.69f, 20.99f, 15.02f, 20.97f, 15.33f)
                    curveTo(20.25f, 14.51f, 19.18f, 14f, 18f, 14f)
                    curveTo(15.79f, 14f, 14f, 15.79f, 14f, 18f)
                    curveTo(14f, 18.75f, 14.21f, 19.46f, 14.58f, 20.06f)
                    curveTo(14.78f, 20.4f, 15.04f, 20.71f, 15.34f, 20.97f)
                    curveTo(15.03f, 20.99f, 14.7f, 21f, 14.35f, 21f)
                    horizontalLineTo(8.65f)
                    curveTo(3.9f, 21f, 2f, 19.1f, 2f, 14.35f)
                    verticalLineTo(8.65f)
                    curveTo(2f, 3.9f, 3.9f, 2f, 8.65f, 2f)
                    horizontalLineTo(14.35f)
                    curveTo(19.1f, 2f, 21f, 3.9f, 21f, 8.65f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(6.72f, 14.42f)
                    curveTo(7.521f, 14.42f, 8.17f, 13.77f, 8.17f, 12.969f)
                    curveTo(8.17f, 12.169f, 7.521f, 11.519f, 6.72f, 11.519f)
                    curveTo(5.919f, 11.519f, 5.27f, 12.169f, 5.27f, 12.969f)
                    curveTo(5.27f, 13.77f, 5.919f, 14.42f, 6.72f, 14.42f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(13.47f, 12f)
                    verticalLineTo(6.34f)
                    curveTo(13.47f, 5.13f, 12.71f, 4.969f, 11.95f, 5.179f)
                    lineTo(9.06f, 5.969f)
                    curveTo(8.54f, 6.109f, 8.17f, 6.53f, 8.17f, 7.129f)
                    verticalLineTo(8.14f)
                    verticalLineTo(8.819f)
                    verticalLineTo(12.969f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12.03f, 13.45f)
                    curveTo(12.831f, 13.45f, 13.48f, 12.801f, 13.48f, 12f)
                    curveTo(13.48f, 11.199f, 12.831f, 10.55f, 12.03f, 10.55f)
                    curveTo(11.229f, 10.55f, 10.58f, 11.199f, 10.58f, 12f)
                    curveTo(10.58f, 12.801f, 11.229f, 13.45f, 12.03f, 13.45f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8.17f, 8.83f)
                    lineTo(13.47f, 7.38f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(22f, 18f)
                    curveTo(22f, 18.75f, 21.79f, 19.46f, 21.42f, 20.06f)
                    curveTo(20.73f, 21.22f, 19.46f, 22f, 18f, 22f)
                    curveTo(16.97f, 22f, 16.04f, 21.61f, 15.34f, 20.97f)
                    curveTo(15.04f, 20.71f, 14.78f, 20.4f, 14.58f, 20.06f)
                    curveTo(14.21f, 19.46f, 14f, 18.75f, 14f, 18f)
                    curveTo(14f, 15.79f, 15.79f, 14f, 18f, 14f)
                    curveTo(19.18f, 14f, 20.25f, 14.51f, 20.97f, 15.33f)
                    curveTo(21.61f, 16.04f, 22f, 16.98f, 22f, 18f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19.07f, 19.04f)
                    lineTo(16.95f, 16.93f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19.05f, 16.96f)
                    lineTo(16.93f, 19.07f)
                }
            }
        }.build()

        return _IconsaxMusicSquareRemove!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicSquareRemove: ImageVector? = null
