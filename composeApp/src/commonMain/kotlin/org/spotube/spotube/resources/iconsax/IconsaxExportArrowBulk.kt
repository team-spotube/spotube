package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxExportArrowBulk: ImageVector
    get() {
        if (_IconsaxExportArrowBulk != null) {
            return _IconsaxExportArrowBulk!!
        }
        _IconsaxExportArrowBulk = ImageVector.Builder(
            name = "IconsaxExportArrowBulk",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.4f,
                strokeAlpha = 0.4f
            ) {
                moveTo(16.8f, 9f)
                horizontalLineTo(7.2f)
                curveTo(4f, 9f, 2f, 11f, 2f, 14.2f)
                verticalLineTo(16.79f)
                curveTo(2f, 20f, 4f, 22f, 7.2f, 22f)
                horizontalLineTo(16.79f)
                curveTo(19.99f, 22f, 21.99f, 20f, 21.99f, 16.8f)
                verticalLineTo(14.2f)
                curveTo(22f, 11f, 20f, 9f, 16.8f, 9f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(15.88f, 5.57f)
                lineTo(12.53f, 2.22f)
                curveTo(12.24f, 1.93f, 11.76f, 1.93f, 11.47f, 2.22f)
                lineTo(8.12f, 5.57f)
                curveTo(7.83f, 5.86f, 7.83f, 6.34f, 8.12f, 6.63f)
                curveTo(8.41f, 6.92f, 8.89f, 6.92f, 9.18f, 6.63f)
                lineTo(11.25f, 4.56f)
                verticalLineTo(15.25f)
                curveTo(11.25f, 15.66f, 11.59f, 16f, 12f, 16f)
                curveTo(12.41f, 16f, 12.75f, 15.66f, 12.75f, 15.25f)
                verticalLineTo(4.56f)
                lineTo(14.82f, 6.63f)
                curveTo(14.97f, 6.78f, 15.16f, 6.85f, 15.35f, 6.85f)
                curveTo(15.54f, 6.85f, 15.73f, 6.78f, 15.88f, 6.63f)
                curveTo(16.18f, 6.34f, 16.18f, 5.87f, 15.88f, 5.57f)
                close()
            }
        }.build()

        return _IconsaxExportArrowBulk!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxExportArrowBulk: ImageVector? = null
