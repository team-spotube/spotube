package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxWifiSquare: ImageVector
    get() {
        if (_IconsaxWifiSquare != null) {
            return _IconsaxWifiSquare!!
        }
        _IconsaxWifiSquare = ImageVector.Builder(
            name = "IconsaxWifiSquare",
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
                path(fill = SolidColor(Color.White)) {
                    moveTo(18f, 10.71f)
                    curveTo(17.84f, 10.71f, 17.68f, 10.66f, 17.54f, 10.55f)
                    curveTo(14.17f, 7.95f, 9.82f, 7.95f, 6.46f, 10.55f)
                    curveTo(6.13f, 10.8f, 5.66f, 10.74f, 5.41f, 10.42f)
                    curveTo(5.16f, 10.09f, 5.22f, 9.62f, 5.54f, 9.37f)
                    curveTo(9.46f, 6.34f, 14.53f, 6.34f, 18.46f, 9.37f)
                    curveTo(18.79f, 9.62f, 18.85f, 10.09f, 18.59f, 10.42f)
                    curveTo(18.45f, 10.61f, 18.22f, 10.71f, 18f, 10.71f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.4f, 13.8f)
                    curveTo(16.24f, 13.8f, 16.08f, 13.75f, 15.94f, 13.64f)
                    curveTo(13.54f, 11.79f, 10.45f, 11.79f, 8.05f, 13.64f)
                    curveTo(7.72f, 13.89f, 7.25f, 13.83f, 7f, 13.51f)
                    curveTo(6.75f, 13.19f, 6.81f, 12.71f, 7.13f, 12.46f)
                    curveTo(10.08f, 10.18f, 13.9f, 10.18f, 16.85f, 12.46f)
                    curveTo(17.18f, 12.71f, 17.24f, 13.18f, 16.98f, 13.51f)
                    curveTo(16.85f, 13.7f, 16.63f, 13.8f, 16.4f, 13.8f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(14.2f, 16.889f)
                    curveTo(14.04f, 16.889f, 13.88f, 16.84f, 13.74f, 16.729f)
                    curveTo(12.68f, 15.91f, 11.31f, 15.91f, 10.25f, 16.729f)
                    curveTo(9.92f, 16.979f, 9.45f, 16.92f, 9.2f, 16.6f)
                    curveTo(8.95f, 16.27f, 9.01f, 15.8f, 9.33f, 15.55f)
                    curveTo(10.92f, 14.319f, 13.06f, 14.319f, 14.65f, 15.55f)
                    curveTo(14.98f, 15.8f, 15.04f, 16.27f, 14.78f, 16.6f)
                    curveTo(14.65f, 16.789f, 14.43f, 16.889f, 14.2f, 16.889f)
                    close()
                }
            }
        }.build()

        return _IconsaxWifiSquare!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxWifiSquare: ImageVector? = null
