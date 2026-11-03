package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxEye: ImageVector
    get() {
        if (_IconsaxEye != null) {
            return _IconsaxEye!!
        }
        _IconsaxEye = ImageVector.Builder(
            name = "IconsaxEye",
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
                    moveTo(21.25f, 9.15f)
                    curveTo(18.94f, 5.52f, 15.56f, 3.43f, 12f, 3.43f)
                    curveTo(10.22f, 3.43f, 8.49f, 3.95f, 6.91f, 4.92f)
                    curveTo(5.33f, 5.9f, 3.91f, 7.33f, 2.75f, 9.15f)
                    curveTo(1.75f, 10.72f, 1.75f, 13.27f, 2.75f, 14.84f)
                    curveTo(5.06f, 18.48f, 8.44f, 20.56f, 12f, 20.56f)
                    curveTo(13.78f, 20.56f, 15.51f, 20.04f, 17.09f, 19.07f)
                    curveTo(18.67f, 18.09f, 20.09f, 16.66f, 21.25f, 14.84f)
                    curveTo(22.25f, 13.28f, 22.25f, 10.72f, 21.25f, 9.15f)
                    close()
                    moveTo(12f, 16.04f)
                    curveTo(9.76f, 16.04f, 7.96f, 14.23f, 7.96f, 12f)
                    curveTo(7.96f, 9.77f, 9.76f, 7.96f, 12f, 7.96f)
                    curveTo(14.24f, 7.96f, 16.04f, 9.77f, 16.04f, 12f)
                    curveTo(16.04f, 14.23f, 14.24f, 16.04f, 12f, 16.04f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 9.141f)
                    curveTo(10.43f, 9.141f, 9.15f, 10.421f, 9.15f, 12.001f)
                    curveTo(9.15f, 13.571f, 10.43f, 14.851f, 12f, 14.851f)
                    curveTo(13.57f, 14.851f, 14.86f, 13.571f, 14.86f, 12.001f)
                    curveTo(14.86f, 10.431f, 13.57f, 9.141f, 12f, 9.141f)
                    close()
                }
            }
        }.build()

        return _IconsaxEye!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxEye: ImageVector? = null
