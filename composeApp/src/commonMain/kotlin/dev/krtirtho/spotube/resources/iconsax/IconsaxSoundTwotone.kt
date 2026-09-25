package dev.krtirtho.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.IconsaxSoundTwotone: ImageVector
    get() {
        if (_IconsaxSoundTwotone != null) {
            return _IconsaxSoundTwotone!!
        }
        _IconsaxSoundTwotone = ImageVector.Builder(
            name = "IconsaxSoundTwotone",
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
                    moveTo(3f, 16.5f)
                    curveTo(2.59f, 16.5f, 2.25f, 16.16f, 2.25f, 15.75f)
                    verticalLineTo(8.25f)
                    curveTo(2.25f, 7.84f, 2.59f, 7.5f, 3f, 7.5f)
                    curveTo(3.41f, 7.5f, 3.75f, 7.84f, 3.75f, 8.25f)
                    verticalLineTo(15.75f)
                    curveTo(3.75f, 16.16f, 3.41f, 16.5f, 3f, 16.5f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(7.5f, 19f)
                    curveTo(7.09f, 19f, 6.75f, 18.66f, 6.75f, 18.25f)
                    verticalLineTo(5.75f)
                    curveTo(6.75f, 5.34f, 7.09f, 5f, 7.5f, 5f)
                    curveTo(7.91f, 5f, 8.25f, 5.34f, 8.25f, 5.75f)
                    verticalLineTo(18.25f)
                    curveTo(8.25f, 18.66f, 7.91f, 19f, 7.5f, 19f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(12f, 21.5f)
                    curveTo(11.59f, 21.5f, 11.25f, 21.16f, 11.25f, 20.75f)
                    verticalLineTo(3.25f)
                    curveTo(11.25f, 2.84f, 11.59f, 2.5f, 12f, 2.5f)
                    curveTo(12.41f, 2.5f, 12.75f, 2.84f, 12.75f, 3.25f)
                    verticalLineTo(20.75f)
                    curveTo(12.75f, 21.16f, 12.41f, 21.5f, 12f, 21.5f)
                    close()
                }
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    moveTo(16.5f, 19f)
                    curveTo(16.09f, 19f, 15.75f, 18.66f, 15.75f, 18.25f)
                    verticalLineTo(5.75f)
                    curveTo(15.75f, 5.34f, 16.09f, 5f, 16.5f, 5f)
                    curveTo(16.91f, 5f, 17.25f, 5.34f, 17.25f, 5.75f)
                    verticalLineTo(18.25f)
                    curveTo(17.25f, 18.66f, 16.91f, 19f, 16.5f, 19f)
                    close()
                }
                path(fill = SolidColor(Color.White)) {
                    moveTo(21f, 16.5f)
                    curveTo(20.59f, 16.5f, 20.25f, 16.16f, 20.25f, 15.75f)
                    verticalLineTo(8.25f)
                    curveTo(20.25f, 7.84f, 20.59f, 7.5f, 21f, 7.5f)
                    curveTo(21.41f, 7.5f, 21.75f, 7.84f, 21.75f, 8.25f)
                    verticalLineTo(15.75f)
                    curveTo(21.75f, 16.16f, 21.41f, 16.5f, 21f, 16.5f)
                    close()
                }
            }
        }.build()

        return _IconsaxSoundTwotone!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxSoundTwotone: ImageVector? = null
