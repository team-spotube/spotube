# AGENTS

## Repo shape (KMP + modules)
- Gradle multi-module: `:composeApp` (main app), `:plugin_interfaces` (plugin API contracts), `:js_plugin_example` (Zipline JS plugin template).
- `:composeApp` uses custom KMP source sets (`mobileMain`, `androidJvmMain`) with explicit `dependsOn`; Gradle prints "Default Kotlin Hierarchy Template Not Applied Correctly" warning. Treat as known/expected.

## Real app entrypoints
- Desktop/JVM: `composeApp/src/jvmMain/kotlin/org/spotube/spotube/main.kt` (`mainClass = org.spotube.spotube.MainKt`).
- Android: `composeApp/src/androidMain/kotlin/org/spotube/spotube/MainActivity.kt`.
- iOS bridge: `composeApp/src/iosMain/kotlin/org/spotube/spotube/MainViewController.kt` and `iosApp/iosApp/ContentView.swift`.

## High-value commands
- Use Gradle wrapper (`./gradlew` or `./gradlew.bat`) only.
- Run desktop: `:composeApp:run`
- Build Android debug: `:composeApp:assembleStableDebug` (or `assembleNightlyDebug`). Android has `stable`/`nightly` product flavors (nightly = beta channel: `.nightly` applicationId suffix, own icons/name in `composeApp/src/nightly/res`). Desktop icons come from `composeApp/icons/{stable,nightly}`, chosen with `-Pnightly=true`.
- Module checks: `:composeApp:check`, `:plugin_interfaces:check`, `:js_plugin_example:check`
- Focused tests: `:composeApp:jvmTest`, `:composeApp:iosSimulatorArm64Test`, `:plugin_interfaces:jvmTest`, `:plugin_interfaces:jsTest`, `:js_plugin_example:jsTest`
- No lint/typecheck/formatter tasks are configured; `:composeApp:check` is the only aggregated check.

## Dependencies
- `gradle/libs.versions.toml` is the single source of truth for all version pins and library declarations.
- Kotlin: `2.4.21`, JVM target: `11` (compile/target compatibility in both `composeApp/build.gradle.kts` and `plugin_interfaces/build.gradle.kts`).
- **compose-webview (desktop) dispatcher hijack:** `dev.nucleusframework:composewebview` transitively pulls `nucleus.decorated-window-tao`, which registers a `TaoMainDispatcherFactory` (`loadPriority = 100`) via `META-INF/services` that hijacks `Dispatchers.Main` away from the Swing EDT. This breaks lifecycle's `enforceMainThreadIfNeeded` (window fails with "Method addObserver must be called on the main thread"). The app overrides it with `SwingMainDispatcherFactory` (`composeApp/src/jvmMain/.../core/coroutines/`, `loadPriority = Int.MAX_VALUE`) which pins `Dispatchers.Main` back to the Swing EDT. Keep that factory + its `META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory` resource; the long-term fix is in the webview (make the Tao dispatcher opt-in or lower its priority).

## Networking
- Never call `HttpClient(...)` directly. Inject `HttpClientFactory` (`commonMain/.../core/network/HttpClientFactory.kt`, a Koin single) and use `.default` / `.json` (shared, never close them) or `.create { ... }` for extra plugins (caller owns and may close it). It owns the one engine-backed client, so the platform default engine is used everywhere (Darwin on iOS, OkHttp on Android/desktop).
- Do not add `ktor-client-cio`: on iOS it competes with Darwin as the default engine and has no TLS on Kotlin/Native, so every HTTPS request fails.

## Group Jam
- Room server: `jam-server/` (Cloudflare Worker + one Durable Object per six-letter room code, TypeScript, wrangler). It relays/retains small JSON frames and serves the clock; no audio passes through it. Protocol in `jam-server/src/protocol.ts`; keep it in sync with the wire frames in `core/jam/JamRoomClient.kt` (bump `PROTOCOL_VERSION` on both sides for breaking changes).
- App side: `core/jam/` — `JamRoomClient` (Ktor WebSocket, reconnects, keep-alives, clock pings), `JamClock`/`JamDriftCorrector` (sync math), `JamRoomService` (host broadcasts queue + playback anchors in server time; guests follow by nudging playback rate, seeking only when far off).
- Cost and battery: the room is billed per incoming message and storage write, and on phones each transmission wakes the cellular radio. Don't add periodic frames or polling. Keep-alives go out once a minute. Guests' sync loop sleeps while the room is paused or buffering, and the host checks its own drift only while playing. Anchors go out on change (plus host drift and a 60 s heartbeat), keep-alives are runtime auto-responses, and anchors aren't persisted. See `jam-server/README.md`.
- A network drop must never crash the app: `JamRoomClient.pump` turns socket failures into a reconnect. `JamSessionSimulationTest.roomSurvivesNetworkLoss` cuts the network with `TcpProxy`.
- Server URL setting: `UserSettings.jamServerUrl` (default `DEFAULT_JAM_SERVER_URL` in `SettingsModels.kt`).
- Tests: `JamSyncTest` (pure), plus `JamRoomClientIntegrationTest` and `JamSessionSimulationTest`, which only run with `JAM_TEST_URL` set (start `npm run dev` in `jam-server/`).

## GraalVM native image (desktop)
- Enabled via `graalvm { }` inside `nucleus.application` in `composeApp/build.gradle.kts`. Nucleus downloads GraalVM CE itself (or uses `$GRAALVM_HOME`). Linux needs `gcc` + `patchelf`.
- Build: `:composeApp:createGraalvmNativeDistributable` (output `composeApp/build/compose/binaries/main/graalvm-app/`), installers via `packageGraalvm{Deb,Rpm,AppImage,Dmg,Msi,Exe}`. A build takes ~10 min and uses all cores and ~15 GB RAM.
- Hand-maintained + agent-recorded metadata lives in `composeApp/graalvm/reachability-metadata.json`. After adding features that use reflection/JNI/JNA (vlcj, Zipline, plugins), run `:composeApp:runWithNativeAgent`, exercise the feature, then close the window normally (the merge only happens on a clean exit).
- `uk.co.caprica.vlcj.binding.lib.LibVlc` must stay registered (`allDeclaredMethods` + `jniAccessible`): JNA direct mapping finds its `native` methods by reflection.
- The `*UberJarForCurrentOS` task excludes other OSes'/CPUs' native libs and stale CEF binaries, because native-image embeds every registered resource into the binary. On Linux, `copyGraalvmJvmSo` is disabled and `copyGraalvmAwtSoLibs` prefers native-image's own libs (10 KB `libjvm.so`/`libjava.so` shims instead of the JDK's 28 MB `libjvm.so`).
- `isProductionBuild` (top of `composeApp/build.gradle.kts`) is true for `package*`/`create*`/`notarize*`/`nativeImageCompile` tasks, or with `-Pproduction=true|false`. It gates the Rust release profile (Gobley `jvmVariant`) so `:composeApp:run` keeps fast debug Rust builds. Put new slow, ship-only optimizations behind it.
- Full user-facing build docs are in `README.md` ("Building from source").

## Codegen and plugin packaging
- JS plugin bundles: `:js_plugin_example:packageDevelopmentPlugin` and `:js_plugin_example:packageProductionPlugin`. Output is `.smplug` files in `js_plugin_example/build/distributions/`.
- Zipline plugin entrypoint: `mainFunction = "org.spotube.js_plugin_example.main"` in `js_plugin_example/build.gradle.kts`. Plugin metadata from `js_plugin_example/plugin.json`.

## Plugin architecture
- `plugin_interfaces` exports `zipline.core` and `semver` as API. `composeApp` depends on it for the plugin system.
- `plugin_interfaces` also has a JS target (`browser()`), used by the plugin system.

## UI component patterns
- **ALL user-facing strings MUST come from `composeApp/src/commonMain/composeResources/values/strings.xml` via `stringResource(Res.string.xxx)`.** Never hardcode a literal string in a `Text(...)`, dialog title, button label, content description, placeholder, or error message inside `@Composable` code. If the string you need doesn't exist yet, add a new `<string name="...">` entry to `strings.xml` (grouped near related keys, following the existing `feature_context_purpose` naming convention) and reference it — do not inline the text "just this once". This applies to every module, including ones you're about to add (dialogs, screens, snackbars, etc.), not just `modules/settings`. `strings.xml` is the single source of truth for copy, not a showcase file for one screen.
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
- `prepare-deps` job checks out & publishes `kdroidFilter/ComposeNativeWebview` (compose-webview) to `mavenLocal`. The spotube gradle plugin and vlcj-bundler now come from Maven Central.
- Build jobs per platform (Android, Linux, Windows, macOS), all `needs: prepare-deps`. Desktop jobs ship GraalVM native installers (`packageGraalvm{Deb,Rpm,Exe,Dmg}`, output in `composeApp/build/compose/binaries/main/graalvm-<format>/`), not jpackage/JVM ones.
- **Stable**: reads `versionName` from `composeApp/build.gradle.kts`, tag = `v{version}`, draft release.
- **Nightly**: builds with `-PversionName=nightly`, tag = `nightly` (updates existing), prerelease.
- **Android signing**: decodes `secrets.KEYSTORE` (base64) → `composeApp/upload-keystore.jks`, writes signing config into `local.properties` from secrets.
- `create-release` job (depends on all builds) uses `softprops/action-gh-release@v2` to create the GitHub release with all artifacts attached.

## Package managers (`packaging/`, `.github/workflows/spotube-publish-binary.yml`)
- `packaging/<target>` (`linux`, `aur`, `flatpak`, `winget`, `chocolatey`) holds JMustache templates (`*.mustache`) plus files copied as is. `:composeApp:renderPackaging<Target>` renders them into `composeApp/build/packaging/<target>` (`RenderPackagingTask` in buildSrc documents the template data). Never hand-write versions, checksums or release notes into these files.
- Checksums come from release assets in `-PpackagingArtifactsDir=<dir>`; a template using a missing value fails the render. Release notes come from `git-cliff --context` with `cliff.toml` (`GitCliffContextTask`): needs `git-cliff` on the PATH and full git history. `gradle.properties` defaults to `nightly=true`, which leaves commits unreleased: pass `-Pnightly=false` for a stable render.
- `packaging/package-linux-tarball.sh` packs `spotube-linux-<version>-<arch>.tar.xz` (the AUR and Flathub source) from the GraalVM distributable and the rendered `linux` dir; the release workflow builds it before the AppImage step.
- The publish workflow (`workflow_dispatch`, input `tag`) renders once, then pushes to Flathub (`team-spotube/org.spotube.spotube`), the AUR (`spotube-bin`), WinGet (PR from `team-spotube/winget-pkgs`) and Chocolatey.

## OpenCode Agent MCP Execution Directives

### 1. Code Discovery & Context Search
- Use `android-studio-mcp` search/symbol tools to locate declarations before making edits across `:composeApp`, `:plugin_interfaces`, or `:js_plugin_example`[cite: 1, 2].
- Keep `gradle/libs.versions.toml` as the single source of truth for dependencies and version updates.

### 2. UI Architecture Constraints
- **Strict ViewModel Separation:** UI composables in `commonMain` must only collect `StateFlow<UiState>` and emit event callbacks. Do NOT write business logic, filtering, or `mutableStateListOf` directly inside `@Composable` functions.
- **Adaptive Components:** Use `AdaptiveDropdownBottomSheet` or `AdaptiveDialogBottomSheet` inside `commonMain` for adaptive layouts[cite: 2]. Do NOT create platform-specific `expect/actual` variants unless platform-native APIs (like windowing or native scrollbars) are explicitly required[cite: 2].

### 3. Verification & Diagnostic Loops
- **Post-Edit Verification:** After editing Kotlin code, validate compilation by running `./gradlew :composeApp:check` or `./gradlew :composeApp:assembleStableDebug` via MCP[cite: 2].
- **Dispatcher Integrity:** Never remove or bypass `SwingMainDispatcherFactory` in `composeApp/src/jvmMain/.../core/coroutines/`[cite: 2]. It is required to prevent `compose-webview` from hijacking `Dispatchers.Main` away from the Swing EDT[cite: 2].
- **Live Preview Iteration:** Use `compose-hot-reload` to evaluate UI layout changes headlessly without forcing full Gradle rebuilds.
