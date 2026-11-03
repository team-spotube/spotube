package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.BrandPatreon: ImageVector
    get() {
        if (_BrandPatreon != null) {
            return _BrandPatreon!!
        }
        _BrandPatreon = ImageVector.Builder(
            name = "BrandPatreon",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0.38f, 0.42f)
                    lineToRelative(512f, 0f)
                    lineToRelative(0f, 512f)
                    lineTo(0.38f, 512.42f)
                    close()
                }
            ) {
                path(
                    fill = SolidColor(Color.Black),
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(490.12f, 154.23f)
                    curveToRelative(-0.09f, -65.37f, -51.01f, -118.95f, -110.74f, -138.29f)
                    curveToRelative(-74.18f, -24.01f, -172.02f, -20.53f, -242.86f, 12.89f)
                    curveToRelative(-85.85f, 40.51f, -112.82f, 129.26f, -113.83f, 217.77f)
                    curveToRelative(-0.82f, 72.77f, 6.44f, 264.43f, 114.54f, 265.8f)
                    curveToRelative(80.33f, 1.02f, 92.29f, -102.48f, 129.45f, -152.33f)
                    curveToRelative(26.44f, -35.47f, 60.49f, -45.48f, 102.4f, -55.85f)
                    curveToRelative(72.03f, -17.83f, 121.13f, -74.68f, 121.03f, -149.99f)
                    close()
                }
            }
        }.build()

        return _BrandPatreon!!
    }

@Suppress("ObjectPropertyName")
private var _BrandPatreon: ImageVector? = null
