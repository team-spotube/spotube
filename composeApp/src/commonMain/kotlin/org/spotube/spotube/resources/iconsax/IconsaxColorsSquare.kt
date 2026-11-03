package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxColorsSquare: ImageVector
    get() {
        if (_IconsaxColorsSquare != null) {
            return _IconsaxColorsSquare!!
        }
        _IconsaxColorsSquare = ImageVector.Builder(
            name = "IconsaxColorsSquare",
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
                    moveTo(16.19f, 2f)
                    horizontalLineTo(7.82f)
                    curveTo(4.17f, 2f, 2f, 4.17f, 2f, 7.81f)
                    verticalLineTo(16.18f)
                    curveTo(2f, 19.82f, 4.17f, 21.99f, 7.81f, 21.99f)
                    horizontalLineTo(16.18f)
                    curveTo(19.82f, 21.99f, 21.99f, 19.82f, 21.99f, 16.18f)
                    verticalLineTo(7.81f)
                    curveTo(22f, 4.17f, 19.83f, 2f, 16.19f, 2f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.6f,
                    strokeAlpha = 0.6f
                ) {
                    moveTo(13.2f, 14.4f)
                    curveTo(13.2f, 15.46f, 12.74f, 16.42f, 12f, 17.08f)
                    curveTo(11.36f, 17.66f, 10.52f, 18f, 9.6f, 18f)
                    curveTo(7.61f, 18f, 6f, 16.39f, 6f, 14.4f)
                    curveTo(6f, 12.74f, 7.13f, 11.34f, 8.65f, 10.93f)
                    curveTo(9.06f, 11.97f, 9.95f, 12.78f, 11.05f, 13.08f)
                    curveTo(11.35f, 13.16f, 11.67f, 13.21f, 12f, 13.21f)
                    curveTo(12.33f, 13.21f, 12.65f, 13.17f, 12.95f, 13.08f)
                    curveTo(13.11f, 13.48f, 13.2f, 13.93f, 13.2f, 14.4f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.6f, 9.6f)
                    curveTo(15.6f, 10.07f, 15.51f, 10.52f, 15.35f, 10.93f)
                    curveTo(14.94f, 11.97f, 14.05f, 12.78f, 12.95f, 13.08f)
                    curveTo(12.65f, 13.16f, 12.33f, 13.21f, 12f, 13.21f)
                    curveTo(11.67f, 13.21f, 11.35f, 13.17f, 11.05f, 13.08f)
                    curveTo(9.95f, 12.78f, 9.06f, 11.98f, 8.65f, 10.93f)
                    curveTo(8.49f, 10.52f, 8.4f, 10.07f, 8.4f, 9.6f)
                    curveTo(8.4f, 7.61f, 10.01f, 6f, 12f, 6f)
                    curveTo(13.99f, 6f, 15.6f, 7.61f, 15.6f, 9.6f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(18f, 14.4f)
                    curveTo(18f, 16.39f, 16.39f, 18f, 14.4f, 18f)
                    curveTo(13.48f, 18f, 12.64f, 17.65f, 12f, 17.08f)
                    curveTo(12.74f, 16.43f, 13.2f, 15.47f, 13.2f, 14.4f)
                    curveTo(13.2f, 13.93f, 13.11f, 13.48f, 12.95f, 13.07f)
                    curveTo(14.05f, 12.77f, 14.94f, 11.97f, 15.35f, 10.92f)
                    curveTo(16.87f, 11.34f, 18f, 12.74f, 18f, 14.4f)
                    close()
                }
            }
        }.build()

        return _IconsaxColorsSquare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxColorsSquare: ImageVector? = null
