package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Iconsax.LineDiscord: ImageVector
    get() {
        if (_LineDiscord != null) {
            return _LineDiscord!!
        }
        _LineDiscord = ImageVector.Builder(
            name = "LineDiscord",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.942f, 5.555f)
                curveTo(17.647f, 4.95f, 16.263f, 4.509f, 14.816f, 4.259f)
                curveTo(14.638f, 4.58f, 14.43f, 5.012f, 14.287f, 5.356f)
                curveTo(12.749f, 5.125f, 11.224f, 5.125f, 9.714f, 5.356f)
                curveTo(9.571f, 5.012f, 9.359f, 4.58f, 9.179f, 4.259f)
                curveTo(7.731f, 4.509f, 6.345f, 4.951f, 5.05f, 5.559f)
                curveTo(2.439f, 9.505f, 1.731f, 13.353f, 2.085f, 17.146f)
                curveTo(3.817f, 18.439f, 5.495f, 19.225f, 7.145f, 19.739f)
                curveTo(7.553f, 19.178f, 7.916f, 18.582f, 8.229f, 17.954f)
                curveTo(7.633f, 17.728f, 7.062f, 17.448f, 6.523f, 17.124f)
                curveTo(6.666f, 17.017f, 6.806f, 16.907f, 6.941f, 16.792f)
                curveTo(10.232f, 18.332f, 13.807f, 18.332f, 17.059f, 16.792f)
                curveTo(17.196f, 16.907f, 17.336f, 17.017f, 17.477f, 17.124f)
                curveTo(16.936f, 17.45f, 16.364f, 17.729f, 15.767f, 17.956f)
                curveTo(16.08f, 18.582f, 16.442f, 19.18f, 16.851f, 19.741f)
                curveTo(18.503f, 19.227f, 20.183f, 18.441f, 21.915f, 17.146f)
                curveTo(22.33f, 12.748f, 21.206f, 8.936f, 18.942f, 5.555f)
                close()
                moveTo(8.678f, 14.813f)
                curveTo(7.69f, 14.813f, 6.88f, 13.891f, 6.88f, 12.768f)
                curveTo(6.88f, 11.645f, 7.672f, 10.721f, 8.678f, 10.721f)
                curveTo(9.683f, 10.721f, 10.493f, 11.643f, 10.476f, 12.768f)
                curveTo(10.477f, 13.891f, 9.683f, 14.813f, 8.678f, 14.813f)
                close()
                moveTo(15.322f, 14.813f)
                curveTo(14.334f, 14.813f, 13.524f, 13.891f, 13.524f, 12.768f)
                curveTo(13.524f, 11.645f, 14.317f, 10.721f, 15.322f, 10.721f)
                curveTo(16.327f, 10.721f, 17.138f, 11.643f, 17.12f, 12.768f)
                curveTo(17.12f, 13.891f, 16.327f, 14.813f, 15.322f, 14.813f)
                close()
            }
        }.build()

        return _LineDiscord!!
    }

@Suppress("ObjectPropertyName")
private var _LineDiscord: ImageVector? = null
