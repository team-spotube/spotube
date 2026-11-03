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

val Iconsax.Iconsax3DotsMore: ImageVector
    get() {
        if (_Iconsax3DotsMore != null) {
            return _Iconsax3DotsMore!!
        }
        _Iconsax3DotsMore = ImageVector.Builder(
            name = "Iconsax3DotsMore",
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
                    moveTo(5f, 10f)
                    curveTo(3.9f, 10f, 3f, 10.9f, 3f, 12f)
                    curveTo(3f, 13.1f, 3.9f, 14f, 5f, 14f)
                    curveTo(6.1f, 14f, 7f, 13.1f, 7f, 12f)
                    curveTo(7f, 10.9f, 6.1f, 10f, 5f, 10f)
                    close()
                }
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f
                ) {
                    moveTo(19f, 10f)
                    curveTo(17.9f, 10f, 17f, 10.9f, 17f, 12f)
                    curveTo(17f, 13.1f, 17.9f, 14f, 19f, 14f)
                    curveTo(20.1f, 14f, 21f, 13.1f, 21f, 12f)
                    curveTo(21f, 10.9f, 20.1f, 10f, 19f, 10f)
                    close()
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f
                ) {
                    moveTo(12f, 10f)
                    curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                    curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                    curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                    curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                    close()
                }
            }
        }.build()

        return _Iconsax3DotsMore!!
    }

@Suppress("ObjectPropertyName")
private var _Iconsax3DotsMore: ImageVector? = null
