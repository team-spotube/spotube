package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.FluentSquareMultiple: ImageVector
    get() {
        if (_FluentSquareMultiple != null) {
            return _FluentSquareMultiple!!
        }
        _FluentSquareMultiple = ImageVector.Builder(
            name = "FluentSquareMultiple",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF212121))) {
                moveTo(7.518f, 5f)
                horizontalLineTo(6.009f)
                arcToRelative(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3.24f, -3f)
                horizontalLineToRelative(8.001f)
                arcTo(4.75f, 4.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 6.75f)
                verticalLineToRelative(8f)
                arcToRelative(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, 3.24f)
                verticalLineToRelative(-1.508f)
                arcToRelative(1.75f, 1.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.5f, -1.732f)
                verticalLineToRelative(-8f)
                arcToRelative(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3.25f, -3.25f)
                horizontalLineToRelative(-8f)
                arcTo(1.75f, 1.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7.518f, 5f)
                close()
                moveTo(5.25f, 6f)
                arcTo(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 9.25f)
                verticalLineToRelative(9.5f)
                arcTo(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.25f, 22f)
                horizontalLineToRelative(9.5f)
                arcTo(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 18.75f)
                verticalLineToRelative(-9.5f)
                arcTo(3.25f, 3.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14.75f, 6f)
                horizontalLineToRelative(-9.5f)
                close()
                moveTo(3.5f, 9.25f)
                curveToRelative(0f, -0.966f, 0.784f, -1.75f, 1.75f, -1.75f)
                horizontalLineToRelative(9.5f)
                curveToRelative(0.967f, 0f, 1.75f, 0.784f, 1.75f, 1.75f)
                verticalLineToRelative(9.5f)
                arcToRelative(1.75f, 1.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.75f, 1.75f)
                horizontalLineToRelative(-9.5f)
                arcToRelative(1.75f, 1.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.75f, -1.75f)
                verticalLineToRelative(-9.5f)
                close()
            }
        }.build()

        return _FluentSquareMultiple!!
    }

@Suppress("ObjectPropertyName")
private var _FluentSquareMultiple: ImageVector? = null
