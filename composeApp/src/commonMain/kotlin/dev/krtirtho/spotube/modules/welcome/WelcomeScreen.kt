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

package dev.krtirtho.spotube.modules.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import dev.krtirtho.spotube.core.ui.base.BaseScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krtirtho.spotube.core.ui.base.GhostButton
import dev.krtirtho.spotube.core.ui.base.OutlineButton
import dev.krtirtho.spotube.core.ui.component.ApplicationMainBar
import dev.krtirtho.spotube.modules.welcome.pages.BrandingPage
import dev.krtirtho.spotube.modules.welcome.pages.DonationPage
import dev.krtirtho.spotube.modules.welcome.pages.HowItWorksPage
import dev.krtirtho.spotube.modules.welcome.pages.PluginsPage
import dev.krtirtho.spotube.resources.iconsax.Iconsax
import dev.krtirtho.spotube.resources.iconsax.IconsaxCheckCircle
import dev.krtirtho.spotube.resources.iconsax.IconsaxNext
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_back
import spotube.composeapp.generated.resources.welcome_skip

/**
 * First-run Welcome/Onboarding screen. It is a route ([dev.krtirtho.spotube.core.navigation.Routes.Welcome])
 * rendered inside the app shell. Uses the application's base controls and keeps plugin setup
 * in-place, with a neutral palette independent of the user's chosen accent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeTheme {
        Box(Modifier.fillMaxSize()) {
            WelcomeBackground()
            BaseScaffold(
                containerColor = Color.Transparent,
                topBar = {
                    ApplicationMainBar(
                        title = {
                            WelcomeWordmark(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            )
                        },
                        backButton = false,
                        transparent = true,
                        actions = {
                            if (!state.isLastPage) {
                                GhostButton(onClick = viewModel::completeOnboarding) {
                                    Text(
                                        text = stringResource(Res.string.welcome_skip),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Central stage (animated page content)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 1280.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                modifier = Modifier.fillMaxSize(),
                                targetState = state.currentPageIndex,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                                (slideOutHorizontally { -it / 4 } + fadeOut())
                                    } else {
                                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                                (slideOutHorizontally { it / 4 } + fadeOut())
                                    }
                                },
                                label = "welcome_page",
                            ) { index ->
                                when (index) {
                                    0 -> BrandingPage()

                                    1 -> DonationPage(
                                        donations = state.donations,
                                        onDonate = viewModel::openDonation,
                                    )

                                    2 -> PluginsPage()

                                    else -> HowItWorksPage(
                                        isPlaying = state.demoPlaying,
                                        onTogglePlayback = viewModel::toggleDemo
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Bottom precision navigation bar: Back | Page Dots | Action Button
                        Row(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Left: Back button
                            if (state.currentPageIndex > 0) {
                                OutlineButton(
                                    onClick = viewModel::previousPage,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.welcome_back),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(76.dp))
                            }

                            // Center: Precision page dots
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                PageIndicator(
                                    pageCount = state.pageCount,
                                    currentPageIndex = state.currentPageIndex,
                                )
                            }

                            // Right: Primary Next / Let's Go action
                            OutlineButton(
                                onClick = viewModel::onPrimary,
                            ) {
                                Text(
                                    text = stringResource(state.primaryLabel),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Icon(
                                    imageVector = if (state.isLastPage) {
                                        Iconsax.IconsaxCheckCircle
                                    } else {
                                        Iconsax.IconsaxNext
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPageIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPageIndex
            val width by animateDpAsState(
                targetValue = if (selected) 20.dp else 6.dp,
                label = "welcome_page_indicator",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
            )
        }
    }
}
