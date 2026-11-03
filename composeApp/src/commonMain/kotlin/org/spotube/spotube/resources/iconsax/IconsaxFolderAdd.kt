package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxFolderAdd: ImageVector
    get() {
        if (_IconsaxFolderAdd != null) {
            return _IconsaxFolderAdd!!
        }
        _IconsaxFolderAdd = ImageVector.Builder(
            name = "IconsaxFolderAdd",
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
                    moveTo(21.74f, 9.44f)
                    horizontalLineTo(2f)
                    verticalLineTo(6.42f)
                    curveTo(2f, 3.98f, 3.98f, 2f, 6.42f, 2f)
                    horizontalLineTo(8.75f)
                    curveTo(10.38f, 2f, 10.89f, 2.53f, 11.54f, 3.4f)
                    lineTo(12.94f, 5.26f)
                    curveTo(13.25f, 5.67f, 13.29f, 5.73f, 13.87f, 5.73f)
                    horizontalLineTo(16.66f)
                    curveTo(19.03f, 5.72f, 21.05f, 7.28f, 21.74f, 9.44f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21.99f, 10.84f)
                    curveTo(21.97f, 10.35f, 21.89f, 9.89f, 21.74f, 9.44f)
                    horizontalLineTo(2f)
                    verticalLineTo(16.65f)
                    curveTo(2f, 19.6f, 4.4f, 22f, 7.35f, 22f)
                    horizontalLineTo(16.65f)
                    curveTo(19.6f, 22f, 22f, 19.6f, 22f, 16.65f)
                    verticalLineTo(11.07f)
                    curveTo(22f, 11f, 22f, 10.91f, 21.99f, 10.84f)
                    close()
                    moveTo(14.5f, 16.25f)
                    horizontalLineTo(12.81f)
                    verticalLineTo(18f)
                    curveTo(12.81f, 18.41f, 12.47f, 18.75f, 12.06f, 18.75f)
                    curveTo(11.65f, 18.75f, 11.31f, 18.41f, 11.31f, 18f)
                    verticalLineTo(16.25f)
                    horizontalLineTo(9.5f)
                    curveTo(9.09f, 16.25f, 8.75f, 15.91f, 8.75f, 15.5f)
                    curveTo(8.75f, 15.09f, 9.09f, 14.75f, 9.5f, 14.75f)
                    horizontalLineTo(11.31f)
                    verticalLineTo(13f)
                    curveTo(11.31f, 12.59f, 11.65f, 12.25f, 12.06f, 12.25f)
                    curveTo(12.47f, 12.25f, 12.81f, 12.59f, 12.81f, 13f)
                    verticalLineTo(14.75f)
                    horizontalLineTo(14.5f)
                    curveTo(14.91f, 14.75f, 15.25f, 15.09f, 15.25f, 15.5f)
                    curveTo(15.25f, 15.91f, 14.91f, 16.25f, 14.5f, 16.25f)
                    close()
                }
            }
        }.build()

        return _IconsaxFolderAdd!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxFolderAdd: ImageVector? = null
