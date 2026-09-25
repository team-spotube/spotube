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

package dev.krtirtho.spotube.modules.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import dev.krtirtho.spotube.PlatformType
import dev.krtirtho.spotube.core.ui.base.ChipTab
import dev.krtirtho.spotube.core.ui.component.AlbumCard
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.core.ui.component.ArtistCard
import dev.krtirtho.spotube.core.ui.component.ErrorDisplay
import dev.krtirtho.spotube.core.ui.component.PlaylistCard
import dev.krtirtho.spotube.core.ui.component.TrackCard
import dev.krtirtho.spotube.core.ui.component.UserCard
import dev.krtirtho.spotube.core.ui.component.FeaturedCarousel
import dev.krtirtho.spotube.core.ui.component.VerticalScrollbar
import dev.krtirtho.spotube.core.ui.component.cards.PlayableCard
import dev.krtirtho.spotube.core.ui.component.dragScrollable
import dev.krtirtho.spotube.core.ui.misc.SkeletonTree
import dev.krtirtho.spotube.core.navigation.NavigationCommands
import dev.krtirtho.spotube.core.navigation.Routes
import dev.krtirtho.spotube.getPlatform
import dev.krtirtho.spotube.modules.shell.LocalAppShellBottomInset
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxMirroringScreen
import dev.krtirtho.spotube.resources.iconsax.User
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeScreenViewModel) {
    val navigationCommands = koinInject<NavigationCommands>()
    val platform = getPlatform()
    val isDesktop = platform.type == PlatformType.Windows ||
            platform.type == PlatformType.Linux ||
            platform.type == PlatformType.MacOS

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val selectedGenreId = when (val s = state) {
        is HomeScreenState.Data -> s.selectedGenreId
        else -> null
    }

    LaunchedEffect(selectedGenreId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItems = layoutInfo.totalItemsCount
                Pair(lastVisibleIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleIndex, totalItems) ->
                if (
                    lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 2
                ) {
                    viewModel.loadMoreData()
                }
            }
    }

    Scaffold(
        topBar = {
            ApplicationMainBar(
                backButton = false,
                title = {
                    Text("Browse")
                },
                actions = {
                    if (!isDesktop) {
                        IconButton(onClick = { navigationCommands.navigateTo(Routes.Devices) }) {
                            Icon(
                                imageVector = Iconsax.IconsaxMirroringScreen,
                                contentDescription = "Devices",
                            )
                        }
                        IconButton(onClick = { navigationCommands.navigateTo(Routes.Jam) }) {
                            Icon(
                                imageVector = Iconsax.User,
                                contentDescription = "Group Jam",
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isDesktop) {
                HomeContent(
                    listState = listState,
                    state = state,
                    onRetry = { viewModel.refresh() },
                    onGenreSelected = { viewModel.selectGenre(it) },
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = state is HomeScreenState.Loading,
                    onRefresh = {
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    HomeContent(
                        listState = listState,
                        state = state,
                        onRetry = { viewModel.refresh() },
                        onGenreSelected = { viewModel.selectGenre(it) },
                    )
                }
            }

            VerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun HomeContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    state: HomeScreenState,
    onRetry: () -> Unit,
    onGenreSelected: (String) -> Unit,
) {
    val shellBottomInset = LocalAppShellBottomInset.current
    val contentPadding = remember(shellBottomInset) {
        PaddingValues(top = 16.dp, bottom = 16.dp + shellBottomInset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        when (state) {
            is HomeScreenState.Loading -> {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        repeat(3) {
                            SkeletonTree(true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .padding(horizontal = 16.dp),
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                    ) {
                                        items(4) {
                                            PlayableCard(
                                                title = "Item Title",
                                                subtitle = "Subtitle",
                                                imageURL = "https://placehold.co/600x400",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is HomeScreenState.Error -> {
                item {
                    ErrorDisplay(
                        errorMessage = state.message,
                        onRetry = onRetry,
                    )
                }
            }

            is HomeScreenState.Data -> {
                if (state.featuredItems.isNotEmpty()) {
                    item {
                        FeaturedCarousel(items = state.featuredItems)
                    }
                }

                if (state.genres.isNotEmpty()) {
                    item {
                        GenreTabs(
                            genres = state.genres,
                            selectedGenreId = state.selectedGenreId,
                            onGenreSelected = onGenreSelected,
                        )
                    }
                }

                val currentSections = state.selectedGenreId?.let { state.browseSections[it] }

                if (currentSections == null) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400",
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(currentSections) { section ->
                        HomeSection(
                            title = section.title,
                            subtitle = section.description,
                            items = section.items,
                        )
                    }
                }

                if (state is HomeScreenState.Data.LoadingMore) {
                    item {
                        SkeletonTree(true) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(4) {
                                        PlayableCard(
                                            title = "Item Title",
                                            subtitle = "Subtitle",
                                            imageURL = "https://placehold.co/600x400",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    items: List<MetadataBrowseItem>,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!subtitle.isNullOrEmpty()) Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-12).dp),
        )

        LazyRow(
            state = rowState,
            modifier = Modifier.dragScrollable(rowState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items(items) { browseItem ->
                when (browseItem) {
                    is MetadataBrowseItem.Album -> AlbumCard(album = browseItem.data)
                    is MetadataBrowseItem.Artist -> ArtistCard(artist = browseItem.data)
                    is MetadataBrowseItem.Playlist -> PlaylistCard(playlist = browseItem.data)
                    is MetadataBrowseItem.Track -> TrackCard(track = browseItem.data)
                    is MetadataBrowseItem.User -> UserCard(user = browseItem.data)
                }
            }
        }
    }
}

@Composable
private fun GenreTabs(
    genres: List<MetadataBrowseGenre>,
    selectedGenreId: String?,
    onGenreSelected: (String) -> Unit,
) {
    val rowState = rememberLazyListState()

    LazyRow(
        state = rowState,
        modifier = Modifier.dragScrollable(rowState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(genres) { genre ->
            ChipTab(
                text = genre.name,
                selected = selectedGenreId == genre.id,
                onClick = { onGenreSelected(genre.id) },
            )
        }
    }
}
