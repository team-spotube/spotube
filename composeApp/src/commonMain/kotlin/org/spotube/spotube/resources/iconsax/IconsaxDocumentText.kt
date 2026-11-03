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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxDocumentText: ImageVector
    get() {
        if (_IconsaxDocumentText != null) {
            return _IconsaxDocumentText!!
        }
        _IconsaxDocumentText = ImageVector.Builder(
            name = "IconsaxDocumentText",
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
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(20.5f, 10.19f)
                    horizontalLineTo(17.61f)
                    curveTo(15.24f, 10.19f, 13.31f, 8.26f, 13.31f, 5.89f)
                    verticalLineTo(3f)
                    curveTo(13.31f, 2.45f, 12.86f, 2f, 12.31f, 2f)
                    horizontalLineTo(8.07f)
                    curveTo(4.99f, 2f, 2.5f, 4f, 2.5f, 7.57f)
                    verticalLineTo(16.43f)
                    curveTo(2.5f, 20f, 4.99f, 22f, 8.07f, 22f)
                    horizontalLineTo(15.93f)
                    curveTo(19.01f, 22f, 21.5f, 20f, 21.5f, 16.43f)
                    verticalLineTo(11.19f)
                    curveTo(21.5f, 10.64f, 21.05f, 10.19f, 20.5f, 10.19f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.8f, 2.21f)
                    curveTo(15.39f, 1.8f, 14.68f, 2.08f, 14.68f, 2.65f)
                    verticalLineTo(6.14f)
                    curveTo(14.68f, 7.6f, 15.92f, 8.81f, 17.43f, 8.81f)
                    curveTo(18.38f, 8.82f, 19.7f, 8.82f, 20.83f, 8.82f)
                    curveTo(21.4f, 8.82f, 21.7f, 8.15f, 21.3f, 7.75f)
                    curveTo(19.86f, 6.3f, 17.28f, 3.69f, 15.8f, 2.21f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(13.5f, 13.75f)
                    horizontalLineTo(7.5f)
                    curveTo(7.09f, 13.75f, 6.75f, 13.41f, 6.75f, 13f)
                    curveTo(6.75f, 12.59f, 7.09f, 12.25f, 7.5f, 12.25f)
                    horizontalLineTo(13.5f)
                    curveTo(13.91f, 12.25f, 14.25f, 12.59f, 14.25f, 13f)
                    curveTo(14.25f, 13.41f, 13.91f, 13.75f, 13.5f, 13.75f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(11.5f, 17.75f)
                    horizontalLineTo(7.5f)
                    curveTo(7.09f, 17.75f, 6.75f, 17.41f, 6.75f, 17f)
                    curveTo(6.75f, 16.59f, 7.09f, 16.25f, 7.5f, 16.25f)
                    horizontalLineTo(11.5f)
                    curveTo(11.91f, 16.25f, 12.25f, 16.59f, 12.25f, 17f)
                    curveTo(12.25f, 17.41f, 11.91f, 17.75f, 11.5f, 17.75f)
                    close()
                }
            }
        }.build()

        return _IconsaxDocumentText!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxDocumentText: ImageVector? = null
