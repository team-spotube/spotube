package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxStopCircle: ImageVector
    get() {
        if (_IconsaxStopCircle != null) {
            return _IconsaxStopCircle!!
        }
        _IconsaxStopCircle = ImageVector.Builder(
            name = "IconsaxStopCircle",
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
                    moveTo(11.97f, 22f)
                    curveTo(17.493f, 22f, 21.97f, 17.523f, 21.97f, 12f)
                    curveTo(21.97f, 6.477f, 17.493f, 2f, 11.97f, 2f)
                    curveTo(6.447f, 2f, 1.97f, 6.477f, 1.97f, 12f)
                    curveTo(1.97f, 17.523f, 6.447f, 22f, 11.97f, 22f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(10.77f, 16.229f)
                    horizontalLineTo(13.23f)
                    curveTo(14.89f, 16.229f, 16.23f, 14.889f, 16.23f, 13.229f)
                    verticalLineTo(10.769f)
                    curveTo(16.23f, 9.11f, 14.89f, 7.77f, 13.23f, 7.77f)
                    horizontalLineTo(10.77f)
                    curveTo(9.11f, 7.77f, 7.77f, 9.11f, 7.77f, 10.769f)
                    verticalLineTo(13.229f)
                    curveTo(7.77f, 14.889f, 9.11f, 16.229f, 10.77f, 16.229f)
                    close()
                }
            }
        }.build()

        return _IconsaxStopCircle!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxStopCircle: ImageVector? = null
