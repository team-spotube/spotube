package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.FluentMinus: ImageVector
    get() {
        if (_FluentMinus != null) {
            return _FluentMinus!!
        }
        _FluentMinus = ImageVector.Builder(
            name = "FluentMinus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF212121))) {
                moveTo(3.755f, 12.5f)
                horizontalLineToRelative(16.492f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -1.5f)
                horizontalLineTo(3.755f)
                arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 1.5f)
                close()
            }
        }.build()

        return _FluentMinus!!
    }

@Suppress("ObjectPropertyName")
private var _FluentMinus: ImageVector? = null
