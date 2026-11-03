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

package org.spotube.spotube.modules.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.spotube.modules.support.donationOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val repository: WelcomeRepository) : ViewModel() {
    private val currentPageIndex = MutableStateFlow(0)

    val uiState: StateFlow<WelcomeUiState> =
        currentPageIndex.map { index ->
            val pageCount = welcomePrimaryLabels.size
            WelcomeUiState(
                currentPageIndex = index,
                pageCount = pageCount,
                isLastPage = index == pageCount - 1,
                primaryLabel = welcomePrimaryLabels[index.coerceIn(0, pageCount - 1)],
                donations = donationOptions)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = WelcomeUiState(
                    currentPageIndex = 0,
                    pageCount = welcomePrimaryLabels.size,
                    isLastPage = welcomePrimaryLabels.size == 1,
                    primaryLabel = welcomePrimaryLabels.first(),
                    donations = donationOptions))

    fun goToPage(index: Int) {
        currentPageIndex.update { index.coerceIn(0, welcomePrimaryLabels.lastIndex) }
    }

    fun onPrimary() {
        if (currentPageIndex.value == welcomePrimaryLabels.lastIndex) {
            completeOnboarding()
        } else {
            goToPage(currentPageIndex.value + 1)
        }
    }

    fun openDonation(url: String) {
        // Only destinations owned by our static funding list can be opened here.
        if (donationOptions.none { it.url == url }) return
        runCatching { org.spotube.spotube.openUrlInBrowser(url) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
        }
    }
}
