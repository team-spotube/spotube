package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.FluentDismiss: ImageVector
    get() {
        if (_FluentDismiss != null) {
            return _FluentDismiss!!
        }
        _FluentDismiss = ImageVector.Builder(
            name = "FluentDismiss",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF212121))) {
                moveToRelative(4.397f, 4.554f)
                lineToRelative(0.073f, -0.084f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.976f, -0.073f)
                lineToRelative(0.084f, 0.073f)
                lineTo(12f, 10.939f)
                lineToRelative(6.47f, -6.47f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = true, isPositiveArc = true, 1.06f, 1.061f)
                lineTo(13.061f, 12f)
                lineToRelative(6.47f, 6.47f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.072f, 0.976f)
                lineToRelative(-0.073f, 0.084f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.976f, 0.073f)
                lineToRelative(-0.084f, -0.073f)
                lineTo(12f, 13.061f)
                lineToRelative(-6.47f, 6.47f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.06f, -1.061f)
                lineTo(10.939f, 12f)
                lineToRelative(-6.47f, -6.47f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.072f, -0.976f)
                lineToRelative(0.073f, -0.084f)
                lineToRelative(-0.073f, 0.084f)
                close()
            }
        }.build()

        return _FluentDismiss!!
    }

@Suppress("ObjectPropertyName")
private var _FluentDismiss: ImageVector? = null
