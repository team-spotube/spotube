package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.FluentMaximize: ImageVector
    get() {
        if (_FluentMaximize != null) {
            return _FluentMaximize!!
        }
        _FluentMaximize = ImageVector.Builder(
            name = "FluentMaximize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF212121))) {
                moveTo(5.75f, 3f)
                horizontalLineToRelative(12.5f)
                arcTo(2.75f, 2.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 5.75f)
                verticalLineToRelative(12.5f)
                arcTo(2.75f, 2.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18.25f, 21f)
                lineTo(5.75f, 21f)
                arcTo(2.75f, 2.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 18.25f)
                lineTo(3f, 5.75f)
                arcTo(2.75f, 2.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.75f, 3f)
                close()
                moveTo(5.75f, 4.5f)
                curveToRelative(-0.69f, 0f, -1.25f, 0.56f, -1.25f, 1.25f)
                verticalLineToRelative(12.5f)
                curveToRelative(0f, 0.69f, 0.56f, 1.25f, 1.25f, 1.25f)
                horizontalLineToRelative(12.5f)
                curveToRelative(0.69f, 0f, 1.25f, -0.56f, 1.25f, -1.25f)
                lineTo(19.5f, 5.75f)
                curveToRelative(0f, -0.69f, -0.56f, -1.25f, -1.25f, -1.25f)
                lineTo(5.75f, 4.5f)
                close()
            }
        }.build()

        return _FluentMaximize!!
    }

@Suppress("ObjectPropertyName")
private var _FluentMaximize: ImageVector? = null
