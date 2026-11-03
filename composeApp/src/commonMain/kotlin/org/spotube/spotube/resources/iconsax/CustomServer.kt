package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.CustomServer: ImageVector
    get() {
        if (_CustomServer != null) {
            return _CustomServer!!
        }
        _CustomServer = ImageVector.Builder(
            name = "CustomServer",
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
                // Layer 1: The solid, bright Status Check Badge (bottom right)
                path(fill = SolidColor(Color.White)) {
                    moveTo(18.5f, 11f)
                    curveTo(14.91f, 11f, 12f, 13.91f, 12f, 17.5f)
                    curveTo(12f, 21.09f, 14.91f, 24f, 18.5f, 24f)
                    curveTo(22.09f, 24f, 25f, 21.09f, 25f, 17.5f)
                    curveTo(25f, 13.91f, 22.09f, 11f, 18.5f, 11f)
                    close()
                    // The cutout shape of the checkmark
                    moveTo(17.2f, 20.3f)
                    lineTo(14.7f, 17.8f)
                    curveTo(14.31f, 17.41f, 14.31f, 16.78f, 14.7f, 16.39f)
                    curveTo(15.09f, 16f, 15.72f, 16f, 16.11f, 16.39f)
                    lineTo(17.9f, 18.18f)
                    lineTo(21.39f, 14.69f)
                    curveTo(21.78f, 14.3f, 22.41f, 14.3f, 22.8f, 14.69f)
                    curveTo(23.19f, 15.08f, 23.19f, 15.71f, 22.8f, 16.1f)
                    lineTo(18.6f, 20.3f)
                    curveTo(18.21f, 20.69f, 17.59f, 20.69f, 17.2f, 20.3f)
                    close()
                }

                // Layer 2: Main Server Rack Chassis (Top & Bottom shells with 40% opacity)
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 0.4f,
                    strokeAlpha = 0.4f
                ) {
                    // Outer border of the double-slot server rack structure
                    moveTo(20.5f, 8.5f)
                    verticalLineTo(7f)
                    curveTo(20.5f, 3.5f, 18f, 1.5f, 13f, 1.5f)
                    horizontalLineTo(7f)
                    curveTo(3f, 1.5f, 1.5f, 3.5f, 1.5f, 7f)
                    verticalLineTo(9.5f)
                    curveTo(1.5f, 11.5f, 3f, 12f, 4.5f, 12f)
                    curveTo(3f, 12f, 1.5f, 12.5f, 1.5f, 14.5f)
                    verticalLineTo(17f)
                    curveTo(1.5f, 20.5f, 3f, 22.5f, 7f, 22.5f)
                    horizontalLineTo(10f)
                    curveTo(10.55f, 22.5f, 11f, 22.05f, 11f, 21.5f)
                    curveTo(11f, 20.95f, 10.55f, 20.5f, 10f, 20.5f)
                    horizontalLineTo(7f)
                    curveTo(4.5f, 20.5f, 3.5f, 19.5f, 3.5f, 17f)
                    verticalLineTo(14.5f)
                    curveTo(3.5f, 13f, 4.5f, 13f, 6f, 13f)
                    curveTo(6.55f, 13f, 7f, 12.55f, 7f, 12f)
                    curveTo(7f, 11.45f, 6.55f, 11f, 6f, 11f)
                    curveTo(4.5f, 11f, 3.5f, 11f, 3.5f, 9.5f)
                    verticalLineTo(7f)
                    curveTo(3.5f, 4.5f, 4.5f, 3.5f, 7f, 3.5f)
                    horizontalLineTo(13f)
                    curveTo(16.5f, 3.5f, 18.5f, 4.5f, 18.5f, 7f)
                    verticalLineTo(8.5f)
                    curveTo(18.5f, 9.05f, 18.95f, 9.5f, 19.5f, 9.5f)
                    curveTo(20.05f, 9.5f, 20.5f, 9.05f, 20.5f, 8.5f)
                    close()
                }

                // Layer 3: Server Front Panel Slots, Buttons, and LEDs (100% white)
                path(fill = SolidColor(Color.White)) {
                    // Top Slot LED 1
                    moveTo(5.5f, 5.5f)
                    curveTo(6.05f, 5.5f, 6.5f, 5.95f, 6.5f, 6.5f)
                    verticalLineTo(7.5f)
                    curveTo(6.5f, 8.05f, 6.05f, 8.5f, 5.5f, 8.5f)
                    curveTo(4.95f, 8.5f, 4.5f, 8.05f, 4.5f, 7.5f)
                    verticalLineTo(6.5f)
                    curveTo(4.5f, 5.95f, 4.95f, 5.5f, 5.5f, 5.5f)
                    close()

                    // Top Slot LED 2
                    moveTo(8.5f, 5.5f)
                    curveTo(9.05f, 5.5f, 9.5f, 5.95f, 9.5f, 6.5f)
                    verticalLineTo(7.5f)
                    curveTo(9.5f, 8.05f, 9.05f, 8.5f, 8.5f, 8.5f)
                    curveTo(7.95f, 8.5f, 7.5f, 8.05f, 7.5f, 7.5f)
                    verticalLineTo(6.5f)
                    curveTo(7.5f, 5.95f, 7.95f, 5.5f, 8.5f, 5.5f)
                    close()

                    // Top Slot Button Strip
                    moveTo(11.5f, 6f)
                    horizontalLineTo(16.5f)
                    curveTo(17.05f, 6f, 17.5f, 6.45f, 17.5f, 7f)
                    curveTo(17.5f, 7.55f, 17.05f, 8f, 16.5f, 8f)
                    horizontalLineTo(11.5f)
                    curveTo(10.95f, 8f, 10.5f, 7.55f, 10.5f, 7f)
                    curveTo(10.5f, 6.45f, 10.95f, 6f, 11.5f, 6f)
                    close()

                    // Bottom Slot LED 1
                    moveTo(5.5f, 14.5f)
                    curveTo(6.05f, 14.5f, 6.5f, 14.95f, 6.5f, 15.5f)
                    verticalLineTo(16.5f)
                    curveTo(6.5f, 17.05f, 6.05f, 17.5f, 5.5f, 17.5f)
                    curveTo(4.95f, 17.5f, 4.5f, 17.05f, 4.5f, 16.5f)
                    verticalLineTo(15.5f)
                    curveTo(4.5f, 14.95f, 4.95f, 14.5f, 5.5f, 14.5f)
                    close()

                    // Bottom Slot LED 2
                    moveTo(8.5f, 14.5f)
                    curveTo(9.05f, 14.5f, 9.5f, 14.95f, 9.5f, 15.5f)
                    verticalLineTo(16.5f)
                    curveTo(9.5f, 17.05f, 9.05f, 17.5f, 8.5f, 17.5f)
                    curveTo(7.95f, 17.5f, 7.5f, 17.05f, 7.5f, 16.5f)
                    verticalLineTo(15.5f)
                    curveTo(7.5f, 14.95f, 7.95f, 14.5f, 8.5f, 14.5f)
                    close()
                }
            }
        }.build()

        return _CustomServer!!
    }

@Suppress("ObjectPropertyName")
private var _CustomServer: ImageVector? = null