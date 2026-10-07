package dev.krtirtho.spotube.core.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.highlight(color: Color): Modifier =
    drawWithCache {
        // Frutiger-Aero glass: a bright specular sheen across the top half plus a
        // faint reflection rising from the bottom edge.
        val topGloss = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = color.alpha * 0.25f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.55f,
        )
        val bottomReflection = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = color.alpha * 0.3f)),
            startY = size.height * 0.72f,
            endY = size.height,
        )
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = topGloss,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = size,
            )
            drawRect(
                brush = bottomReflection,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = size,
            )
        }
    }

@BaseUIPhonePreview
@Composable
private fun HighlightModifierPreview() {
    BaseUIPreview {
        var highlighted by remember { mutableStateOf(true) }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(
                        if (highlighted) {
                            Modifier.highlight(Color.White.copy(alpha = 0.45f))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { highlighted = !highlighted },
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}
