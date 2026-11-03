package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxBox: ImageVector
    get() {
        if (_IconsaxBox != null) {
            return _IconsaxBox!!
        }
        _IconsaxBox = ImageVector.Builder(
            name = "IconsaxBox",
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
                    moveTo(20.1f, 6.94f)
                    curveTo(20.1f, 7.48f, 19.81f, 7.97f, 19.35f, 8.22f)
                    lineTo(17.61f, 9.16f)
                    lineTo(16.13f, 9.95f)
                    lineTo(13.06f, 11.61f)
                    curveTo(12.73f, 11.79f, 12.37f, 11.88f, 12f, 11.88f)
                    curveTo(11.63f, 11.88f, 11.27f, 11.79f, 10.94f, 11.61f)
                    lineTo(4.65f, 8.22f)
                    curveTo(4.19f, 7.97f, 3.9f, 7.48f, 3.9f, 6.94f)
                    curveTo(3.9f, 6.4f, 4.19f, 5.91f, 4.65f, 5.66f)
                    lineTo(6.62f, 4.6f)
                    lineTo(8.19f, 3.75f)
                    lineTo(10.94f, 2.27f)
                    curveTo(11.6f, 1.91f, 12.4f, 1.91f, 13.06f, 2.27f)
                    lineTo(19.35f, 5.66f)
                    curveTo(19.81f, 5.91f, 20.1f, 6.4f, 20.1f, 6.94f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(9.9f, 12.79f)
                    lineTo(4.05f, 9.86f)
                    curveTo(3.6f, 9.63f, 3.08f, 9.66f, 2.65f, 9.92f)
                    curveTo(2.22f, 10.18f, 1.97f, 10.64f, 1.97f, 11.14f)
                    verticalLineTo(16.67f)
                    curveTo(1.97f, 17.63f, 2.5f, 18.49f, 3.36f, 18.92f)
                    lineTo(9.21f, 21.84f)
                    curveTo(9.41f, 21.94f, 9.63f, 21.99f, 9.85f, 21.99f)
                    curveTo(10.11f, 21.99f, 10.37f, 21.92f, 10.6f, 21.77f)
                    curveTo(11.03f, 21.51f, 11.28f, 21.05f, 11.28f, 20.55f)
                    verticalLineTo(15.02f)
                    curveTo(11.29f, 14.08f, 10.76f, 13.22f, 9.9f, 12.79f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(22.03f, 11.15f)
                    verticalLineTo(16.68f)
                    curveTo(22.03f, 17.63f, 21.5f, 18.49f, 20.64f, 18.92f)
                    lineTo(14.79f, 21.85f)
                    curveTo(14.59f, 21.95f, 14.37f, 22f, 14.15f, 22f)
                    curveTo(13.89f, 22f, 13.63f, 21.93f, 13.39f, 21.78f)
                    curveTo(12.97f, 21.52f, 12.71f, 21.06f, 12.71f, 20.56f)
                    verticalLineTo(15.04f)
                    curveTo(12.71f, 14.08f, 13.24f, 13.22f, 14.1f, 12.79f)
                    lineTo(16.25f, 11.72f)
                    lineTo(17.75f, 10.97f)
                    lineTo(19.95f, 9.87f)
                    curveTo(20.4f, 9.64f, 20.92f, 9.66f, 21.35f, 9.93f)
                    curveTo(21.77f, 10.19f, 22.03f, 10.65f, 22.03f, 11.15f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(17.61f, 9.16f)
                    lineTo(16.13f, 9.95f)
                    lineTo(6.62f, 4.6f)
                    lineTo(8.19f, 3.75f)
                    lineTo(17.37f, 8.93f)
                    curveTo(17.47f, 8.99f, 17.55f, 9.07f, 17.61f, 9.16f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(17.75f, 10.971f)
                    verticalLineTo(13.241f)
                    curveTo(17.75f, 13.651f, 17.41f, 13.991f, 17f, 13.991f)
                    curveTo(16.59f, 13.991f, 16.25f, 13.651f, 16.25f, 13.241f)
                    verticalLineTo(11.721f)
                    lineTo(17.75f, 10.971f)
                    close()
                }
            }
        }.build()

        return _IconsaxBox!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxBox: ImageVector? = null
