<div align="center">
  <img width="600" src="assets/branding/spotube_banner.png" alt="Spotube Logo">

A cross-platform extensible open-source music streaming platform.<br>
Bring your own music metadata/playlist/audio-source with plugins created by community or by yourself. A small step towards the decentralized music streaming era!

<a href="https://spotube.cc"><img alt="Visit the website" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/website_vector.svg"></a>
<a href="https://discord.gg/uJ94vxB6vg"><img alt="Discord Server" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg"></a>

<a href="https://patreon.com/krtirtho"><img alt="Support me on Patron" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/patreon-singular_vector.svg"></a>
<a href="https://www.buymeacoffee.com/krtirtho"><img alt="Buy me a Coffee" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg"></a>

[![HackerNews](https://hackerbadge.vercel.app/api?id=39066136&type=dark)](https://news.ycombinator.com/item?id=39066136)

<a href="https://opencollective.com/spotube"><img src="https://opencollective.com/spotube/donate/button.png?color=blue" alt="Donate to our Open Collective" height="45"></a>

---

![Spotube Desktop](assets/branding/spotube-screenshot.png)

![Spotube Mobile](assets/branding/mobile-screenshots/combined.jpg)

</div>

## 🌃 Features

- 🧩 Plugin powered, supports any platform or custom music service through plugins.
- 🗺️ Community driven plugins for popular platforms or create your own.
- ⬇️ Freely downloadable tracks with tagged metadata.
- 🖥️ 📱 Cross-platform support.
- 🪶 Small size & less data usage.
- 🕒 Time synced lyrics regardless of the plugin support.
- ✋ No telemetry, diagnostics or user data collection.
- 🚀 Native performance.
- 📖 Open source/libre software.
- 🔉 Playback control is done locally, not on the server.

### ⬇️ Download

Download Spotube from the Official Website: https://spotube.cc

### 🔄 Nightly Builds

Grab the latest nightly builds of Spotube [from the Offical Website](https://spotube.cc/downloads/nightly).

### 🧩 Plugins

Spotube supports metadata and source plugins compiled as `.smplug` files.

You can install plugins directly in Spotube:
1. Navigate to **Settings** > **Plugins** > **Install a Plugin**.
2. Under **Download from URL**, paste the direct plugin URL, for example:
   - **Spotify Plugin (Latest)**: `https://github.com/pgwiz/spotube-plugin-spotify/releases/latest/download/plugin.smplug`
   - **Spotify Plugin (v1.0.0)**: `https://github.com/pgwiz/spotube-plugin-spotify/releases/download/1.0.0/plugin.smplug`
   - **Legacy Plugin Reference**: `https://github.com/sonic-liberation/spotube-plugin-spotify/releases/download/0.2.2/plugin.smplug` *(Format: `https://github.com/$LEGACY_REPO/releases/download/$LEGACY_TAG/plugin.smplug`)*
3. Click **Download**, review permissions, and confirm.

## 🕳️ Building from source

<a href="https://github.com/team-spotube/spotube/actions"><img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/KRTirtho/spotube/spotube-release-binary.yml?+label=Build%20Status"></a>

Spotube is a Kotlin Multiplatform + Compose Multiplatform app. The desktop app is built with the [Nucleus](https://nucleusframework.dev) Gradle plugin and released as a **GraalVM native image**: an ahead-of-time compiled binary that needs no JVM installed.

### Prerequisites

- **JDK 21** to run Gradle. The Gradle daemon JDK and the GraalVM toolchain are provisioned automatically.
- **Rust** (`rustup` + `cargo`). The `composeApp` Rust library is built through [Gobley](https://gobley.dev).
- **Android SDK**, only for Android builds.
- **Unpublished dependency.** One dependency isn't on Maven Central yet. Clone it and run `./gradlew publishToMavenLocal` in it:
  - [kdroidFilter/ComposeNativeWebview](https://github.com/kdroidFilter/ComposeNativeWebview), at the commit pinned in [`spotube-release-binary.yml`](.github/workflows/spotube-release-binary.yml) (`COMPOSE_WEBVIEW_COMMIT`)
- **Linux runtime libraries** (needed to run the app, including `:composeApp:run`). The `.deb`/`.rpm` install these as dependencies from your distro. For a source build, install them yourself, **including VLC**. Only the `.AppImage` is self-contained (it bundles VLC, GTK 3 and WebKitGTK, and uses just the host's glibc, graphics, X11 and sound libraries):

  | Distro | Command |
  |---|---|
  | Debian/Ubuntu | `sudo apt-get install libvlc5 vlc-plugin-base libwebkit2gtk-4.1-0 libgtk-3-0 libasound2 libgl1 libxtst6 libxi6 libxrender1` |
  | Fedora | `sudo dnf install vlc-libs webkit2gtk4.1 gtk3 alsa-lib mesa-libGL libXtst libXi libXrender`. `vlc-libs` is only in [RPM Fusion](https://rpmfusion.org/Configuration), so enable it first. |
  | Arch / CachyOS / Manjaro | `sudo pacman -S vlc webkit2gtk-4.1 gtk3 alsa-lib libglvnd libxtst libxi libxrender` |
  | openSUSE | `sudo zypper install libvlc5 libwebkit2gtk-4_1-0 libgtk-3-0 libasound2 Mesa-libGL1 libXtst6 libXi6 libXrender1` (for wide codec support, switch to the [Packman](https://en.opensuse.org/Additional_package_repositories#Packman) repo's VLC) |

  Arch-based distros and openSUSE have no native package here. Use the `.AppImage`. The `.rpm` is built with Fedora package names, so it won't resolve on openSUSE.

  On Ubuntu 24.04+/Debian 13 `libgtk-3-0` and `libasound2` are named `libgtk-3-0t64` and `libasound2t64`; apt resolves this automatically.

Always use the Gradle wrapper (`./gradlew`, or `gradlew.bat` on Windows).

### Development

```bash
./gradlew :composeApp:run                    # desktop (JVM), fastest edit/run loop
./gradlew :composeApp:assembleStableDebug    # Android debug APK (or assembleNightlyDebug)
```

Development builds stay fast. The release-only optimizations (the Rust release profile with LTO and symbol stripping, the native image, native-library pruning) are only applied when you build something to ship. See [Production vs development builds](#production-vs-development-builds).

### Building the desktop app (GraalVM native image)

#### Native toolchain

[Nucleus downloads and caches GraalVM CE](https://nucleusframework.dev/en/docs/performance/graalvm/) under `~/.gradle/nucleus/graalvm` on the first native build. Set `GRAALVM_HOME` to use your own GraalVM instead. Each OS also needs a native C toolchain:

| OS | Requirements |
|---|---|
| Linux | `gcc`, `zlib` headers, `patchelf`; plus `rpm` for `.rpm` packages, `fuse` for `.AppImage`, and, for the AppImage's bundled libraries, `zstd` (plus `ar`/`tar` and `readelf`, from binutils). Debian/Ubuntu: `sudo apt-get install build-essential zlib1g-dev patchelf rpm fuse zstd`. Fedora: `sudo dnf install gcc zlib-devel patchelf rpm-build fuse zstd` (also install the [Linux runtime libraries](#prerequisites) to run what you build) |
| macOS | Xcode Command Line Tools (`xcode-select --install`) |
| Windows | Visual Studio Build Tools with the "Desktop development with C++" workload. Build from an "x64 Native Tools" prompt if `native-image` can't find MSVC. |

A native build is a whole-program compile. It takes **about 10 minutes**, uses **every CPU core**, and peaks at roughly **15 GB of RAM** (it gets slower, not broken, with less). That only happens for the native tasks below, never for `run`.

#### Build and run

```bash
# App folder: binary + its native libs, in composeApp/build/compose/binaries/main/graalvm-app/
./gradlew :composeApp:createGraalvmNativeDistributable

# Run that folder's binary directly
./composeApp/build/compose/binaries/main/graalvm-app/org.spotube.spotube/spotube

# Or build and launch in one step
./gradlew :composeApp:runGraalvmNativeDistributable
```

Keep the `spotube` binary next to the `.so`/`.dll`/`lib/` files it was built with; it loads them from its own folder.

#### Installers

Each OS can only build its own installers:

| OS | Command | Output |
|---|---|---|
| Linux | `./gradlew :composeApp:packageGraalvmDeb :composeApp:packageGraalvmRpm`, then `./gradlew :composeApp:packageGraalvmAppImage -PappImage=true` | `composeApp/build/compose/binaries/main/graalvm-{deb,rpm,appimage}/` |
| Windows | `./gradlew :composeApp:packageGraalvmExe` (or `packageGraalvmMsi`) | `composeApp/build/compose/binaries/main/graalvm-exe/` |
| macOS | `./gradlew :composeApp:packageGraalvmDmg` | `composeApp/build/compose/binaries/main/graalvm-dmg/` |

Add `-Pnightly=true` for the nightly channel (separate icons and name).

The `.deb` and `.rpm` use your distro's shared libraries and declare them as dependencies, so the package manager installs them (on Fedora, `vlc-libs` needs RPM Fusion enabled). The `.AppImage` is the self-contained one: it bundles VLC, GTK 3 and WebKitGTK (see [What the AppImage bundles](#what-the-appimage-bundles)).

#### What the AppImage bundles

Build it with `./gradlew :composeApp:packageGraalvmAppImage -PappImage=true`. Without `-PappImage=true` nothing is bundled and the app uses the system libraries, which is what the `.deb`/`.rpm` and `:composeApp:run` do.

The bundle follows the [AppImage exclude list](https://github.com/AppImageCommunity/pkg2appimage/blob/master/excludelist) and what Tauri apps ship. It carries VLC (audio-only, from the [vlcj-bundler](https://github.com/team-spotube/vlcj-bundler-gradle-plugin) plugin's `bundleVlc` option), GTK 3, WebKitGTK 4.1, GLib/GIO, pango/cairo and their dependencies, plus glib-networking for HTTPS in the webview. It leaves the host's glibc, `libstdc++`, `libz`, GL/EGL/DRM, X11/xcb, ALSA, fontconfig/freetype/harfbuzz and the Wayland libraries alone, so the host's graphics, display and sound stack is used. Everything is taken from the Ubuntu 24.04 archive, so the AppImage needs glibc 2.38 or newer.

How it works, in short:
- [`DownloadLinuxGtkBundleTask`](buildSrc/src/main/kotlin/org/spotube/gradle/DownloadLinuxGtkBundleTask.kt) downloads the packages, keeps the libraries reachable from GTK/WebKit and relocates them with `patchelf`. Needs `ar`, `tar`, `zstd`, `readelf` and `patchelf`.
- WebKitGTK has its helper-process path compiled in, so the library is patched to look relative to the working directory, and at startup the app moves there ([`AppImageEnvironment`](composeApp/src/jvmMain/kotlin/org/spotube/spotube/core/appimage/AppImageEnvironment.kt)). It also points `GIO_EXTRA_MODULES` at the bundled TLS module and sets `WEBKIT_DISABLE_DMABUF_RENDERER=1`, because the bundled WebKitGTK 2.44's dmabuf renderer never paints (or aborts) against a newer host Mesa/compositor. The display backend is left alone: forcing X11 doesn't help, since the webview doesn't paint on XWayland either.
- Startup order matters: `main()` calls `GraalVmInitializer.initialize()` first, since the VLC loader and the environment setup read `compose.application.resources.dir`.

#### Reflection metadata (when a feature breaks only in the native build)

A native image is a closed world: anything the app reaches through reflection, JNI or JNA (vlcj, the Zipline/QuickJS plugin engine, NewPipe, ktor, datastore, …) must be declared at build time. Nucleus generates most of this automatically. The rest is in [`composeApp/graalvm/reachability-metadata.json`](composeApp/graalvm/reachability-metadata.json).

If something works in `:composeApp:run` but throws `ClassNotFoundException`, `NoSuchMethodException`, `MissingReflectionRegistrationError` or similar in the native binary, record what's missing:

```bash
./gradlew :composeApp:runWithNativeAgent
```

The app starts on the JVM with GraalVM's tracing agent attached. Use the broken feature (and anything near it), then **close the window normally**: the recorded entries are merged into `composeApp/graalvm/` only on a clean exit, not with Ctrl+C. Commit the updated JSON and rebuild the native image.

`./gradlew :composeApp:runGraalvmNative` gives a faster debugging loop: a quick-build (`-Ob`) native image that reports exactly which registration is missing.

Keep the hand-written `uk.co.caprica.vlcj.binding.lib.LibVlc` entry. JNA's direct mapping looks up its `native` methods through reflection, and without the entry, playback fails to initialize.

### Production vs development builds

Production mode is switched on automatically when you ask Gradle for a shippable artifact: any `package*`, `create*`, `notarize*` or `nativeImageCompile` task. You can force it either way with `-Pproduction=true` or `-Pproduction=false`.

| | Development (`run`, debug builds) | Production (installers, native image) |
|---|---|---|
| Rust library (desktop) | `debug` profile, fast incremental compile | `release` profile with LTO, single codegen unit, stripped (31 MB → 1.8 MB) |
| Desktop app | JVM, hot-reloadable | GraalVM native image, `-Os` (size-optimized) |
| Native libraries | everything on the classpath | only the host OS/CPU's natives; other OSes' and CPUs' libraries are pruned from the uber JAR the image is compiled from |
| Skia (Skiko) | extracted from the JAR | loaded from `lib/` next to the binary, not embedded a second time |
| JDK runtime libs (Linux) | n/a | native-image's ~10 KB `libjvm.so`/`libjava.so` shims instead of the JDK's (28 MB `libjvm.so`) |

Android picks the Rust profile from its build type (`debug` or `release`) on its own.

### Releases

Releases are built by [`spotube-release-binary.yml`](.github/workflows/spotube-release-binary.yml), run manually (`workflow_dispatch`) as `stable`, `beta` or `nightly`. A beta is the stable build released like a stable release (draft, not a pre-release), tagged `v<version>-beta.N` (set `betaNumber` in `composeApp/build.gradle.kts` to N, and back to `null` for stable). Each desktop platform (Linux x86_64 and aarch64, Windows, macOS) builds its native installers on its own runner. Android builds a signed APK.

## 👥 The Spotube team

- [Kingkor Roy Tirtho](https://github.com/team-spotube) - The Founder, Maintainer and Lead Developer
- [Owen Connor](https://github.com/owencz1998) - The Cool Discord Moderator
- [Piotr Rogowski](https://github.com/karniv00l) - The MacOS Developer
- [Rusty Apple](https://github.com/RustyApple) - The Mysterious Unknown Guy

## 💼 License

Spotube is open source and licensed under the [AGPL-3.0-or-later](/LICENSE) License.

<div align="center"><h4>© Copyright Spotube 2026</h4></div>
