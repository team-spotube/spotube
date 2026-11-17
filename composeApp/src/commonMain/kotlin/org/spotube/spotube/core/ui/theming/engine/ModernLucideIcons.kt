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

package org.spotube.spotube.core.ui.theming.engine

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Check
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Grid
import compose.icons.feathericons.List
import compose.icons.feathericons.Lock
import compose.icons.feathericons.LogIn
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.RefreshCw
import org.spotube.spotube.resources.iconsax.*

/**
 * The app's Iconsax / Feather icons mapped to their closest Lucide equivalents, for the Modern theme.
 *
 * Keyed by the source vector's `ImageVector.name` (read from the vectors themselves, so a rename can't
 * silently break the mapping). Brand logos (GitHub, Discord, Patreon, ...) are not mapped: Lucide has no
 * brand icons, so they keep their original artwork.
 */
internal val lucideIconReplacements: Map<String, ImageVector> by lazy {
    mapOf(
        Iconsax.IconsaxHome to Lucide.House,
        Iconsax.IconsaxHomeBroken to Lucide.House,
        Iconsax.IconsaxSearch to Lucide.Search,
        Iconsax.IconsaxSearchBroken to Lucide.Search,
        Iconsax.IconsaxSetting to Lucide.Settings,
        Iconsax.IconsaxSettingTwotone to Lucide.Settings,
        Iconsax.IconsaxSetting2 to Lucide.Settings,
        Iconsax.IconsaxVerticalSetting to Lucide.SlidersVertical,
        Iconsax.User to Lucide.User,
        Iconsax.IconsaxUserRemove to Lucide.UserMinus,
        Iconsax.IconsaxPlay to Lucide.Play,
        Iconsax.Play to Lucide.Play,
        Iconsax.IconsaxPlayCircle to Lucide.CirclePlay,
        Iconsax.IconsaxPlayCircle2 to Lucide.CirclePlay,
        Iconsax.IconsaxPause to Lucide.Pause,
        Iconsax.IconsaxPauseCircle to Lucide.CirclePause,
        Iconsax.IconsaxStopCircle to Lucide.CircleStop,
        Iconsax.IconsaxNext to Lucide.SkipForward,
        Iconsax.IconsaxPrevious to Lucide.SkipBack,
        Iconsax.IconsaxShuffle to Lucide.Shuffle,
        Iconsax.IconsaxRepeatMusic to Lucide.Repeat,
        Iconsax.IconsaxRepeateMusic to Lucide.Repeat,
        Iconsax.IconsaxRepeatArrow to Lucide.Repeat,
        Iconsax.IconsaxRepeateOne to Lucide.Repeat1,
        Iconsax.IconsaxVolumeLow to Lucide.Volume1,
        Iconsax.IconsaxVolumeHigh to Lucide.Volume2,
        Iconsax.IconsaxVolumeCross to Lucide.VolumeX,
        Iconsax.IconsaxSound to Lucide.Volume2,
        Iconsax.IconsaxSoundTwotone to Lucide.AudioLines,
        Iconsax.IconsaxMusic to Lucide.Music,
        Iconsax.IconsaxMusicCircle to Lucide.Music,
        Iconsax.IconsaxMusicPlay to Lucide.Disc3,
        Iconsax.IconsaxMusicFilter to Lucide.ListFilter,
        Iconsax.IconsaxFilterSearch to Lucide.ListFilter,
        Iconsax.IconsaxMusicPlaylist to Lucide.ListMusic,
        Iconsax.IconsaxMusicDashboard to Lucide.ListMusic,
        Iconsax.IconsaxMusicLibrary to Lucide.Library,
        Iconsax.IconsaxMusicLibraryOutline to Lucide.Library,
        Iconsax.IconsaxMusicSquareRemove to Lucide.ListMinus,
        Iconsax.IconsaxMusicSquareRemoveFilled to Lucide.ListMinus,
        Iconsax.IconsaxCd to Lucide.Disc3,
        Iconsax.IconsaxHeart to Lucide.Heart,
        Iconsax.IconsaxHeart2 to Lucide.Heart,
        Iconsax.IconsaxAdd to Lucide.Plus,
        Iconsax.IconsaxAddSquare to Lucide.SquarePlus,
        Iconsax.IconsaxCloseSquare to Lucide.SquareX,
        Iconsax.IconsaxMinusSquare to Lucide.SquareMinus,
        Iconsax.IconsaxCheckSquare to Lucide.SquareCheck,
        Iconsax.IconsaxCheckCircle to Lucide.CircleCheck,
        Iconsax.IconsaxSquare to Lucide.Square,
        Iconsax.IconsaxTrash to Lucide.Trash2,
        Iconsax.IconsaxEdit to Lucide.Pencil,
        Iconsax.IconsaxInformation to Lucide.Info,
        Iconsax.IconsaxForbidden to Lucide.Ban,
        Iconsax.Iconsax3DotsMore to Lucide.Ellipsis,
        Iconsax.IconsaxDragHandle to Lucide.GripVertical,
        Iconsax.IconsaxSort to Lucide.ArrowUpDown,
        Iconsax.IconsaxTextalignLeft to Lucide.TextAlignStart,
        Iconsax.IconsaxArrowDown4 to Lucide.ArrowDown,
        Iconsax.IconsaxArrowSquareUp to Lucide.SquareArrowUp,
        Iconsax.ArrowLeft3 to Lucide.ArrowLeft,
        Iconsax.SwapHorizontal2 to Lucide.ArrowLeftRight,
        Iconsax.IconsaxRefreshRight to Lucide.RotateCw,
        Iconsax.IconsaxRefreshArrow2 to Lucide.RefreshCw,
        Iconsax.InconsaxClock to Lucide.Clock,
        Iconsax.IconsaxFolder to Lucide.Folder,
        Iconsax.IconsaxFolderOpen to Lucide.FolderOpen,
        Iconsax.IconsaxFolderAdd to Lucide.FolderPlus,
        Iconsax.IconsaxBox to Lucide.Package,
        Iconsax.IconsaxBoxAdd to Lucide.PackagePlus,
        Iconsax.IconsaxDocumentText to Lucide.FileText,
        Iconsax.IconsaxDocumentDownload to Lucide.FileDown,
        Iconsax.IconsaxDirectboxReceive to Lucide.Download,
        Iconsax.IconsaxImportArrow2 to Lucide.Import,
        Iconsax.IconsaxImportArrow2Bulk to Lucide.Import,
        Iconsax.IconsaxExportArrowBulk to Lucide.Upload,
        Iconsax.IconsaxShare to Lucide.Share2,
        Iconsax.IconsaxLink to Lucide.Link,
        Iconsax.IconsaxGlobe to Lucide.Globe,
        Iconsax.IconsaxLocation to Lucide.MapPin,
        Iconsax.IconsaxLanguageSquare to Lucide.Languages,
        Iconsax.IconsaxWifiSquare to Lucide.Wifi,
        Iconsax.IconsaxMirroringScreen to Lucide.MonitorSmartphone,
        Iconsax.IconsaxMirrorScreenRegular to Lucide.MonitorSmartphone,
        Iconsax.CustomServer to Lucide.Server,
        Iconsax.PhosphorDatabaseDuotone to Lucide.Database,
        Iconsax.IconsaxColorSwatch to Lucide.Palette,
        Iconsax.IconsaxColorsSquare to Lucide.Palette,
        Iconsax.IconsaxMoon to Lucide.Moon,
        Iconsax.IconsaxMagic to Lucide.Sparkles,
        Iconsax.IconsaxEye to Lucide.Eye,
        Iconsax.IconsaxTag to Lucide.Tag,
        Iconsax.IconsaxFormatCircle to Lucide.Shapes,
        Iconsax.IconsaxSidebarLeftBroken to Lucide.PanelLeft,
        Iconsax.IconsaxSidebarRightBroken to Lucide.PanelRight,
        Iconsax.FluentDismiss to Lucide.X,
        Iconsax.FluentMinus to Lucide.Minus,
        Iconsax.FluentMaximize to Lucide.Maximize,
        Iconsax.FluentSquareMultiple to Lucide.Copy,
        FeatherIcons.ChevronRight to Lucide.ChevronRight,
        FeatherIcons.ChevronLeft to Lucide.ChevronLeft,
        FeatherIcons.Check to Lucide.Check,
        FeatherIcons.Copy to Lucide.Copy,
        FeatherIcons.RefreshCw to Lucide.RefreshCw,
        FeatherIcons.Maximize2 to Lucide.Maximize2,
        FeatherIcons.LogIn to Lucide.LogIn,
        FeatherIcons.Lock to Lucide.Lock,
        FeatherIcons.List to Lucide.List,
        FeatherIcons.Grid to Lucide.LayoutGrid,
        FeatherIcons.ArrowRight to Lucide.ArrowRight,
        FeatherIcons.AlertCircle to Lucide.CircleAlert,
    ).entries.associate { (from, to) -> from.name to to }
}
