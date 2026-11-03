package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.PhosphorDatabaseDuotone: ImageVector
    get() {
        if (_PhosphorDatabaseDuotone != null) {
            return _PhosphorDatabaseDuotone!!
        }
        _PhosphorDatabaseDuotone = ImageVector.Builder(
            name = "PhosphorDatabaseDuotone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0.2f,
                strokeAlpha = 0.2f
            ) {
                moveTo(216f, 80f)
                curveToRelative(0f, 26.51f, -39.4f, 48f, -88f, 48f)
                reflectiveCurveTo(40f, 106.51f, 40f, 80f)
                reflectiveCurveToRelative(39.4f, -48f, 88f, -48f)
                reflectiveCurveTo(216f, 53.49f, 216f, 80f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(128f, 24f)
                curveTo(74.17f, 24f, 32f, 48.6f, 32f, 80f)
                verticalLineToRelative(96f)
                curveToRelative(0f, 31.4f, 42.17f, 56f, 96f, 56f)
                reflectiveCurveToRelative(96f, -24.6f, 96f, -56f)
                lineTo(224f, 80f)
                curveTo(224f, 48.6f, 181.83f, 24f, 128f, 24f)
                close()
                moveTo(208f, 128f)
                curveToRelative(0f, 9.62f, -7.88f, 19.43f, -21.61f, 26.92f)
                curveTo(170.93f, 163.35f, 150.19f, 168f, 128f, 168f)
                reflectiveCurveToRelative(-42.93f, -4.65f, -58.39f, -13.08f)
                curveTo(55.88f, 147.43f, 48f, 137.62f, 48f, 128f)
                lineTo(48f, 111.36f)
                curveToRelative(17.06f, 15f, 46.23f, 24.64f, 80f, 24.64f)
                reflectiveCurveToRelative(62.94f, -9.68f, 80f, -24.64f)
                close()
                moveTo(69.61f, 53.08f)
                curveTo(85.07f, 44.65f, 105.81f, 40f, 128f, 40f)
                reflectiveCurveToRelative(42.93f, 4.65f, 58.39f, 13.08f)
                curveTo(200.12f, 60.57f, 208f, 70.38f, 208f, 80f)
                reflectiveCurveToRelative(-7.88f, 19.43f, -21.61f, 26.92f)
                curveTo(170.93f, 115.35f, 150.19f, 120f, 128f, 120f)
                reflectiveCurveToRelative(-42.93f, -4.65f, -58.39f, -13.08f)
                curveTo(55.88f, 99.43f, 48f, 89.62f, 48f, 80f)
                reflectiveCurveTo(55.88f, 60.57f, 69.61f, 53.08f)
                close()
                moveTo(186.39f, 202.92f)
                curveTo(170.93f, 211.35f, 150.19f, 216f, 128f, 216f)
                reflectiveCurveToRelative(-42.93f, -4.65f, -58.39f, -13.08f)
                curveTo(55.88f, 195.43f, 48f, 185.62f, 48f, 176f)
                lineTo(48f, 159.36f)
                curveToRelative(17.06f, 15f, 46.23f, 24.64f, 80f, 24.64f)
                reflectiveCurveToRelative(62.94f, -9.68f, 80f, -24.64f)
                lineTo(208f, 176f)
                curveTo(208f, 185.62f, 200.12f, 195.43f, 186.39f, 202.92f)
                close()
            }
        }.build()

        return _PhosphorDatabaseDuotone!!
    }

@Suppress("ObjectPropertyName")
private var _PhosphorDatabaseDuotone: ImageVector? = null
