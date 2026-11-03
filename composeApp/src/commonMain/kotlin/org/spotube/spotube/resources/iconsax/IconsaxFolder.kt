package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxFolder: ImageVector
    get() {
        if (_IconsaxFolder != null) {
            return _IconsaxFolder!!
        }
        _IconsaxFolder = ImageVector.Builder(
            name = "IconsaxFolder",
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
                path(fill = SolidColor(Color.White)) {
                    moveTo(22f, 11.07f)
                    verticalLineTo(16.65f)
                    curveTo(22f, 19.6f, 19.6f, 22f, 16.65f, 22f)
                    horizontalLineTo(7.35f)
                    curveTo(4.4f, 22f, 2f, 19.6f, 2f, 16.65f)
                    verticalLineTo(9.44f)
                    horizontalLineTo(21.74f)
                    curveTo(21.89f, 9.89f, 21.97f, 10.35f, 21.99f, 10.84f)
                    curveTo(22f, 10.91f, 22f, 11f, 22f, 11.07f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(21.74f, 9.44f)
                    horizontalLineTo(2f)
                    verticalLineTo(6.42f)
                    curveTo(2f, 3.98f, 3.98f, 2f, 6.42f, 2f)
                    horizontalLineTo(8.75f)
                    curveTo(10.38f, 2f, 10.89f, 2.53f, 11.54f, 3.4f)
                    lineTo(12.94f, 5.26f)
                    curveTo(13.25f, 5.67f, 13.29f, 5.73f, 13.87f, 5.73f)
                    horizontalLineTo(16.66f)
                    curveTo(19.03f, 5.72f, 21.05f, 7.28f, 21.74f, 9.44f)
                    close()
                }
            }
        }.build()

        return _IconsaxFolder!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxFolder: ImageVector? = null
