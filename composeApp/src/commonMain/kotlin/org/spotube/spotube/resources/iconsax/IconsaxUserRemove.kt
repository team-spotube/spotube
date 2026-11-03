package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxUserRemove: ImageVector
    get() {
        if (_IconsaxUserRemove != null) {
            return _IconsaxUserRemove!!
        }
        _IconsaxUserRemove = ImageVector.Builder(
            name = "IconsaxUserRemove",
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
                    moveTo(21.09f, 21.5f)
                    curveTo(21.09f, 21.78f, 20.87f, 22f, 20.59f, 22f)
                    horizontalLineTo(3.41f)
                    curveTo(3.13f, 22f, 2.91f, 21.78f, 2.91f, 21.5f)
                    curveTo(2.91f, 17.36f, 6.99f, 14f, 12f, 14f)
                    curveTo(13.03f, 14f, 14.03f, 14.14f, 14.95f, 14.41f)
                    curveTo(14.36f, 15.11f, 14f, 16.02f, 14f, 17f)
                    curveTo(14f, 17.75f, 14.21f, 18.46f, 14.58f, 19.06f)
                    curveTo(14.78f, 19.4f, 15.04f, 19.71f, 15.34f, 19.97f)
                    curveTo(16.04f, 20.61f, 16.97f, 21f, 18f, 21f)
                    curveTo(19.12f, 21f, 20.13f, 20.54f, 20.85f, 19.8f)
                    curveTo(21.01f, 20.34f, 21.09f, 20.91f, 21.09f, 21.5f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21.88f, 16.04f)
                    curveTo(21.78f, 15.65f, 21.62f, 15.26f, 21.4f, 14.91f)
                    curveTo(21.25f, 14.65f, 21.05f, 14.4f, 20.83f, 14.17f)
                    curveTo(20.11f, 13.45f, 19.17f, 13.06f, 18.21f, 13.01f)
                    curveTo(17.12f, 12.94f, 16.01f, 13.34f, 15.17f, 14.17f)
                    curveTo(14.38f, 14.96f, 13.98f, 16.01f, 14f, 17.06f)
                    curveTo(14.01f, 18.06f, 14.41f, 19.06f, 15.17f, 19.83f)
                    curveTo(15.7f, 20.36f, 16.35f, 20.71f, 17.04f, 20.87f)
                    curveTo(17.42f, 20.97f, 17.82f, 21.01f, 18.22f, 20.98f)
                    curveTo(19.17f, 20.94f, 20.1f, 20.56f, 20.83f, 19.83f)
                    curveTo(21.86f, 18.8f, 22.21f, 17.35f, 21.88f, 16.04f)
                    close()
                    moveTo(19.6f, 18.6f)
                    curveTo(19.31f, 18.89f, 18.83f, 18.89f, 18.54f, 18.6f)
                    lineTo(17.99f, 18.05f)
                    lineTo(17.46f, 18.58f)
                    curveTo(17.17f, 18.87f, 16.69f, 18.87f, 16.4f, 18.58f)
                    curveTo(16.11f, 18.28f, 16.11f, 17.81f, 16.4f, 17.52f)
                    lineTo(16.93f, 16.99f)
                    lineTo(16.42f, 16.49f)
                    curveTo(16.13f, 16.19f, 16.13f, 15.72f, 16.42f, 15.42f)
                    curveTo(16.72f, 15.13f, 17.19f, 15.13f, 17.49f, 15.42f)
                    lineTo(17.99f, 15.93f)
                    lineTo(18.52f, 15.4f)
                    curveTo(18.81f, 15.11f, 19.28f, 15.11f, 19.58f, 15.4f)
                    curveTo(19.87f, 15.69f, 19.87f, 16.17f, 19.58f, 16.46f)
                    lineTo(19.05f, 16.99f)
                    lineTo(19.6f, 17.54f)
                    curveTo(19.89f, 17.83f, 19.89f, 18.31f, 19.6f, 18.6f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 12f)
                    curveTo(14.761f, 12f, 17f, 9.761f, 17f, 7f)
                    curveTo(17f, 4.239f, 14.761f, 2f, 12f, 2f)
                    curveTo(9.239f, 2f, 7f, 4.239f, 7f, 7f)
                    curveTo(7f, 9.761f, 9.239f, 12f, 12f, 12f)
                    close()
                }
            }
        }.build()

        return _IconsaxUserRemove!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxUserRemove: ImageVector? = null
