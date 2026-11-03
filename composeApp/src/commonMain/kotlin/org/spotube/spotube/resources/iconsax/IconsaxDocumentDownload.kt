package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxDocumentDownload: ImageVector
    get() {
        if (_IconsaxDocumentDownload != null) {
            return _IconsaxDocumentDownload!!
        }
        _IconsaxDocumentDownload = ImageVector.Builder(
            name = "IconsaxDocumentDownload",
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
                    moveTo(20.5f, 10.19f)
                    horizontalLineTo(17.61f)
                    curveTo(15.24f, 10.19f, 13.31f, 8.26f, 13.31f, 5.89f)
                    verticalLineTo(3f)
                    curveTo(13.31f, 2.45f, 12.86f, 2f, 12.31f, 2f)
                    horizontalLineTo(8.07f)
                    curveTo(4.99f, 2f, 2.5f, 4f, 2.5f, 7.57f)
                    verticalLineTo(16.43f)
                    curveTo(2.5f, 20f, 4.99f, 22f, 8.07f, 22f)
                    horizontalLineTo(15.93f)
                    curveTo(19.01f, 22f, 21.5f, 20f, 21.5f, 16.43f)
                    verticalLineTo(11.19f)
                    curveTo(21.5f, 10.64f, 21.05f, 10.19f, 20.5f, 10.19f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(15.8f, 2.21f)
                    curveTo(15.39f, 1.8f, 14.68f, 2.08f, 14.68f, 2.65f)
                    verticalLineTo(6.14f)
                    curveTo(14.68f, 7.6f, 15.92f, 8.81f, 17.43f, 8.81f)
                    curveTo(18.38f, 8.82f, 19.7f, 8.82f, 20.83f, 8.82f)
                    curveTo(21.4f, 8.82f, 21.7f, 8.15f, 21.3f, 7.75f)
                    curveTo(19.86f, 6.3f, 17.28f, 3.69f, 15.8f, 2.21f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12.28f, 14.72f)
                    curveTo(11.99f, 14.43f, 11.51f, 14.43f, 11.22f, 14.72f)
                    lineTo(10.5f, 15.44f)
                    verticalLineTo(11.25f)
                    curveTo(10.5f, 10.84f, 10.16f, 10.5f, 9.75f, 10.5f)
                    curveTo(9.34f, 10.5f, 9f, 10.84f, 9f, 11.25f)
                    verticalLineTo(15.44f)
                    lineTo(8.28f, 14.72f)
                    curveTo(7.99f, 14.43f, 7.51f, 14.43f, 7.22f, 14.72f)
                    curveTo(6.93f, 15.01f, 6.93f, 15.49f, 7.22f, 15.78f)
                    lineTo(9.22f, 17.78f)
                    curveTo(9.23f, 17.79f, 9.24f, 17.79f, 9.24f, 17.8f)
                    curveTo(9.3f, 17.86f, 9.38f, 17.91f, 9.46f, 17.95f)
                    curveTo(9.56f, 17.98f, 9.65f, 18f, 9.75f, 18f)
                    curveTo(9.85f, 18f, 9.94f, 17.98f, 10.03f, 17.94f)
                    curveTo(10.12f, 17.9f, 10.2f, 17.85f, 10.28f, 17.78f)
                    lineTo(12.28f, 15.78f)
                    curveTo(12.57f, 15.49f, 12.57f, 15.01f, 12.28f, 14.72f)
                    close()
                }
            }
        }.build()

        return _IconsaxDocumentDownload!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxDocumentDownload: ImageVector? = null
