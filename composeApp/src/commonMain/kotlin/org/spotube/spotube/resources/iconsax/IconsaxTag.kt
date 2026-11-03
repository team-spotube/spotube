package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxTag: ImageVector
    get() {
        if (_IconsaxTag != null) {
            return _IconsaxTag!!
        }
        _IconsaxTag = ImageVector.Builder(
            name = "IconsaxTag",
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
                    moveTo(4.17f, 15.3f)
                    lineTo(8.7f, 19.83f)
                    curveTo(10.56f, 21.69f, 13.58f, 21.69f, 15.45f, 19.83f)
                    lineTo(19.84f, 15.44f)
                    curveTo(21.7f, 13.58f, 21.7f, 10.56f, 19.84f, 8.69f)
                    lineTo(15.3f, 4.17f)
                    curveTo(14.35f, 3.22f, 13.04f, 2.71f, 11.7f, 2.78f)
                    lineTo(6.7f, 3.02f)
                    curveTo(4.7f, 3.11f, 3.11f, 4.7f, 3.01f, 6.69f)
                    lineTo(2.77f, 11.69f)
                    curveTo(2.71f, 13.04f, 3.22f, 14.35f, 4.17f, 15.3f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(9.5f, 12.38f)
                    curveTo(11.091f, 12.38f, 12.38f, 11.091f, 12.38f, 9.5f)
                    curveTo(12.38f, 7.91f, 11.091f, 6.62f, 9.5f, 6.62f)
                    curveTo(7.91f, 6.62f, 6.62f, 7.91f, 6.62f, 9.5f)
                    curveTo(6.62f, 11.091f, 7.91f, 12.38f, 9.5f, 12.38f)
                    close()
                }
            }
        }.build()

        return _IconsaxTag!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxTag: ImageVector? = null
