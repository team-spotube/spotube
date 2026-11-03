package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxTextalignLeft: ImageVector
    get() {
        if (_IconsaxTextalignLeft != null) {
            return _IconsaxTextalignLeft!!
        }
        _IconsaxTextalignLeft = ImageVector.Builder(
            name = "IconsaxTextalignLeft",
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
                    moveTo(21f, 5.25f)
                    horizontalLineTo(3f)
                    curveTo(2.59f, 5.25f, 2.25f, 4.91f, 2.25f, 4.5f)
                    curveTo(2.25f, 4.09f, 2.59f, 3.75f, 3f, 3.75f)
                    horizontalLineTo(21f)
                    curveTo(21.41f, 3.75f, 21.75f, 4.09f, 21.75f, 4.5f)
                    curveTo(21.75f, 4.91f, 21.41f, 5.25f, 21f, 5.25f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(12.47f, 10.25f)
                    horizontalLineTo(3f)
                    curveTo(2.59f, 10.25f, 2.25f, 9.91f, 2.25f, 9.5f)
                    curveTo(2.25f, 9.09f, 2.59f, 8.75f, 3f, 8.75f)
                    horizontalLineTo(12.47f)
                    curveTo(12.88f, 8.75f, 13.22f, 9.09f, 13.22f, 9.5f)
                    curveTo(13.22f, 9.91f, 12.89f, 10.25f, 12.47f, 10.25f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21f, 15.25f)
                    horizontalLineTo(3f)
                    curveTo(2.59f, 15.25f, 2.25f, 14.91f, 2.25f, 14.5f)
                    curveTo(2.25f, 14.09f, 2.59f, 13.75f, 3f, 13.75f)
                    horizontalLineTo(21f)
                    curveTo(21.41f, 13.75f, 21.75f, 14.09f, 21.75f, 14.5f)
                    curveTo(21.75f, 14.91f, 21.41f, 15.25f, 21f, 15.25f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(12.47f, 20.25f)
                    horizontalLineTo(3f)
                    curveTo(2.59f, 20.25f, 2.25f, 19.91f, 2.25f, 19.5f)
                    curveTo(2.25f, 19.09f, 2.59f, 18.75f, 3f, 18.75f)
                    horizontalLineTo(12.47f)
                    curveTo(12.88f, 18.75f, 13.22f, 19.09f, 13.22f, 19.5f)
                    curveTo(13.22f, 19.91f, 12.89f, 20.25f, 12.47f, 20.25f)
                    close()
                }
            }
        }.build()

        return _IconsaxTextalignLeft!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxTextalignLeft: ImageVector? = null
