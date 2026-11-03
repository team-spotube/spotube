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

package org.spotube.spotube.modules.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.modules.plugin.PluginManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.spotube.spotube.core.i18n.getString
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.common_unknown_error

sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    sealed interface Data : HomeScreenState {
        val featuredItems: List<MetadataBrowseItem>
        val genres: List<MetadataBrowseGenre>
        val selectedGenreId: String?
        val browseSections: Map<String, List<MetadataBrowseSection>?>
        val paginationStrategies: Map<String, PaginationStrategy?>

        data class Loaded(
            override val featuredItems: List<MetadataBrowseItem>,
            override val genres: List<MetadataBrowseGenre>,
            override val selectedGenreId: String?,
            override val browseSections: Map<String, List<MetadataBrowseSection>?>,
            override val paginationStrategies: Map<String, PaginationStrategy?>) : Data {
            fun toLoadingMore(): LoadingMore = LoadingMore(
                featuredItems = featuredItems,
                genres = genres,
                selectedGenreId = selectedGenreId,
                browseSections = browseSections,
                paginationStrategies = paginationStrategies)
        }

        data class LoadingMore(
            override val featuredItems: List<MetadataBrowseItem>,
            override val genres: List<MetadataBrowseGenre>,
            override val selectedGenreId: String?,
            override val browseSections: Map<String, List<MetadataBrowseSection>?>,
            override val paginationStrategies: Map<String, PaginationStrategy?>) : Data
    }

    data class Error(val message: String) : HomeScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModel(
    private val repository: HomeScreenRepository, private val pluginManager: PluginManager
) : ViewModel(), KoinComponent {
    private val logger by injectLogger<HomeScreenViewModel>()

    private val state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            pluginManager.selectedMetadataPlugin
                .filterNotNull()
                .flatMapLatest { it.loggedInFlow }
                .distinctUntilChanged().collect {
                    loadInitialData()
                }
        }
    }

    private suspend fun loadInitialData() = runCatching {
        state.value = HomeScreenState.Loading
        val featuredItems = repository.featuredItems() ?: emptyList()
        val genres = repository.genres() ?: emptyList()
        val firstGenreId = genres.firstOrNull()?.id
        val browseSections = mutableMapOf<String, List<MetadataBrowseSection>?>()
        val paginationStrategies = mutableMapOf<String, PaginationStrategy?>()
        if (firstGenreId != null) {
            val result = repository.list(firstGenreId)
            browseSections[firstGenreId] = result?.items ?: emptyList()
            paginationStrategies[firstGenreId] = result?.nextPagination
        }
        state.value = HomeScreenState.Data.Loaded(
            featuredItems = featuredItems,
            genres = genres,
            selectedGenreId = firstGenreId,
            browseSections = browseSections,
            paginationStrategies = paginationStrategies)
    }.onFailure { e ->
        logger.e(e) { "Failed to load home screen data" }
        state.value = HomeScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun selectGenre(genreId: String) {
        val currentState = state.value
        if (currentState is HomeScreenState.Data) {
            state.value = when (currentState) {
                is HomeScreenState.Data.Loaded -> currentState.copy(selectedGenreId = genreId)
                is HomeScreenState.Data.LoadingMore -> currentState.copy(selectedGenreId = genreId)
            }
            if (currentState.browseSections[genreId] == null) {
                viewModelScope.launch {
                    val result = repository.list(genreId)
                    val current = state.value
                    if (current is HomeScreenState.Data) {
                        state.value = HomeScreenState.Data.Loaded(
                            featuredItems = current.featuredItems,
                            genres = current.genres,
                            selectedGenreId = genreId,
                            browseSections = current.browseSections + (genreId to (result?.items ?: emptyList())),
                            paginationStrategies = current.paginationStrategies + (genreId to result?.nextPagination))
                    }
                }
            }
        }
    }

    suspend fun loadMoreData() = runCatching {
        val currentState = state.value
        if (currentState is HomeScreenState.Data.Loaded) {
            val genreId = currentState.selectedGenreId ?: return@runCatching
            val pagination = currentState.paginationStrategies[genreId] ?: return@runCatching
            state.value = currentState.toLoadingMore()
            val result = repository.list(genreId, pagination)
            val existingSections = currentState.browseSections[genreId] ?: emptyList()
            state.value = HomeScreenState.Data.Loaded(
                featuredItems = currentState.featuredItems,
                genres = currentState.genres,
                selectedGenreId = genreId,
                browseSections = currentState.browseSections + (genreId to (existingSections + (result?.items ?: emptyList()))),
                paginationStrategies = currentState.paginationStrategies + (genreId to result?.nextPagination))
        }
    }.onFailure { e ->
        logger.e(e) { "Failed to load more home screen data" }
        state.value = HomeScreenState.Error(e.message ?: getString(Res.string.common_unknown_error))
    }

    fun refresh() {
        viewModelScope.launch {
            repository.invalidateCaches()
            loadInitialData()
        }
    }
}
