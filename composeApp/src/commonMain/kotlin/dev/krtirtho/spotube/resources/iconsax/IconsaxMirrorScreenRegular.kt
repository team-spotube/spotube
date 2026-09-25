package dev.krtirtho.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxMirrorScreenRegular: ImageVector
    get() {
        if (_IconsaxMirrorScreenRegular != null) {
            return _IconsaxMirrorScreenRegular!!
        }
        _IconsaxMirrorScreenRegular = ImageVector.Builder(
            name = "IconsaxMirrorScreenRegular",
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
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(2f, 9f)
                    verticalLineTo(8f)
                    curveTo(2f, 5f, 4f, 3f, 7f, 3f)
                    horizontalLineTo(17f)
                    curveTo(20f, 3f, 22f, 5f, 22f, 8f)
                    verticalLineTo(16f)
                    curveTo(22f, 19f, 20f, 21f, 17f, 21f)
                    horizontalLineTo(16f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(3.69f, 11.711f)
                    curveTo(8.31f, 12.301f, 11.7f, 15.701f, 12.3f, 20.321f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(2.62f, 15.07f)
                    curveTo(6.01f, 15.5f, 8.5f, 18f, 8.94f, 21.39f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(1.98f, 18.859f)
                    curveTo(3.67f, 19.079f, 4.92f, 20.319f, 5.14f, 22.019f)
                }
            }
        }.build()

        return _IconsaxMirrorScreenRegular!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMirrorScreenRegular: ImageVector? = null
