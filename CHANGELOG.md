# Changelog

## [6.0.0-beta.0](https://github.com/team-spotube/spotube/compare/v5.1.2...v6.0.0-beta.0) (2026-10-26)

### Features

- Migration to Compose Multiplatform
- Add ChipTab component and refactor SearchScreen to use it
- Add AutocompleteTextField component and integrate it into SearchScreen
- Enhance AutocompleteTextField with focus management and dropdown behavior
- Enhance AppSidebar with improved button interactions and visual feedback
- Implement endless playback recommendations in DeviceAudioPlayerQueue
- Add GhostIconButton component and enhance button interactions in TrackList
- Replace IconButton with GhostIconButton in AppExpandedPlayer and AppLargePlayer for improved visual consistency
- Replace IconButton with GhostIconButton in PlayerQueueContent and PlayableCard for visual consistency
- Enhance UI components with LocalBaseUITheme for consistent styling and improved interactions
- Enhance button styles and interactions in Buttons and Theme components for improved UI consistency
- Simplify highlight implementation in UI components for improved readability and maintainability
- Add genre selection and tabs to HomeScreen for improved content navigation
- Implement artist overview screen with loading, error handling, and pagination for albums, related artists, and featured playlists
- Add genre and featured browsing capabilities to MediaBrowseHelper
- Implement playlist creation and editing functionality with UI components
- Add 'Add to Playlist' functionality with UI components and dialogs
- Add Toggle component and update UI to use it in place of Switch
- Implement settings section cards for updates, desktop, language, caching, and plugins
- Update icons in settings sections to use Iconsax library for improved UI consistency
- Replace FeatherIcons with Iconsax in PluginCard and PluginPermissionsDialog for improved UI consistency
- Refactor PluginPermissionsDialog to use custom UI components for improved consistency and readability
- Enhance UI with shared element transitions and improve state management in album and playlist screens
- Integrate Uniffi for audio metadata reading and writing with Lofty library
- Replace FeatherIcons with Iconsax in window controls and update sidebar with better design
- Implement haze effect and UI enhancements in AppLargePlayer and AppShell
- Enhance haze effect integration in AppShell and AppSidebar for improved UI experience
- Add adaptive dialog bottom sheet component and enhance plugin installation UI
- Enhance AdaptiveDialogBottomSheet with dismiss icon and improve plugin screen layout
- Implement CollectionView component and refactor Album, Playlist, SavedTracks screens for improved UI consistency
- Add current time management in AudioPlayer for improved playback control
- Simplify playback control logic in AudioPlayer by removing redundant state checks
- Introduce AudioPlayerInterface and refactor AudioPlayer implementations for better abstraction
- Update skipToNext and skipToPrevious methods in AudioPlayer for improved media item management
- Refactor LyricsViewModel to use AudioPlayerInterface for improved abstraction
- Add Discord Rich Presence integration for enhanced media playback visibility
- Implement System Tray integration for window management and media controls
- Add hover effect to menu items in System Tray for improved user experience
- Add SSH debug session setup on failure for improved troubleshooting
- Implement blacklist functionality for tracks and artists with UI updates
- **systemtray**: Self-manage tray lifecycle via Koin DI
- **plugin_interfaces**: Update Maven publishing configuration and add GitHub Actions workflow
- **plugin_discovery**: Implement GitHub plugin discovery and add logo support in UI
- **plugin_discovery**: Implement GitHub plugin discovery and add logo support and information dialog in UI
- **plugin_install**: Add plugin installation dialog and release fetching functionality
- Update dependencies and configure compose hot reload settings
- Improve NewPipe video rank matching
- Integrate Nucleus framework for enhanced window management and webview support
- Implement system tray functionality with Compose Native Tray integration
- **mediacontrol**: Implement SystemMediaControlService for OS media integration
- **devices**: Add device discovery and remote control features
- **deeplinks**: Implement deep link handling for jam sessions
- **remote-control**: Implement remote playback functionality and UI components
- **remote-control**: Enhance queue management and player state synchronization
- **remote-control**: Enhance remote playback functionality with new commands and UI integration
- **webrtc**: Update ICE server configuration and enhance logging for peer connection events
- **jam-session**: Add connection state management and update UI feedback for session status
- **jam-session**: Enhance jam session functionality with improved playback synchronization and error handling
- **remote-control**: Refactor service registration and cleanup logic for improved reliability
- **jam-session**: Integrate MQTT broker configuration and enhance jam session settings
- **jam-session**: Implement track addition to jam session and enhance UI feedback for jam state
- **jam-session**: Enhance guest functionality with track addition and participant moderation options
- **release**: Add support for Linux ARM64 builds and artifact uploads
- **player-options**: Add PlayerOptionsViewModel and enhance player UI with options menu
- **player-options**: Enhance options menu with localized strings and icons
- **jam-settings**: Implement JamSettings screen and view model for broker configuration
- Refactor track source management to support plugin-specific caching and retrieval
- Implement shuffle play functionality for albums, playlists, and saved tracks
- **navigation**: Refactor sidebar and bottombar navigation structure with new tab and group models
- **search**: Enhance LazyRow with drag scrolling functionality
- **autocomplete**: Simplify focus management and remove unused key event handling
- **update**: Implement update checker with dialog for available updates
- **update**: Refactor update checking mechanism and implement new dialog for app and plugin updates
- **scrobble**: Implement ScrobbleManager to handle track scrobbling and integrate with audio player
- **welcome**: Implement onboarding flow with multiple pages and donation options
- **previews**: Add comprehensive previews for UI components including sliders, toggles, checkboxes, and text fields
- **settings**: Add 'About' section and improve header visibility in settings sections
- **surface**: Implement Frutiger-Aero themed surfaces with glass and acrylic styles
- **ui**: Replace Scaffold with BaseScaffold for consistent surface theming across screens
- **ui**: New custom surface integration on all components
- **onboarding**: Enhance welcome screen with structured guide steps and improved layout
- **player**: Add content descriptions for play, pause, and add to queue actions
- **support**: Implement support reminder dialog and donation options
- **player**: Refactor artist display and improve search field labels
- **search**: Enhance search functionality with recent search suggestions and improved UI components
- **sidebar**: Adjust spacing in sidebar and setting cards for improved layout
- **jam**: Implement group jam functionality with UI components and state management
- **library**: Enhance library UI with new tab titles, filtering options, and improved state management
- **library**: Add Library screen list mode beside grid mode, Local Track, Download screen, Local Library folders dialog visual improvement
- **ui**: Integrate LocalTextStyle for dynamic tinting in player controls and UI components
- **navigation**: Add custom navigation transition specifications for push and pop actions
- **i18n**: Replace hardcoded strings with localized resources across multiple components
- **plugin**: Implement MetadataPluginGuard to manage plugin dependencies and user prompts
- **ui**: Replace IndeterminateProgress with new loading components and enhance dialog structures
- **ui**: Update MillenniumModalBottomSheet with compact padding and spacing adjustments
- **plugin**: Add login prompt for metadata plugin access and implement authentication view model
- **user-agents**: Implement dynamic user agent fetching and caching mechanism
- **lyrics**: Implement SyncedLyricsList component and enhance LyricsViewModel for better state management
- **server**: Implement port fallback mechanism for playback proxy server
- **window**: Implement window state persistence and restoration
- Enhance Android build configuration with stable/nightly flavors and update launcher icons
- **graalvm**: Enable GraalVM native image support and update packaging configurations
- **local-tracks**: Implement LocalFolder screen and view model for folder navigation
- **navigation**: Implement fade-through and stack transition animations for navigation
- **remote**: Redesign remote control and devices screens, fix connection issues
- Download diagnostics, live connect status, remote settings on devices screen
- Enhance loading states for various UI components and update dependencies
- **packaging**: Flathub, AUR, WinGet and Chocolatey publishing
- **plugin**: Implement automatic plugin updates and enhance JSON parsing

### Bug Fixes

- **newpipe**: Fallback to muxed streams if no audio stream is available
- Dismiss search dropdown and keyboard on submission
- Custom image helper null exception
- **plugin**: Reload Zipline service on same-version replace
- Convert buildCurrentMediaItemList to a suspend function for coroutine support
- Enhance Discord RPC connection management with cooldown and error handling
- Cached track audio not being invalidated causing alternative track source to not being played
- Play next not working
- Update pendingNextIndex when playing a track from the playlist
- **webview**: Linux webview crashing the app
- Add mavenLocal() to plugin and dependency resolution management
- **plugin**: Handle service closure exception during authentication check
- **sidebar**: Add spacer to AppSidebar for improved layout
- **remote-control**: Update connection handling and improve logging for remote control events
- Add window insets to top bar and bottom navigation bar, queue sheet
- **stream-proxy**: Stream upstream audio instead of buffering the whole track
- **newpipe**: Fall back through multiple searches when ISRC search misses
- **audio-api**: Enhance fallback mechanism for text search results in YouTube
- **audio-player**: Android shuffle wasnt working
- **stream-proxy**: Improve logging for cache timeout and handle disconnection scenarios
- **macos**: Add GraalVM runtime dependency and initialize GraalVm in main function
- **android**: Create intermediate directories for Rust targets to prevent runtime errors
- **android**: Update sharing functionality to use chooser intent with new task flag
- **android**: Simplify service start logic for PlaybackService to avoid crashes on background playback
- **android**: Update playback state management to ensure guests follow host's play state
- **android**: App stuck on splash screen from main-thread string loading
- **downloads**: Respect download format/quality and never save manifests or video
- **ios**: Run Zipline on an 8 MiB stack thread
- **server**: Don't crash when the proxy port is still in use
- **ios**: Stream formats AVPlayer can play
- **gradle**: Restore VFS watch setting and clean up properties
- **build**: Restore version catalog lost in dev merge
- **ios**: Make the login WebView identify as Mobile Safari
- Update website URLs from spotube.org to spotube.cc

## [5.1.2](https://github.com/team-spotube/spotube/compare/v5.1.1...v5.1.2) (2026-06-05)

### Bug Fixes

- **newpipe**: Fallback to muxed streams if no audio stream is available
- Dismiss search dropdown and keyboard on submission
- Custom image helper null exception

## [5.1.1](https://github.com/team-spotube/spotube/compare/v5.1.0...v5.1.1) (2026-02-24)

### Bug Fixes

- Upgrade NewPipeExtractor to latest version fixing playback issue
- Lyrics not working

## [5.1.0](https://github.com/team-spotube/spotube/compare/v5.0.0...v5.1.0) (2025-11-14)

### Features

- Show plugin source and set the only plugin as default if no plugin is there
- **playback**: Add dab music source
- **playback**: Add uncompressed flac playback support
- Add plugin audio source models and api service
- **plugins**: Filter plugins by abilities in plugins page and show abilities as
  badge
- **plugins**: Filter plugins by abilities in plugins page and show abilities as
  badge
- Add setting default audio source support
- Move away from track source query and preferences audio quality and codec
- Add NewPipe support for desktop platforms
- Add default plugin loading capability
- **queue**: Add multi-select and bulk actions to queue
  ([#2839](https://github.com/team-spotube/spotube/issues/2839))
- **android**: Add 16KB page size support

### Bug Fixes

- Change plugin download directory to application support
- **playback**: Play next not working
- Downloaded tracks are not tagged with metadata
- Download not working in different devices and slow
- **playback**: Use stream instead of chunked serving of audio bytes

## [5.0.0](https://github.com/team-spotube/spotube/compare/v4.0.2...v5.0.0) (2025-09-08)

### Features

- Add ISRC track search for YouTube
  ([#2594](https://github.com/team-spotube/spotube/issues/2594))
- Add new icons #2676 by @alexio-dev
  ([#2678](https://github.com/team-spotube/spotube/issues/2678))
- Add connect confirmation dialog
- Add metadata api service and models
- **metadata-plugin**: Add pagination support, feed and playlist CRUD endpoints
- **metadata-plugin**: Add local storage api
- Add webview, totp and setInterval apis for plugins
- Enhance local storage and webview APIs with improved error handling and
  resource management
- Enhance local storage and webview APIs with improved error handling and
  resource management
- **metadata_plugin**: Add logout method
- Update plugin configuration with more fields
- Implement metadata plugins based on hetu
- Update models to match hetu_spotube_plugin signature
- Add user endpoint calls in metadata and paginated async notifiers
- Add playlist endpoint and providers
- Add albums metadata endpoint and provider
- Add artist and album providers
- Add track endpoint for metadata service
- Remove green corp names formally
- **metadata**: Add plugin form
- Add support for entity specific search
- Enhance image handling
- Add support for automatic plugin repository from github and codeberg
- Use isolate for youtube_explode engine
- Add repository and plugin API version fields to metadata plugins
- Update new pipe version
- **metadata**: Add plugin update checker and dialog for available updates
- Optimize track options and related artists
- Add plugin scrobbling support and support button
- Add ErrorBox and NoDefaultMetadataPlugin components

### Bug Fixes

- Calling /track/:streamId endpoint causes active sourced track to be anything
- **mobile**: Dialogs in bottom sheet are not opening
- Default accent color is orange but it shows blue in settings
- Artist images are not loading up
- CVE: Remote path traversal through websocket when devices are on same network
- Endless playback not working
- **android**: NewPipe invalid search content filters
- Make YoutubeExplode engine faster
- Create and delete playlist not working
- Local track not working and images of local not showing up
- Local playback not working for tracks with special # (hashtag) characters
- Inaccessible streaming url causing rapid skips
- **yt**: Fallback to different search result if all streaming url is
  inaccessible
- **yt**: Fallback to different search result if all streaming url is
  inaccessible
- **playback**: Skip network requests if cached file already exists
- Yt-dlp playback not working and add partial support for HLS streaming
- Windows webview2 environment permission issue
- **playback**: Play not fetching full playlist if playlist is too long
- **track_options**: Tapping on option doesn't close the menu
- **playback**: Alternative track sources switch not working
- **ui**: Lyrics white text in white background and small player buttons

### Translation

- Add Traditional Chinese translation
  ([#2762](https://github.com/team-spotube/spotube/issues/2762))
- Fix Japanese translations
  ([#2732](https://github.com/team-spotube/spotube/issues/2732))
- Correction of the dutch language
  ([#1306](https://github.com/team-spotube/spotube/issues/1306))

## [4.0.2](https://github.com/team-spotube/spotube/compare/v4.0.1...v4.0.2) (2025-03-16)

### Bug Fixes

- invalid access token exception #2525

## [4.0.1](https://github.com/team-spotube/spotube/compare/v4.0.0...v4.0.1) (2025-03-15)

### Bug Fixes

- **android**: navigation overlaying in app navigation
- add to playlist not working in smaller screen devices
- language picker search broken
- **generate_playlist**: create playlist not adding tracks nor navigating to
  playlist page
- **desktop**: double titlebar in local library folders and massive space in
  overlay player
- **generate_playlist**: create playlist not adding tracks nor navigating to
  playlist page
- **desktop**: double titlebar in local library folders and massive space in
  overlay player
- lastfm form broken in other locales #2447
- Greedy Green Corp login broken due to new totp requirement #2494
- Greedy Green Corp authentication 429 errors

### Features

- **local_library**: add support for x-flac, opus and x-wav
- **translation**: add tagalog language support #2504
- **translation**: add tamil translation for spotube #2501

## [4.0.0](https://github.com/team-spotube/spotube/compare/v3.9.0...v4.0.0) (2025-03-07)

## Changes

### Bug Fixes

- SafeArea for global bottom items not working
- SafeArea not working for bottom floating widgets
- youtube video unplayable issue and use more ytClients
- **ios**: downloads not working due to permission errors (#2180)
- remove automaticallyImplyLeading from root tabs
- **android**: back button not working and safe area issues
- duplicates in recent section
- youtube source taking too long to buffer
- youtube tracks keeps skipping despite being matched correctly
- follow artist not working #2057
- youtube_explode_dart failing for many videos due to youtube ios client visitor
  data change
- youtube_explode_dart failing for many videos due to youtube ios client visitor
  data change
- piped api not working

### Features

- **android**: home widget support (#2148)
- rewrite entire app in shadcn-ui replacing material eww
- grid/list customizable playbutton view
- flag to hide Greedy Green Corp generated images with patterns
- show placeholder images where there is no item or on empty page
- pause playback when no internet connection
- implement yt-dlp for desktop and NewPipeExtractor for Android (#2316)
- custom piped & invidious instance support

## [3.9.0](https://github.com/team-spotube/spotube/compare/v3.8.3...v3.9.0) (2024-12-08)

## Changes

### Bug Fixes

- UI glitch when loading more user artists and albums
- selecting an Alternative Track Source removes the current song from the queue
  #2039
- **mobile**: ensure audio session is activated when playback is resumed after
  interruption #2092
- selecting an Alternative Track Source removes the current song from the queue
  #2039
- **mobile**: ensure audio session is activated when playback is resumed after
  interruption #2092

### Features

- add invidious audio source and fix auto skipping tracks (#2005)
- track caching and cached track export support (#2117)

## [3.8.3](https://github.com/team-spotube/spotube/compare/v3.8.2...v3.8.3) (2024-10-09)

## Changes

### Bug Fixes

- update youtube_explode_dart to 2.2.3 to fix no playback (#1980)

### Features

- **macos**: enable same window webview support

## [3.8.2](https://github.com/team-spotube/spotube/compare/v3.8.1...v3.8.2) (2024-09-30)

## Changes

### Bug Fixes

- endless song loading issue and no playback #1925

## [3.8.1](https://github.com/team-spotube/spotube/compare/v3.8.0...v3.8.1) (2024-09-15)

## Changes

### Bug Fixes

- **translations**: correct some basque incorrect translations (#1815)
- **lyrics**: LRCLIB lyrics should be usable without logging in #1803
- playlist displaying descriptions unescaped html #1784
- **android**: pressing back while the player is open doesn't take to previous
  page
- **android**: pressing back while the player is open doesn't take to previous
  page
- handle dublicated items in playback queue correctly #1852
- **desktop**: scrollbar overlapping with more options of tracks and playlists
- **discord**: stop discord rpc from try update presence when not connected
- **stats**: minutes page shows plays and streams page shows minutes which
  should be the opposite #1880
- **stats**: minutes page shows plays and streams page shows minutes which
  should be the opposite #1880
- **android**: clears queue upon swiping away notification
- **player**: shuffle button state resets after closing page #1657
- getting started page login page exception #1800
- **mobile**: queue doesn't persist
- local tracks takes time to load
- start radio not working #1629

### Features

- **desktop**: show error dialog if webview is not found on login #1871
- manually detect and define touch behavior #1763

## [3.8.0](https://github.com/team-spotube/spotube/compare/v3.7.1...v3.8.0) (2024-06-06)

### Features

- translations: make state page's hard coded strings translatable (#1719)
- discord: add listening activity type
- discord: album art, playing time and play pause support (#1765)
- linux: Use XDG_STATE_HOME to storage logs (#1675)
- discord rpc for macOS, windows-arm64 and linux-arm64 (#1713)
- desktop: implement webview based login
- stats: add lazy loading support

### Bug Fixes

- translations: fix Russian translations (#1696)
- ios: permission exception
- linux: tray icon wrong name for flatpak
- windows: app crashes when no internet
- windows: local tracks plays but disabled playback controls
- go to track album shows up for local tracks
- local track metadata timeout
- windows: window stretching #1553
- android: app getting killed from background
- linux: OS Media control not working for Flatpak #1627
- incorrect datatype used for MPRIS position property #1521
- Too many artists for a track causing overflows
- playlist share button does not work #1639
- unescape html escape values #1300
- lyrics page doesn't scroll to top after song ends #885
- changed source doesn't get saved and uses the wrong once again
- null exception in album page navigated from /home
- popup menu item opacity
- linux: change app id in flatpak environment

## [3.7.1](https://github.com/team-spotube/spotube/compare/v3.7.0...v3.7.1) (2024-06-06)

### Bug Fixes

- alternative sources not showing up for SongLink matched results
  ([37d002d](https://github.com/team-spotube/spotube/commit/37d002d133cacb3a34884713ac8f6637694af57c))
- **android:** Media Controls not working above Android 14
  [#1561](https://github.com/team-spotube/spotube/issues/1561)
  ([3394c1b](https://github.com/team-spotube/spotube/commit/3394c1b0574e44dc624b2b2f0bf32f343ae9f049))
- browse anonymously button takes to wrong route
  ([73c5b30](https://github.com/team-spotube/spotube/commit/73c5b30b63a4c82bb7ad5b52bc10c5594566a800))
- **desktop:** titlebar drag to move not working
  ([5f280a1](https://github.com/team-spotube/spotube/commit/5f280a19f4d5f8882ae2ff60c6cc595ded7a5a1d))
- **desktop:** window is not centered
  ([47f98b9](https://github.com/team-spotube/spotube/commit/47f98b98aafab9b426733ed44cab2be8a646a98e))
- **ios:** download not working
  [#1575](https://github.com/team-spotube/spotube/issues/1575)
  ([6591ec0](https://github.com/team-spotube/spotube/commit/6591ec0e1b441dd8fd535eace19a58c7749389ca))
- **linux:** application window not visible after launch
  ([8fc44ed](https://github.com/team-spotube/spotube/commit/8fc44ed6550e8b2b804991ff82df08afb1c94ca8))
- local track not showing up in queue
  ([d82261c](https://github.com/team-spotube/spotube/commit/d82261cb25ece63f85af0e40216cf32dccdc9dd5))
- use weak match for Jiosaavn fallback to improve matching
  ([6cb2986](https://github.com/team-spotube/spotube/commit/6cb29868d2030b5e9312863c17e8f24889942e24))
- **windows:** media controls not showing up
  [#1542](https://github.com/team-spotube/spotube/issues/1542)
  ([d7d864f](https://github.com/team-spotube/spotube/commit/d7d864ff2bc937675a544a7edf645c5148ec836a))
- **windows:** revert Flutter version to 3.19.6 to avoid distortion
  [#1553](https://github.com/team-spotube/spotube/issues/1553)
  ([982cf0b](https://github.com/team-spotube/spotube/commit/982cf0bd435638fa20f17ef527fe21d031b5ffaf))

## [3.7.0](https://github.com/team-spotube/spotube/compare/v3.6.0...v3.7.0) (2024-06-03)

### Features

- local library folder cards
  ([fc5bfa0](https://github.com/team-spotube/spotube/commit/fc5bfa089ce2f46ab786565d6750564d704ee7e0))
- Local music library ([#1479](https://github.com/team-spotube/spotube/issues/1479))
  ([22caa81](https://github.com/team-spotube/spotube/commit/22caa818f4ac31626aaff6952e43512b42237d00))
- personalized stats based on local music history
  ([#1522](https://github.com/team-spotube/spotube/issues/1522))
  ([82307bc](https://github.com/team-spotube/spotube/commit/82307bc030035b03ab1b8d8ec7b24da19a866b12))
- play initially available tracks of playlist/album immediately and fetch rest
  in background [#670](https://github.com/team-spotube/spotube/issues/670)
  ([02acbd9](https://github.com/team-spotube/spotube/commit/02acbd93271145dde365f6c547e0d9d902be65f1))
- **player:** add volume slider floating label showing percentage
  ([#1445](https://github.com/team-spotube/spotube/issues/1445))
  ([8fad225](https://github.com/team-spotube/spotube/commit/8fad2251b3536e9468e0fb193939ead98bad3bc6)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1171](https://github.com/team-spotube/spotube/issues/1171)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
- **translations:** add Basque translation
  ([#1493](https://github.com/team-spotube/spotube/issues/1493))
  ([dbc1c45](https://github.com/team-spotube/spotube/commit/dbc1c452dd53153c61589f956ea9836cea7bf2bb))
- **translations:** add Finnish translations
  ([#1449](https://github.com/team-spotube/spotube/issues/1449))
  ([edc997e](https://github.com/team-spotube/spotube/commit/edc997e7470ce17f60c96b8198dc8851cbf21f18)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1171](https://github.com/team-spotube/spotube/issues/1171)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
- **translations:** add georgian language
  ([#1450](https://github.com/team-spotube/spotube/issues/1450))
  ([1e7f0e1](https://github.com/team-spotube/spotube/commit/1e7f0e1fe71e0a8d86614fc884861f8791469112))
- **translations:** add Indonesian translation
  ([#1426](https://github.com/team-spotube/spotube/issues/1426))
  ([0280654](https://github.com/team-spotube/spotube/commit/0280654bb6bad373aee521f5a866228d2d38f038)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1171](https://github.com/team-spotube/spotube/issues/1171)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
- **translations:** Improve tr locales
  ([#1419](https://github.com/team-spotube/spotube/issues/1419))
  ([bf45681](https://github.com/team-spotube/spotube/commit/bf45681deb951c772bf6ca05e213c949c04bded1)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1171](https://github.com/team-spotube/spotube/issues/1171)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
- upgrade to Flutter 3.22.0
  ([71341ec](https://github.com/team-spotube/spotube/commit/71341ec0bda6ed985b43836712075b97a2cf8bac))

### Bug Fixes

- fallback to LRCLIB when lyrics line less than 6 lines
  [#1461](https://github.com/team-spotube/spotube/issues/1461)
  ([9aea354](https://github.com/team-spotube/spotube/commit/9aea35468fa7cd176ddc8810b37b90c2d8246931))
- **linux:** tray icon not showing
  [#541](https://github.com/team-spotube/spotube/issues/541)
  ([7ac7917](https://github.com/team-spotube/spotube/commit/7ac791757abb30f40374c169c4211916287bb3f3))
- local track not showing up in queue
  ([d82261c](https://github.com/team-spotube/spotube/commit/d82261cb25ece63f85af0e40216cf32dccdc9dd5))
- **macos:** Logs directory not created by default
  [#1353](https://github.com/team-spotube/spotube/issues/1353)
  ([4ca8939](https://github.com/team-spotube/spotube/commit/4ca893950b07f678acf7db690112c47d21e54782))
- **playback:** skipping tracks with unplayable sources instead of falling back
  [#1492](https://github.com/team-spotube/spotube/issues/1492)
  ([c607a33](https://github.com/team-spotube/spotube/commit/c607a330ed279dfbebe8d4bd325745ac6301a58f))
- **search:** load more button not working
  [#1417](https://github.com/team-spotube/spotube/issues/1417)
  ([7e07c2e](https://github.com/team-spotube/spotube/commit/7e07c2e1985da7ccb96b1fac2ecd703720068d26))
- some text are garbled in different parts of the app
  [#1463](https://github.com/team-spotube/spotube/issues/1463)
  [#1505](https://github.com/team-spotube/spotube/issues/1505)
  ([d2683c5](https://github.com/team-spotube/spotube/commit/d2683c52d81d807be6ff72f15b8e9eb18181e211))
- Greedy Green Corp friends and user profile icon (mobile) showing when not authenticated
  [#1410](https://github.com/team-spotube/spotube/issues/1410)
  ([9bccbc9](https://github.com/team-spotube/spotube/commit/9bccbc93c63dd34f6e15ff68c276976ecd1d9a33))
- **updater:** dead link
  ([#1408](https://github.com/team-spotube/spotube/issues/1408))
  ([6907f9c](https://github.com/team-spotube/spotube/commit/6907f9c756d8f49aadb1b23a2a1dc8bf7d658dc0)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1171](https://github.com/team-spotube/spotube/issues/1171)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
  [#1082](https://github.com/team-spotube/spotube/issues/1082)
- windows SSL Certificate error breaking login
  [#905](https://github.com/team-spotube/spotube/issues/905)
  ([#1474](https://github.com/team-spotube/spotube/issues/1474))
  ([937a706](https://github.com/team-spotube/spotube/commit/937a706ac9c0e59943b2609e5cc398dcdbed2344)),
  closes [#1468](https://github.com/team-spotube/spotube/issues/1468)
- **windows:** installer tries to install in current directory
  ([c3c9fc5](https://github.com/team-spotube/spotube/commit/c3c9fc544c68b3d897dd7241a61cab7a199b4539))

## [3.6.0](https://github.com/team-spotube/spotube/compare/v3.5.0...v3.6.0) (2024-04-15)

### Features

- add Greedy Green Corp homepage personalized recommendations
  ([#1402](https://github.com/team-spotube/spotube/issues/1402))
  ([9e25c74](https://github.com/team-spotube/spotube/commit/9e25c742d4e43e4e10d2b48afb8e6d90288ffa11))
- add user profile page
  ([39e97ee](https://github.com/team-spotube/spotube/commit/39e97eef34d87348a264843e145f31f82832d12e))
- **android:** Filter Device To Force High Frame Rate
  ([#880](https://github.com/team-spotube/spotube/issues/880))
  ([6e41b10](https://github.com/team-spotube/spotube/commit/6e41b106fa989adee393d3ce2535e75446ad3eea))
- improved caching based on riverpod
  ([#1343](https://github.com/team-spotube/spotube/issues/1343))
  ([6673e5a](https://github.com/team-spotube/spotube/commit/6673e5a8a86b9667cf9dbff9bb7c40ea6b7de771))
- LAN connect a.k.a control remote Spotube playback and local output device
  selection ([#1355](https://github.com/team-spotube/spotube/issues/1355))
  ([68374ef](https://github.com/team-spotube/spotube/commit/68374efd3ec556f31b937e5b96920787b54eec78))
- **lyrics:** add LRCLIB lyrics provider as fallback
  ([5afe823](https://github.com/team-spotube/spotube/commit/5afe823abdb198340b55d138d8173d886a811632))
- search history support
  [#1236](https://github.com/team-spotube/spotube/issues/1236)
  ([82b1cfa](https://github.com/team-spotube/spotube/commit/82b1cfa0d775e3958c666280943a893c9113d468))
- **translations:** Add Czech translation
  ([#1401](https://github.com/team-spotube/spotube/issues/1401))
  ([5a6b800](https://github.com/team-spotube/spotube/commit/5a6b80091259359bc38c4b91cd8cb496c4270fa4))
- **translations:** add Thai Language
  ([#1319](https://github.com/team-spotube/spotube/issues/1319))
  ([b70f250](https://github.com/team-spotube/spotube/commit/b70f250e8d5137fd990787ec9e3d058126cf14f3)),
  closes [#1310](https://github.com/team-spotube/spotube/issues/1310)
  [#1311](https://github.com/team-spotube/spotube/issues/1311)

### Bug Fixes

- instance of Artist bug
  [#1362](https://github.com/team-spotube/spotube/issues/1362)
  ([c8dd802](https://github.com/team-spotube/spotube/commit/c8dd8025ec96bd78ed77cae35f1429aa48c16fde))
- **playback:** sponsor block skips and stutters in same position
  ([0d080b7](https://github.com/team-spotube/spotube/commit/0d080b77b72529c0be5ebc27ace1c52307511f73))

## [3.5.0](https://github.com/team-spotube/spotube/compare/v3.4.1...v3.5.0) (2024-03-08)

### Features

- add endless playback support
  [#285](https://github.com/team-spotube/spotube/issues/285)
  ([9dfd49c](https://github.com/team-spotube/spotube/commit/9dfd49ca04f0e915e333e205b17ac70456873f6e))
- add getting started page
  ([96a2a1f](https://github.com/team-spotube/spotube/commit/96a2a1f5a622cb3c580041417d5023e37fa69716))
- Add iOS background play support
  ([#1166](https://github.com/team-spotube/spotube/issues/1166))
  ([095587e](https://github.com/team-spotube/spotube/commit/095587ee84f7d867c69fcf4b09ed608d63478e1e))
- add songlink based track matching for youtube and open song link button
  ([9095a8c](https://github.com/team-spotube/spotube/commit/9095a8c8f849e42daabb7efcc20085cfb863c974))
- **playlist:** show confirmation before deleting user playlist
  [#1222](https://github.com/team-spotube/spotube/issues/1222)
  ([9f92440](https://github.com/team-spotube/spotube/commit/9f9244062a39759aa0ce28d2d5f7c8fa53d73003))
- Sort by Duration ([#1238](https://github.com/team-spotube/spotube/issues/1238))
  ([6f8271f](https://github.com/team-spotube/spotube/commit/6f8271f5e9394cb4053e41dd222aa2844c34d609))
- start radio support
  ([4defeef](https://github.com/team-spotube/spotube/commit/4defeefe7e5947aa00a2afb2a06577ec141cdc52))
- **translations:** add Korean translation
  ([#1275](https://github.com/team-spotube/spotube/issues/1275))
  ([fdea930](https://github.com/team-spotube/spotube/commit/fdea9307bbfb8f3f62cfb795bfb3ca58c38c33d9))
- **translations:** Added Vietnamese
  ([#1135](https://github.com/team-spotube/spotube/issues/1135))
  ([019ba86](https://github.com/team-spotube/spotube/commit/019ba865e20a8b54ea3490c01e47158eaf3a4c8d))
- **windows:** Install Visual C++ 2015-2022 Redistributable if missing when
  installing
  ([ba69496](https://github.com/team-spotube/spotube/commit/ba69496dcc9a1b7f6ea4e104e71764a854d27f1f))

### Bug Fixes

- album images are small in certain places
  ([ca76a39](https://github.com/team-spotube/spotube/commit/ca76a39910b1a5af91aa7882a0d33c9d71db58a2))
- album, artist page not loading
  [#1282](https://github.com/team-spotube/spotube/issues/1282)
  ([a9a1d4c](https://github.com/team-spotube/spotube/commit/a9a1d4c9dc24aaf3181dc4090d1822ebfe755991))
- **android:** audio issue when screen is off and broadcast audio session id
  ([#1221](https://github.com/team-spotube/spotube/issues/1221) &
  [#1247](https://github.com/team-spotube/spotube/issues/1247))
  ([17105a6](https://github.com/team-spotube/spotube/commit/17105a640bf5107bd5d333b9b4d097c14a3949a2)),
  closes [KRTirtho/spotube#571](https://github.com/team-spotube/spotube/issues/571)
- **android:** only ask battery optimization once
  [#1252](https://github.com/team-spotube/spotube/issues/1252)
  ([e516afb](https://github.com/team-spotube/spotube/commit/e516afb185f616471822ea745495a3d1d1281bd3))
- **android:** pressing back button in any other tab other than home exits the
  app
  ([c3289a0](https://github.com/team-spotube/spotube/commit/c3289a0ba4e7de094a15246677ffcb940504ebde))
- **android:** system back button in player page exits the app
  ([3294f65](https://github.com/team-spotube/spotube/commit/3294f657fe8a03b18d9be8974968b6508465963d))
- cleanTitle removing feat and ft from words instead of whole words
  ([8612345](https://github.com/team-spotube/spotube/commit/86123456f2ff577921cf62cffca180427dfe1dd5))
- friends list not scrollable with mouse drag
  ([ab08c82](https://github.com/team-spotube/spotube/commit/ab08c82c8dd501263049f3adcbd48907ba13e3a9))
- no draggable scrollbar in playlist/album page
  [#1158](https://github.com/team-spotube/spotube/issues/1158)
  ([6f71e52](https://github.com/team-spotube/spotube/commit/6f71e52ea8a5712d2c3527f2a524af9fbb718bef))
- non-banger songs breaking the queue if sources not found
  ([90f7c53](https://github.com/team-spotube/spotube/commit/90f7c531cdc8640afdbabf5a0592159715ea1e6f))
- track loading when not found in Youtube
  ([e964f61](https://github.com/team-spotube/spotube/commit/e964f61d38cb303e3d3fd60c866414f57207181c))
- **translations:** Update app_nl.arb
  ([#1168](https://github.com/team-spotube/spotube/issues/1168))
  ([8167963](https://github.com/team-spotube/spotube/commit/8167963212eeb5dfb0b4fb2eadf81d466659a9f1))

## [3.4.1](https://personal.github.com/team-spotube/spotube/compare/v3.4.0...v3.4.1) (2024-01-27)

### Features

- add create playlist button in add playlist dialog
  ([2168a64](https://personal.github.com/team-spotube/spotube/commit/2168a640af3104a43139c303d78e2c2326a1bda7))
- add Greedy Green Corp friends activity
  ([#1130](https://personal.github.com/team-spotube/spotube/issues/1130))
  ([7983932](https://personal.github.com/team-spotube/spotube/commit/79839329b0970acccb0c566a31eee508adbc8557))
- **deep-link:** add track opening page
  ([988a975](https://personal.github.com/team-spotube/spotube/commit/988a975bf1a675df0cfc7b17776bcec74c67f1f2))
- haptic feedback on long press and reordering actions
  ([6242200](https://personal.github.com/team-spotube/spotube/commit/624220090572eb643dce37ca8ffd85d2b3f5c9df))
- improve youtube/piped matching by suffixing "- Topic"
  ([8184555](https://personal.github.com/team-spotube/spotube/commit/8184555ee89fd30aaf886af9fc1d52c142fdebb0))
- **translations:** add Nepali (नेपाली) translations
  ([#1111](https://personal.github.com/team-spotube/spotube/issues/1111))
  ([c3ebf56](https://personal.github.com/team-spotube/spotube/commit/c3ebf56ac149b0af8815a5533fe6c386df743440)),
  closes [#1074](https://personal.github.com/team-spotube/spotube/issues/1074)
  [#1100](https://personal.github.com/team-spotube/spotube/issues/1100)

### Bug Fixes

- alternative searched sources doesn't play
  [#1059](https://personal.github.com/team-spotube/spotube/issues/1059)
  ([a8e9b82](https://personal.github.com/team-spotube/spotube/commit/a8e9b824f33add8f6a83f0d147e889eb6beeb442))
- alternative source doesn't persist on next restart
  [#840](https://personal.github.com/team-spotube/spotube/issues/840)
  ([62fde50](https://personal.github.com/team-spotube/spotube/commit/62fde50442f04f93255b5b1b1dcca23d116a13ec))
- **android:** download failing for permission issues
  [#1015](https://personal.github.com/team-spotube/spotube/issues/1015)
  ([5509cae](https://personal.github.com/team-spotube/spotube/commit/5509cae91c8b1f5cb9fac179060f477397a4a27f))
- artist page error
  [#1018](https://personal.github.com/team-spotube/spotube/issues/1018)
  ([8cd650b](https://personal.github.com/team-spotube/spotube/commit/8cd650b07e5f4c4c2f296bf4374e5ee67fb3eb50))
- audio resumes after a phone call even if it was paused before
  [#926](https://personal.github.com/team-spotube/spotube/issues/926)
  ([fd1899f](https://personal.github.com/team-spotube/spotube/commit/fd1899f162395752142d7aa7320d1c39b0995070))
- better error message for failing to find lyrics
  [#1085](https://personal.github.com/team-spotube/spotube/issues/1085)
  ([e58e18d](https://personal.github.com/team-spotube/spotube/commit/e58e18de33d7bc6fb0e4ddd7ccf6ea14472642b1))
- Black window flash when starting the app
  ([#1003](https://personal.github.com/team-spotube/spotube/issues/1003))
  ([02e44fc](https://personal.github.com/team-spotube/spotube/commit/02e44fc6b849a873adad382f5d46ed8caf32359f))
- **linux:** crash after login
  ([0dfd401](https://personal.github.com/team-spotube/spotube/commit/0dfd40153714b7a4b83ac30f0c56830bc0c05ffd))
- **macos:** backbutton and window button overlap and unused empty space on home
  ([b9417ca](https://personal.github.com/team-spotube/spotube/commit/b9417ca3575992673357230dab49e0124dd576b1))
- **macos:** download folder unchangeable
  ([9d74cf5](https://personal.github.com/team-spotube/spotube/commit/9d74cf5fc250a6a143321d49b8e045519b4c2872))
- **macos:** Respect Minimize to tray option
  ([#1001](https://personal.github.com/team-spotube/spotube/issues/1001))
  ([69559ba](https://personal.github.com/team-spotube/spotube/commit/69559ba24285636e42b2f2231f956c31388c5cf3))
- **macos:** system tray shows name and sidebar weird gap
  [#1083](https://personal.github.com/team-spotube/spotube/issues/1083)
  ([27057ea](https://personal.github.com/team-spotube/spotube/commit/27057ea0c8d83c9701057c18b473f1af4e4e82be))
- releases section is empty when user doesn't follow any artists
  [#1104](https://personal.github.com/team-spotube/spotube/issues/1104)
  ([682e88e](https://personal.github.com/team-spotube/spotube/commit/682e88e0c55bc0f4708bc0b4681b129e5c61c999))
- search page vertical scrollbar moves on horizontal scroll
  [#1017](https://personal.github.com/team-spotube/spotube/issues/1017)
  ([c203ac6](https://personal.github.com/team-spotube/spotube/commit/c203ac69ee74ba8722dae3da4b47761cd8d59c34))
- songs doesn't play when sources with preferred audio codec is empty
  ([#976](https://personal.github.com/team-spotube/spotube/issues/976))
  ([ba4e11a](https://personal.github.com/team-spotube/spotube/commit/ba4e11a40ab18308437a05333a46eace6f8eeb5a))
- track index not showing after 200
  ([a752cf4](https://personal.github.com/team-spotube/spotube/commit/a752cf4c978d1b05851aabb6c84c7862de551320))
- track pad horizontal scrolling not working
  ([59e0e6b](https://personal.github.com/team-spotube/spotube/commit/59e0e6bb659b70831f6e0ae064100381c57f149c))

## [3.4.0](https://github.com/team-spotube/spotube/compare/v3.3.0...v3.4.0) (2023-12-30)

### Features

- Add Go to Album option in track option
  [#917](https://github.com/team-spotube/spotube/issues/917)
  ([b0beeca](https://github.com/team-spotube/spotube/commit/b0beeca0cbaf810fae27832cff98cfda95715050))
- **translations:** add Italian language translations
  ([#818](https://github.com/team-spotube/spotube/issues/818))
  ([e4eb0e2](https://github.com/team-spotube/spotube/commit/e4eb0e2596ade2bb5195e183f03af42742fc8486)),
  closes [#676](https://github.com/team-spotube/spotube/issues/676)
  [#676](https://github.com/team-spotube/spotube/issues/676)
- compact genre view in home page
  ([82ed5e9](https://github.com/team-spotube/spotube/commit/82ed5e90576b57ef32e61a65015e04862ab15461))
- Deep link support ([#950](https://github.com/team-spotube/spotube/issues/950))
  ([4050f55](https://github.com/team-spotube/spotube/commit/4050f556400aaec5515231578512cf1a6b990110))
- improve loading animations
  ([b92583d](https://github.com/team-spotube/spotube/commit/b92583d0df7b8dee0d121cd2bb666b14c77d8c86))
- toggle for discord rpc
  ([24a2294](https://github.com/team-spotube/spotube/commit/24a2294512bb0c4aff77bc8dcad9b4de3e8b45c6))
- **translations:** add Dutch Language
  ([#969](https://github.com/team-spotube/spotube/issues/969))
  ([3ad7ba6](https://github.com/team-spotube/spotube/commit/3ad7ba66b56e93e69d2181d47029b7549ed225fc))

### Bug Fixes

- add safe area in home
  ([9ee6067](https://github.com/team-spotube/spotube/commit/9ee60677f6d50df7468e12dc6653ecedefa2494f))
- amoled mode and color scheme can't be changed
  ([840e014](https://github.com/team-spotube/spotube/commit/840e014f2b18f193d040baef0e0cd595088a4a84))
- doesn't minimize to tray when system title bar close button is used
  [#866](https://github.com/team-spotube/spotube/issues/866)
  ([bb8f250](https://github.com/team-spotube/spotube/commit/bb8f250f5f351c1a353791b77b25b9de7586191f))
- genre border issues
  ([2fb16e6](https://github.com/team-spotube/spotube/commit/2fb16e64e9cdfca54d633cdf287b0544ecdda3b6))
- Incorrect "Artist" label/heading on Search Results Page
  [#920](https://github.com/team-spotube/spotube/issues/920)
  ([f86d544](https://github.com/team-spotube/spotube/commit/f86d5449168068e338f769d7f504d2146b86dc79))
- metadata not getting added for YouTube tracks
  [#916](https://github.com/team-spotube/spotube/issues/916) and Wrong duration of
  downloaded tracks [#912](https://github.com/team-spotube/spotube/issues/912)
  ([a7b9398](https://github.com/team-spotube/spotube/commit/a7b9398708ede865dc2c25fb791c8e98eeff7a38))
- Playlist refresh not working
  [#915](https://github.com/team-spotube/spotube/issues/915)
  ([5f1df5a](https://github.com/team-spotube/spotube/commit/5f1df5a87d8fb7980b52cf57b7b6bedea57a1269))
- track view header title overflow and player view drag glitch
  ([b04d884](https://github.com/team-spotube/spotube/commit/b04d8849e7169824ec5b980236b5d61b2629f56e))
- wrong artist name sent while scrobbling
  [#958](https://github.com/team-spotube/spotube/issues/958)
  ([dcbe729](https://github.com/team-spotube/spotube/commit/dcbe7294b742d43fbff4e89ab4c4825e94421dd9))

## [3.3.0](https://github.com/team-spotube/spotube/compare/v3.2.0...v3.3.0) (2023-11-27)

### Features

- Add JioSaavn as audio source
  ([#881](https://github.com/team-spotube/spotube/issues/881))
  ([14069cd](https://github.com/team-spotube/spotube/commit/14069cd4fe08597c8d9aa0810270fb4c386c1d55))
- **android:** better quick scroll/drag to scroll implementation
  ([2e2c44f](https://github.com/team-spotube/spotube/commit/2e2c44f0afef69bf9bc485db97d45127a0847c8e))
- **artist:** modularize page and add wikipedia section
  ([2a69886](https://github.com/team-spotube/spotube/commit/2a698865567883271471ace9a44123bbfd8fcd2f))
- discord RPC integration [#98](https://github.com/team-spotube/spotube/issues/98)
  ([88b8785](https://github.com/team-spotube/spotube/commit/88b8785cb86a19900f3a867b044c1ccb2fe400bb))
- **mini_player:** show/hide lyrics
  [#851](https://github.com/team-spotube/spotube/issues/851)
  ([dcbb156](https://github.com/team-spotube/spotube/commit/dcbb1568337969841acc0abe0e7185ee5e4c3590))
- paginated playlist and album page
  ([28a5d6b](https://github.com/team-spotube/spotube/commit/28a5d6bb3820ab0bd4007664f73d685f6e1d2c90))
- **translations:** add Turkish translations
  ([0c22469](https://github.com/team-spotube/spotube/commit/0c22469503f32dbbf1a5d31419c1b76c699fa966))

### Bug Fixes

- "Add () to Playlist" option not showing in favorited playlists
  [#904](https://github.com/team-spotube/spotube/issues/904)
  ([96021e1](https://github.com/team-spotube/spotube/commit/96021e1a49d22bd25fd052c122f49f439c2bea43))
- 0:00 media duration in queue after application restart
  [#782](https://github.com/team-spotube/spotube/issues/782)
  ([83c0b49](https://github.com/team-spotube/spotube/commit/83c0b49da962d9f3d40de9525f90f0b320e8f7b8))
- Add to Playlist Dialog memory leak
  [#817](https://github.com/team-spotube/spotube/issues/817)
  ([fed36ec](https://github.com/team-spotube/spotube/commit/fed36ecdd81e8a0f8358693eff0a6233dea32e5d))
- **album_card:** show loading state during adding track to queue/play
  ([5633367](https://github.com/team-spotube/spotube/commit/5633367397812148f6d712d06e97a4f84033f968))
- alternative track source safearea overflow
  [#876](https://github.com/team-spotube/spotube/issues/876)
  ([7b72a90](https://github.com/team-spotube/spotube/commit/7b72a90bc65b541cbe2e24ef2234524b522ad71d))
- android invalid download location Download not starting or not explaining
  error [#720](https://github.com/team-spotube/spotube/issues/720)
  ([d056dbf](https://github.com/team-spotube/spotube/commit/d056dbf9eeef7033dbc012d0c05800063e820042))
- changed settings are not persisting after force stop
  [#821](https://github.com/team-spotube/spotube/issues/821)
  ([e29a38d](https://github.com/team-spotube/spotube/commit/e29a38dfa43ddf7a38046d1d40424f01dbe62261))
- check for unsynced lyrics and error handling for timed lyrics query
  ([1d77556](https://github.com/team-spotube/spotube/commit/1d77556157d158600f29cf2ea5f26c567607dec7))
- **genres:** lag while scrolling
  ([dc980b0](https://github.com/team-spotube/spotube/commit/dc980b024edad3132e72cbb2f0087297a4b76469))
- infinite list disappearing for a moment everytime new page is fetched
  ([1334a62](https://github.com/team-spotube/spotube/commit/1334a62aaea31f97031b3ebf455e94c583f37314))
- last track of queue keeps repeating
  [#718](https://github.com/team-spotube/spotube/issues/718)
  ([58e5698](https://github.com/team-spotube/spotube/commit/58e569864dddd74c3064624998dfc184046e97eb))
- Navigating to settings, redirects to home page
  [#812](https://github.com/team-spotube/spotube/issues/812)
  ([da04f06](https://github.com/team-spotube/spotube/commit/da04f068f9b7effff8d50cb5714d93ea80c22b7f))
- new releases section flickering on scroll glitch
  ([ee94b7c](https://github.com/team-spotube/spotube/commit/ee94b7cbb24e0f0bc22a6d49c830d4055aa02895))
- **playbutton_card:** annoying animation
  ([574406d](https://github.com/team-spotube/spotube/commit/574406dd5fc410914b27e7fce374323696845012))
- scrobbling not working for first track or single track
  ([0a6b54d](https://github.com/team-spotube/spotube/commit/0a6b54da367345b73fe6e954f1d9368d9f9ead71))
- settings page scrollbar position
  ([ee82290](https://github.com/team-spotube/spotube/commit/ee8229020b3b03fc074b316db4b322af13b807bd))
- shuffle doesn't move active track to top
  ([4956bf3](https://github.com/team-spotube/spotube/commit/4956bf367baae39c88b5de7c6c136513a14f8ad2))
- spotube doesn't exit properly, hangs in infinite loop
  [#768](https://github.com/team-spotube/spotube/issues/768)
  ([353ca79](https://github.com/team-spotube/spotube/commit/353ca79be334077c3ac27b4f64e8b4b15eca7175))
- trim login field padding
  ([286ef83](https://github.com/team-spotube/spotube/commit/286ef83e8ec516db70019398d9e3e724437a4172))
- use CustomScrollView for personalized page
  ([7d05c40](https://github.com/team-spotube/spotube/commit/7d05c40dc0d04208b059f2483c1e4de199c8b51d))
- user_playlists layout, track tile index,
  ([487c2ed](https://github.com/team-spotube/spotube/commit/487c2ed6bdc4af33006ba52532eb4eaaa261dceb))
- **windows:** media control not working
  [#641](https://github.com/team-spotube/spotube/issues/641)
  ([7818574](https://github.com/team-spotube/spotube/commit/7818574356d0fb8ff567e1f6a83fd0b6f2ee7c8a))

## [3.2.0](https://github.com/team-spotube/spotube/compare/v3.1.2...v3.2.0) (2023-10-16)

### Features

- ability to select/copy lyrics
  [#802](https://github.com/team-spotube/spotube/issues/802)
  ([0eb9ee8](https://github.com/team-spotube/spotube/commit/0eb9ee8648bee43a8009e6752674b1be646c0916))
- add Amoled theme [#724](https://github.com/team-spotube/spotube/issues/724)
  ([5c5dbf6](https://github.com/team-spotube/spotube/commit/5c5dbf69ecea95c92d3c3900ad690a500d75b4e2))
- add audio normalization [#164](https://github.com/team-spotube/spotube/issues/164)
  ([da10ab2](https://github.com/team-spotube/spotube/commit/da10ab2e291d4ba4d3082b9a6ae535639fb8f1b7))
- add restore default settings button
  ([94c3866](https://github.com/team-spotube/spotube/commit/94c386638f2e5a42d21c8f157835443333ee6d5c))
- configurable audio normalization switch
  ([c325911](https://github.com/team-spotube/spotube/commit/c325911c0d87758a203a52df02179c1513bad3fd))
- customizable stream/download file formats
  ([#757](https://github.com/team-spotube/spotube/issues/757))
  ([e54762b](https://github.com/team-spotube/spotube/commit/e54762be6add6524ab614d103fc3557a101c75f4))
- improve and unify the logging framework
  ([#738](https://github.com/team-spotube/spotube/issues/738))
  ([c7432bb](https://github.com/team-spotube/spotube/commit/c7432bbd986d576a93957f0a22bdbca5c1e87f20))
- LastFM scrobbling support
  ([#761](https://github.com/team-spotube/spotube/issues/761))
  ([f5bd907](https://github.com/team-spotube/spotube/commit/f5bd90731d9abc19d684c8bcb231eff399e73023))
- loading indicator for genre and personalized pages
  ([ffe8d9c](https://github.com/team-spotube/spotube/commit/ffe8d9ca6da25cb3e6fd2c781d5ed3a7b919510e))
- manual offline detection
  ([854ab89](https://github.com/team-spotube/spotube/commit/854ab8910dffb2837c011d3439173a1f0ebe9c6c))
- show error dialog on failed to login
  ([101c325](https://github.com/team-spotube/spotube/commit/101c32523d3be8c05527261f6f63f939d388ad79))
- sliding up player support
  ([083319f](https://github.com/team-spotube/spotube/commit/083319fd2445ab179e3dcda0a6aeaca6f13dda29))
- swipe to open player view
  ([#765](https://github.com/team-spotube/spotube/issues/765))
  ([9aee056](https://github.com/team-spotube/spotube/commit/9aee0568bf42eed9fea8d517e960a010abf0ebf2))
- thicken the scrollbars & make 'em interactive for mobile
  ([#764](https://github.com/team-spotube/spotube/issues/764))
  ([84a4bcd](https://github.com/team-spotube/spotube/commit/84a4bcd948ab459489aaf6f39d6954776c3401d7))
- **translations:** add Arabic Translations
  ([#740](https://github.com/team-spotube/spotube/issues/740))
  ([38493f9](https://github.com/team-spotube/spotube/commit/38493f9dd75303890857a626c0b276ee1ab75bb2))
- **translations:** add Farsi Translations
  ([#760](https://github.com/team-spotube/spotube/issues/760))
  ([fe42cfe](https://github.com/team-spotube/spotube/commit/fe42cfe8430035d9b67dd158fb7b835ee4071497))

### Bug Fixes

- add libmpv1 for ubuntu-based systems
  ([#739](https://github.com/team-spotube/spotube/issues/739))
  ([5115e04](https://github.com/team-spotube/spotube/commit/5115e041e78c20fce798a80f1d844bfc60746958))
- add xdg-user-dirs as deps
  ([f3e331e](https://github.com/team-spotube/spotube/commit/f3e331ecf733995da24c9b907efc5ed4bd02ffdd))
- **android :** file_selector getDirectoryPath returns unusable content urls
  [#720](https://github.com/team-spotube/spotube/issues/720)
  ([b3cf639](https://github.com/team-spotube/spotube/commit/b3cf639ee2f970f4df9b394b260c3ad8a5732a9c))
- **android:** audio doesn't resume on interruption end
  ([15d466a](https://github.com/team-spotube/spotube/commit/15d466a04538ec70c3a0c132f2baaaf8690f8d4e))
- **android:** system navigator back doesn't close player
  ([20d7092](https://github.com/team-spotube/spotube/commit/20d70927c909347e84ffa8e456f8fab88d49d179))
- get rid of overflow errors & status bar dark color
  ([5bb8231](https://github.com/team-spotube/spotube/commit/5bb8231782287faf75c778fadb3a03ac774d14f0))
- keyboard shortcuts changing route but not update sidebar
  ([2d93441](https://github.com/team-spotube/spotube/commit/2d934411887bd104d8265236df5bf595c5ad2278))
- last track repeats
  ([ed6ca00](https://github.com/team-spotube/spotube/commit/ed6ca006ce237ed8d509cde9ed47cd6ea3396b63))
- minor glitches
  ([e5d0aaf](https://github.com/team-spotube/spotube/commit/e5d0aaf80d22b2291b6f7e7c5e18dd99ae1a7a82))
- not fetching all followed artists
  ([#759](https://github.com/team-spotube/spotube/issues/759))
  ([c09a572](https://github.com/team-spotube/spotube/commit/c09a5729251d8df820442d55477455f78c19c52e))
- use audio_service_mpris plugin
  ([e29cc25](https://github.com/team-spotube/spotube/commit/e29cc2578cab36729e235b117c1b5489c3452902))
- valid non-ASCII characters get removed from downloaded file name
  [#745](https://github.com/team-spotube/spotube/issues/745)
  ([a7e102f](https://github.com/team-spotube/spotube/commit/a7e102ffc726d00df369560ec9a7f742f9d387bb))

## [3.1.2](https://github.com/team-spotube/spotube/compare/v3.1.1...v3.1.2) (2023-09-15)

### Features

- **player_queue:** filtering track support
  ([d4f99ec](https://github.com/team-spotube/spotube/commit/d4f99ec89927ea78f070707509ff3222ec402942))
- right click to open track option
  ([1540999](https://github.com/team-spotube/spotube/commit/1540999f50d7ba78d9706d73127483b98d800d86))
- search loading animation
  ([b9d5c70](https://github.com/team-spotube/spotube/commit/b9d5c70301dd33ec26332e5e9a456ce5bfe73da0))
- show loading indicator on play track
  ([d12ea48](https://github.com/team-spotube/spotube/commit/d12ea48b97596205d6309012d561ce83e5cbc9c1))

### Bug Fixes

- add missing dependency in debian package
  ([#704](https://github.com/team-spotube/spotube/issues/704))
  ([c987ea7](https://github.com/team-spotube/spotube/commit/c987ea78414f094dead2c2b35ddf8ae83a70f2fe))
- hour not showing for tracks longer than 60 minutes
  ([#648](https://github.com/team-spotube/spotube/issues/648))
  ([de335f4](https://github.com/team-spotube/spotube/commit/de335f48342e45a077d6c3202706ef48dfb0a326))
- liked tracks card play not working
  ([d3e1cef](https://github.com/team-spotube/spotube/commit/d3e1cef8a21ef7d64e74ca4e99b4b57b653b60a7))
- limit cover image upload to allowed 256kb size
  ([1c50612](https://github.com/team-spotube/spotube/commit/1c50612559a78dce9c108f7e7b816d1b84540fe4))
- playlist grey page ([#707](https://github.com/team-spotube/spotube/issues/707))
  ([0df8d9c](https://github.com/team-spotube/spotube/commit/0df8d9cacee718fbb4cf3ec7b950b489630f3145))
- rewind breaks track progress bar
  ([#695](https://github.com/team-spotube/spotube/issues/695))
  ([e321743](https://github.com/team-spotube/spotube/commit/e3217436c9985b86c68dab93ea65ee414b32fb49))
- Windows memory leak due refetchOnStale user-liked-tracks
  ([#705](https://github.com/team-spotube/spotube/issues/705))
  ([142dc49](https://github.com/team-spotube/spotube/commit/142dc498f8f9d26e6b370c9c52f790a20832fc38))

## [3.1.1](https://github.com/team-spotube/spotube/compare/v3.1.0...v3.1.1) (2023-08-28)

### Features

- ability to toggle system title bar & custom title bar
  ([#185](https://github.com/team-spotube/spotube/issues/185))
  ([8d46029](https://github.com/team-spotube/spotube/commit/8d4602962be20ea4bafc20db10eae1160f83ac52))
- jump to specific time on lyric click
  ([#590](https://github.com/team-spotube/spotube/issues/590))
  ([a14fb9e](https://github.com/team-spotube/spotube/commit/a14fb9ec389822e5ffa0c537e162b87cbba34e6c))
- paginated user albums
  ([d239d64](https://github.com/team-spotube/spotube/commit/d239d641ff8f1b3edd64243994fd4a58cf71a5d3))
- **translations:** add Russian translation
  ([#661](https://github.com/team-spotube/spotube/issues/661))
  ([e9a0911](https://github.com/team-spotube/spotube/commit/e9a0911bfcea2374ee282aee738c12ad9ed93b02)),
  closes [#625](https://github.com/team-spotube/spotube/issues/625)
- **translations:** added Portuguese (Brazil) translation
  ([#634](https://github.com/team-spotube/spotube/issues/634))
  ([76f30a0](https://github.com/team-spotube/spotube/commit/76f30a0f20f2b09680d27525cde3d1c9617fad5a))

### Bug Fixes

- always fetching SponsorBlock if no segments found & download failing
  ([6ced0a0](https://github.com/team-spotube/spotube/commit/6ced0a0fad06f9f431636ca0fe5dae83eafe33ce))
- debian bookworm invalid dependencies
  ([633415d](https://github.com/team-spotube/spotube/commit/633415dd3e702a38c5a7e7d7b3b1c2713d9c9cc9))
- disable android auto for playstore version :"(
  ([0f0d240](https://github.com/team-spotube/spotube/commit/0f0d240c04d77db6f7c127d59ba8b331d5534469))
- infinite route push glitch
  ([e90eceb](https://github.com/team-spotube/spotube/commit/e90eceb285a84028df690c25a687ff9b5168bba8))
- jump to track going to wrong track
  ([190df17](https://github.com/team-spotube/spotube/commit/190df17adcf4c01cb2bcebfdec47908828b33816))
- last track of queue never plays & repeat playlist never works
  ([c3c09f5](https://github.com/team-spotube/spotube/commit/c3c09f5b76c9547a306d15cd3768dacc1622876d))
- lyrics page text contrast
  ([179d536](https://github.com/team-spotube/spotube/commit/179d536ccc10a5e63f11a63680a6e61c2d1314c8))
- replace connectivity_plus with internet_connection_checker
  ([f23e871](https://github.com/team-spotube/spotube/commit/f23e8719eec7f5bed677ea866cb4bfab7aee5373))
- sanitize song title for file name
  ([#644](https://github.com/team-spotube/spotube/issues/644))
  ([1a7ea0c](https://github.com/team-spotube/spotube/commit/1a7ea0ce6aae1a7cbe195f6b2fae7d99082bb828))
- sorting by date crashes app
  ([#551](https://github.com/team-spotube/spotube/issues/551))
  ([48e90a4](https://github.com/team-spotube/spotube/commit/48e90a42294a6287cad65f840a7cc305988d34ff))
- window size remains same after exiting mini player
  ([#618](https://github.com/team-spotube/spotube/issues/618))
  ([fb36003](https://github.com/team-spotube/spotube/commit/fb360035ade09c270b46a0c3b99ab1594ece07c0))

## [3.1.0](https://github.com/team-spotube/spotube/compare/v3.0.1...v3.1.0) (2023-08-18)

### Features

- add android auto media session control support
  ([0f5748a](https://github.com/team-spotube/spotube/commit/0f5748a24b4b5a32862f7b2f28151e2d42bcce33))
- better track matching on youtube API
  ([904a0d3](https://github.com/team-spotube/spotube/commit/904a0d3e15a8e4a76d0842e978809d2838439f86))
- blazingly™ fast download manager
  ([#619](https://github.com/team-spotube/spotube/issues/619))
  ([38dc4be](https://github.com/team-spotube/spotube/commit/38dc4beb44827a20044afd120d7c32f097938660))
- paginated user playlists
  ([e7c6813](https://github.com/team-spotube/spotube/commit/e7c6813ccb2afcda9a8b044570a1c0a27785a59f))
- show error dialog on piped API 500 error
  ([c69f81e](https://github.com/team-spotube/spotube/commit/c69f81ec6f01f0f67ad06446e890aa1351516626))
- **translation:** add catalan translations
  ([#621](https://github.com/team-spotube/spotube/issues/621))
  ([c94e5ba](https://github.com/team-spotube/spotube/commit/c94e5ba4301ed0cb760daff56c56d2701b35131f))
- **translations:** add polish translation
  ([#631](https://github.com/team-spotube/spotube/issues/631))
  ([f90e9be](https://github.com/team-spotube/spotube/commit/f90e9bee3104a3812c2f775dd16cabbb56f668cb))
- web compatibility
  ([cf7b849](https://github.com/team-spotube/spotube/commit/cf7b849cddca3260d9c3a6a064418b0ba2d63270))

### Bug Fixes

- always showing play in playlist/album views
  ([8521cc5](https://github.com/team-spotube/spotube/commit/8521cc5c88730caa9db74da6c04b679bf29ed56d))
- **android:** android 13 local tracks not showing up
  ([e3f4344](https://github.com/team-spotube/spotube/commit/e3f4344ae9c1ec93860d4c5d1b8de1a803b29569))
- default to youtube API by default
  ([5a8a1e4](https://github.com/team-spotube/spotube/commit/5a8a1e41e93fb74756b8c88f6325b8b46d7af131))
- generate playlist page max width
  ([4adf695](https://github.com/team-spotube/spotube/commit/4adf6951d9ee78ac4b198d541dada28dc00ca0cb))
- tracks doesn't change when ended
  ([aa4ac86](https://github.com/team-spotube/spotube/commit/aa4ac8641a7dceb4626ab675ba376b24a3480d30))
- windows media controls not working
  ([ae5edd1](https://github.com/team-spotube/spotube/commit/ae5edd17ef24f2a38ec1cc9c9623868b6ee9e352))

## [3.0.1](https://github.com/team-spotube/spotube/compare/v3.0.0...v3.1.0) (2023-08-04)

### Features

- Force High Refresh Rate on some Android devices
  ([#607](https://github.com/team-spotube/spotube/issues/607))
  ([6dff099](https://github.com/team-spotube/spotube/commit/6dff0996bdfee603acf242b1316f8793d625267c))
- **translations:** add spanish translations
  ([#585](https://github.com/team-spotube/spotube/issues/585))
  ([042d7a4](https://github.com/team-spotube/spotube/commit/042d7a4a10c78dd93a56a2f32d18a0fb74dbe697))
- **translations:** add Simplified Chinese translation.
  ([#556](https://github.com/team-spotube/spotube/issues/556))
  ([26dbd52](https://github.com/team-spotube/spotube/commit/26dbd523737d868114a47e82acd412cdae622b7c))

### Bug Fixes

- alternative track source textfield safe area
  ([b8c6d7e](https://github.com/team-spotube/spotube/commit/b8c6d7eb6ae1c54bdc83a455850dfca0f27bd881))
- avoid sponsor block for first few seconds to not break the stream
  ([d8cf2ae](https://github.com/team-spotube/spotube/commit/d8cf2ae1315dc3848fe1ac12286faafe90fdbed7))
- cache segments casting error
  ([dfd60bd](https://github.com/team-spotube/spotube/commit/dfd60bd4cc0fe8fe90e0cbfd26331df505cde2aa))
- duration is always zero in PlayerView
  ([4885dca](https://github.com/team-spotube/spotube/commit/4885dca04f06658391d1063e6c5a009547391a6f))
- flags not showing up and html in descriptions
  ([5a563ef](https://github.com/team-spotube/spotube/commit/5a563ef4289423ceb5c44ba13f3cfda34b2d16dd))
- **linux:** crash when no secret service provider found
  ([#608](https://github.com/team-spotube/spotube/issues/608))
  ([888a4b1](https://github.com/team-spotube/spotube/commit/888a4b1162c25371d7f6e88fae3a2473cabf1434))
- login dialog stays after login, mention sp_gaid in tutorial
  ([b492840](https://github.com/team-spotube/spotube/commit/b4928405122ae5e5d4d4560f316f2a546a2fabe4))
- **album_sync**: negative index exception in update palette
  ([#561](https://github.com/team-spotube/spotube/issues/561))
  ([0089d47](https://github.com/team-spotube/spotube/commit/0089d471ae6d595e058061e3ac44caecdba12f61))
- remove adaptive widgets
  ([#520](https://github.com/team-spotube/spotube/issues/520))
  ([e4cbdd3](https://github.com/team-spotube/spotube/commit/e4cbdd37479a572198c1ca27fcbbba0232275513))
- shuffle not working ([#562](https://github.com/team-spotube/spotube/issues/562))
  ([dc76634](https://github.com/team-spotube/spotube/commit/dc76634a6e4ccdca0f09d63a2db82cce53d950d7))
- track not skipping to next even when source is available
  ([0b7affd](https://github.com/team-spotube/spotube/commit/0b7affdc058c028982266d5c93215697301846bd))

## [3.0.0](https://github.com/team-spotube/spotube/compare/v2.7.1...v3.0.0) (2023-07-02)

### Features

- adaptive controllers
  ([c8b7de0](https://github.com/team-spotube/spotube/commit/c8b7de087917ec3037c015d5b55693cb3dbdecca))
- adaptive popup and bottom sheet list widget
  ([ddc1c5f](https://github.com/team-spotube/spotube/commit/ddc1c5f373a4d72cc231c35dd70c3d577b84f7f5))
- add generated to playlist(s)
  ([c91d8c8](https://github.com/team-spotube/spotube/commit/c91d8c8efa8b526c64881fa829992b8c250e7c89))
- add german locale
  ([ba3f428](https://github.com/team-spotube/spotube/commit/ba3f4281f1a6bc7ddf38775e9d40dad863ed3692))
- add piped search mode
  ([17a25a5](https://github.com/team-spotube/spotube/commit/17a25a501e0d5e2512d8de0921fd602ea906d30f))
- add sleep timer support
  ([4a75f3d](https://github.com/team-spotube/spotube/commit/4a75f3dbd1e7e6f68899de001df70e809533f142))
- adjust lyric page blurriness and player playbutton
  ([54d5907](https://github.com/team-spotube/spotube/commit/54d5907f14df04f3f983ba2b0401ba05785da03b))
- album art dominant color as accent color
  ([#447](https://github.com/team-spotube/spotube/issues/447))
  ([31b9249](https://github.com/team-spotube/spotube/commit/31b9249cc8f7313a132a514a9ca825c2ae1e2256))
- **android:** add splash screen
  ([c232fcc](https://github.com/team-spotube/spotube/commit/c232fcc6dd1479ed33a8baa9887de2702a8ea22e))
- **android:** disable battery optimization for better playback
  ([fe5b429](https://github.com/team-spotube/spotube/commit/fe5b429ddacc576fc9fdb5e66718782cda163b27))
- artist card redesign
  ([92a418c](https://github.com/team-spotube/spotube/commit/92a418c8a8a9df99e27407b628e5e3cc9ccb4115))
- Better download manager with download progress
  ([6752adc](https://github.com/team-spotube/spotube/commit/6752adc9398818f51b69fced226b4b8410fb9e9b))
- better language picker, adaptive select tile and settings section contrast
  ([6430a25](https://github.com/team-spotube/spotube/commit/6430a2587075aa24483ab26ce6f0f6b2b630e139))
- cache encryption for sensitive data
  ([b110d83](https://github.com/team-spotube/spotube/commit/b110d834561ac53129ac9ec80238c014c84832ec))
- color scheme picker dialog vertical list view instead of wrap
  ([bb60b01](https://github.com/team-spotube/spotube/commit/bb60b01ef2f2ba3da8f6fe3a19add168b2ee8a4e))
- compact and adaptive playbutton card design
  ([eeb8cab](https://github.com/team-spotube/spotube/commit/eeb8cabf491d5242bd434b3c71c39363f24bdcf9))
- compact button tabbar
  ([67380f6](https://github.com/team-spotube/spotube/commit/67380f68765f18e4dcd3d60117083c7e9c6761c2))
- create a basic installer script
  ([1763a36](https://github.com/team-spotube/spotube/commit/1763a36a262178306df61d4588c17ff795a32790))
- curved navigation bar
  ([776edf8](https://github.com/team-spotube/spotube/commit/776edf84afcf99f96cf6e337b0c84ed89034ca8e))
- custom error toast
  ([96f04c1](https://github.com/team-spotube/spotube/commit/96f04c17565c0ab7f115d5c1f167f6660a69480d))
- custom playlist generator
  ([f4b0d13](https://github.com/team-spotube/spotube/commit/f4b0d134ca724b75bf65b885bce4dba206f1e090))
- desktop mini player support
  ([471812d](https://github.com/team-spotube/spotube/commit/471812d789eb2c861268ab8451d50104ac2fbe2e))
- **desktop:** close button for minimize notification
  ([1688f99](https://github.com/team-spotube/spotube/commit/1688f99096af940ead65e67832c6f061a6f635ac))
- **desktop:** show minimized to system tray notification
  ([296f96c](https://github.com/team-spotube/spotube/commit/296f96cf17cf21ff09b406cc24952ff60da52d5e))
- disable/enable smtc on demand
  ([7fa50e5](https://github.com/team-spotube/spotube/commit/7fa50e5c5ee9ca3ba95dca55f8a4831047f17570))
- download button on each track
  ([925fa86](https://github.com/team-spotube/spotube/commit/925fa86271fa10ff77b8137ba8d09b8067d0e819))
- enable caching of queue
  ([ec11af5](https://github.com/team-spotube/spotube/commit/ec11af53a16c435fbea3d0c81910dca371be9ce7))
- heart button animation
  ([8432dc6](https://github.com/team-spotube/spotube/commit/8432dc6286fbdfda52bbeb39c6d4ababa05881bc))
- improved track item API and UI
  ([617aa89](https://github.com/team-spotube/spotube/commit/617aa89409ce29eb3c197aee5c1189d763a3913c))
- **installer:** get latest version from Github API
  ([957c085](https://github.com/team-spotube/spotube/commit/957c085e1243ec0af5bc45d38691c74e2ba91ad8))
- **local_tracks:** delete local track
  ([#484](https://github.com/team-spotube/spotube/issues/484))
  ([52835b2](https://github.com/team-spotube/spotube/commit/52835b2ce2212925f80e1a1595c0ca30e6860a8d))
- locale category/genre title
  ([88137f0](https://github.com/team-spotube/spotube/commit/88137f01b27150b306327a01e67ec8a35a60e82e))
- **locale:** add bengali translations for search page
  ([a1cdbad](https://github.com/team-spotube/spotube/commit/a1cdbad18782a74b43f0625facfe3e35c516bf43))
- **locale:** localize search, library, lyrics, artist with both Bengali and
  English
  ([11fe9ec](https://github.com/team-spotube/spotube/commit/11fe9ec74462441b67a0ab0df73824f36dd15e2d))
- **locale:** player, playlist view, track tile bengali and english translations
  ([c55133d](https://github.com/team-spotube/spotube/commit/c55133dc8bba307823e5b67a30cfab03c923cb7f))
- localize settings, about, login, player queue with Bengali and English
  translations
  ([a5c36bb](https://github.com/team-spotube/spotube/commit/a5c36bbb20cc69d609bfb5ab973c7e288c1ea9de))
- logs page in settings
  ([b78e7f5](https://github.com/team-spotube/spotube/commit/b78e7f57a05db344aae59206cbb0f43b3ee199a9))
- macos title bar spacing and lyrics page margin separate
  ([a0b3771](https://github.com/team-spotube/spotube/commit/a0b377104f9822561d3b46dbc6551bb561842480))
- make snackbar floating
  ([9dbb817](https://github.com/team-spotube/spotube/commit/9dbb8171a6d6b81120ca7ccd74577e5c890ff930))
- merge floating player with nav bar and nav bar translucent bg
  ([a90261e](https://github.com/team-spotube/spotube/commit/a90261ed199f6cff9e8d0fe24934f8f1d8e9ed98))
- **mini_player:** remove window shadow
  ([6259014](https://github.com/team-spotube/spotube/commit/625901482ada4b441b838f640c0ab7167119b321))
- **mini_player:** show/hide UI on hover toggle
  ([2e8b647](https://github.com/team-spotube/spotube/commit/2e8b647a51f87840c2bd39f0a1dc25ddc91528fc))
- new sidebar widget and translucent bottom player
  ([4ba1e70](https://github.com/team-spotube/spotube/commit/4ba1e70636b4ba43697663128fc5422b1d0b2a2f))
- newly released albums of user followed artist
  ([33cb794](https://github.com/team-spotube/spotube/commit/33cb7947d63d0a2692a004f87a2ccd5777bf054e))
- optimize image load + genre page and reduce page size of loaded categories
  ([7131efa](https://github.com/team-spotube/spotube/commit/7131efa07fdbcf17965fc59ff635a6198b0e5e25))
- persistent volume percentage
  ([3724bd5](https://github.com/team-spotube/spotube/commit/3724bd5a10eef7a099d6f596fd038e6fea228359))
- personal playlist recommendations
  ([ae820a2](https://github.com/team-spotube/spotube/commit/ae820a22f291082c49554d621c25cc62212a6708))
- piped instance picker on settings
  ([bed0d3b](https://github.com/team-spotube/spotube/commit/bed0d3bd70438df413633ee03fd258a2ca4a1688))
- platform specific title bar buttons
  ([6267720](https://github.com/team-spotube/spotube/commit/62677209a23172162defb7a8e542d981569eba08))
- **playback:** integrate android, ios, macos with JustAudio
  ([d487fe5](https://github.com/team-spotube/spotube/commit/d487fe55630993e2b729050ebd0bf4e1e4be1fb3))
- **playback:** use assets_audio_player to fix macos double duration problems
  and android high loading latency
  ([1fff0f1](https://github.com/team-spotube/spotube/commit/1fff0f1bd0d811c348f293f733cbcb7cd57e02f8))
- player details dialog and separate location of lyrics button in player page
  ([ce38233](https://github.com/team-spotube/spotube/commit/ce38233de8f4775018a1d01e951b1635776fe743))
- **player:** add playlist related methods to audio player
  ([f1080e1](https://github.com/team-spotube/spotube/commit/f1080e1675aee1208d05658adfabfbed04ff45b6))
- **player:** animated gradient background
  ([49b5d0e](https://github.com/team-spotube/spotube/commit/49b5d0e6948d80abb8ee09203e0d655d68377245))
- **player:** custom playlist implementation for media_kit to replace
  unpredictable playlist of mpv
  ([eaf65b6](https://github.com/team-spotube/spotube/commit/eaf65b6db208aaad745821d4d42afc05f51cee7c))
- **player:** proper coloring of elements
  ([b2c4ea1](https://github.com/team-spotube/spotube/commit/b2c4ea13f6157c2b7bec3957e1f7f50fbf0002c7))
- **player:** replace bg blur with gradient, proper fg color and align title and
  artist name
  ([159f03e](https://github.com/team-spotube/spotube/commit/159f03e7ca62e6b3b86389e2795da84de61fba78))
- playlist create support for generated playlist
  ([91c72f9](https://github.com/team-spotube/spotube/commit/91c72f9ec9556f301c5d129fc82e19e791a02fbe))
- playlist generation all parameters support
  ([9877d5f](https://github.com/team-spotube/spotube/commit/9877d5f51736db03d5839dadf164d11d0cce82f0))
- **playlist,album page:** play and shuffle take full width on smaller screens,
  add new xs breakpoint
  ([dce1b88](https://github.com/team-spotube/spotube/commit/dce1b88694cfcb6b7e63d6ee614ac1dbbd017f6e))
- **queue:** add track(s) for playing next
  ([#460](https://github.com/team-spotube/spotube/issues/460))
  ([cac8ea6](https://github.com/team-spotube/spotube/commit/cac8ea638812f5d9cb4305144b6351141a2cf407))
- **queue:** reorder tracks support
  ([441b43b](https://github.com/team-spotube/spotube/commit/441b43bef6b92fd7df6c4e1bef39d67b4a76cd22))
- re-designed playlist/album page
  ([0cedc7a](https://github.com/team-spotube/spotube/commit/0cedc7a4187771efce8152003f890e242116c78c))
- re-introduce youtube API along with piped
  ([b54ee96](https://github.com/team-spotube/spotube/commit/b54ee96233b29d7517eba66e3f8dd9270c2790df))
- reactive volume slider and slicker bottom bar with lowered height
  ([9d14517](https://github.com/team-spotube/spotube/commit/9d14517202d5c9d993a947808bf0c6520ed54ea3))
- remove SponsorBlock in favor of YT Music and remove pocketbase backend track
  support
  ([fb780da](https://github.com/team-spotube/spotube/commit/fb780da327a213d7a82cbc3b567ece858dc2f0e8))
- repeat button all 3 mode and disable player controls when track is fetching
  ([1418378](https://github.com/team-spotube/spotube/commit/14183781dd3f1e16c121e78ad637a326de7b5dcf))
- replace YouTube API with piped API
  ([1ecc36d](https://github.com/team-spotube/spotube/commit/1ecc36da57af61fd9c2ca928589088cd4325f605))
- responsive playlist generate page and scrollable multi autocomplete
  ([d57aad5](https://github.com/team-spotube/spotube/commit/d57aad5612f7622dcd638ea8c0ec4d96f741de2b))
- search alternative track source
  ([dfea195](https://github.com/team-spotube/spotube/commit/dfea195ec178de733717cfe3226cede7521ee2d3))
- setup localization (l10n) and language switcher, add sidebar and navbar locale
  ([f12d812](https://github.com/team-spotube/spotube/commit/f12d81259f9e7005e681a7ca9867291d9228a8b1))
- show album release year
  ([#387](https://github.com/team-spotube/spotube/issues/387)) and fix layout of
  artist's album
  ([6a6ddf6](https://github.com/team-spotube/spotube/commit/6a6ddf6e1f6dc72b794cae49adf8348da272babd))
- show country code piped instance list
  ([60328a6](https://github.com/team-spotube/spotube/commit/60328a6bafcbff1b7d0ee5099825f0e3d545b60f))
- show loading when track metadata is being fetched, android, ios, macos enable
  shuffling
  ([bf59570](https://github.com/team-spotube/spotube/commit/bf59570251720a80efe0aa6be481899864da5079))
- sort tracks by newest and oldest dates
  ([b4713e3](https://github.com/team-spotube/spotube/commit/b4713e377a938cbebe70089874216f86fe550c34))
- supabase integration
  ([8bcce92](https://github.com/team-spotube/spotube/commit/8bcce9282eae08c5996a27f16f89cbc187a06823))
- system tray support ([#31](https://github.com/team-spotube/spotube/issues/31))
  ([06a0437](https://github.com/team-spotube/spotube/commit/06a043764d9f65fb448fcf088ccf2737145e23e8))
- track populate sibling support
  ([3aeb026](https://github.com/team-spotube/spotube/commit/3aeb026776716b6e2eb89c8406a4996a86c7ca60))
- **translation:** add hindi and french translations using ChatGPT
  ([6d836bd](https://github.com/team-spotube/spotube/commit/6d836bdb658c180ca8e2c71e7e290fafa3520727))
- **translation:** add Japanase locale
  ([4b52a71](https://github.com/team-spotube/spotube/commit/4b52a71c0914bda6c831d8f637a5934f7bcf8fcb))
- use system color scheme
  ([862c4b8](https://github.com/team-spotube/spotube/commit/862c4b8faf2c751d803e373e29981a116bf08ed5))
- volume slider in player page
  ([7abe2c1](https://github.com/team-spotube/spotube/commit/7abe2c10735bc38c644487139557a731d25e80e6))
- windows OS media control panel support
  ([f0b426a](https://github.com/team-spotube/spotube/commit/f0b426ae89f2e01f4a9c8757ef4e0b4a21b50c7b))

### Bug Fixes

- add to playlist dialog not showing playlist name
  ([8944581](https://github.com/team-spotube/spotube/commit/8944581c09eec0162220e7ff684205484fafb599))
- album sync not working
  ([74906f3](https://github.com/team-spotube/spotube/commit/74906f393250934c36530a73ad7312f59f8627ed))
- alternative track source not playing new source
  ([a9b5a71](https://github.com/team-spotube/spotube/commit/a9b5a714e47d40407d799966ae95f84338f9b59a))
- **android:** use multi assetAudioPlayer instance fix patch and disable
  Pre-download and play by default in Android too
  ([cdb3268](https://github.com/team-spotube/spotube/commit/cdb32685e4bbb899706ed16d58ef9a3a074e283a))
- **artist:** follower count shows as float when < 1000
  ([#482](https://github.com/team-spotube/spotube/issues/482))
  ([fd1846e](https://github.com/team-spotube/spotube/commit/fd1846eecf9632e59e4b70fb70e97c556b6374f5))
- bottom navbar first item icon color not changing on primary color change
  ([6eb4244](https://github.com/team-spotube/spotube/commit/6eb4244f3244a96fe6858261534cc03eb3de803c))
- cached currently playing track infinite loading
  ([9401718](https://github.com/team-spotube/spotube/commit/94017189c6b9bf55ec62cbf29cd6b0e9fffca42a))
- cached queue tracks expired stream
  ([ed29ab5](https://github.com/team-spotube/spotube/commit/ed29ab5137416d9fb2e7e9fe840f56ef52df6f61))
- collection currently playing state persist on restart
  ([1c89e3e](https://github.com/team-spotube/spotube/commit/1c89e3efb0f05c648fc1c8e09039e62333de18d1))
- color not syncing and add new screenshot
  ([6205501](https://github.com/team-spotube/spotube/commit/62055018feade0b895663a0bfc5f85f265ae2154))
- content going below bottom player or nav bar
  ([1bdce9f](https://github.com/team-spotube/spotube/commit/1bdce9fe964de88a667bb160846c11dc70b77c00))
- disable background_downloader due to android build failures
  ([7d23bee](https://github.com/team-spotube/spotube/commit/7d23beec5ef07c4d649185a69e7a2b9697dc6953))
- disable play when loading track and buffering event
  ([30c933c](https://github.com/team-spotube/spotube/commit/30c933cdf3d4524be164e171094afdd27b0252b7))
- error log
  ([e3d8239](https://github.com/team-spotube/spotube/commit/e3d8239b9f5700bfb17c4758d95ba1db1f0e718a))
- excessive repaints caused by Player progress bar
  ([09b24cf](https://github.com/team-spotube/spotube/commit/09b24cf1fd1644c549f85904545db54b39cc2431))
- failed download no error icon
  ([1266a3f](https://github.com/team-spotube/spotube/commit/1266a3f1607de11e793a294071850996527d494a))
- **home:** bottom player transparency
  ([20c424c](https://github.com/team-spotube/spotube/commit/20c424c77fe273a693213ebf88d50e4025bc8608))
- language changer not working
  ([7b7b1f2](https://github.com/team-spotube/spotube/commit/7b7b1f2647591b7cd4cc7841526716c6a2877e55))
- less frequent position updates
  ([0a49b56](https://github.com/team-spotube/spotube/commit/0a49b56566abd00cf7703e4207cfa90f93c381fd))
- linux mpris not showing up and overall media notification service
  ([1abcad1](https://github.com/team-spotube/spotube/commit/1abcad1de510c209a34196f2de17045af4dd3bc2))
- local tracks getting fetched on first load
  ([73c012c](https://github.com/team-spotube/spotube/commit/73c012c71ab5050636f79e010d654b4390978ee7))
- local tracks not working when there's a invalid music file in the folder
  ([5855820](https://github.com/team-spotube/spotube/commit/5855820569dfad7cd26f1e0f0c985babd0d9485d))
- lyrics page blur in player and cut off text when line too big
  ([6b4584e](https://github.com/team-spotube/spotube/commit/6b4584e91bd4f4aee0c56e48a7aec7015c7c418b))
- macos build by removing media_kit native event loop
  ([62fc773](https://github.com/team-spotube/spotube/commit/62fc7739b508f0e874978408a2bab0a1d422deb6))
- macos build error, mobile player duration and playing state and background
  disposal of player
  ([be91e33](https://github.com/team-spotube/spotube/commit/be91e33828630c7062886cd15e4d57496daaa4d5))
- **macos,ios:** use regular shared prefs
  ([1b5bfec](https://github.com/team-spotube/spotube/commit/1b5bfec27fbcfe9faabff64d46296bdeebe00161))
- memoize child of animated widget and make player bg animation faster
  ([fcb5c8f](https://github.com/team-spotube/spotube/commit/fcb5c8f8dabd0d4e3033f80ea3e5d006243cdfb5))
- mini player not working in release mode
  ([28ff321](https://github.com/team-spotube/spotube/commit/28ff3216efee81184798eedfbb10ba66395bbf36))
- **mkPlayer:** remove method and wrong active index on modifying playlist
  ([3bafa7b](https://github.com/team-spotube/spotube/commit/3bafa7b80c963fa52b90ed4cb1393fb121cac713))
- mobile audio notification not working
  ([8f9303b](https://github.com/team-spotube/spotube/commit/8f9303bc0fddb9d179303a1f0eb76dd5b02410e7))
- multiple instance of theme
  ([4ec0424](https://github.com/team-spotube/spotube/commit/4ec04240a5bde6af5c920a61ab6260e7a93bfc54))
- navigation to settings not working
  ([ce10aa1](https://github.com/team-spotube/spotube/commit/ce10aa1fe2c95d4738835687f613930cf7829f3a))
- no progress update when track changed
  ([6ae8964](https://github.com/team-spotube/spotube/commit/6ae896441a787fce1bc6e5eb5379856dc2f4e96d))
- null exception on proxy playlist and audio player
  ([a455a89](https://github.com/team-spotube/spotube/commit/a455a89c5861fd455f6950c7b68beae24bdcc6ed))
- overflowing clickable artists links
  ([4077fac](https://github.com/team-spotube/spotube/commit/4077fac39fb667b87e959e53d2dcaceefb63cd2d))
- personalized playlists not loading
  ([caa3408](https://github.com/team-spotube/spotube/commit/caa340803fdf3859fe5a8a996abae1502ef2e4e7))
- playback not moving to next track after a track ends
  ([27e8acb](https://github.com/team-spotube/spotube/commit/27e8acbfe75a37c0a8fa69a444fdd86e92dbe4f0))
- **player:** gradient bg not taking full height
  ([62ad86e](https://github.com/team-spotube/spotube/commit/62ad86e88d74b5114af78138b221314192e5a801))
- **player:** playback element placement
  ([5e47faa](https://github.com/team-spotube/spotube/commit/5e47faa6060d7a8aa0d143060e812dc06b8dd790))
- **player:** queue button not showing when not logged in
  ([6c2d655](https://github.com/team-spotube/spotube/commit/6c2d65587b0e6e167be1d0b086df103c7e72d4b2))
- **player:** volume slider, prefetching of media_kit and stuttering on
  sponsorblock skip
  ([1f32554](https://github.com/team-spotube/spotube/commit/1f3255481f058c50968561db88172e56b58494f4))
- playlist generate slider shape
  ([2b35c04](https://github.com/team-spotube/spotube/commit/2b35c044adb15a97a58692e7880694a251899732))
- pop sheet list not scrollable
  ([cca5625](https://github.com/team-spotube/spotube/commit/cca5625df7e432da8581a4504306baad154deb48))
- re-enable add to queue and play next support, favorite button query exceptions
  ([e529c79](https://github.com/team-spotube/spotube/commit/e529c79c4f0cd964b7d89e010d3fe51378ea7222))
- re-enable download manager
  ([ea45c4f](https://github.com/team-spotube/spotube/commit/ea45c4f42ae89b8991e470e84a5290b3be3b0f36))
- remove unnecessary broadcast stream conversions
  ([bf04962](https://github.com/team-spotube/spotube/commit/bf04962e90ea5345a2bc0d4793999f7db712cab2))
- remove useBreakpoints as it clogs up memory with unnecessary state updates
  ([e1c0f5c](https://github.com/team-spotube/spotube/commit/e1c0f5cf1e4cece2c4aa235bfbf8511ad7b1fe59))
- replace download multiple pops and add translations
  ([4a21249](https://github.com/team-spotube/spotube/commit/4a21249ee386426b0974451542a93e84f532fb3f))
- screen breakpoints and persist lyrics delay across screens
  ([df79638](https://github.com/team-spotube/spotube/commit/df79638fb622a55aaa2b36c9a1425c2d9c4a8e52))
- sidebar task counter badge and bottom player play button progress color
  ([af278d8](https://github.com/team-spotube/spotube/commit/af278d8feaa08c14528627766bce6d724b846954))
- status bar color of playlist/album page
  ([65fa3cb](https://github.com/team-spotube/spotube/commit/65fa3cb624c240360de5a06778a1f72ad10bbe2d))
- system color scheme not persisting on restart when system color scheme changed
  ([e04515d](https://github.com/team-spotube/spotube/commit/e04515d8e213b4c7f85d11385959a33b042bd9b1))
- track collection view status bar not transparent
  ([9251121](https://github.com/team-spotube/spotube/commit/9251121ba0154599975e33819a43719477c644f8))
- track doesn't play after change
  ([17e5ab6](https://github.com/team-spotube/spotube/commit/17e5ab611cc417cce7c17cafc7045f5aa2eb970e))
- track stops at last second
  ([f554f6d](https://github.com/team-spotube/spotube/commit/f554f6d43bb714f662a27977f501d7ad44b070c3))
- **track_collection_view:** keyboard focus on scroll and no space for search
  results in playlist/album
  ([7a8bd92](https://github.com/team-spotube/spotube/commit/7a8bd921047e3766dbbf24449e2873afe3dbecf8))
- track_table_view table headers
  ([d88d287](https://github.com/team-spotube/spotube/commit/d88d287fc586ec33351de9f3b4359f189054868b))
- track_tile active and blacklist color, playbutton card action positioning
  ([3f5a1b9](https://github.com/team-spotube/spotube/commit/3f5a1b9587efe9b7b2c69008345867933f79ec67))
- use id based source getters instead of index
  ([a074463](https://github.com/team-spotube/spotube/commit/a0744630ba2dc713babdb8db6500f9dd0f1e6096))

### [2.7.1](https://github.com/team-spotube/spotube/compare/v2.7.0...v2.7.1) (2023-04-10)

### Bug Fixes

- fallback for lyrics when anonymous
  ([f160ec7](https://github.com/team-spotube/spotube/commit/f160ec767d9941d33f83aba1752b28df629d0e10))
- **android:** audio notification stuck in play state
  ([448c9b3](https://github.com/team-spotube/spotube/commit/448c9b39f407668ad92a695afe3c9741baeca20d))
- **macos:** crashing on startup
  ([c46b428](https://github.com/team-spotube/spotube/commit/c46b4284b1d46a614cbcebc8c2f2e52714921b9b))
- Greedy Green Corp query hooks overriding default query params
  ([ec9a02e](https://github.com/team-spotube/spotube/commit/ec9a02e8b8d988e15ed58027054d2a9090d98873))

## [2.7.0](https://github.com/team-spotube/spotube/compare/v2.6.0...v2.7.0) (2023-03-07)

### Features

- add or remove track, playlist or album to queue support
  ([b8f3493](https://github.com/team-spotube/spotube/commit/b8f3493138a9acd91d19efe67cfd1c0c7c269ae6))
- basic command line argument support
  ([025c1ae](https://github.com/team-spotube/spotube/commit/025c1ae20461c2ac9124b3ef41e21ff01f100498))
- black list artist or track
  ([947c143](https://github.com/team-spotube/spotube/commit/947c14353e15227400a6310673f3b850b2ff024f))
- bring pre download on desktop, disable pre download for long videos
  ([1d82bb0](https://github.com/team-spotube/spotube/commit/1d82bb098717c7321d3e338f071c7661987fc3be))
- category/genre filter
  ([1dfec05](https://github.com/team-spotube/spotube/commit/1dfec05eec7ee60cc9f6a3a97af37aef112063f1))
- centralized icon collection with new icon set and nav bar labels hidden
  ([e7acb9e](https://github.com/team-spotube/spotube/commit/e7acb9ed5cb02826b8da559818f1fccfcf7f143c))
- compact search bar for genres and user_local_tracks page
  ([c343ccc](https://github.com/team-spotube/spotube/commit/c343ccc2932868e3c1205d8cc625a9dfe9d78707))
- compatibility with fl-query nextPage method change
  ([7617439](https://github.com/team-spotube/spotube/commit/761743991520609dd2b2dcb12cd6e4e75a8f6925))
- configure pocketbase, generate dart types, update playback to use server
  instead of hive cache
  ([ad90c11](https://github.com/team-spotube/spotube/commit/ad90c11ab0c9f1aaba9ae9226d6076ea590f1a29))
- failsafe pocketbase requests, removal of unneeded preferences options &
  vertical playbutton actions
  ([d68d150](https://github.com/team-spotube/spotube/commit/d68d150d3f42260f889d86927378c2f746bb6993))
- **home:** personalized section
  ([9080441](https://github.com/team-spotube/spotube/commit/9080441b875ceb91260bbad79291365a98d5be95))
- individual shuffle and repeat/loop button of player
  ([f79223c](https://github.com/team-spotube/spotube/commit/f79223cd41c61d9836d25e7bc2811c6515ba00c8))
- **lyrics:** use official Greedy Green Corp API for fetching lyrics and add zoom controls
  ([10d0660](https://github.com/team-spotube/spotube/commit/10d0660972f008df0d11c280b681ce3b78f05d0b))
- **mobile:** pull to refresh support in all refreshable list views
  ([9f959ce](https://github.com/team-spotube/spotube/commit/9f959ce77cd95cfc34d01af1f5cf53dd4206b6a6))
- new logo and compact search in playlist/album in mobile
  ([dc96cb3](https://github.com/team-spotube/spotube/commit/dc96cb38cea8dc13738083f4850d22792d071019))
- search/filter tracks inside playlist or album
  ([a06cd0d](https://github.com/team-spotube/spotube/commit/a06cd0da84cc03a2a7cadbc80d70556cb0cf9310))
- show snackbar on adding playlist or tracks to queue
  ([6bc1d32](https://github.com/team-spotube/spotube/commit/6bc1d32a88ae516f77d149b83bcd536d2c888513))
- **theme:** use material3 monet for colors and remove background color
  preference
  ([60ede5f](https://github.com/team-spotube/spotube/commit/60ede5f92b732691d53850290d9667435298a857))
- use catcher to handle exceptions
  ([84d94b0](https://github.com/team-spotube/spotube/commit/84d94b05bc269a1676a261df2b12e508e10e4c0e))
- use typed assets instead of hard coded paths
  ([59561ab](https://github.com/team-spotube/spotube/commit/59561abdc2540576fc95b34b3b55def63567000a))
- user local tracks searchbar
  ([e7f3f4e](https://github.com/team-spotube/spotube/commit/e7f3f4eae49fe27a52fc3866fa4f6f2efb2aa479))
- **user-library:** filtering support for user albums and user artists
  ([0b58155](https://github.com/team-spotube/spotube/commit/0b58155d434f2de6359be77d7beee4484dbb7b2a))
- **user-library:** search for user playlists
  ([af4d56f](https://github.com/team-spotube/spotube/commit/af4d56fd41e57cbe6d87883e87e6b4469aaba52f))

### Bug Fixes

- **about:** license text hidden in the bottom of smaller screen devices
  ([e158dd0](https://github.com/team-spotube/spotube/commit/e158dd0cec5657e495b538e86c412b06974a9f49))
- **about:** wrong link of License
  ([a4a7f1a](https://github.com/team-spotube/spotube/commit/a4a7f1a74f9df82927403ca93aec508a13315ae8))
- genre and sidebar user logo not loading
  ([710f172](https://github.com/team-spotube/spotube/commit/710f172dee45f60ed3e5ed83017eb538d6a626bf))
- lyrics modal sheet out of safe area so use 80% of screen height instead of
  full
  ([3db28f4](https://github.com/team-spotube/spotube/commit/3db28f43b4200d03f7758e8c395d8430e0f89333))
- lyrics not changing on track change
  ([c809d2d](https://github.com/team-spotube/spotube/commit/c809d2daba4beaea7c4f16c6bb0edef9efa825b8))
- lyrics not refetching when tracked changed while being in another page and
  sidebar user avatar not showing on startup
  ([bd12675](https://github.com/team-spotube/spotube/commit/bd126751e9594fbc926bbcad7b9a2c577fce074a))
- macOS logo placement
  ([c6a5d5f](https://github.com/team-spotube/spotube/commit/c6a5d5f7b1b1fad3a0b5e63c02c847a149e72efe))
- mobile track collection search bar position and page_window_title_bar
  exception on mobile platforms
  ([d0aaa97](https://github.com/team-spotube/spotube/commit/d0aaa971fe358b9cb5dc7a35cc82eaf6520f7ab4))
- **play_overlay:** show progress indicator on song loading
  ([7803a48](https://github.com/team-spotube/spotube/commit/7803a48237c91f2a57bcc86fbd30ad879142c8ff))
- **playback:** not skipping track's sponsorblock segments
  ([60a5847](https://github.com/team-spotube/spotube/commit/60a5847ae68836bbbeef748254c674c81fa5c3ea))
- playbutton card play state not changing
  ([ee46d09](https://github.com/team-spotube/spotube/commit/ee46d0970be9e227793494a41e25c0c469847cd0))
- **playbutton_card:** play and add to queue needs 2 clicks work
  ([bdd7098](https://github.com/team-spotube/spotube/commit/bdd70984e6670813e508786e74cd2ea4a1fe1d53))
- **playbutton_card:** play and non play state correction
  ([b327ffb](https://github.com/team-spotube/spotube/commit/b327ffb1084b43e5c78e13994f65fb30b3a7e67e))
- **playbutton_card:** title text overflow
  ([39ee0a9](https://github.com/team-spotube/spotube/commit/39ee0a92a8f3d74d243db206fe034330f75c0588))
- **playbutton:** playing state is not updating when playlist is actually
  playing
  ([9bad8c9](https://github.com/team-spotube/spotube/commit/9bad8c9eb88f7c91091a669b642b92474df0f128))
- **player_queue:** large clear button and macos exception
  ([0e43504](https://github.com/team-spotube/spotube/commit/0e43504e18d2315fb1b7975b67bd2c596cbfb1bc))
- **playlist_queue:** load method not preserving the active track before
  filtering blacklisted tracks
  ([42b3e11](https://github.com/team-spotube/spotube/commit/42b3e111f844f6de6a145de2760ccfd7e97e623b))
- pre downloading not working properly, audio service circular deps and sibling
  not loading for backend track
  ([3ccb525](https://github.com/team-spotube/spotube/commit/3ccb525260a83ba54021a353b15ed3cda6e9c876))
- search track play button isn't working
  ([0751f5e](https://github.com/team-spotube/spotube/commit/0751f5e3173882f3aeed67027854e5054b689693))
- **search:** grey screen, only tracks update on new search string,
  playlists,albums,artists show up before hitting return/submit
  ([a774817](https://github.com/team-spotube/spotube/commit/a774817240ef813cb95f82f53ccb798ef9acb51d))
- **search:** has to submit twice for search results
  ([f5dc76a](https://github.com/team-spotube/spotube/commit/f5dc76a98f55f0f032a6fe4208465899f932355a))
- titlebar maximize+restore button not working and less responsive title bar
  buttons
  ([8a6ba3b](https://github.com/team-spotube/spotube/commit/8a6ba3b35f0b6b42cf60920e945ac2065c886ecb))
- **track_collection_view:** hide search bar when sliver is collapsed
  ([3d6d244](https://github.com/team-spotube/spotube/commit/3d6d2444beed153a2b6663d6153684b2974f4152))
- **track_tile:** cannot see track index above 99
  ([78b3273](https://github.com/team-spotube/spotube/commit/78b3273e441cdaa6d4a410ddfe29837dc1aa7000))
- **track_tile:** track action popup not showing on narrow screens
  ([0c54f2d](https://github.com/team-spotube/spotube/commit/0c54f2dcd4474b63db4c517b0e7332cbd3ab51e9))
- **ui:** scaffold exception in fluent_ui
  ([8ce2192](https://github.com/team-spotube/spotube/commit/8ce2192e5cb08e3a8be5ead510ab35b274bef2ef))
- use chosen market for new release
  ([c6bf9b6](https://github.com/team-spotube/spotube/commit/c6bf9b67995161a8bf7c3782188d01e8859c18e9))

## [2.6.0](https://github.com/team-spotube/spotube/compare/v2.5.0...v2.6.0) (2022-12-09)

### Features

- add selected tracks to playlists, optimistic playlist remove track
  ([3386dac](https://github.com/team-spotube/spotube/commit/3386dac78ee49b9e3504f5c05bf1e7b362a2e8a2))
- added shuffle button in playlist and album section
  ([1fad95f](https://github.com/team-spotube/spotube/commit/1fad95f6e370606a9faf6f2bb9738dc360e23918))
- **android-playback:** option to download track bytes and play instead of
  Streaming
  ([dcc8ba5](https://github.com/team-spotube/spotube/commit/dcc8ba5a54286b252d53c9c14918971bf7bea8cc))
- change default platform option and platform specific back button
  ([36c5e02](https://github.com/team-spotube/spotube/commit/36c5e02f18374100f61cc3f2957c27bfc0d8511f))
- dialog logo for macos, settings more width for country picker
  ([5e96913](https://github.com/team-spotube/spotube/commit/5e96913ba34230e7a15d62060d5dae28d80e3630))
- initial platform_ui integration
  ([9eee573](https://github.com/team-spotube/spotube/commit/9eee573ce928aa6c03dcb50bf1521350d2de32cc))
- libadwaita theming, track tile and PlayButtonCard play button icon fix
  ([e795e23](https://github.com/team-spotube/spotube/commit/e795e23e42e5f1f832963b2a4506df89b7df5baa))
- **lyrics:** tabs for both synced and static lyrics
  [#182](https://github.com/team-spotube/spotube/issues/182)
  ([6b6907a](https://github.com/team-spotube/spotube/commit/6b6907af3fdb327312ad4dd9e16b3e2a850ed896))
- new refined about page, update checker only check for same update channel
  ([4cadfa9](https://github.com/team-spotube/spotube/commit/4cadfa93750cc9b3f8fbe7b60f8161a77d2a12f6))
- pause track when seeking forward/back and keep audio session alive when
  paused/interrupted
  ([bc8a04e](https://github.com/team-spotube/spotube/commit/bc8a04e5442ba3abe1b04ab325769559f37d9802))
- platform bottom navigation bar add
  ([ff14469](https://github.com/team-spotube/spotube/commit/ff1446982f0260c6fe231970aa6ed61c273fdf07))
- platform slider and progress indicator integration
  ([46b00ba](https://github.com/team-spotube/spotube/commit/46b00bafdf71a4add61cf50168a32198c3293181))
- platform title bar buttons add
  ([54048cb](https://github.com/team-spotube/spotube/commit/54048cbfc37d9a40c020eb81ab67b9dd5428f0d7))
- **playback:** change current track youtube source panel and tooltips for
  player icon buttons
  ([4b21cc8](https://github.com/team-spotube/spotube/commit/4b21cc829954fb079d1a0081b9147377063da3ec))
- Player and Playbutton theme respect to platform
  ([512446d](https://github.com/team-spotube/spotube/commit/512446dcab72aa1d7bce18e5a5793f6be8f30fcb))
- player queue and sibling tracks platform decoration
  ([39a7794](https://github.com/team-spotube/spotube/commit/39a77945d132d90ff323b0a22edc2a12a4749888))
- **PlayerView:** shortcut button for opening lyrics
  [#273](https://github.com/team-spotube/spotube/issues/273)
  ([1d4847a](https://github.com/team-spotube/spotube/commit/1d4847ab0a0b18d5bc27257b3db863b995dc5843))
- rename files to snake_case and reorganize folder structure
  ([7c25e1c](https://github.com/team-spotube/spotube/commit/7c25e1cc8a35eb8aee2268799c05f299204fa3f5))
- replace all types of buttons with platform buttons
  ([69739b4](https://github.com/team-spotube/spotube/commit/69739b457296a6b209aa6f73beb378ae1f089ac5))
- rpm packaging support
  ([067e9ac](https://github.com/team-spotube/spotube/commit/067e9ac53ee85775c9ec35457fac9e064e72e4c0))
- **search:** infinite scroll for tracks, artists, playlists and albums
  ([e6761a6](https://github.com/team-spotube/spotube/commit/e6761a6f8eadf4ab260723253a8e00121b6365b5))
- set platform to default platform on start up
  ([472da6b](https://github.com/team-spotube/spotube/commit/472da6b8b1c3e06b666da58351f3feafbfe6c98a))
- shuffle keep playing track at top, linux title bar drag no working
  ([1223cf2](https://github.com/team-spotube/spotube/commit/1223cf2629c6615b0c48e9e6742b68341f33c7f8))
- sidebar download count and proper progress color in playbutton
  ([a10bc5b](https://github.com/team-spotube/spotube/commit/a10bc5b8d89207ac86872dd25f3258e47c2141a5))
- static shimmer for track tile, playbutton card and track tile
  ([3ed8b0f](https://github.com/team-spotube/spotube/commit/3ed8b0fda2cb8145a2bc6c8f8c6af82db6a40547))
- tablet mode navigation bar & windows semi transparent bg,
  ([3282370](https://github.com/team-spotube/spotube/commit/3282370f74f323c00116fa8626fd1440ee9d4922))
- **title_bar:** platform specific title bar
  ([e659e3c](https://github.com/team-spotube/spotube/commit/e659e3c56fb02ad8a1c5114bf80074f0324cc4f1))
- titlebar complete compatibility, platform specific login, library tabbar in
  titlebar
  ([b3c27d1](https://github.com/team-spotube/spotube/commit/b3c27d1fca233ea079803fe49134abc528376df3))
- use platform checkbox
  ([2211505](https://github.com/team-spotube/spotube/commit/2211505d713cef752d07cafb9791a49e8095eee2))
- window blur effect add
  ([b0db5e7](https://github.com/team-spotube/spotube/commit/b0db5e7d8246f98835e7ce6656c8aa7620e46bec))

### Bug Fixes

- **ArtistCard:** linux shadow
  ([c186881](https://github.com/team-spotube/spotube/commit/c1868817e5abb8a4152646f00a0395933fee7823))
- **auth:** refresh access token timer not working
  ([b3ac5ca](https://github.com/team-spotube/spotube/commit/b3ac5ca3bbb6d5af154f4b5d715d1f19ca2f46e2))
- bottom navigation bar settings tile not active when selected
  ([43557e4](https://github.com/team-spotube/spotube/commit/43557e40df269757c2d5236a455308ea6478d95a))
- dialog logo in android, lyrics visible timer adjust button
  ([3c6803b](https://github.com/team-spotube/spotube/commit/3c6803bb3fac8eee9166764089724194a48509c6))
- heart button showing when not logged in, wrong login redirect
  ([4dc26af](https://github.com/team-spotube/spotube/commit/4dc26af23d12f76cbfdfbf4e37b0c11fcc484d3f))
- horizontal infinite lists doesn't fill the screen
  ([69995be](https://github.com/team-spotube/spotube/commit/69995bea1c6342c9212e5b22ef50bdfd6e7eba45))
- ios dialog action buttons, local tracks crashing app, shimmer color and
  android wrong status bar color
  ([90c1200](https://github.com/team-spotube/spotube/commit/90c1200a087f796690de0cfc8cc607d2bff44282))
- **login:** not working in android in Brazil or Ukraine regions
  ([0b79a11](https://github.com/team-spotube/spotube/commit/0b79a1181c37cf06fbfa3bfb3854cfd47097016e))
- **macos:** black text in dark mode
  ([fb9c0e4](https://github.com/team-spotube/spotube/commit/fb9c0e44be93997fc852bf0260e8a8608000c023))
- **macos:** white text color in dark mode, text field white background
  ([e086b52](https://github.com/team-spotube/spotube/commit/e086b520e745e65771136cbfa842ae0693c44872))
- **mobile:** SafeArea bugs and back button color
  ([a8330ef](https://github.com/team-spotube/spotube/commit/a8330ef2e1112012bbae19ee6a5c27a26c5fb719))
- null exception in themes
  ([9465d92](https://github.com/team-spotube/spotube/commit/9465d92fa032b8598a0752767dcec9af2541d222))
- platform_ui local path
  ([00d0d38](https://github.com/team-spotube/spotube/commit/00d0d38b5450aeb877195afdfb9424f83762d178))
- player view artist link when local playlist is playing, lyric delay adjust
  button alignment
  ([ee5c417](https://github.com/team-spotube/spotube/commit/ee5c417ac396ef0b1796fa74a6a494181e6e0396))
- remove windows background
  ([6942964](https://github.com/team-spotube/spotube/commit/694296418787c460bb3fa63ab30f3b0eed9184dc))
- search field ios dark icon , lyrics tabbar ios background color
  ([be56ad4](https://github.com/team-spotube/spotube/commit/be56ad44773ebcd14777d80b61e26875698dc18a))
- settings Title alignment and play button card ripple effect in other platforms
  ([3b6bf27](https://github.com/team-spotube/spotube/commit/3b6bf27a984f5d4836143638396ed4b467c0eae7))
- shuffle play logic
  ([65cad07](https://github.com/team-spotube/spotube/commit/65cad07e3a6e2188c53159057f9c3d4fe89706ea))
- small minwidth of window in desktop, linux wrong light theme accent color,
  search field transparent background
  ([5b0e22c](https://github.com/team-spotube/spotube/commit/5b0e22c1b639f2f57d92cb70cd11f56e30e0a457))
- tooltips of menu and adaptive pop up menu
  ([261aaf1](https://github.com/team-spotube/spotube/commit/261aaf191c51bc12b28c602ee160d53d3eacf3a5))
- update download dialog blocking the UI
  ([3925f74](https://github.com/team-spotube/spotube/commit/3925f743951e51f138cc3ca865fa167c34e776ef))
- user playlists not updating after creating/deleting, artist follow not
  updating after follow/unfollow
  ([6cc2a18](https://github.com/team-spotube/spotube/commit/6cc2a185d0c4c19f176e6f65b8ada19ebc76af5e))
- **windows:** windows global title bar
  ([bd18f19](https://github.com/team-spotube/spotube/commit/bd18f198217538f0089d5a1c4288dd97f982661b))

## [2.5.0](https://github.com/team-spotube/spotube/compare/v2.4.1...v2.5.0) (2022-10-13)

### Features

- animated transition of root PageWindowTitleBar
  ([ff35e06](https://github.com/team-spotube/spotube/commit/ff35e06a6605fc7ec762e716fb7bdf6f7eb45732))
- **auth:** new authentication flow using cookies and webview in android
  ([756b910](https://github.com/team-spotube/spotube/commit/756b91007ee747c10ed10aa7060af49b555a2eaf))
- **downloader:** replace /skip all choice for downloaded tracks
  ([88d7ce5](https://github.com/team-spotube/spotube/commit/88d7ce55a59f673d60cd9e85ab062bcb1b7dcbc3))
- implemented go_route shell/nested route
  ([3e498a4](https://github.com/team-spotube/spotube/commit/3e498a4827a1118e0b23faec7cf114272f7838d4))
- **keyboard shortcuts:** play/pause on space, seek position on left/right
  ([2734454](https://github.com/team-spotube/spotube/commit/2734454717bbfb5d0621c6ea72fa755ef4fc8602))
- **keyboard-shortcuts:** home sidebar tab navigation and close app
  ([8f258e7](https://github.com/team-spotube/spotube/commit/8f258e709ada418dbeef8d272af370b1741afd9c))
- smoother list using fl_query and waypoint
  ([c77b0e1](https://github.com/team-spotube/spotube/commit/c77b0e198b215180d863747e35998a17aff92720))
- sort tracks in playlist, album and local tracks
  ([cb4bd25](https://github.com/team-spotube/spotube/commit/cb4bd25df154455d225c426cfeaaea36ac09e9b7))
- use of smaller sized images in `TrackTile`
  ([0ca97b4](https://github.com/team-spotube/spotube/commit/0ca97b495f2a9ece8356f7813fc0e37d1cdb8608))
- volume slider mouse scroll and preference for Rotating Album Art
  [#255](https://github.com/team-spotube/spotube/issues/255)
  ([edb6f3c](https://github.com/team-spotube/spotube/commit/edb6f3cd1c9ee2961040b2fe7a91c48577cee4f7))

### Bug Fixes

- **android:** file_picker and permission_handler failure for sdk < 33
  ([139d4dc](https://github.com/team-spotube/spotube/commit/139d4dc033d9aaa1d6882bf0f53e96a3b1e87c95))
- cached local track is fetched from network
  ([abf4a57](https://github.com/team-spotube/spotube/commit/abf4a5763a2faeedeb93d54e66c1f2482295b326))
- categories not showing for oauth exception
  ([4df917e](https://github.com/team-spotube/spotube/commit/4df917e65ee20cbcf42394cc141b1cdcdd6cc914))
- **desktop:** maximized window size is stored and window maximized state
  doesn't persist
  ([91d5d10](https://github.com/team-spotube/spotube/commit/91d5d1003b09530ff3bc9a0aa93e382e943977e0))
- local audio doesn't get refreshed after getting permission
  ([618c6da](https://github.com/team-spotube/spotube/commit/618c6da0ebddf3cc8e216743bbbb9220bcf40521))
- no appropriate output when playlist is empty
  [#201](https://github.com/team-spotube/spotube/issues/201)
  ([dbb81de](https://github.com/team-spotube/spotube/commit/dbb81de763df60eba62ef1256a7161ea6ca59b66))
- PlayerOverlay not hiding when not playing and unneeded bottom space in
  TrackTableView
  ([0ebac05](https://github.com/team-spotube/spotube/commit/0ebac05a4be8e8f744a6c672d3bb9807d6f02e10))
- **web:** not building due to metadata_god ffi
  ([1191bf2](https://github.com/team-spotube/spotube/commit/1191bf232d0797aaae7eff2f5d570acd49ce61bd))

## [2.4.1](https://github.com/team-spotube/spotube/compare/v2.4.0...v2.4.1) (2022-09-13)

### Features

- add macos audio metadata tags support
  ([5866b0f](https://github.com/team-spotube/spotube/commit/5866b0fcd661cf32060bb1485ea81634fbb9b90a))
- remove macos bounds for reading and writing audio metadata
  ([16064f6](https://github.com/team-spotube/spotube/commit/16064f68e882b091401ace4b895e387f46635800))
- **search:** horizontal swipe scroll support for Desktop platform
  ([d5ff927](https://github.com/team-spotube/spotube/commit/d5ff927c7273b6e72c5d775ee777f2cbd0d6d05c))

### Bug Fixes

- **artist-page:** SpotubeMarqueeText used in ArtistCard crashes the app
  ([4279541](https://github.com/team-spotube/spotube/commit/427954150ab65b250e79fc844fc864abff5b6972))
- **layout:** Fix adaptive UI not working correctly by providing a overriding
  option
  ([8c7adde](https://github.com/team-spotube/spotube/commit/8c7adde890105e0267b71994b7928277f84553e5))
- **local-track:** throwing exception when downloadLocation is empty
  ([1a3556d](https://github.com/team-spotube/spotube/commit/1a3556d39e8473cadb6143192c48465dc6485599))

## [2.4.0](https://github.com/team-spotube/spotube/compare/v2.3.0...v2.4.0) (2022-09-09)

### Features

- Ability to change download location added
  ([816707c](https://github.com/team-spotube/spotube/commit/816707c643f8d60d25bc08fd4c8005daa2ba9e63))
- add download multi tracks support for mobile platform
  ([0476bf7](https://github.com/team-spotube/spotube/commit/0476bf7ceece034a927d1df6099d8b33036f8a9b))
- add download queue for desktop & initial playlist download support
  ([08f913e](https://github.com/team-spotube/spotube/commit/08f913e9761d0f5c447af9dfb6eedb44b675498c))
- add download tab on library
  ([8d77b69](https://github.com/team-spotube/spotube/commit/8d77b6900a81aab020e19397e788964b0ac499ff))
- add web support although nothing works just as expected
  ([2818ed5](https://github.com/team-spotube/spotube/commit/2818ed5c9dadb9185a52762599c1dd0acd81e6bf))
- **broken:** Broken Warning! Initial Local Audio Player
  ([c3bf511](https://github.com/team-spotube/spotube/commit/c3bf5119ebb7c17e8c32f149598508674b0acd39))
- **download:** track table view multi select improvement, tap to play track
  support, existing track replace confirmation dialog and bulk download
  confirmation dialog
  ([e217553](https://github.com/team-spotube/spotube/commit/e21755322f2cd5f1fba00c5c8cd5c5d1f79e459d))
- **local-tracks:** complete support for local tracks
  ([e206f16](https://github.com/team-spotube/spotube/commit/e206f16723ac989ad58006c1b3c90c6691d8cab3))
- **mpris:** MPRIS metadata are now updated in realtime
  ([d9addcd](https://github.com/team-spotube/spotube/commit/d9addcda8e9562803bd73016148fab22560ee050))
- **playback:** add repeat track support
  [#166](https://github.com/team-spotube/spotube/issues/166)
  ([cae9993](https://github.com/team-spotube/spotube/commit/cae99934299bd197c68f626d6c10158d449770b9))
- **synced-lyrics:** animated active text size
  ([531fae6](https://github.com/team-spotube/spotube/commit/531fae64f94b21551a7a0da363a9ab0d44f5d3b1))
- **ui:** adaptive TrackTile actions & Setting ListTile
  ([615d5ce](https://github.com/team-spotube/spotube/commit/615d5ce901eb0512e84a120b7309c9053238ee36))

### Bug Fixes

- **adaptive-list-tile:** dialog content not updating when content has changed
  ([a1d4230](https://github.com/team-spotube/spotube/commit/a1d423090c854ebe319a0fa03fd6e5c4007b1387))
- album & playlist card, player view and album view play button logic
  ([55852bd](https://github.com/team-spotube/spotube/commit/55852bd15bc709d61fbba8cbea01ceca791d154c))
- **docs:** indentions
  ([4a291d5](https://github.com/team-spotube/spotube/commit/4a291d5f20dabe68f3ed64071624dcbed8327329))
- **downloader:** downloaded track is corrupted for tagging
  ([2ab1fba](https://github.com/team-spotube/spotube/commit/2ab1fba3d64147e3c5cf34756dce1cf6046d410a))
- **downloader:** flutter downloader exception on desktop platform and too much
  width of TrackTile index no.
  ([d668760](https://github.com/team-spotube/spotube/commit/d6687603d148ad936530cca4d09e128a59b79b19))
- dropped flutter_downloader deps due to slow download speed and UserDownloads
  not showing for anonymous
  ([307a8e2](https://github.com/team-spotube/spotube/commit/307a8e21df1e39123a1dca4c1b063eab50359581))
- flutter_downloader manifest configuration breaking android support
  ([f3a0f78](https://github.com/team-spotube/spotube/commit/f3a0f78fb92ff7ee38b5a9ef9954575d4282f954))
- login screen not using safearea and no dialog bg-color found on light mode in
  AdaptivePopupMenuButton
  ([92bc611](https://github.com/team-spotube/spotube/commit/92bc611c5e901dcabf34086be9287ac20317259a))
- **performance:** always running marquee text causes high GPU usage
  [#175](https://github.com/team-spotube/spotube/issues/175) and UserArtist overflow
  on smaller displays
  ([a23ce61](https://github.com/team-spotube/spotube/commit/a23ce614467b4297f495b824f0958ff07c21ae92))
- **playback:** shuffle button sometimes gets stuck and stops working
  [#183](https://github.com/team-spotube/spotube/issues/183)
  ([4240433](https://github.com/team-spotube/spotube/commit/4240433e3dde6ab948d2674e07e41c27c1f6eac8))
- **player-overlay:** flickering when a track is changed or navigated to another
  page
  ([e48b67c](https://github.com/team-spotube/spotube/commit/e48b67cd47ae54ad9268aead268e444836a67b0d))
- **sidebar:** user image url
  ([747efc6](https://github.com/team-spotube/spotube/commit/747efc6ee66bc6c7c917cc02bd134968a0781701))
- **synced-lyrics:** active lyrics contrast ratio
  ([aba1ba9](https://github.com/team-spotube/spotube/commit/aba1ba932592923720a36395c057f78820dafecf))
- tabbar overflow in small screen, artist card too small title and synced lyrics
  contrast increased
  ([585de8c](https://github.com/team-spotube/spotube/commit/585de8c1def9750826568317109b242a5e18f28c))

# v2.3.0

### New

- Playback Cache Support. So unfinished playlist and tracks remains cached &
  starts automatically when application is launched again
- Playback Cache Support. So unfinished playlist and tracks remains cached &
  starts automatically when application is launched again
- Login Screen guided tutorial about how to obtain Client ID & Client Secret
- Signed Android Application so now longer need to uninstall the old version for
  installing the new one
- Signed Android Application so now longer need to uninstall the old version for
  installing the new one
- OS Media controls for Linux. Keyboard media keys now work in Linux
- New better, consistent & predictable Audio engine with proper event firing
  support (https://github.com/team-spotube/spotube/pull/131)
- Custom Lyrics delay time. Can be used to delay negative amount of time too
- Playback Queue View support. Currently playing tracks or playlist can be
  viewed or changed from it or for doing other actions too
  (https://github.com/team-spotube/spotube/issues/126)
- Android SeekBar support in Notification Panel & Lock Screen
- New Blur background design adapted to multiple components including Floating
  Player, Player View & Lyrics Tab
- New HighContrast Color Scheme addition which reduces battery consumption on
  OLED or AMOLED display devices
  (https://github.com/team-spotube/spotube/issues/137)

### Improved

- Loading screens & animations. Now uses Skeleton Loading
- Playlist & Album Pages now show Album Art & extra metadata as Header with
  vibrant gradient background in a Sliver
- Playback is now more consistent & the API is simpler. Also its the single
  source of truth for AudioPlayback instead of the AudioServiceHandler
- Playlist & Album Pages now show Album Art & extra metadata as Header with
  vibrant gradient background in a Sliver
- Playback is now more consistent & the API is simpler. Also its the single
  source of truth for AudioPlayback instead of the AudioServiceHandler
- Android Statusbar background color is now adaptive & less glitchy
- Home Genre playlists can be scrolled horizontally by dragging with mouse even
  in Desktop edition
- Track match Cache support for previously played tracks. This dramatically
  reduces track change latency & load on the YouTube search engine too
- Home Genre playlists can be scrolled horizontally by dragging with mouse even
  in Desktop edition
- Track match Cache support for previously played tracks. This dramatically
  reduces track change latency & load on the YouTube search engine too

### Bug Fixes

- API rate limits inside TrackTile for multiple Follow queries at once
- Player doesn't stop when Application is exits or closed
- First Track of Playlist doesn't load sometimes
- Download Button doesn't show done symbol when track is already saved
  (https://github.com/team-spotube/spotube/issues/138)
- Downloaded Music is 0kb sized when lyrics are downloaded alongside
  (https://github.com/team-spotube/spotube/issues/122)

# v2.2.1

### Improved

- Page transitions defaulted to material you design

### Bug fixes

- Mini Player flickering on random state updates
- Track More Options not showing when not logged in
- Wrong link to Client ID & Client Secret tutorial in Login page
- Changing preferences in Settings resets the entire Playback

# v2.2.0

### New

- Update checker
- Share options for playlists & track
- Android Skip to Next/Previous track from notification/lockscreen
  (https://github.com/team-spotube/spotube/issues/91)
- Custom Accent Color Scheme support (Dark + Light)
- Custom Background Color Scheme support (Dark + Light)
- User customizable Audio Quality Option
- User customizable Track Matching Algorithm Option
- Material 3 Design Language and Flutter 3.0
- Caching in Playlists, Album, Search, Playlist Categories, Artist Profile &
  Lyrics
- M1 Mac support via MacOS Universal Binary (untested)
  (https://github.com/team-spotube/spotube/pull/87)

### Improved

- Authentication is now persistent (no more re-login)
- Settings Page. Shows application details in About Dialog
- Playlist Create Dialog Scrollable

### Bug fixes

- private playlists of current user aren't shown fix
  (https://github.com/team-spotube/spotube/issues/92)
- refresh token error causing re-login (culprit: internal lib Greedy Green Corp-dart)
- Typo in Login instructions URL

# v2.1.0

### New

- Synced Lyrics (with fallback genius lyrics)
- Playlist create/delete
- Add/Remove tracks to own playlists
- Custom YouTube track search term template
- Downloading lyrics along with a track (can be toggled)
- Customize Marketplace location

### Improved

- Greedy Green Corp track to youtube track algorithm
- Genius lyrics matching algorithm
- Download track. Checks if already exists & replaces on user command
- Wide screen responsiveness & adaptation
- Bigger Title display (replaced word-break with Marquee Text for better
  visibility) (https://github.com/team-spotube/spotube/pull/47)

### Bug fixes

- Sequential playlist playback not working with latest webkit2gtk
  (https://github.com/team-spotube/spotube/issues/46)
- Theme modification state doesn't persist
  (https://github.com/team-spotube/spotube/issues/54)
- Wrong URI path for "Login with Greedy Green Corp" tutorial
  (https://github.com/team-spotube/spotube/issues/69)
- Card shadow showing in the background of TitleBar & Searchbar

# v2.0.0

### New

- Android Support https://github.com/team-spotube/spotube/issues/24
- Responsive UI (Mobile, Tablet)
- Anonymous/Guest Account
- Mini floating player
- Full page PlayerView for smaller devices
- Horizontal CategoryCard Scroll & pagination for quicker access to Playlists
- Bottom bar for smaller devices
- Collapsed Sidebar for medium sized devices
- Persists Volume level
- Android NavigationPanel controls (OS media controls of Android)

### Improved

- Search - now scrolls & paginates for Playlists & Albums
- Authentication - allows guest accounts making authentication optional
- Lyrics - can be fetched without requiring GeniusAccessToken. This makes
  geniusAccessToken optional
- Lyrics - can be fetched without requiring GeniusAccessToken. This makes
  geniusAccessToken optional
- UI snappiness & faster load times
- Simpler logic, faster calculations & better caching (flutter_hooks)
- shared state management - uses riverpod & hooks combination

### Bug fixes

- Can't play any song in macos https://github.com/team-spotube/spotube/issues/23
- Downloaded tracks can't be played as they're WebAudio (.weba) instead of MP3
- delay while changing Playlist/Single tracks

# v1.2.0

### New

- Global custom reconfigurable _hotkey_ support for playback controls
  (play-pause/next/previous)
- Global custom reconfigurable _hotkey_ support for playback controls
  (play-pause/next/previous)
- Credit section in the Settings page with important links

### Improved

- Macos support
- Genius (Lyrics Provider) access_token can be saved in the Login page too
- Better theme for dropdown-buttons

### Bug fixes

- broken authentication IPC on Mac OS
  (https://github.com/team-spotube/spotube/pull/18)
- Mac OS's global appmenu's default APP_NAME replaced with Spotube
- location of back button on macOS (https://github.com/team-spotube/spotube/pull/21)
- windows titlebar buttons appears on Mac OS
- genius access_token not loading on initial app start

# v1.1.0

### New

- MacOS support https://github.com/team-spotube/spotube/pull/7
- Download currently playing track to `/home/<user>/Downloads/Spotube` (Linux,
  MacOS) or `C:\Users\<user>\Downloads\Spotube` (Windows)
- Play playlist from any song (index) instead of only the first track
- AlbumCard for showing album's metadata
- AlbumView aka show album tracks
- Play an album
- ArtistCard for showing artist metadata on the fly
- ArtistProfile for showing complete details of the artist
- Play artist's top tracks
- View Artist's "Fans also like" section
- Search page
- Play tracks from search result
- Click to open artist-profile/album everywhere in the application

### Improved

- UserLibrary album & artist tab
- PlaylistView simplified layout with `ListView` instead of `TableView`
- Control Theme from settings manually
- `PageWindowTitleBar` now acts as `appBar`

### Bug fixes

- Unsafe access to album art/artist/user Images with `.first` or `.last` causing
  accessing empty List error
- `url_launcher`'s unstable `canLaunch` method blocks OAuth login in certain
  \*nix OSs
- Unsafe access to album art/artist/user Images with `.first` or `.last` causing
  accessing empty List error
- `url_launcher`'s unstable `canLaunch` method blocks OAuth login in certain
  \*nix OSs
- Refresh token gets revoked & doesn't get renewed automatically

# v1.0.1

### Improved

- Placeholder avatar for User section powered by dicebear.com

### Bug fixes

- No fallback/placeholder image causing undefined behavior (#2)
- Unsafe access to empty List with List.first/List.last

# v1.0.0

### New

- Complete re-write in Flutter/Dart (799e13c)
- mpv & youtube-dl runtime dependencies dropped (07b1891)
- just_audio (libwinmedia + libwebkit2gtk-4.0-dev) + youtube_explode based
  playback & streaming
- just_audio (libwinmedia + libwebkit2gtk-4.0-dev) + youtube_explode based
  playback & streaming
- lyrics are provided by genius.com (requires access_token) (d647d5e)
- inno_setup based windows/win32 GUI installer (dbf8a34)

### Improved

- Lower RAM & CPU usage. 2x less RAM usage & 20% less CPU usage
- Faster playback & smooth track change with proper shuffling support
- Automatic Dark mode support (system)
- 54% smaller bundle size (after compression)
- Available through package managers in Linux (Debian, Arch, Flatpak & AppImage)

# v0.0.3

### New

- Automated installer for Windows (now doesn't require manual mpv-player
  install)
- Automated installer for Windows (now doesn't require manual mpv-player
  install)
- Playback caching
- Retry button for ManualLyricDialog
- Support for downloading track
- Redirect to youtube video by clicking on the title of the track

### Improved

- Inapp Shortcuts.Now it doesn't interfere while typing in a input box in Search
  page
- Inapp Shortcuts.Now it doesn't interfere while typing in a input box in Search
  page

### Bug fixes

- Cached image didn't get deleted after exiting certain cache limit fix. Cache
  gets recreated after exiting the limit
- Cached image didn't get deleted after exiting certain cache limit fix. Cache
  gets recreated after exiting the limit

# v0.0.2

### New

- Lyric Seek
- Support for images in playlist cards
- Infinite Query/Pagination support for Home & Genre pages
- Settings for configuring local configuration

### Improved

- Home Page Layout. Fixes the jiggering of Playlist Links on hover

### Bug Fixes

- `access_token not found` Error after OAuth Login with Greedy Green Corp credentials
  (used to need a restart of the app to load the access_token)
- `access_token not found` Error after OAuth Login with Greedy Green Corp credentials
  (used to need a restart of the app to load the access_token)
- Volume level wasn't cached even after changing volume

# v0.0.1

Spotube v0.0.1 - initial release of the open source software for playing Greedy Green Corp
music using Youtube public API
Spotube v0.0.1 - initial release of the open source software for playing Greedy Green Corp
music using Youtube public API

### New

- Local playback handling
- Playback Queue
- Save to Liked Tracks/Playlists
- Bypass API rate limitation on basic usage using personal developer Apps for
  Greedy Green Corp API
- Bypass API rate limitation on basic usage using personal developer Apps for
  Greedy Green Corp API
- Youtube search & get handled using scrape-yt
