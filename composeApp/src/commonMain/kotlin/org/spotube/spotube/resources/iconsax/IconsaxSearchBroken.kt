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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxSearchBroken: ImageVector
    get() {
        if (_IconsaxSearchBroken != null) {
            return _IconsaxSearchBroken!!
        }
        _IconsaxSearchBroken = ImageVector.Builder(
            name = "IconsaxSearchBroken",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.5f, 2f)
                curveTo(16.75f, 2f, 21f, 6.25f, 21f, 11.5f)
                curveTo(21f, 16.75f, 16.75f, 21f, 11.5f, 21f)
                curveTo(6.25f, 21f, 2f, 16.75f, 2f, 11.5f)
                curveTo(2f, 7.8f, 4.11f, 4.6f, 7.2f, 3.03f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 22f)
                lineTo(20f, 20f)
            }
        }.build()

        return _IconsaxSearchBroken!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSearchBroken: ImageVector? = null
