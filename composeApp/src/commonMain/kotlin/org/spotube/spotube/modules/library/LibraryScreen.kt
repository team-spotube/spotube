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

package org.spotube.spotube.modules.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import com.composeunstyled.currentWindowWidthBreakpoint
import org.spotube.spotube.getPlatform
import org.spotube.spotube.isDesktop
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.core.ui.theming.AppIcon
import org.spotube.spotube.modules.downloads.DownloadsScreen
import org.spotube.spotube.modules.library.album.LibraryAlbumsScreen
import org.spotube.spotube.modules.library.artist.LibraryArtistsScreen
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksScreen
import org.spotube.spotube.modules.library.local_tracks.LibraryLocalTracksViewModel
import org.spotube.spotube.modules.library.local_tracks.BrowserFileActionButtons
import org.spotube.spotube.modules.library.local_tracks.BrowserFileActionLabel
import org.spotube.spotube.modules.library.playlist.LibraryPlaylistsScreen
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxFilterSearch
import org.spotube.spotube.resources.iconsax.IconsaxGlobe
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.IconsaxRefreshRight
import org.spotube.spotube.resources.iconsax.IconsaxSearchBroken
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.adaptive.WindowBreakpoints
import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumDropdownMenu
import org.spotube.millennium.composables.MillenniumDropdownMenuItem
import org.spotube.millennium.composables.MillenniumIconButton
import org.spotube.millennium.composables.MillenniumIconButtonStyle
import org.spotube.millennium.composables.MillenniumTextField
import compose.icons.FeatherIcons
import compose.icons.feathericons.Grid
import compose.icons.feathericons.List
import spotube.composeapp.generated.resources.*

@Composable
fun LibraryScreen(libraryState: LibraryState = koinInject()) {
    val state by libraryState.uiState.collectAsStateWithLifecycle()
    val expandedDesktop = getPlatform().isDesktop() &&
            (currentWindowWidthBreakpoint() isAtLeast WindowBreakpoints.Expanded)
    val isCompact = currentWindowWidthBreakpoint() isBelow WindowBreakpoints.Expanded
    val compactDesktop = getPlatform().isDesktop() && isCompact
    val viewMode = state.viewModeOverride ?: defaultLibraryViewMode(isCompact)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    if (state.currentTab == LibraryTab.Downloads && !isCompact) {
        DownloadsScreen(
            headerInAppBar = true,
            topContent = { if (!expandedDesktop) LibraryTabs(state, libraryState::onTabSelected) }
        )
        return
    }

    val localTracksViewModel = if (state.currentTab == LibraryTab.LocalTracks) koinViewModel<LibraryLocalTracksViewModel>() else null
    val fileActionsInAppBar = !isCompact && localTracksViewModel != null
    val contextualFileActions = if (fileActionsInAppBar) {
        val localState by requireNotNull(localTracksViewModel).state.collectAsStateWithLifecycle()
        localState.takeIf { it.hasFileActions }
    } else null

    Column(Modifier.fillMaxSize()) {
        ApplicationMainBar(
            backButton = false,
            title = {
                if (contextualFileActions != null && (expandedDesktop || !state.searchMode)) {
                    BrowserFileActionLabel(contextualFileActions)
                } else LibraryToolbarTitle(state, expandedDesktop, compactDesktop)
            },
            actions = {
                if (contextualFileActions != null) {
                    BrowserFileActionButtons(contextualFileActions, requireNotNull(localTracksViewModel), inAppBar = true)
                }
                LibraryToolbarActions(
                    state = state,
                    expandedDesktop = expandedDesktop,
                    onOpenSearch = libraryState::openSearch,
                    onCloseSearch = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        libraryState.closeSearch()
                    },
                    onCreatePlaylist = libraryState::showCreatePlaylist,
                    onFilterSource = { libraryState.setSourceFilter(state.currentTab, it) },
                    onRefresh = libraryState::refreshLocalTracks,
                    onToggleView = { libraryState.toggleViewMode(isCompact) },
                    viewMode = viewMode,
                    compactDesktop = compactDesktop,
                    expandedSearchWidth = if (contextualFileActions == null) 300.dp else 160.dp
                )
            }
        )
        if (!expandedDesktop) {
            LibraryTabs(state, libraryState::onTabSelected)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            when (state.currentTab) {
                // Not guarded: on-device playlists/albums/artists work without a plugin; the
                // screens show an inline sign-in hint when online content is unavailable.
                LibraryTab.Playlists -> LibraryPlaylistsScreen(
                    showCreatePlaylist = state.showCreatePlaylist,
                    viewMode = viewMode
                )

                LibraryTab.Albums -> LibraryAlbumsScreen(viewMode = viewMode)
                LibraryTab.Artists -> LibraryArtistsScreen(viewMode = viewMode)
                LibraryTab.LocalTracks -> LibraryLocalTracksScreen(
                    viewModel = requireNotNull(localTracksViewModel), fileActionsInAppBar = fileActionsInAppBar
                )
                LibraryTab.Downloads -> DownloadsScreen()
            }
        }
    }
}

@Composable
private fun LibraryTabs(state: LibraryToolbarUiState, onTabSelected: (LibraryTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LibraryTab.entries) { tab ->
            MillenniumButton(
                onClick = { onTabSelected(tab) },
                style = if (state.currentTab == tab) MillenniumButtonStyle.Primary else MillenniumButtonStyle.Secondary
            ) { Text(stringResource(tab.titleResource)) }
        }
    }
}

@Composable
internal fun LibraryToolbarTitle(
    state: LibraryToolbarUiState,
    expandedDesktop: Boolean,
    compactDesktop: Boolean = false
) {
    if (!expandedDesktop && state.searchMode) {
        LibraryFilterField(
            state.searchState,
            state.currentTab.filterPlaceholder,
            Modifier.fillMaxWidth(),
            autoFocus = true
        )
    } else if (!expandedDesktop) {
        Text(
            stringResource(if (compactDesktop) Res.string.library_toolbar_compact_title else Res.string.library_screen_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RowScope.LibraryToolbarActions(
    state: LibraryToolbarUiState,
    expandedDesktop: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onFilterSource: (LibrarySourceFilter) -> Unit = {},
    onToggleView: () -> Unit = {},
    viewMode: LibraryViewMode = state.viewModeOverride ?: defaultLibraryViewMode(!expandedDesktop),
    compactDesktop: Boolean = false,
    expandedSearchWidth: Dp = 300.dp
) {
    val actionModifier = Modifier.size(if (compactDesktop) 32.dp else 40.dp)
    if (!expandedDesktop && state.searchMode) {
        MillenniumIconButton(
            icon = Iconsax.IconsaxCloseSquare,
            contentDescription = stringResource(Res.string.library_action_close_search),
            onClick = onCloseSearch,
            modifier = actionModifier,
            style = MillenniumIconButtonStyle.Quiet
        )
    } else {
        if (state.canFilter) {
            if (expandedDesktop) {
                LibraryFilterField(
                    state.searchState,
                    state.currentTab.filterPlaceholder,
                    Modifier.width(expandedSearchWidth)
                )
            } else {
                MillenniumIconButton(
                    icon = Iconsax.IconsaxSearchBroken,
                    contentDescription = stringResource(Res.string.library_action_search),
                    onClick = onOpenSearch,
                    modifier = actionModifier,
                    style = MillenniumIconButtonStyle.Quiet
                )
            }
        }
        if (state.canFilterSource) {
            LibrarySourceFilterMenu(state, onFilterSource, actionModifier, expandedDesktop)
        }
        if (state.canCreatePlaylist) {
            MillenniumIconButton(
                icon = Iconsax.IconsaxAddSquare,
                contentDescription = stringResource(Res.string.library_action_create_playlist),
                onClick = onCreatePlaylist,
                modifier = actionModifier,
                style = MillenniumIconButtonStyle.Quiet
            )
        }
        if (state.canRefresh) {
            MillenniumIconButton(
                icon = Iconsax.IconsaxRefreshRight,
                contentDescription = stringResource(Res.string.library_action_refresh_local_tracks),
                onClick = onRefresh,
                loading = state.isRefreshing,
                modifier = actionModifier,
                style = MillenniumIconButtonStyle.Quiet
            )
        }
        if (state.canChangeView) {
            MillenniumIconButton(
                icon = if (viewMode == LibraryViewMode.List) FeatherIcons.Grid else FeatherIcons.List,
                contentDescription = stringResource(if (viewMode == LibraryViewMode.List) Res.string.library_action_grid_view else Res.string.library_action_list_view),
                onClick = onToggleView,
                modifier = actionModifier,
                style = MillenniumIconButtonStyle.Quiet
            )
        }
    }
}

@Composable
private fun LibrarySourceFilterMenu(
    state: LibraryToolbarUiState,
    onFilterSource: (LibrarySourceFilter) -> Unit,
    actionModifier: Modifier,
    expandedDesktop: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val active = state.sourceFilter != LibrarySourceFilter.All
    MillenniumDropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        anchor = {
            if (expandedDesktop) {
                MillenniumButton(
                    onClick = { expanded = true },
                    modifier = Modifier.padding(vertical = 4.dp),
                    minSize = DpSize(0.dp, 36.dp),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    style = if (active) MillenniumButtonStyle.Primary else MillenniumButtonStyle.Secondary
                ) {
                    AppIcon(Iconsax.IconsaxFilterSearch, null, modifier = Modifier.size(16.dp))
                    Text(stringResource(state.sourceFilter.labelResource))
                }
            } else {
                MillenniumIconButton(
                    icon = Iconsax.IconsaxFilterSearch,
                    contentDescription = stringResource(Res.string.library_filter_source_content_description),
                    onClick = { expanded = true },
                    modifier = actionModifier,
                    style = if (active) MillenniumIconButtonStyle.Primary else MillenniumIconButtonStyle.Quiet
                )
            }
        }
    ) {
        LibrarySourceFilter.entries.forEach { filter ->
            MillenniumDropdownMenuItem(
                selected = state.sourceFilter == filter,
                onClick = { onFilterSource(filter) },
                leading = {
                    AppIcon(
                        when (filter) {
                            LibrarySourceFilter.All -> Iconsax.IconsaxMusic
                            LibrarySourceFilter.Online -> Iconsax.IconsaxGlobe
                            LibrarySourceFilter.Local -> Iconsax.IconsaxMusicLibrary
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(filter.labelResource)) }
            )
        }
    }
}

@Composable
private fun LibraryFilterField(
    state: TextFieldState,
    placeholder: StringResource,
    modifier: Modifier,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    MillenniumTextField(
        state = state,
        placeholder = stringResource(placeholder),
        minimumHeight = 36.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier.padding(vertical = 4.dp).focusRequester(focusRequester)
    )
}
