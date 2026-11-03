package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxEdit: ImageVector
    get() {
        if (_IconsaxEdit != null) {
            return _IconsaxEdit!!
        }
        _IconsaxEdit = ImageVector.Builder(
            name = "IconsaxEdit",
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
                    moveTo(15.48f, 3f)
                    horizontalLineTo(7.52f)
                    curveTo(4.07f, 3f, 2f, 5.06f, 2f, 8.52f)
                    verticalLineTo(16.47f)
                    curveTo(2f, 19.94f, 4.07f, 22f, 7.52f, 22f)
                    horizontalLineTo(15.47f)
                    curveTo(18.93f, 22f, 20.99f, 19.94f, 20.99f, 16.48f)
                    verticalLineTo(8.52f)
                    curveTo(21f, 5.06f, 18.93f, 3f, 15.48f, 3f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21.02f, 2.98f)
                    curveTo(19.23f, 1.18f, 17.48f, 1.14f, 15.64f, 2.98f)
                    lineTo(14.51f, 4.1f)
                    curveTo(14.41f, 4.2f, 14.38f, 4.34f, 14.42f, 4.47f)
                    curveTo(15.12f, 6.92f, 17.08f, 8.88f, 19.53f, 9.58f)
                    curveTo(19.56f, 9.59f, 19.61f, 9.59f, 19.64f, 9.59f)
                    curveTo(19.74f, 9.59f, 19.84f, 9.55f, 19.91f, 9.48f)
                    lineTo(21.02f, 8.36f)
                    curveTo(21.93f, 7.45f, 22.38f, 6.58f, 22.38f, 5.69f)
                    curveTo(22.38f, 4.79f, 21.93f, 3.9f, 21.02f, 2.98f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(17.86f, 10.42f)
                    curveTo(17.59f, 10.29f, 17.33f, 10.16f, 17.09f, 10.01f)
                    curveTo(16.89f, 9.89f, 16.69f, 9.76f, 16.5f, 9.62f)
                    curveTo(16.34f, 9.52f, 16.16f, 9.37f, 15.98f, 9.22f)
                    curveTo(15.96f, 9.21f, 15.9f, 9.16f, 15.82f, 9.08f)
                    curveTo(15.51f, 8.83f, 15.18f, 8.49f, 14.87f, 8.12f)
                    curveTo(14.85f, 8.1f, 14.79f, 8.04f, 14.74f, 7.95f)
                    curveTo(14.64f, 7.84f, 14.49f, 7.65f, 14.36f, 7.44f)
                    curveTo(14.25f, 7.3f, 14.12f, 7.1f, 14f, 6.89f)
                    curveTo(13.85f, 6.64f, 13.72f, 6.39f, 13.6f, 6.13f)
                    curveTo(13.47f, 5.85f, 13.37f, 5.59f, 13.28f, 5.34f)
                    lineTo(7.9f, 10.72f)
                    curveTo(7.55f, 11.07f, 7.21f, 11.73f, 7.14f, 12.22f)
                    lineTo(6.71f, 15.2f)
                    curveTo(6.62f, 15.83f, 6.79f, 16.42f, 7.18f, 16.81f)
                    curveTo(7.51f, 17.14f, 7.96f, 17.31f, 8.46f, 17.31f)
                    curveTo(8.57f, 17.31f, 8.68f, 17.3f, 8.79f, 17.29f)
                    lineTo(11.76f, 16.87f)
                    curveTo(12.25f, 16.8f, 12.91f, 16.47f, 13.26f, 16.11f)
                    lineTo(18.64f, 10.73f)
                    curveTo(18.39f, 10.65f, 18.14f, 10.54f, 17.86f, 10.42f)
                    close()
                }
            }
        }.build()

        return _IconsaxEdit!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxEdit: ImageVector? = null
