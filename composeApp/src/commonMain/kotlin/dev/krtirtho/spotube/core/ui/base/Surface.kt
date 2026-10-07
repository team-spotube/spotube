/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.core.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * The Frutiger-Aero material families available to [BaseSurface].
 *
 * - [Watery]: deep aqua water with caustic light and bubbles.
 * - [Icy]: pale frosted ice with crystalline facets and frost.
 * - [Chrome]: brushed metal with a hard specular reflection.
 * - [Acrylic]: translucent tinted panel with fine grain.
 * - [Glass]: clear, glossy glass with a strong sheen.
 *
 * Each style doubles as a full app theme (see [BaseUIColorScheme]).
 */
@Serializable
enum class SurfaceStyle {
    Watery,
    Icy,
    Chrome,
    Acrylic,
    Glass,
}

/**
 * A themed surface panel rendered in one of the Frutiger-Aero [SurfaceStyle] materials.
 *
 * Distinct from [androidx.compose.material3.Surface] — this draws decorative material
 * backgrounds (gradients, gloss, grain, bubbles, facets) and provides a matching
 * [LocalContentColor] so text/content inside stays legible.
 */
@Composable
fun BaseSurface(
    modifier: Modifier = Modifier,
    style: SurfaceStyle = SurfaceStyle.Glass,
    shape: Shape = RectangleShape,
    elevation: Dp? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues.Zero,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val onSurface = MaterialTheme.colorScheme.onSurface

    val resolvedElevation = elevation ?: when (style) {
        SurfaceStyle.Watery -> 14.dp
        SurfaceStyle.Icy -> 12.dp
        SurfaceStyle.Chrome -> 10.dp
        SurfaceStyle.Acrylic -> 16.dp
        SurfaceStyle.Glass -> 12.dp
    }
    val resolvedContentColor = contentColor ?: when (style) {
        SurfaceStyle.Watery -> Color.White
        SurfaceStyle.Icy -> if (isLight) Color(0xFF0B3A55) else Color(0xFFDCEBF7)
        SurfaceStyle.Chrome -> if (isLight) Color(0xFF1B2733) else Color(0xFFE7ECF2)
        SurfaceStyle.Acrylic, SurfaceStyle.Glass -> onSurface
    }
    val ambient = when (style) {
        SurfaceStyle.Watery -> Color(0xFF0E6FB0).copy(alpha = 0.38f)
        SurfaceStyle.Icy -> Color(0xFF3FA9E0).copy(alpha = 0.3f)
        SurfaceStyle.Chrome -> Color.Black.copy(alpha = 0.3f)
        SurfaceStyle.Acrylic -> onSurface.copy(alpha = 0.22f)
        SurfaceStyle.Glass -> onSurface.copy(alpha = 0.18f)
    }
    val spot = ambient.copy(alpha = (ambient.alpha + 0.08f).coerceAtMost(1f))

    val borderBrush = when (style) {
        SurfaceStyle.Watery -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.12f)),
        )

        SurfaceStyle.Icy -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f)),
        )

        SurfaceStyle.Chrome -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.28f)),
        )

        SurfaceStyle.Acrylic -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.12f)),
        )

        SurfaceStyle.Glass -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f)),
        )
    }

    // Translucent materials (Glass, Acrylic) must not use a drop shadow: it is drawn
    // behind the node and, because the panel is see-through, shows up as a dark inset
    // shape. Their depth comes from the edge, sheen and bevel instead.
    val castsShadow = style != SurfaceStyle.Glass && style != SurfaceStyle.Acrylic

    CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
        Box(
            modifier = modifier
                .then(
                    if (castsShadow) {
                        Modifier.shadow(
                            elevation = resolvedElevation,
                            shape = shape,
                            ambientColor = ambient,
                            spotColor = spot,
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .drawWithCache {
                    onDrawBehind { drawSurfaceMaterial(style, isLight) }
                }
                .border(BorderStroke(1.dp, borderBrush), shape)
                .padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * Paints the full-bleed material of [style] as a page background: the material body plus
 * its own sheen and reflections, but no panel bevel (a backdrop has no edges).
 */
fun Modifier.baseSurfaceBackdrop(style: SurfaceStyle, isLight: Boolean): Modifier =
    drawWithCache {
        onDrawBehind { drawSurfaceBase(style, isLight) }
    }

private fun DrawScope.drawSurfaceMaterial(style: SurfaceStyle, isLight: Boolean) {
    drawSurfaceBase(style, isLight)
    drawInnerBevel(isLight)
}

private fun DrawScope.drawSurfaceBase(style: SurfaceStyle, isLight: Boolean) {
    when (style) {
        SurfaceStyle.Watery -> drawWatery(isLight)
        SurfaceStyle.Icy -> drawIcy(isLight)
        SurfaceStyle.Chrome -> drawChrome(isLight)
        SurfaceStyle.Acrylic -> drawAcrylic(isLight)
        SurfaceStyle.Glass -> drawGlass(isLight)
    }
}

private fun DrawScope.drawGlass(isLight: Boolean) {
    // See-through glass: a faint blue-tinted fill so whatever is behind shows through,
    // defined by its bright edge, sheen and shadow rather than an opaque body.
    val tint = if (isLight) Color(0xFFCFE0F2) else Color(0xFF9FB8D6)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                tint.copy(alpha = if (isLight) 0.4f else 0.2f),
                tint.copy(alpha = if (isLight) 0.24f else 0.1f),
            ),
        ),
    )
    drawGloss(if (isLight) 0.55f else 0.16f)
    drawBottomReflection(if (isLight) 0.4f else 0.1f)
    drawBubbles(seed = 11, count = 3, alpha = if (isLight) 0.4f else 0.14f)
}

private fun DrawScope.drawAcrylic(isLight: Boolean) {
    // See-through acrylic: a translucent tinted sheet with a fine grain.
    val tint = if (isLight) Color(0xFFCFD9E8) else Color(0xFF8FA2B8)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                tint.copy(alpha = if (isLight) 0.45f else 0.22f),
                tint.copy(alpha = if (isLight) 0.28f else 0.12f),
            ),
        ),
    )

    // Fine grain so the panel reads as a translucent acrylic sheet.
    val random = Random(23)
    repeat(180) {
        val light = random.nextBoolean()
        drawCircle(
            color = if (light) {
                Color.White.copy(alpha = if (isLight) 0.16f else 0.06f)
            } else {
                Color.Black.copy(alpha = if (isLight) 0.06f else 0.08f)
            },
            radius = 0.7f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
        )
    }
    drawGloss(if (isLight) 0.4f else 0.12f, heightFraction = 0.45f)
}

private fun DrawScope.drawChrome(isLight: Boolean) {
    if (isLight) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFF8FBFF),
                0.16f to Color(0xFFD6DEE7),
                0.4f to Color(0xFF8E99A8),
                0.5f to Color(0xFFEDF2F7),
                0.62f to Color(0xFF9AA5B4),
                1f to Color(0xFF6C7787),
            ),
        )
        // Hard specular reflection near the top.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                startY = size.height * 0.05f,
                endY = size.height * 0.22f,
            ),
        )
    } else {
        // Dark gunmetal for the night variant.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF3B424C),
                0.16f to Color(0xFF2A303A),
                0.4f to Color(0xFF1B2028),
                0.5f to Color(0xFF343B45),
                0.62f to Color(0xFF232830),
                1f to Color(0xFF141820),
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                startY = size.height * 0.05f,
                endY = size.height * 0.22f,
            ),
        )
    }
    // Darkened lower edge for the metal bevel.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
            startY = size.height * 0.78f,
            endY = size.height,
        ),
    )
}

private fun DrawScope.drawIcy(isLight: Boolean) {
    val colors = if (isLight) {
        listOf(Color(0xFFF4FCFF), Color(0xFFCBE9FB), Color(0xFF9BD3F0))
    } else {
        // Snowy-night ice: dim slate-blue with faint frost.
        listOf(Color(0xFF2A3B4D), Color(0xFF1B2A39), Color(0xFF0F1A26))
    }
    drawRect(brush = Brush.verticalGradient(colors))

    val frost = if (isLight) Color.White else Color(0xFFB8CCE0)

    // Crystalline facets (kept inset so they are never clipped by the shape).
    val random = Random(37)
    repeat(4) {
        val s = size.minDimension * (0.2f + random.nextFloat() * 0.3f)
        val x = random.nextFloat() * (size.width - s).coerceAtLeast(0f)
        val y = random.nextFloat() * (size.height - s).coerceAtLeast(0f)
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + s, y + s * 0.4f)
            lineTo(x + s * 0.4f, y + s)
            close()
        }
        drawPath(path, frost.copy(alpha = if (isLight) 0.25f else 0.12f))
    }

    // Frost speckles.
    repeat(90) {
        drawCircle(
            color = frost.copy(alpha = if (isLight) 0.5f else 0.28f),
            radius = 0.8f,
            center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
        )
    }
    drawGloss(if (isLight) 0.6f else 0.12f, heightFraction = 0.5f)
}

private fun DrawScope.drawWatery(isLight: Boolean) {
    val colors = if (isLight) {
        listOf(Color(0xFFA6E3FF), Color(0xFF3FB6EA), Color(0xFF0E6FB0))
    } else {
        // Moonlit night water: deep blue with a pale silver sheen.
        listOf(Color(0xFF1C3A5E), Color(0xFF102942), Color(0xFF071827))
    }
    drawRect(brush = Brush.verticalGradient(colors))

    val sheen = if (isLight) Color.White else Color(0xFFB9D2F0)

    // Caustic / moonlight pooling from the surface.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(sheen.copy(alpha = if (isLight) 0.35f else 0.22f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.5f,
        ),
    )

    val random = Random(53)
    repeat(5) {
        val rx = size.width * (0.12f + random.nextFloat() * 0.2f)
        val cx = rx + random.nextFloat() * (size.width - 2 * rx).coerceAtLeast(0f)
        val cy = rx * 0.5f + random.nextFloat() * (size.height * 0.6f - rx).coerceAtLeast(0f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(sheen.copy(alpha = if (isLight) 0.3f else 0.18f), Color.Transparent),
                center = Offset(cx, cy),
                radius = rx,
            ),
            topLeft = Offset(cx - rx, cy - rx * 0.5f),
            size = Size(rx * 2f, rx),
        )
    }

    drawBubbles(seed = 71, count = 6, alpha = if (isLight) 0.4f else 0.22f)
}

private fun DrawScope.drawGloss(alpha: Float, heightFraction: Float = 0.5f) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = size.height * heightFraction,
        ),
    )
}

private fun DrawScope.drawBottomReflection(alpha: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = alpha)),
            startY = size.height * 0.7f,
            endY = size.height,
        ),
    )
}

/** A bright inner top edge plus a shaded inner bottom edge, giving the pane a glass bevel. */
private fun DrawScope.drawInnerBevel(isLight: Boolean) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = if (isLight) 0.85f else 0.22f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.1f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (isLight) 0.08f else 0.28f)),
            startY = size.height * 0.9f,
            endY = size.height,
        ),
    )
}

private fun DrawScope.drawBubbles(seed: Int, count: Int, alpha: Float) {
    val random = Random(seed)
    repeat(count) {
        val radius = size.minDimension * (0.05f + random.nextFloat() * 0.14f)
        // Keep bubbles inset so they are never clipped into hard arcs at the edges.
        val cx = radius + random.nextFloat() * (size.width - 2 * radius).coerceAtLeast(0f)
        val cy = radius + random.nextFloat() * (size.height - 2 * radius).coerceAtLeast(0f)
        val center = Offset(cx, cy)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

@BaseUIPhonePreview
@BaseUIDesktopPreview
@Composable
private fun BaseSurfacePreview() {
    BaseUIPreview {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SurfaceStyle.entries.forEach { style ->
                BaseSurface(
                    style = style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                ) {
                    Text(
                        text = style.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@BaseUIPhonePreview
@Composable
private fun BaseSurfaceGridPreview() {
    BaseUIPreview {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SurfaceStyle.entries.chunked(2).forEach { rowStyles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    rowStyles.forEach { style ->
                        BaseSurface(
                            style = style,
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = style.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Frutiger Aero",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    repeat(2 - rowStyles.size) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@BaseUIPhonePreview
@BaseUIDesktopPreview
@Composable
private fun SurfaceThemeBackdropPreview() {
    val isLight = LocalBaseUIColors.current.isLight
    val onSurface = LocalBaseUIColors.current.onSurface
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SurfaceStyle.entries.forEach { style ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .baseSurfaceBackdrop(style, isLight),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = style.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (style == SurfaceStyle.Watery) Color.White else onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
