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

package dev.krtirtho.spotube.modules.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.krtirtho.spotube.core.navigation.NavigationState
import dev.krtirtho.spotube.core.navigation.Navigator
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.copyPadding
import dev.krtirtho.spotube.core.ui.base.copyShape
import dev.krtirtho.spotube.modules.downloads.DownloadBadgeIndicator
import dev.krtirtho.spotube.modules.library.LibraryState
import dev.krtirtho.spotube.modules.library.LibraryTab
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxMirroringScreen
import dev.krtirtho.spotube.resources.iconsax.IconsaxSidebarLeftBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxSidebarRightBroken
import dev.krtirtho.spotube.resources.iconsax.User
import dev.krtirtho.spotube.tabs
import org.jetbrains.compose.resources.Font
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.cookie_regular

@Composable
fun AppSidebar(
    hazeState: HazeState,
    navigator: Navigator,
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    libraryState: LibraryState = koinInject()
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val width by animateDpAsState(targetValue = if (expanded) 236.dp else 86.dp)
    val currentLibraryTab by libraryState.currentTab.collectAsState()
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .hazeEffect(hazeState) {
                blurEffect {
                    blurRadius = 20.dp
                    colorEffects = listOf(
                        HazeColorEffect.tint(surfaceTint)
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = "Spotube",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily(
                            listOf(
                                Font(Res.font.cookie_regular, weight = FontWeight.Normal)
                            )
                        )
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            GhostIconButton(
                onClick = { expanded = !expanded }
            ) {
                Icon(
                    imageVector = if (expanded) Iconsax.IconsaxSidebarLeftBroken else Iconsax.IconsaxSidebarRightBroken,
                    contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        tabs.forEach { (label, icon, activeIcon, screen) ->
            val selected = navigationState.topLevelRoute == screen

            if (screen == Routes.Library) {
                AnimatedVisibility(visible = expanded) {
                    Text(
                        text = "LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                LibraryTab.entries.forEach { tab ->
                    val isSubSelected = selected && currentLibraryTab == tab

                    SidebarItem(
                        label = tab.title,
                        activeIcon = tab.icon,
                        onClick = {
                            libraryState.currentTab.value = tab
                            navigator.navigate(screen)
                        },
                        selected = isSubSelected,
                        expanded = expanded,
                        showDownloadBadge = tab == LibraryTab.Downloads,
                    )
                }
            } else {
                SidebarItem(
                    label = label,
                    activeIcon = activeIcon,
                    onClick = { navigator.navigate(screen) },
                    selected = selected,
                    expanded = expanded,
                )
            }
        }
        }

        SidebarItem(
            label = "Devices",
            activeIcon = Iconsax.IconsaxMirroringScreen,
            onClick = { navigator.navigate(Routes.Devices) },
            selected = false,
            expanded = expanded,
        )
        SidebarItem(
            label = "Group Jam",
            activeIcon = Iconsax.User,
            onClick = { navigator.navigate(Routes.Jam) },
            selected = false,
            expanded = expanded,
        )

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun SidebarItem(
    label: String,
    activeIcon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    showDownloadBadge: Boolean = false,
) {
    val itemContent: @Composable RowScope.() -> Unit = {
        Box(modifier = Modifier.size(20.dp)) {
            Icon(
                imageVector = activeIcon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (showDownloadBadge) {
                DownloadBadgeIndicator(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }
        Spacer(modifier = if (expanded) Modifier.weight(1f) else Modifier)
    }

    val buttonModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp)
    val contentPadding = PaddingValues(horizontal = 7.dp, vertical = 6.dp)

    if (selected) {
        SecondaryButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.secondary
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(8.dp)),
            content = itemContent,
        )
    } else {
        OutlineButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.outline
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(8.dp)),
            hoverOnly = true,
            content = itemContent,
        )
    }
}