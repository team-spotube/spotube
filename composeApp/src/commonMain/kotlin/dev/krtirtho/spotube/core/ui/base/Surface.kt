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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
 * The role a surface plays within a theme. Each style produces a distinct material for
 * every role, so a theme is composed from them:
 *
 * - [PrimaryBackground] — the subtle main background (pages, panels).
 * - [SecondaryBackground] — a distinct, complementary background hue that pairs with the
 *   primary (grouped/alternate sections).
 * - [PrimaryAccent] — the primary accent surface for interactive elements (buttons,
 *   toggles, active chips).
 * - [SecondaryAccent] — a complementary accent surface (secondary actions, highlights).
 * - [InverseBackground] — the inverted-brightness background (tooltips, snackbars).
 * - [InverseAction] — the inverted-brightness action surface.
 */
@Serializable
enum class SurfaceRole {
    PrimaryBackground,
    SecondaryBackground,
    PrimaryAccent,
    SecondaryAccent,
    InverseBackground,
    InverseAction,
}

/**
 * A themed surface panel rendered in one of the Frutiger-Aero [SurfaceStyle] materials,
 * in the given [SurfaceRole].
 *
 * Distinct from [androidx.compose.material3.Surface] — this draws decorative material
 * backgrounds (gradients, gloss, grain, bubbles, facets) and provides a matching
 * [LocalContentColor] so text/content inside stays legible.
 */
@Composable
fun BaseSurface(
    modifier: Modifier = Modifier,
    style: SurfaceStyle = LocalBaseUIColors.current.surfaceTheme,
    role: SurfaceRole = SurfaceRole.PrimaryBackground,
    shape: Shape = RectangleShape,
    elevation: Dp? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues.Zero,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val ambientScheme = LocalBaseUIColors.current
    val isLight = ambientScheme.isLight
    // A surface is rendered in its own style's colour scheme, so previewing a style shows
    // that style's real accent instead of the ambient theme's. When the requested style is
    // the ambient theme, the ambient scheme is used so a user-chosen accent still applies.
    val ownScheme = remember(style, isLight) {
        baseUIColorScheme(style, isLight, style.accentColor(isLight))
    }
    val scheme = if (ambientScheme.surfaceTheme == style) ambientScheme else ownScheme
    val palette = surfacePalette(style, isLight)

    val baseElevation = when (style) {
        SurfaceStyle.Watery -> 14.dp
        SurfaceStyle.Icy -> 12.dp
        SurfaceStyle.Chrome -> 10.dp
        SurfaceStyle.Acrylic -> 16.dp
        SurfaceStyle.Glass -> 12.dp
    }
    val resolvedElevation = elevation ?: when (role) {
        SurfaceRole.PrimaryBackground -> baseElevation
        SurfaceRole.SecondaryBackground -> baseElevation + 2.dp
        SurfaceRole.PrimaryAccent, SurfaceRole.SecondaryAccent -> baseElevation + 6.dp
        SurfaceRole.InverseBackground -> baseElevation
        SurfaceRole.InverseAction -> baseElevation + 6.dp
    }
    val resolvedContentColor = contentColor ?: when (role) {
        SurfaceRole.PrimaryBackground -> scheme.onBackgroundPrimary
        SurfaceRole.SecondaryBackground -> scheme.onBackgroundSecondary
        SurfaceRole.PrimaryAccent -> scheme.onAccentPrimary
        SurfaceRole.SecondaryAccent -> scheme.onAccentSecondary
        SurfaceRole.InverseBackground -> scheme.onBackgroundInverse
        SurfaceRole.InverseAction -> scheme.onActionInverse
    }
    val ambient = when (role) {
        SurfaceRole.PrimaryAccent -> scheme.accentPrimary.copy(alpha = 0.42f)
        SurfaceRole.SecondaryAccent -> scheme.accentSecondary.copy(alpha = 0.42f)
        SurfaceRole.InverseAction -> scheme.actionInverse.copy(alpha = 0.42f)
        SurfaceRole.InverseBackground -> Color.Black.copy(alpha = 0.3f)
        else -> palette.onSurface.copy(alpha = 0.2f)
    }
    val spot = ambient.copy(alpha = (ambient.alpha + 0.07f).coerceAtMost(1f))
    val borderBrush = roleBorderBrush(style, role, isLight, scheme)

    // Translucent materials (Glass, Acrylic) must not use a drop shadow: it is drawn
    // behind the node and, because the panel is see-through, shows up as a dark inset
    // shape. Accent and secondary/inverse roles are filled, so they can cast one.
    val translucent = role == SurfaceRole.PrimaryBackground &&
            (style == SurfaceStyle.Glass || style == SurfaceStyle.Acrylic)

    CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
        Box(
            modifier = modifier
                .then(
                    if (!translucent) {
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
                    onDrawBehind { drawSurfaceMaterial(style, role, isLight, scheme) }
                }
                .border(BorderStroke(1.dp, borderBrush), shape)
                .padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * Paints the full-bleed material of [style] as a page background: the primary material
 * body plus its sheen and reflections, but no panel bevel (a backdrop has no edges).
 */
fun Modifier.baseSurfaceBackdrop(style: SurfaceStyle, isLight: Boolean): Modifier =
    drawWithCache {
        onDrawBehind {
            drawStyleBase(style, isLight)
            drawStyleDecorations(style, isLight)
        }
    }

/**
 * Overlays a [SurfaceStyle]'s material character (gloss, bubbles, facets, specular band,
 * grain) and its inner bevel on a node that already carries a base fill. Compact components
 * — buttons, toggles, chips — use this to share the exact look of a [BaseSurface] in [role]
 * without redrawing the fill (which comes from their own interaction-state brush).
 */
fun Modifier.baseSurfaceTexture(
    style: SurfaceStyle,
    role: SurfaceRole,
    isLight: Boolean,
): Modifier = drawWithCache {
    onDrawBehind {
        val effectiveLight = when (role) {
            SurfaceRole.InverseBackground, SurfaceRole.InverseAction -> !isLight
            else -> isLight
        }
        drawStyleDecorations(style, effectiveLight)
        drawInnerBevel(effectiveLight)
    }
}

/** Ambient-scheme overload of [baseSurfaceTexture] for a single role. */
@Composable
fun Modifier.baseSurfaceTexture(role: SurfaceRole): Modifier {
    val scheme = LocalBaseUIColors.current
    return baseSurfaceTexture(scheme.surfaceTheme, role, scheme.isLight)
}

/**
 * Paints the full [SurfaceStyle] material for [role] — base fill, material character and inner
 * bevel — using the ambient [BaseUIColorScheme]. Use it to make a chrome panel (a sidebar, a
 * player bar) a first-class surface rather than a flat Material surface.
 */
@Composable
fun Modifier.baseSurfaceMaterial(
    role: SurfaceRole = SurfaceRole.PrimaryBackground,
    style: SurfaceStyle = LocalBaseUIColors.current.surfaceTheme,
): Modifier {
    val scheme = LocalBaseUIColors.current
    return drawWithCache {
        onDrawBehind { drawSurfaceMaterial(style, role, scheme.isLight, scheme) }
    }
}

private fun DrawScope.drawSurfaceMaterial(
    style: SurfaceStyle,
    role: SurfaceRole,
    isLight: Boolean,
    scheme: BaseUIColorScheme,
) {
    // Role base fill.
    when (role) {
        SurfaceRole.PrimaryBackground -> drawStyleBase(style, isLight)

        // A distinct, complementary background hue (keeps the style's texture below).
        SurfaceRole.SecondaryBackground -> drawRect(color = scheme.backgroundSecondary)

        SurfaceRole.PrimaryAccent -> drawAccentBase(scheme.accentPrimary)
        SurfaceRole.SecondaryAccent -> drawAccentBase(scheme.accentSecondary)

        // Opaque inverted background so translucent styles still read as a solid surface.
        SurfaceRole.InverseBackground -> drawRect(color = scheme.backgroundInverse)

        SurfaceRole.InverseAction -> drawAccentBase(scheme.actionInverse)
    }

    // The material's own character (bubbles, facets, grain, specular band, sheen).
    val effectiveLight = when (role) {
        SurfaceRole.InverseBackground, SurfaceRole.InverseAction -> !isLight
        else -> isLight
    }
    drawStyleDecorations(style, effectiveLight)
    drawInnerBevel(effectiveLight)
}

// --- Material base gradients (one per style) ---------------------------------

private fun DrawScope.drawStyleBase(style: SurfaceStyle, isLight: Boolean) {
    when (style) {
        SurfaceStyle.Glass -> {
            val tint = if (isLight) Color(0xFFCFE0F2) else Color(0xFF9FB8D6)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = if (isLight) 0.4f else 0.2f),
                        tint.copy(alpha = if (isLight) 0.24f else 0.1f),
                    ),
                ),
            )
        }

        SurfaceStyle.Acrylic -> {
            val tint = if (isLight) Color(0xFFCFD9E8) else Color(0xFF8FA2B8)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = if (isLight) 0.45f else 0.22f),
                        tint.copy(alpha = if (isLight) 0.28f else 0.12f),
                    ),
                ),
            )
        }

        SurfaceStyle.Chrome -> {
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
            } else {
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
            }
        }

        SurfaceStyle.Icy -> {
            val colors = if (isLight) {
                listOf(Color(0xFFF4FCFF), Color(0xFFCBE9FB), Color(0xFF9BD3F0))
            } else {
                listOf(Color(0xFF2A3B4D), Color(0xFF1B2A39), Color(0xFF0F1A26))
            }
            drawRect(brush = Brush.verticalGradient(colors))
        }

        SurfaceStyle.Watery -> {
            // A clear-but-soft aqua column: unmistakably water, without a heavy flat blue.
            val colors = if (isLight) {
                listOf(Color(0xFFD8F0FD), Color(0xFFB0DEF7), Color(0xFF83C8EE))
            } else {
                listOf(Color(0xFF123249), Color(0xFF0B2135), Color(0xFF06141F))
            }
            drawRect(brush = Brush.verticalGradient(colors))
        }
    }
}

/** The accent-filled base used by [SurfaceRole.Action]. */
private fun DrawScope.drawAccentBase(accent: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                lerp(accent, Color.White, 0.45f),
                accent,
                lerp(accent, Color.Black, 0.16f),
            ),
        ),
    )
}

// --- Material decorations (one per style) ------------------------------------

private fun DrawScope.drawStyleDecorations(style: SurfaceStyle, isLight: Boolean) {
    when (style) {
        SurfaceStyle.Glass -> {
            drawGloss(if (isLight) 0.55f else 0.16f)
            drawBottomReflection(if (isLight) 0.4f else 0.1f)
            drawBubbles(seed = 11, count = 3, alpha = if (isLight) 0.4f else 0.14f)
        }

        SurfaceStyle.Acrylic -> {
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

        SurfaceStyle.Chrome -> {
            // Hard specular reflection near the top.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isLight) 0.85f else 0.18f),
                        Color.Transparent,
                    ),
                    startY = size.height * 0.05f,
                    endY = size.height * 0.22f,
                ),
            )
            // Darkened lower edge for the metal bevel.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                    startY = size.height * 0.78f,
                    endY = size.height,
                ),
            )
        }

        SurfaceStyle.Icy -> {
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

        SurfaceStyle.Watery -> {
            val sheen = if (isLight) Color.White else Color(0xFFAFD3F2)

            // Caustic light pooling from the surface.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        sheen.copy(alpha = if (isLight) 0.6f else 0.2f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
            )

            val random = Random(53)
            // Refracted caustic blobs — the bright, rippling patterns of moving water.
            repeat(9) {
                val rx = size.minDimension * (0.14f + random.nextFloat() * 0.26f)
                val cx = rx + random.nextFloat() * (size.width - 2 * rx).coerceAtLeast(0f)
                val cy = rx + random.nextFloat() * (size.height - 2 * rx).coerceAtLeast(0f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sheen.copy(alpha = if (isLight) 0.5f else 0.22f),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = rx,
                    ),
                    topLeft = Offset(cx - rx, cy - rx * 0.7f),
                    size = Size(rx * 2f, rx * 1.4f),
                )
            }

            // Ripple crests: thin horizontal highlights that read as surface waves.
            repeat(3) {
                val y = size.height * (0.18f + random.nextFloat() * 0.64f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            sheen.copy(alpha = if (isLight) 0.4f else 0.16f),
                            Color.Transparent,
                        ),
                        startY = y - size.height * 0.035f,
                        endY = y + size.height * 0.035f,
                    ),
                )
            }

            drawBubbles(seed = 71, count = 11, alpha = if (isLight) 0.6f else 0.3f, rim = true)
        }
    }
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

private fun DrawScope.drawBubbles(seed: Int, count: Int, alpha: Float, rim: Boolean = false) {
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
        if (rim) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = radius,
                center = center,
                style = Stroke(width = (radius * 0.14f).coerceAtLeast(0.8f)),
            )
        }
    }
}

private fun styleBorderBrush(style: SurfaceStyle): Brush = when (style) {
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

private fun roleBorderBrush(
    style: SurfaceStyle,
    role: SurfaceRole,
    isLight: Boolean,
    scheme: BaseUIColorScheme,
): Brush = when (role) {
    SurfaceRole.PrimaryBackground -> styleBorderBrush(style)

    SurfaceRole.SecondaryBackground -> Brush.verticalGradient(
        listOf(
            lerp(scheme.backgroundSecondary, Color.White, 0.5f)
                .copy(alpha = if (isLight) 0.85f else 0.6f),
            Color.White.copy(alpha = 0.12f),
        ),
    )

    SurfaceRole.PrimaryAccent -> accentEdgeBrush(scheme.accentPrimary)
    SurfaceRole.SecondaryAccent -> accentEdgeBrush(scheme.accentSecondary)

    SurfaceRole.InverseBackground -> Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f)),
    )

    SurfaceRole.InverseAction -> accentEdgeBrush(scheme.actionInverse)
}

private fun accentEdgeBrush(accent: Color): Brush = Brush.verticalGradient(
    listOf(lerp(accent, Color.White, 0.6f), lerp(accent, Color.Black, 0.2f)),
)

/**
 * One preview showing everything about surfaces: every [SurfaceStyle] crossed with every
 * [SurfaceRole], plus the full-bleed backdrops — no need to scroll through many previews.
 */
@BaseUIDesktopPreview
@Composable
private fun SurfacePreview() {
    BaseUIPreview {
        val colors = LocalBaseUIColors.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SurfaceStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SurfaceRole.entries.forEach { role ->
                        BaseSurface(
                            style = style,
                            role = role,
                            modifier = Modifier
                                .weight(1f)
                                .height(84.dp),
                            contentPadding = PaddingValues(10.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = style.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = role.name,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            // Full-bleed backdrops (the page background material, without the panel bevel).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SurfaceStyle.entries.forEach { style ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .baseSurfaceBackdrop(style, colors.isLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = style.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}
