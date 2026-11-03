package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxMusicPlay: ImageVector
    get() {
        if (_IconsaxMusicPlay != null) {
            return _IconsaxMusicPlay!!
        }
        _IconsaxMusicPlay = ImageVector.Builder(
            name = "IconsaxMusicPlay",
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
                    moveTo(13.18f, 11.861f)
                    curveTo(12.78f, 11.861f, 12.42f, 11.641f, 12.25f, 11.281f)
                    lineTo(10.8f, 8.391f)
                    lineTo(10.38f, 9.171f)
                    curveTo(10.15f, 9.601f, 9.69f, 9.871f, 9.2f, 9.871f)
                    horizontalLineTo(8.47f)
                    curveTo(8.06f, 9.871f, 7.72f, 9.531f, 7.72f, 9.121f)
                    curveTo(7.72f, 8.711f, 8.06f, 8.371f, 8.47f, 8.371f)
                    horizontalLineTo(9.11f)
                    lineTo(9.9f, 6.911f)
                    curveTo(10.09f, 6.571f, 10.47f, 6.341f, 10.83f, 6.361f)
                    curveTo(11.22f, 6.361f, 11.57f, 6.591f, 11.75f, 6.931f)
                    lineTo(13.18f, 9.791f)
                    lineTo(13.52f, 9.101f)
                    curveTo(13.75f, 8.641f, 14.2f, 8.361f, 14.72f, 8.361f)
                    horizontalLineTo(15.53f)
                    curveTo(15.94f, 8.361f, 16.28f, 8.701f, 16.28f, 9.111f)
                    curveTo(16.28f, 9.521f, 15.94f, 9.861f, 15.53f, 9.861f)
                    horizontalLineTo(14.82f)
                    lineTo(14.11f, 11.271f)
                    curveTo(13.93f, 11.641f, 13.58f, 11.861f, 13.18f, 11.861f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(2.75f, 18.651f)
                    curveTo(2.34f, 18.651f, 2f, 18.311f, 2f, 17.901f)
                    verticalLineTo(12.201f)
                    curveTo(1.95f, 9.491f, 2.96f, 6.931f, 4.84f, 5.011f)
                    curveTo(6.72f, 3.101f, 9.24f, 2.051f, 11.95f, 2.051f)
                    curveTo(17.49f, 2.051f, 22f, 6.561f, 22f, 12.101f)
                    verticalLineTo(17.801f)
                    curveTo(22f, 18.211f, 21.66f, 18.551f, 21.25f, 18.551f)
                    curveTo(20.84f, 18.551f, 20.5f, 18.211f, 20.5f, 17.801f)
                    verticalLineTo(12.101f)
                    curveTo(20.5f, 7.391f, 16.67f, 3.551f, 11.95f, 3.551f)
                    curveTo(9.64f, 3.551f, 7.5f, 4.441f, 5.91f, 6.061f)
                    curveTo(4.31f, 7.691f, 3.46f, 9.861f, 3.5f, 12.181f)
                    verticalLineTo(17.891f)
                    curveTo(3.5f, 18.311f, 3.17f, 18.651f, 2.75f, 18.651f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(5.94f, 12.451f)
                    horizontalLineTo(5.81f)
                    curveTo(3.71f, 12.451f, 2f, 14.161f, 2f, 16.261f)
                    verticalLineTo(18.141f)
                    curveTo(2f, 20.241f, 3.71f, 21.951f, 5.81f, 21.951f)
                    horizontalLineTo(5.94f)
                    curveTo(8.04f, 21.951f, 9.75f, 20.241f, 9.75f, 18.141f)
                    verticalLineTo(16.261f)
                    curveTo(9.75f, 14.161f, 8.04f, 12.451f, 5.94f, 12.451f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(18.19f, 12.451f)
                    horizontalLineTo(18.06f)
                    curveTo(15.96f, 12.451f, 14.25f, 14.161f, 14.25f, 16.261f)
                    verticalLineTo(18.141f)
                    curveTo(14.25f, 20.241f, 15.96f, 21.951f, 18.06f, 21.951f)
                    horizontalLineTo(18.19f)
                    curveTo(20.29f, 21.951f, 22f, 20.241f, 22f, 18.141f)
                    verticalLineTo(16.261f)
                    curveTo(22f, 14.161f, 20.29f, 12.451f, 18.19f, 12.451f)
                    close()
                }
            }
        }.build()

        return _IconsaxMusicPlay!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMusicPlay: ImageVector? = null
