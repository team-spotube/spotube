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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krtirtho.spotube.resources.iconsax.ArrowLeft3
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.Iconsax3DotsMore
import dev.krtirtho.spotube.resources.iconsax.IconsaxAddSquare
import dev.krtirtho.spotube.resources.iconsax.IconsaxArrowSquareUp
import dev.krtirtho.spotube.resources.iconsax.IconsaxDocumentText
import dev.krtirtho.spotube.resources.iconsax.IconsaxHeart
import dev.krtirtho.spotube.resources.iconsax.IconsaxMagic
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import dev.krtirtho.spotube.resources.iconsax.IconsaxShare
import dev.krtirtho.spotube.resources.iconsax.User

private val BadgeShape = RoundedCornerShape(11.dp)
private const val DisabledContentAlpha = 0.38f

private val ButtonMinHeight = 40.dp
private val SquareButtonSize = 40.dp

private data class ResolvedButtonState(
    val colors: BaseUITheme.ButtonColors,
    val shape: Shape,
    val shadow: BaseUITheme.Shadow,
    val border: BaseUITheme.Border,
    val padding: PaddingValues,
)

@Composable
private fun resolveButtonState(
    style: BaseUITheme.ButtonStyle,
    isPressed: Boolean,
    isHovered: Boolean,
): ResolvedButtonState {
    return when {
        isPressed -> ResolvedButtonState(
            style.colors.pressed,
            style.shape.pressed,
            style.shadow.pressed,
            style.border.pressed,
            style.padding.pressed
        )

        isHovered -> ResolvedButtonState(
            style.colors.hovered,
            style.shape.hovered,
            style.shadow.hovered,
            style.border.hovered,
            style.padding.hovered
        )

        else -> ResolvedButtonState(
            style.colors.normal,
            style.shape.normal,
            style.shadow.normal,
            style.border.normal,
            style.padding.normal
        )
    }
}

private fun Modifier.applyShadow(
    shadow: BaseUITheme.Shadow,
    shape: Shape,
): Modifier {
    return if (shadow.elevation > 0.dp) {
        this.shadow(
            elevation = shadow.elevation,
            shape = shape,
            ambientColor = shadow.ambientColor,
            spotColor = shadow.spotColor,
        )
    } else {
        this
    }
}

/**
 * Overlays the ambient [SurfaceStyle] material for a colour's [BaseUITheme.ButtonColors.surfaceRole],
 * so an actionable item looks exactly like the matching [BaseSurface] role.
 */
@Composable
internal fun Modifier.applySurfaceMaterial(colors: BaseUITheme.ButtonColors): Modifier =
    applySurfaceMaterial(colors.surfaceRole)

@Composable
internal fun Modifier.applySurfaceMaterial(role: SurfaceRole?): Modifier {
    val scheme = LocalBaseUIColors.current
    return role?.let { baseSurfaceTexture(scheme.surfaceTheme, it, scheme.isLight) } ?: this
}

/**
 * The generic button gloss. Textured items get their sheen from the surface material, so it
 * is only applied to colours without a [BaseUITheme.ButtonColors.surfaceRole].
 */
internal fun Modifier.applyHighlight(colors: BaseUITheme.ButtonColors): Modifier =
    if (colors.surfaceRole == null) highlight(colors.highlight) else this

@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hoverOnly: Boolean = false,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveButtonState(style, isPressed, isHovered)
    val lift = if (isHovered && !isPressed) (-1).dp else 0.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer { translationY = lift.toPx() }
            .then(
                if (hoverOnly && !isHovered) Modifier else Modifier.applyShadow(
                    state.shadow,
                    state.shape
                )
            )
            .clip(state.shape)
            .then(
                if (hoverOnly && !isHovered) Modifier else Modifier.background(
                    state.colors.background,
                    state.shape
                )
                    .applySurfaceMaterial(state.colors)
                    .border(BorderStroke(state.border.width, state.border.color), state.shape)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .then(if (hoverOnly && !isHovered) Modifier else Modifier.applyHighlight(state.colors))
            .padding(state.padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.alpha(if (enabled) 1f else DisabledContentAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                CompositionLocalProvider(LocalContentColor provides state.colors.foreground) {
                    content()
                }
            },
        )
    }
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveButtonState(style, isPressed, isHovered)
    val lift = if (isHovered && !isPressed) (-1).dp else 0.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer { translationY = lift.toPx() }
            .applyShadow(state.shadow, state.shape)
            .clip(state.shape)
            .background(state.colors.background, state.shape)
            .applySurfaceMaterial(state.colors)
            .border(BorderStroke(state.border.width, state.border.color), state.shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .applyHighlight(state.colors)
            .padding(state.padding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides state.colors.foreground) {
            Row(
                modifier = Modifier.alpha(if (enabled) 1f else DisabledContentAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.secondary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveButtonState(style, isPressed, isHovered)
    val lift = if (isHovered && !isPressed) (-1).dp else 0.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer { translationY = lift.toPx() }
            .applyShadow(state.shadow, state.shape)
            .clip(state.shape)
            .background(state.colors.background, state.shape)
            .applySurfaceMaterial(state.colors)
            .border(BorderStroke(state.border.width, state.border.color), state.shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .applyHighlight(state.colors)
            .padding(state.padding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides state.colors.foreground) {
            Row(
                modifier = Modifier.alpha(if (enabled) 1f else DisabledContentAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.iconButtons.outline
    OutlineButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        theme = style,
    ) {
        content()
    }
}

@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.ghost
    OutlineButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        hoverOnly = true,
        theme = style,
        content = content,
    )
}

@Composable
fun GhostIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.iconButtons.ghost
    OutlineButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        hoverOnly = true,
        theme = style,
    ) {
        content()
    }
}

@Composable
fun PrimaryIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.iconButtons.primary
    PrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        theme = style,
    ) {
        content()
    }
}

@Composable
fun SecondaryIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.iconButtons.secondary
    SecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        theme = style,
    ) {
        content()
    }
}

@Composable
fun ButtonBadge(
    count: Int,
    modifier: Modifier = Modifier,
    theme: BaseUITheme.ButtonStyle? = null,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val state = resolveButtonState(style, isPressed = false, isHovered = false)
    Box(
        modifier = modifier
            .heightIn(min = 22.dp)
            .defaultMinSize(minWidth = 22.dp)
            .background(state.colors.background, BadgeShape)
            .border(1.dp, state.border.color, BadgeShape)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            color = state.colors.foreground.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun GroupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveButtonState(style, isPressed, isHovered)
    val overlay = when {
        isPressed -> state.colors.background
        isHovered -> state.colors.background
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val lift = if (isHovered && !isPressed) (-1).dp else 0.dp

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .graphicsLayer { translationY = lift.toPx() }
            .background(overlay)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(state.padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun GroupIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val state = resolveButtonState(style, isPressed, isHovered)
    val overlay = when {
        isPressed -> state.colors.background
        isHovered -> state.colors.background
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val lift = if (isHovered && !isPressed) (-1).dp else 0.dp

    Box(
        modifier = modifier
            .size(SquareButtonSize)
            .graphicsLayer { translationY = lift.toPx() }
            .background(overlay)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else DisabledContentAlpha),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun ButtonGroup(
    modifier: Modifier = Modifier,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val state = resolveButtonState(style, isPressed = false, isHovered = false)
    val groupShape = state.shape
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .applyShadow(state.shadow, groupShape),
        shape = groupShape,
        color = Color.Transparent,
        border = BorderStroke(state.border.width, state.border.color),
    ) {
        Box(
            modifier = Modifier.background(state.colors.background, groupShape),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    }
}

@Composable
fun ButtonGroupDivider(
    theme: BaseUITheme.ButtonStyle? = null,
) {
    val baseTheme = LocalBaseUITheme.current
    val style = theme ?: baseTheme.buttons.outline
    val state = resolveButtonState(style, isPressed = false, isHovered = false)
    Box(
        modifier = Modifier
            .width(1.dp)
            .heightIn(min = 20.dp)
            .background(state.border.color),
    )
}

@BaseUIPhonePreview
@BaseUIDesktopPreview
@Composable
private fun ButtonsPreview() {
    BaseUIPreview {
        var liked by remember { mutableStateOf(false) }
        var likeCount by remember { mutableIntStateOf(128) }
        var following by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxShare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Share", maxLines = 1, softWrap = false)
                }
                SecondaryButton(onClick = {}) {
                    Text("Secondary", maxLines = 1, softWrap = false)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxAddSquare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Sign up",
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
                GhostButton(onClick = {}) {
                    Text("Ghost", maxLines = 1, softWrap = false)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineButton(onClick = {}, enabled = false) {
                    Text("Disabled", maxLines = 1, softWrap = false)
                }
                SecondaryButton(onClick = {}, enabled = false) {
                    Text("Disabled", maxLines = 1, softWrap = false)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(onClick = {}, enabled = false) {
                    Text("Disabled", maxLines = 1, softWrap = false)
                }
                OutlineButton(onClick = {}) {
                    Text("Forward", maxLines = 1, softWrap = false)
                    Icon(
                        imageVector = Iconsax.IconsaxArrowSquareUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineButton(
                    onClick = {
                        liked = !liked
                        likeCount += if (liked) 1 else -1
                    },
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxHeart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(if (liked) "Liked" else "Like", maxLines = 1, softWrap = false)
                    ButtonBadge(count = likeCount)
                }
                PrimaryButton(onClick = { following = !following }) {
                    Text(
                        if (following) "Following" else "Follow",
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@BaseUIPhonePreview
@Composable
private fun IconButtonsPreview() {
    BaseUIPreview {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Icon buttons",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxMagic,
                        contentDescription = "Magic",
                        modifier = Modifier.size(18.dp),
                    )
                }
                GhostIconButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxHeart,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(18.dp),
                    )
                }
                PrimaryIconButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxAddSquare,
                        contentDescription = "Add",
                        modifier = Modifier.size(18.dp),
                    )
                }
                SecondaryIconButton(onClick = {}) {
                    Icon(
                        imageVector = Iconsax.IconsaxShare,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                text = "Variable icon button",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VariableIconButton(
                    variant = VariableIconButtonVariant.Outline,
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxDocumentText,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                VariableIconButton(
                    variant = VariableIconButtonVariant.Ghost,
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Iconsax.Iconsax3DotsMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                VariableIconButton(
                    variant = VariableIconButtonVariant.Primary,
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Iconsax.IconsaxMagic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                VariableIconButton(
                    variant = VariableIconButtonVariant.Secondary,
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Iconsax.User,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@BaseUIDesktopPreview
@Composable
private fun ButtonGroupPreview() {
    BaseUIPreview {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Button groups",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ButtonGroup {
                    GroupButton(onClick = {}) {
                        Icon(
                            imageVector = Iconsax.IconsaxDocumentText,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Documents", maxLines = 1, softWrap = false)
                    }
                    ButtonGroupDivider()
                    GroupButton(onClick = {}) {
                        Icon(
                            imageVector = Iconsax.IconsaxShare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Export", maxLines = 1, softWrap = false)
                    }
                    ButtonGroupDivider()
                    GroupIconButton(onClick = {}) {
                        Icon(
                            imageVector = Iconsax.Iconsax3DotsMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                ButtonGroup {
                    GroupIconButton(onClick = {}) {
                        Icon(
                            imageVector = Iconsax.ArrowLeft3,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    ButtonGroupDivider()
                    GroupIconButton(onClick = {}) {
                        Icon(
                            imageVector = Iconsax.IconsaxNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                ButtonBadge(count = 8)
            }
        }
    }
}

enum class VariableIconButtonVariant {
    Outline,
    Ghost,
    Primary,
    Secondary,
}

@Composable
fun VariableIconButton(
    variant: VariableIconButtonVariant = VariableIconButtonVariant.Outline,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    theme: BaseUITheme.ButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    when (variant) {
        VariableIconButtonVariant.Outline -> IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            theme = theme,
            content = content,
        )

        VariableIconButtonVariant.Ghost -> GhostIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            theme = theme,
            content = content,
        )

        VariableIconButtonVariant.Primary -> PrimaryIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            theme = theme,
            content = content,
        )

        VariableIconButtonVariant.Secondary -> SecondaryIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            theme = theme,
            content = content,
        )
    }
}
