package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxMirroringScreen: ImageVector
    get() {
        if (_IconsaxMirroringScreen != null) {
            return _IconsaxMirroringScreen!!
        }
        _IconsaxMirroringScreen = ImageVector.Builder(
            name = "IconsaxMirroringScreen",
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
                    moveTo(2f, 9f)
                    verticalLineTo(8f)
                    curveTo(2f, 5.24f, 4.24f, 3f, 7f, 3f)
                    horizontalLineTo(17f)
                    curveTo(19.76f, 3f, 22f, 5.24f, 22f, 8f)
                    verticalLineTo(16f)
                    curveTo(22f, 18.76f, 19.76f, 21f, 17f, 21f)
                    horizontalLineTo(16f)
                    horizontalLineTo(7f)
                    curveTo(4.24f, 21f, 2f, 18.76f, 2f, 16f)
                    verticalLineTo(9f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(16.14f, 15.439f)
                    curveTo(15.77f, 15.439f, 15.45f, 15.159f, 15.4f, 14.789f)
                    curveTo(14.97f, 11.409f, 12.51f, 8.949f, 9.13f, 8.519f)
                    curveTo(8.72f, 8.469f, 8.43f, 8.089f, 8.48f, 7.679f)
                    curveTo(8.53f, 7.269f, 8.91f, 6.979f, 9.32f, 7.029f)
                    curveTo(13.39f, 7.549f, 16.36f, 10.519f, 16.89f, 14.599f)
                    curveTo(16.94f, 15.009f, 16.65f, 15.389f, 16.24f, 15.439f)
                    curveTo(16.21f, 15.439f, 16.18f, 15.439f, 16.14f, 15.439f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(13.44f, 16.298f)
                    curveTo(13.07f, 16.298f, 12.75f, 16.018f, 12.7f, 15.648f)
                    curveTo(12.39f, 13.268f, 10.66f, 11.528f, 8.27f, 11.218f)
                    curveTo(7.86f, 11.168f, 7.57f, 10.788f, 7.62f, 10.378f)
                    curveTo(7.67f, 9.968f, 8.05f, 9.678f, 8.46f, 9.728f)
                    curveTo(11.54f, 10.128f, 13.79f, 12.368f, 14.19f, 15.458f)
                    curveTo(14.24f, 15.868f, 13.95f, 16.248f, 13.54f, 16.298f)
                    curveTo(13.51f, 16.298f, 13.47f, 16.298f, 13.44f, 16.298f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(10.39f, 16.809f)
                    curveTo(10.02f, 16.809f, 9.7f, 16.529f, 9.65f, 16.159f)
                    curveTo(9.52f, 15.139f, 8.78f, 14.399f, 7.76f, 14.269f)
                    curveTo(7.35f, 14.219f, 7.06f, 13.839f, 7.11f, 13.429f)
                    curveTo(7.16f, 13.019f, 7.54f, 12.729f, 7.95f, 12.779f)
                    curveTo(9.64f, 12.999f, 10.92f, 14.279f, 11.14f, 15.969f)
                    curveTo(11.19f, 16.379f, 10.9f, 16.759f, 10.49f, 16.809f)
                    curveTo(10.46f, 16.809f, 10.42f, 16.809f, 10.39f, 16.809f)
                    close()
                }
            }
        }.build()

        return _IconsaxMirroringScreen!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMirroringScreen: ImageVector? = null
