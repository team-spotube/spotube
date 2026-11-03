package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxImportArrow2: ImageVector
    get() {
        if (_IconsaxImportArrow2 != null) {
            return _IconsaxImportArrow2!!
        }
        _IconsaxImportArrow2 = ImageVector.Builder(
            name = "IconsaxImportArrow2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fillAlpha = 0.4f,
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.4f,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22.001f, 2f)
                lineTo(13.801f, 10.2f)
            }
            path(
                fillAlpha = 0.4f,
                stroke = SolidColor(Color.White),
                strokeAlpha = 0.4f,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 6.17f)
                verticalLineTo(11f)
                horizontalLineTo(17.83f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 2f)
                horizontalLineTo(9f)
                curveTo(4f, 2f, 2f, 4f, 2f, 9f)
                verticalLineTo(15f)
                curveTo(2f, 20f, 4f, 22f, 9f, 22f)
                horizontalLineTo(15f)
                curveTo(20f, 22f, 22f, 20f, 22f, 15f)
                verticalLineTo(13f)
            }
        }.build()

        return _IconsaxImportArrow2!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxImportArrow2: ImageVector? = null
