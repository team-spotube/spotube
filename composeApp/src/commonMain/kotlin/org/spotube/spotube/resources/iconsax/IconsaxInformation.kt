package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxInformation: ImageVector
    get() {
        if (_IconsaxInformation != null) {
            return _IconsaxInformation!!
        }
        _IconsaxInformation = ImageVector.Builder(
            name = "IconsaxInformation",
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
                    moveTo(10.75f, 2.45f)
                    curveTo(11.45f, 1.86f, 12.58f, 1.86f, 13.26f, 2.45f)
                    lineTo(14.84f, 3.8f)
                    curveTo(15.14f, 4.05f, 15.71f, 4.26f, 16.11f, 4.26f)
                    horizontalLineTo(17.81f)
                    curveTo(18.87f, 4.26f, 19.74f, 5.13f, 19.74f, 6.19f)
                    verticalLineTo(7.89f)
                    curveTo(19.74f, 8.29f, 19.95f, 8.85f, 20.2f, 9.15f)
                    lineTo(21.55f, 10.73f)
                    curveTo(22.14f, 11.43f, 22.14f, 12.56f, 21.55f, 13.24f)
                    lineTo(20.2f, 14.82f)
                    curveTo(19.95f, 15.12f, 19.74f, 15.68f, 19.74f, 16.08f)
                    verticalLineTo(17.78f)
                    curveTo(19.74f, 18.84f, 18.87f, 19.71f, 17.81f, 19.71f)
                    horizontalLineTo(16.11f)
                    curveTo(15.71f, 19.71f, 15.15f, 19.92f, 14.85f, 20.17f)
                    lineTo(13.27f, 21.52f)
                    curveTo(12.57f, 22.11f, 11.44f, 22.11f, 10.76f, 21.52f)
                    lineTo(9.18f, 20.17f)
                    curveTo(8.88f, 19.92f, 8.31f, 19.71f, 7.92f, 19.71f)
                    horizontalLineTo(6.17f)
                    curveTo(5.11f, 19.71f, 4.24f, 18.84f, 4.24f, 17.78f)
                    verticalLineTo(16.07f)
                    curveTo(4.24f, 15.68f, 4.04f, 15.11f, 3.79f, 14.82f)
                    lineTo(2.44f, 13.23f)
                    curveTo(1.86f, 12.54f, 1.86f, 11.42f, 2.44f, 10.73f)
                    lineTo(3.79f, 9.14f)
                    curveTo(4.04f, 8.84f, 4.24f, 8.28f, 4.24f, 7.89f)
                    verticalLineTo(6.2f)
                    curveTo(4.24f, 5.14f, 5.11f, 4.27f, 6.17f, 4.27f)
                    horizontalLineTo(7.9f)
                    curveTo(8.3f, 4.27f, 8.86f, 4.06f, 9.16f, 3.81f)
                    lineTo(10.75f, 2.45f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 16.869f)
                    curveTo(11.45f, 16.869f, 11f, 16.419f, 11f, 15.869f)
                    curveTo(11f, 15.319f, 11.44f, 14.869f, 12f, 14.869f)
                    curveTo(12.55f, 14.869f, 13f, 15.319f, 13f, 15.869f)
                    curveTo(13f, 16.419f, 12.56f, 16.869f, 12f, 16.869f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 13.721f)
                    curveTo(11.59f, 13.721f, 11.25f, 13.381f, 11.25f, 12.971f)
                    verticalLineTo(8.131f)
                    curveTo(11.25f, 7.721f, 11.59f, 7.381f, 12f, 7.381f)
                    curveTo(12.41f, 7.381f, 12.75f, 7.721f, 12.75f, 8.131f)
                    verticalLineTo(12.961f)
                    curveTo(12.75f, 13.381f, 12.42f, 13.721f, 12f, 13.721f)
                    close()
                }
            }
        }.build()

        return _IconsaxInformation!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxInformation: ImageVector? = null
