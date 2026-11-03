package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxForbidden: ImageVector
    get() {
        if (_IconsaxForbidden != null) {
            return _IconsaxForbidden!!
        }
        _IconsaxForbidden = ImageVector.Builder(
            name = "IconsaxForbidden",
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
                    moveTo(14.9f, 2f)
                    horizontalLineTo(9.1f)
                    curveTo(8.42f, 2f, 7.46f, 2.4f, 6.98f, 2.88f)
                    lineTo(2.88f, 6.98f)
                    curveTo(2.4f, 7.46f, 2f, 8.42f, 2f, 9.1f)
                    verticalLineTo(14.9f)
                    curveTo(2f, 15.58f, 2.4f, 16.54f, 2.88f, 17.02f)
                    lineTo(6.98f, 21.12f)
                    curveTo(7.46f, 21.6f, 8.42f, 22f, 9.1f, 22f)
                    horizontalLineTo(14.9f)
                    curveTo(15.58f, 22f, 16.54f, 21.6f, 17.02f, 21.12f)
                    lineTo(21.12f, 17.02f)
                    curveTo(21.6f, 16.54f, 22f, 15.58f, 22f, 14.9f)
                    verticalLineTo(9.1f)
                    curveTo(22f, 8.42f, 21.6f, 7.46f, 21.12f, 6.98f)
                    lineTo(17.02f, 2.88f)
                    curveTo(16.54f, 2.4f, 15.58f, 2f, 14.9f, 2f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(3.02f, 21.751f)
                    curveTo(2.83f, 21.751f, 2.64f, 21.681f, 2.49f, 21.531f)
                    curveTo(2.2f, 21.241f, 2.2f, 20.761f, 2.49f, 20.471f)
                    lineTo(20.47f, 2.491f)
                    curveTo(20.76f, 2.201f, 21.24f, 2.201f, 21.53f, 2.491f)
                    curveTo(21.82f, 2.781f, 21.82f, 3.261f, 21.53f, 3.551f)
                    lineTo(3.55f, 21.531f)
                    curveTo(3.41f, 21.681f, 3.22f, 21.751f, 3.02f, 21.751f)
                    close()
                }
            }
        }.build()

        return _IconsaxForbidden!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxForbidden: ImageVector? = null
