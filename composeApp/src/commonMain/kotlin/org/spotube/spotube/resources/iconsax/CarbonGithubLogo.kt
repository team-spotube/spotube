package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.CarbonGithubLogo: ImageVector
    get() {
        if (_CarbonGithubLogo != null) {
            return _CarbonGithubLogo!!
        }
        _CarbonGithubLogo = ImageVector.Builder(
            name = "CarbonGithubLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1f
            ) {
                moveTo(16f, 2f)
                arcToRelative(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4.43f, 27.28f)
                curveToRelative(0.7f, 0.13f, 1f, -0.3f, 1f, -0.67f)
                reflectiveCurveToRelative(0f, -1.21f, 0f, -2.38f)
                curveToRelative(-3.89f, 0.84f, -4.71f, -1.88f, -4.71f, -1.88f)
                arcTo(3.71f, 3.71f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.24f, 22.3f)
                curveToRelative(-1.27f, -0.86f, 0.1f, -0.85f, 0.1f, -0.85f)
                arcTo(2.94f, 2.94f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.48f, 22.9f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.08f, 1.16f)
                arcToRelative(2.93f, 2.93f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.88f, -1.87f)
                curveToRelative(-3.1f, -0.36f, -6.37f, -1.56f, -6.37f, -6.92f)
                arcToRelative(5.4f, 5.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.44f, -3.76f)
                arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.14f, -3.7f)
                reflectiveCurveToRelative(1.17f, -0.38f, 3.85f, 1.43f)
                arcToRelative(13.3f, 13.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 0f)
                curveToRelative(2.67f, -1.81f, 3.84f, -1.43f, 3.84f, -1.43f)
                arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.14f, 3.7f)
                arcToRelative(5.4f, 5.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.44f, 3.76f)
                curveToRelative(0f, 5.38f, -3.27f, 6.56f, -6.39f, 6.91f)
                arcToRelative(3.33f, 3.33f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.95f, 2.59f)
                curveToRelative(0f, 1.87f, 0f, 3.38f, 0f, 3.84f)
                reflectiveCurveToRelative(0.25f, 0.81f, 1f, 0.67f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 2f)
                close()
            }
        }.build()

        return _CarbonGithubLogo!!
    }

@Suppress("ObjectPropertyName")
private var _CarbonGithubLogo: ImageVector? = null
