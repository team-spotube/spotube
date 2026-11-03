package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxLink: ImageVector
    get() {
        if (_IconsaxLink != null) {
            return _IconsaxLink!!
        }
        _IconsaxLink = ImageVector.Builder(
            name = "IconsaxLink",
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
                    moveTo(13.06f, 10.94f)
                    curveTo(15.31f, 13.19f, 15.31f, 16.83f, 13.06f, 19.07f)
                    curveTo(10.81f, 21.31f, 7.17f, 21.32f, 4.93f, 19.07f)
                    curveTo(2.69f, 16.82f, 2.68f, 13.18f, 4.93f, 10.94f)
                }
                path(
                    fillAlpha = 0.4f,
                    stroke = SolidColor(Color.White),
                    strokeAlpha = 0.4f,
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(10.59f, 13.41f)
                    curveTo(8.25f, 11.07f, 8.25f, 7.27f, 10.59f, 4.92f)
                    curveTo(12.93f, 2.57f, 16.73f, 2.58f, 19.08f, 4.92f)
                    curveTo(21.43f, 7.26f, 21.42f, 11.06f, 19.08f, 13.41f)
                }
            }
        }.build()

        return _IconsaxLink!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxLink: ImageVector? = null
