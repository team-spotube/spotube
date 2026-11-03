package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.BrandOpencollective: ImageVector
    get() {
        if (_BrandOpencollective != null) {
            return _BrandOpencollective!!
        }
        _BrandOpencollective = ImageVector.Builder(
            name = "BrandOpencollective",
            defaultWidth = 64.dp,
            defaultHeight = 64.dp,
            viewportWidth = 64f,
            viewportHeight = 64f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFB8D3F4)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(52.402f, 31.916f)
                curveToRelative(0f, 4.03f, -1.17f, 7.895f, -3.178f, 11.087f)
                lineToRelative(8.196f, 8.23f)
                curveToRelative(4.014f, -5.375f, 6.523f, -12.094f, 6.523f, -19.318f)
                reflectiveCurveToRelative(-2.51f, -13.942f, -6.523f, -19.318f)
                lineToRelative(-8.196f, 8.23f)
                curveToRelative(2.007f, 3.192f, 3.178f, 6.887f, 3.178f, 11.087f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF3385FF)),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(32.004f, 52.41f)
                curveToRelative(-11.207f, 0f, -20.406f, -9.24f, -20.406f, -20.493f)
                reflectiveCurveToRelative(9.2f, -20.493f, 20.406f, -20.493f)
                curveToRelative(4.182f, 0f, 7.86f, 1.176f, 11.04f, 3.36f)
                lineToRelative(8.196f, -8.23f)
                curveTo(45.887f, 2.52f, 39.197f, 0f, 32.004f, 0f)
                curveTo(14.44f, 0f, 0.057f, 14.278f, 0.057f, 32.084f)
                reflectiveCurveTo(14.44f, 64f, 32.004f, 64f)
                curveToRelative(7.36f, 0f, 14.05f, -2.52f, 19.403f, -6.55f)
                lineToRelative(-8.196f, -8.23f)
                curveToRelative(-3.178f, 2.016f, -7.025f, 3.192f, -11.207f, 3.192f)
                close()
            }
        }.build()

        return _BrandOpencollective!!
    }

@Suppress("ObjectPropertyName")
private var _BrandOpencollective: ImageVector? = null
