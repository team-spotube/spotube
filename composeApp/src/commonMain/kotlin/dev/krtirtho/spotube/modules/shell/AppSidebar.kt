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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import dev.krtirtho.spotube.NavigationItem
import dev.krtirtho.spotube.core.navigation.NavigationState
import dev.krtirtho.spotube.core.navigation.Navigator
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.core.ui.base.GhostIconButton
import dev.krtirtho.spotube.core.ui.base.LocalBaseUITheme
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.base.SecondaryButton
import dev.krtirtho.spotube.core.ui.base.copyPadding
import dev.krtirtho.spotube.core.ui.base.copyShape
import dev.krtirtho.spotube.core.ui.component.VerticalScrollbar
import dev.krtirtho.spotube.modules.downloads.DownloadBadgeIndicator
import dev.krtirtho.spotube.modules.library.LibraryState
import dev.krtirtho.spotube.modules.library.LibraryTab
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxSetting2
import dev.krtirtho.spotube.resources.iconsax.IconsaxSidebarLeftBroken
import dev.krtirtho.spotube.resources.iconsax.IconsaxSidebarRightBroken
import dev.krtirtho.spotube.sidebarTabs
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
    val listState = rememberLazyListState()

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

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = listState) {
                    items(sidebarTabs.size) { index ->
                        when (val tab = sidebarTabs[index]) {
                            is NavigationItem.Group<*> -> {
                                androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                                    Text(
                                        text = tab.title.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 8.dp)
                                    )
                                }
                                tab.items.forEach {
                                    val selected = navigationState.topLevelRoute == it.route
                                    SidebarItem(
                                        label = it.title,
                                        activeIcon = it.icon,
                                        onClick = {
                                            if (it.route == Routes.Library) {
                                                libraryState.currentTab.value =
                                                    it.data as LibraryTab
                                            }
                                            navigator.navigate(it.route)
                                        },
                                        selected = if (it.route == Routes.Library) selected && currentLibraryTab == it.data else selected,
                                        expanded = expanded,
                                        showDownloadBadge = it.route == Routes.Library
                                                && it.data == LibraryTab.Downloads
                                    )
                                }

                            }

                            is NavigationItem.Tab<*> -> {
                                val selected = navigationState.topLevelRoute == tab.route
                                SidebarItem(
                                    label = tab.title,
                                    activeIcon = tab.icon,
                                    onClick = {
                                        navigator.navigate(tab.route)
                                    },
                                    selected = selected,
                                    expanded = expanded,
                                )
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
        HorizontalDivider()
        SidebarItem(
            label = "Settings",
            activeIcon = Iconsax.IconsaxSetting2,
            onClick = {
                navigator.navigate(Routes.Settings)
            },
            selected = navigationState.topLevelRoute == Routes.Settings,
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
        Box(modifier = Modifier.size(16.dp)) {
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
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
        .padding(horizontal = 7.dp, vertical = 2.dp)
    val contentPadding = PaddingValues(horizontal = 5.dp)

    if (selected) {
        SecondaryButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.secondary
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(5.dp)),
            content = itemContent,
        )
    } else {
        OutlineButton(
            onClick = onClick,
            modifier = buttonModifier,
            theme = LocalBaseUITheme.current.buttons.outline
                .copyPadding(contentPadding)
                .copyShape(RoundedCornerShape(5.dp)),
            hoverOnly = true,
            content = itemContent,
        )
    }
}