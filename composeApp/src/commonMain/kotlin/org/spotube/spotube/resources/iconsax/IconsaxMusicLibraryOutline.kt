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

val Iconsax.IconsaxMusicLibraryOutline: ImageVector
    get() {
        if (_IconsaxMusicLibraryOutline != null) {
            return _IconsaxMusicLibraryOutline!!
        }
        _IconsaxMusicLibraryOutline = ImageVector.Builder(
            name = "IconsaxMusicLibraryOutline",
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
                    moveTo(22f, 13f)
                    verticalLineTo(17f)
                    curveTo(22f, 20.5f, 20f, 22f, 17f, 22f)
                    horizontalLineTo(7f)
                    curveTo(4f, 22f, 2f, 20.5f, 2f, 17f)
                    verticalLineTo(13f)
                    curveTo(2f, 10.35f, 3.15f, 8.85f, 5f, 8.28f)
                    curveTo(5.6f, 8.09f, 6.27f, 8f, 7f, 8f)
                    horizontalLineTo(17f)
                    curveTo(17.73f, 8f, 18.4f, 8.09f, 19f, 8.28f)
                    curveTo(20.85f, 8.85f, 22f, 10.35f, 22f, 13f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19f, 7f)
                    verticalLineTo(8.28f)
                    curveTo(18.4f, 8.09f, 17.73f, 8f, 17f, 8f)
                    horizontalLineTo(7f)
                    curveTo(6.27f, 8f, 5.6f, 8.09f, 5f, 8.28f)
                    verticalLineTo(7f)
                    curveTo(5f, 5.9f, 5.9f, 5f, 7f, 5f)
                    horizontalLineTo(17f)
                    curveTo(18.1f, 5f, 19f, 5.9f, 19f, 7f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(16f, 3.51f)
                    verticalLineTo(5f)
                    horizontalLineTo(8f)
                    verticalLineTo(3.51f)
                    curveTo(8f, 2.68f, 8.68f, 2f, 9.51f, 2f)
                    horizontalLineTo(14.49f)
                    curveTo(15.32f, 2f, 16f, 2.68f, 16f, 3.51f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(9.07f, 19.451f)
                    curveTo(9.799f, 19.451f, 10.39f, 18.86f, 10.39f, 18.131f)
                    curveTo(10.39f, 17.402f, 9.799f, 16.81f, 9.07f, 16.81f)
                    curveTo(8.341f, 16.81f, 7.75f, 17.402f, 7.75f, 18.131f)
                    curveTo(7.75f, 18.86f, 8.341f, 19.451f, 9.07f, 19.451f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(15.25f, 17.25f)
                    verticalLineTo(12.08f)
                    curveTo(15.25f, 10.98f, 14.56f, 10.82f, 13.86f, 11.02f)
                    lineTo(11.21f, 11.74f)
                    curveTo(10.73f, 11.87f, 10.4f, 12.25f, 10.4f, 12.8f)
                    verticalLineTo(13.72f)
                    verticalLineTo(14.34f)
                    verticalLineTo(18.13f)
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(13.93f, 18.57f)
                    curveTo(14.659f, 18.57f, 15.25f, 17.979f, 15.25f, 17.25f)
                    curveTo(15.25f, 16.521f, 14.659f, 15.93f, 13.93f, 15.93f)
                    curveTo(13.201f, 15.93f, 12.61f, 16.521f, 12.61f, 17.25f)
                    curveTo(12.61f, 17.979f, 13.201f, 18.57f, 13.93f, 18.57f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(10.4f, 14.35f)
                    lineTo(15.25f, 13.03f)
                }
            }
        }.build()

        return _IconsaxMusicLibraryOutline!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicLibraryOutline: ImageVector? = null
