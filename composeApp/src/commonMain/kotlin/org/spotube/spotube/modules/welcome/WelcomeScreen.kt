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


import org.spotube.millennium.composables.MillenniumButton
import org.spotube.millennium.composables.MillenniumButtonStyle
import org.spotube.millennium.composables.MillenniumCard
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumTabGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.spotube.spotube.core.ui.component.ApplicationMainBar
import org.spotube.spotube.modules.welcome.pages.BrandingPage
import org.spotube.spotube.modules.welcome.pages.DonationPage
import org.spotube.spotube.modules.welcome.pages.HowItWorksPage
import org.spotube.spotube.modules.welcome.pages.PluginsPage
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_skip

/**
 * First-run Welcome/Onboarding screen. It is a route
 * ([org.spotube.spotube.core.navigation.Routes.Welcome]) rendered inside the app shell.
 *
 * The steps are exposed through a Millennium tab group rather than ad-hoc dots or a footer
 * walk-through, so the screen reads as one deliberate panel on every window size.
 * Each step owns its content and the footer only carries the forward action.
 */
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeTheme {
        Box(Modifier.fillMaxSize()) {
            WelcomeBackground()
            Column(modifier = Modifier.fillMaxSize()) {
                ApplicationMainBar(
                    title = {
                        WelcomeWordmark(
                            fontSize = AppTheme.typography.titleLarge.fontSize)
                    },
                    backButton = false,
                    transparent = true,
                    actions = {
                        if (!state.isLastPage) {
                            MillenniumButton(
                                onClick = viewModel::completeOnboarding,
                                style = MillenniumButtonStyle.Quiet) {
                                Text(
                                    text = stringResource(Res.string.welcome_skip),
                                    color = AppTheme.colors.onSurfaceVariant)
                            }
                        }
                    })

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center) {
                    MillenniumCard(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)) {
                        Column(Modifier.fillMaxSize()) {
                            WelcomeStepSwitcher(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                state = state,
                                viewModel = viewModel)
                            MillenniumSeparator()
                            WelcomeActionBar(
                                state = state,
                                viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepSwitcher(
    state: WelcomeUiState,
    viewModel: WelcomeViewModel,
    modifier: Modifier = Modifier) {
    val labels = welcomeStepLabels.map { stringResource(it) }
    MillenniumTabGroup(
        selectedTab = state.currentPageIndex,
        onSelectedTabChange = viewModel::goToPage,
        tabs = labels.indices.toList(),
        modifier = modifier,
        tabLabel = { labels[it] }) { index ->
        when (index) {
            0 -> BrandingPage()

            1 -> DonationPage(
                donations = state.donations,
                onDonate = viewModel::openDonation)

            2 -> PluginsPage()

            else -> HowItWorksPage()
        }
    }
}

@Composable
private fun WelcomeActionBar(
    state: WelcomeUiState,
    viewModel: WelcomeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        MillenniumButton(
            onClick = viewModel::onPrimary,
            style = MillenniumButtonStyle.Secondary) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(state.primaryLabel),
                    fontWeight = FontWeight.SemiBold)
                MillenniumIcon(
                    imageVector = if (state.isLastPage) {
                        Iconsax.IconsaxCheckCircle
                    } else {
                        Iconsax.IconsaxNext
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}
