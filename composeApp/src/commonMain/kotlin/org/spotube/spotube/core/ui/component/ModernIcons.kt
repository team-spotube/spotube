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

package org.spotube.spotube.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Cast
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.Disc
import compose.icons.feathericons.Download
import compose.icons.feathericons.Heart
import compose.icons.feathericons.Home
import compose.icons.feathericons.Info
import compose.icons.feathericons.List
import compose.icons.feathericons.Moon
import compose.icons.feathericons.MoreHorizontal
import compose.icons.feathericons.Music
import compose.icons.feathericons.PlusSquare
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Repeat
import compose.icons.feathericons.Search
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Shuffle
import compose.icons.feathericons.Sidebar
import compose.icons.feathericons.SkipBack
import compose.icons.feathericons.SkipForward
import compose.icons.feathericons.User
import compose.icons.feathericons.Volume1
import compose.icons.feathericons.Volume2
import compose.icons.feathericons.VolumeX
import compose.icons.feathericons.X
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.Iconsax3DotsMore
import org.spotube.spotube.resources.iconsax.IconsaxAddSquare
import org.spotube.spotube.resources.iconsax.IconsaxArrowDown4
import org.spotube.spotube.resources.iconsax.IconsaxCd
import org.spotube.spotube.resources.iconsax.IconsaxCloseSquare
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxHeart
import org.spotube.spotube.resources.iconsax.IconsaxHeart2
import org.spotube.spotube.resources.iconsax.IconsaxHome
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxMirrorScreenRegular
import org.spotube.spotube.resources.iconsax.IconsaxMoon
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxMusicDashboard
import org.spotube.spotube.resources.iconsax.IconsaxMusicFilter
import org.spotube.spotube.resources.iconsax.IconsaxMusicLibrary
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import org.spotube.spotube.resources.iconsax.IconsaxRepeatMusic
import org.spotube.spotube.resources.iconsax.IconsaxRepeateMusic
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.IconsaxSetting2
import org.spotube.spotube.resources.iconsax.IconsaxShuffle
import org.spotube.spotube.resources.iconsax.IconsaxSidebarLeftBroken
import org.spotube.spotube.resources.iconsax.IconsaxSidebarRightBroken
import org.spotube.spotube.resources.iconsax.IconsaxSoundTwotone
import org.spotube.spotube.resources.iconsax.IconsaxVolumeCross
import org.spotube.spotube.resources.iconsax.IconsaxVolumeHigh
import org.spotube.spotube.resources.iconsax.IconsaxVolumeLow
import org.spotube.spotube.resources.iconsax.User

/*
 * The Modern style draws chrome icons as thin lines (Feather) instead of the filled, two-tone
 * icon set. Only chrome is mapped: navigation, window and transport controls. State icons that
 * rely on being filled (play/pause, a liked heart, repeat-one) keep their own glyph.
 */

private val lineIcons: List<Pair<ImageVector, ImageVector>> by lazy {
    listOf(
        Iconsax.IconsaxHome to FeatherIcons.Home,
        Iconsax.IconsaxSearch to FeatherIcons.Search,
        Iconsax.IconsaxMusicDashboard to FeatherIcons.List,
        Iconsax.IconsaxCd to FeatherIcons.Disc,
        Iconsax.User to FeatherIcons.User,
        Iconsax.IconsaxMusicLibrary to FeatherIcons.Music,
        Iconsax.IconsaxDirectboxReceive to FeatherIcons.Download,
        Iconsax.IconsaxMirrorScreenRegular to FeatherIcons.Cast,
        Iconsax.IconsaxSoundTwotone to FeatherIcons.Radio,
        Iconsax.IconsaxHeart2 to FeatherIcons.Heart,
        Iconsax.IconsaxHeart to FeatherIcons.Heart,
        Iconsax.IconsaxSetting2 to FeatherIcons.Settings,
        Iconsax.IconsaxSidebarLeftBroken to FeatherIcons.Sidebar,
        Iconsax.IconsaxSidebarRightBroken to FeatherIcons.Sidebar,
        Iconsax.IconsaxCloseSquare to FeatherIcons.X,
        Iconsax.IconsaxPrevious to FeatherIcons.SkipBack,
        Iconsax.IconsaxNext to FeatherIcons.SkipForward,
        Iconsax.IconsaxShuffle to FeatherIcons.Shuffle,
        Iconsax.IconsaxRepeateMusic to FeatherIcons.Repeat,
        Iconsax.IconsaxRepeatMusic to FeatherIcons.Repeat,
        Iconsax.IconsaxMusicFilter to FeatherIcons.List,
        Iconsax.IconsaxMusic to FeatherIcons.Music,
        Iconsax.Iconsax3DotsMore to FeatherIcons.MoreHorizontal,
        Iconsax.IconsaxMoon to FeatherIcons.Moon,
        Iconsax.IconsaxInformation to FeatherIcons.Info,
        Iconsax.IconsaxVolumeCross to FeatherIcons.VolumeX,
        Iconsax.IconsaxVolumeLow to FeatherIcons.Volume1,
        Iconsax.IconsaxVolumeHigh to FeatherIcons.Volume2,
        Iconsax.IconsaxArrowDown4 to FeatherIcons.ChevronDown,
        Iconsax.IconsaxAddSquare to FeatherIcons.PlusSquare,
    )
}

/** This icon as the Modern style draws it: its line counterpart when it has one. */
@Composable
fun ImageVector.styled(): ImageVector {
    if (!AppTheme.isModern) return this
    // Icons are cached singletons, so identity is enough and avoids deep vector comparisons.
    return lineIcons.firstOrNull { it.first === this }?.second ?: this
}
