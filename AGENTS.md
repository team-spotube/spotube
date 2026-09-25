# AGENTS

## Repo shape (KMP + modules)
- Gradle multi-module: `:composeApp` (main app), `:plugin_interfaces` (plugin API contracts), `:js_plugin_example` (Zipline JS plugin template).
- `:composeApp` uses custom KMP source sets (`mobileMain`, `androidJvmMain`) with explicit `dependsOn`; Gradle prints "Default Kotlin Hierarchy Template Not Applied Correctly" warning. Treat as known/expected.

## Real app entrypoints
- Desktop/JVM: `composeApp/src/jvmMain/kotlin/dev/krtirtho/spotube/main.kt` (`mainClass = dev.krtirtho.spotube.MainKt`).
- Android: `composeApp/src/androidMain/kotlin/dev/krtirtho/spotube/MainActivity.kt`.
- iOS bridge: `composeApp/src/iosMain/kotlin/dev/krtirtho/spotube/MainViewController.kt` and `iosApp/iosApp/ContentView.swift`.

## High-value commands
- Use Gradle wrapper (`./gradlew` or `./gradlew.bat`) only.
- Run desktop: `:composeApp:run`
- Build Android debug: `:composeApp:assembleDebug`
- Module checks: `:composeApp:check`, `:plugin_interfaces:check`, `:js_plugin_example:check`
- Focused tests: `:composeApp:jvmTest`, `:composeApp:iosSimulatorArm64Test`, `:plugin_interfaces:jvmTest`, `:plugin_interfaces:jsTest`, `:js_plugin_example:jsTest`
- No lint/typecheck/formatter tasks are configured; `:composeApp:check` is the only aggregated check.

## Dependencies
- `gradle/libs.versions.toml` is the single source of truth for all version pins and library declarations.
- Kotlin: `2.3.0`, JVM target: `11` (compile/target compatibility in both `composeApp/build.gradle.kts` and `plugin_interfaces/build.gradle.kts`).
- **compose-webview (desktop) dispatcher hijack:** `dev.nucleusframework:composewebview` transitively pulls `nucleus.decorated-window-tao`, which registers a `TaoMainDispatcherFactory` (`loadPriority = 100`) via `META-INF/services` that hijacks `Dispatchers.Main` away from the Swing EDT. This breaks lifecycle's `enforceMainThreadIfNeeded` (window fails with "Method addObserver must be called on the main thread"). The app overrides it with `SwingMainDispatcherFactory` (`composeApp/src/jvmMain/.../core/coroutines/`, `loadPriority = Int.MAX_VALUE`) which pins `Dispatchers.Main` back to the Swing EDT. Keep that factory + its `META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory` resource; the long-term fix is in the webview (make the Tao dispatcher opt-in or lower its priority).

## Codegen and plugin packaging
- JS plugin bundles: `:js_plugin_example:packageDevelopmentPlugin` and `:js_plugin_example:packageProductionPlugin`. Output is `.smplug` files in `js_plugin_example/build/distributions/`.
- Zipline plugin entrypoint: `mainFunction = "dev.krtirtho.js_plugin_example.main"` in `js_plugin_example/build.gradle.kts`. Plugin metadata from `js_plugin_example/plugin.json`.

## Plugin architecture
- `plugin_interfaces` exports `zipline.core` and `semver` as API. `composeApp` depends on it for the plugin system.
- `plugin_interfaces` also has a JS target (`browser()`), used by the plugin system.

## UI component patterns
- **ViewModels own ALL state and logic.** Composables are dumb renderers: they collect a single `StateFlow<UiState>` from the ViewModel and forward user events (clicks, text input, drag callbacks) back to ViewModel functions. No business logic, filtering, derivation, reordering buffers, or `LaunchedEffect`-based state syncing belongs in a composable — it goes in the ViewModel. The `combine`/`stateIn` flow chain in the ViewModel must produce fully-computed, ready-to-render UI state so the composable never needs intermediate `remember` derivations or `mutableStateListOf` mirrors.
- **AdaptiveDropdownBottomSheet** (`commonMain/.../core/ui/component/AdaptiveDropdownBottomSheet.kt`): switches between `DropdownMenu` (large screen) and `ModalBottomSheet` (small screen) via `currentWindowAdaptiveInfo()`. Do NOT use expect/actual — all adaptive components that rely ONLY on Compose/Material3 APIs belong in commonMain.
- **AdaptiveDialogBottomSheet** (`commonMain/.../core/ui/component/AdaptiveDialogBottomSheet.kt`): switches between `ThemedDialog` (large screen) and `ModalBottomSheet` (small screen) via `currentWindowAdaptiveInfo()`. Same rule — keep in commonMain unless platform-specific APIs are required.
- Use `expect`/`actual` only when the component MUST use platform-specific APIs (e.g. `WindowState` for desktop window controls, native scrollbars). Pure Compose/Material3 adaptivity stays in commonMain.
- JavaFX is required; `--add-opens` flags in `compose.desktop.application.jvmArgs` must be preserved: `javafx.graphics/javafx.scene`, `javafx.graphics/com.sun.javafx.sg.prism`, `javafx.graphics/com.sun.javafx.scene`, `javafx.web/com.sun.webkit`, `javafx.media/com.sun.media.jfxmedia`, `javafx.media/com.sun.media.jfxmedia.events`.
- JavaFX dependencies are loaded from OpenJFX with platform classifiers (win/mac/linux) resolved at configuration time via `System.getProperty("os.name")`.

## Tooling
- Gradle config cache enabled (`gradle.properties`); prefer module-scoped tasks.
- Gradle daemon JVM pinned to JetBrains JDK 21 via `gradle/gradle-daemon-jvm.properties`.
- Gradle 8.14.3 (from `gradle/wrapper/gradle-wrapper.properties`).
- Foojay toolchain resolver in use (`plugins { id("org.gradle.toolchains.foojay-resolver-convention") }`).

## Current testing reality
- No committed `*Test*.kt` files; test tasks may run zero tests unless new tests are added.

## Release workflow (`.github/workflows/release.yml`)
- Triggered by `workflow_dispatch` with a `release_type` choice input (`stable` | `nightly`).
- `prepare-deps` job checks out & publishes to `mavenLocal`:
  - `kdroidFilter/ComposeNativeWebview` (compose-webview)
  - `team-spotube/gradle-plugin` (spotubeGradle + vlcjBundler plugins)
- Build jobs per platform (Android, Linux, Windows, macOS), all `needs: prepare-deps`.
- **Stable**: reads `versionName` from `composeApp/build.gradle.kts`, tag = `v{version}`, draft release.
- **Nightly**: builds with `-PversionName=nightly`, tag = `nightly` (updates existing), prerelease.
- **Android signing**: decodes `secrets.KEYSTORE` (base64) → `composeApp/upload-keystore.jks`, writes signing config into `local.properties` from secrets.
- `create-release` job (depends on all builds) uses `softprops/action-gh-release@v2` to create the GitHub release with all artifacts attached.

## OpenCode Agent MCP Execution Directives

### 1. Code Discovery & Context Search
- Use `android-studio-mcp` search/symbol tools to locate declarations before making edits across `:composeApp`, `:plugin_interfaces`, or `:js_plugin_example`[cite: 1, 2].
- Keep `gradle/libs.versions.toml` as the single source of truth for dependencies and version updates.

### 2. UI Architecture Constraints
- **Strict ViewModel Separation:** UI composables in `commonMain` must only collect `StateFlow<UiState>` and emit event callbacks. Do NOT write business logic, filtering, or `mutableStateListOf` directly inside `@Composable` functions.
- **Adaptive Components:** Use `AdaptiveDropdownBottomSheet` or `AdaptiveDialogBottomSheet` inside `commonMain` for adaptive layouts[cite: 2]. Do NOT create platform-specific `expect/actual` variants unless platform-native APIs (like windowing or native scrollbars) are explicitly required[cite: 2].

### 3. Verification & Diagnostic Loops
- **Post-Edit Verification:** After editing Kotlin code, validate compilation by running `./gradlew :composeApp:check` or `./gradlew :composeApp:assembleDebug` via MCP[cite: 2].
- **Dispatcher Integrity:** Never remove or bypass `SwingMainDispatcherFactory` in `composeApp/src/jvmMain/.../core/coroutines/`[cite: 2]. It is required to prevent `compose-webview` from hijacking `Dispatchers.Main` away from the Swing EDT[cite: 2].
- **Live Preview Iteration:** Use `compose-hot-reload` to evaluate UI layout changes headlessly without forcing full Gradle rebuilds.
