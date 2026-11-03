package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxColorSwatch: ImageVector
    get() {
        if (_IconsaxColorSwatch != null) {
            return _IconsaxColorSwatch!!
        }
        _IconsaxColorSwatch = ImageVector.Builder(
            name = "IconsaxColorSwatch",
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
                    moveTo(22f, 16.5f)
                    verticalLineTo(19.5f)
                    curveTo(22f, 21f, 21f, 22f, 19.5f, 22f)
                    horizontalLineTo(6f)
                    curveTo(6.41f, 22f, 6.83f, 21.94f, 7.22f, 21.81f)
                    curveTo(7.33f, 21.77f, 7.44f, 21.73f, 7.55f, 21.68f)
                    curveTo(7.9f, 21.54f, 8.24f, 21.34f, 8.54f, 21.08f)
                    curveTo(8.63f, 21.01f, 8.73f, 20.92f, 8.82f, 20.83f)
                    lineTo(8.86f, 20.79f)
                    lineTo(15.66f, 14f)
                    horizontalLineTo(19.5f)
                    curveTo(21f, 14f, 22f, 15f, 22f, 16.5f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.6f,
                    strokeAlpha = 0.6f
                ) {
                    moveTo(18.37f, 11.291f)
                    lineTo(15.66f, 14.001f)
                    lineTo(8.86f, 20.791f)
                    curveTo(9.56f, 20.071f, 10f, 19.081f, 10f, 18.001f)
                    verticalLineTo(8.341f)
                    lineTo(12.71f, 5.631f)
                    curveTo(13.77f, 4.571f, 15.19f, 4.571f, 16.25f, 5.631f)
                    lineTo(18.37f, 7.751f)
                    curveTo(19.43f, 8.811f, 19.43f, 10.231f, 18.37f, 11.291f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(7.5f, 2f)
                    horizontalLineTo(4.5f)
                    curveTo(3f, 2f, 2f, 3f, 2f, 4.5f)
                    verticalLineTo(18f)
                    curveTo(2f, 18.27f, 2.03f, 18.54f, 2.08f, 18.8f)
                    curveTo(2.11f, 18.93f, 2.14f, 19.06f, 2.18f, 19.19f)
                    curveTo(2.23f, 19.34f, 2.28f, 19.49f, 2.34f, 19.63f)
                    curveTo(2.35f, 19.64f, 2.35f, 19.65f, 2.35f, 19.65f)
                    curveTo(2.36f, 19.65f, 2.36f, 19.65f, 2.35f, 19.66f)
                    curveTo(2.49f, 19.94f, 2.65f, 20.21f, 2.84f, 20.46f)
                    curveTo(2.95f, 20.59f, 3.06f, 20.71f, 3.17f, 20.83f)
                    curveTo(3.28f, 20.95f, 3.4f, 21.05f, 3.53f, 21.15f)
                    lineTo(3.54f, 21.16f)
                    curveTo(3.79f, 21.35f, 4.06f, 21.51f, 4.34f, 21.65f)
                    curveTo(4.35f, 21.64f, 4.35f, 21.64f, 4.35f, 21.65f)
                    curveTo(4.5f, 21.72f, 4.65f, 21.77f, 4.81f, 21.82f)
                    curveTo(4.94f, 21.86f, 5.07f, 21.89f, 5.2f, 21.92f)
                    curveTo(5.46f, 21.97f, 5.73f, 22f, 6f, 22f)
                    curveTo(6.41f, 22f, 6.83f, 21.94f, 7.22f, 21.81f)
                    curveTo(7.33f, 21.77f, 7.44f, 21.73f, 7.55f, 21.68f)
                    curveTo(7.9f, 21.54f, 8.24f, 21.34f, 8.54f, 21.08f)
                    curveTo(8.63f, 21.01f, 8.73f, 20.92f, 8.82f, 20.83f)
                    lineTo(8.86f, 20.79f)
                    curveTo(9.56f, 20.07f, 10f, 19.08f, 10f, 18f)
                    verticalLineTo(4.5f)
                    curveTo(10f, 3f, 9f, 2f, 7.5f, 2f)
                    close()
                    moveTo(6f, 19.5f)
                    curveTo(5.17f, 19.5f, 4.5f, 18.83f, 4.5f, 18f)
                    curveTo(4.5f, 17.17f, 5.17f, 16.5f, 6f, 16.5f)
                    curveTo(6.83f, 16.5f, 7.5f, 17.17f, 7.5f, 18f)
                    curveTo(7.5f, 18.83f, 6.83f, 19.5f, 6f, 19.5f)
                    close()
                }
            }
        }.build()

        return _IconsaxColorSwatch!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxColorSwatch: ImageVector? = null
