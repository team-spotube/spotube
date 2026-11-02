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

import gobley.gradle.GobleyHost
import gobley.gradle.Variant
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustAndroidTarget
import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy
import org.spotube.gradle.DownloadLinuxGtkBundleTask
import org.spotube.gradle.GitCliffContextTask
import org.spotube.gradle.RenderPackagingTask
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.zipline.gradle.plugin)
    alias(libs.plugins.vlcjBundler)
    alias(libs.plugins.uniffi)
    alias(libs.plugins.cargo)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.neucleusFramework)
    kotlin("plugin.atomicfu") version libs.versions.kotlin
}

val bundleId = "org.spotube.spotube"
val version = "6.0.0"
// Set to N to release <version>-beta.N (same app id, icons and name as stable); null for a plain stable release.
// Installer metadata (packageVersion) must stay numeric, so it keeps using plain `version`.
val betaNumber: Int? = 0
// Betas share the stable app id, so bump this for every beta: Android only updates to a higher versionCode.
val buildNumber = 46
// Desktop icon channel; Android picks its icons via the stable/nightly product flavor. Use -Pnightly=true.
val isNightly = providers.gradleProperty("nightly").map { it.toBoolean() }.getOrElse(false)
val desktopIconDir = if (isNightly) "icons/nightly" else "icons/stable"
// Nightlies are numbered by the CI run (-PbuildNumber=<run>, or GITHUB_RUN_NUMBER) so every build is traceable to its run.
val nightlyBuildNumber = providers.gradleProperty("buildNumber").orNull?.toIntOrNull()
    ?: System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
    ?: buildNumber
// Production = shipping artifacts (installers, distributables, native images). Size/speed
// optimizations that slow builds down (Rust release + LTO) only apply then, never to `run`/dev.
// Override with -Pproduction=true|false.
val isProductionBuild = providers.gradleProperty("production").map { it.toBoolean() }.getOrElse(
    gradle.startParameter.taskNames.any { task ->
        task.substringAfterLast(':').let { name ->
            name.startsWith("package") || name.startsWith("create") ||
                name.startsWith("notarize") || name == "nativeImageCompile"
        }
    }
)
val targetName = "Spotube"
val homePageUrl = "https://spotube.cc"
val repositoryUrl = "https://github.com/team-spotube/spotube"
val appVersionName = if (betaNumber != null) "$version-beta.$betaNumber" else version
val appBuildNumber = if (isNightly) nightlyBuildNumber else buildNumber
val appRepositoryUrl = repositoryUrl
// CI reads the release version from here instead of scraping this file.
tasks.register("printVersion") {
    val appVersion = appVersionName
    doLast { println(appVersion) }
}

// Package manager manifests: packaging/<target> rendered into build/packaging/<target> with the version, the
// release assets' checksums (-PpackagingArtifactsDir=<dir>) and the git-cliff changelog. See RenderPackagingTask.
val gitCliffContext = tasks.register<GitCliffContextTask>("gitCliffContext") {
    configFile.set(rootProject.layout.projectDirectory.file("cliff.toml"))
    repositoryDir.set(rootProject.layout.projectDirectory)
    // Nightlies are not a release: their commits stay unreleased
    if (!isNightly) tag.set("v$appVersionName")
    outputFile.set(layout.buildDirectory.file("packaging/changelog-context.json"))
}
val packagingRootDir = rootProject.layout.projectDirectory
val packagingArtifactsDir = packagingRootDir.dir(providers.gradleProperty("packagingArtifactsDir"))
listOf("linux", "aur", "flatpak", "winget", "chocolatey").forEach { target ->
    tasks.register<RenderPackagingTask>("renderPackaging${target.replaceFirstChar { it.uppercase() }}") {
        description = "Renders packaging/$target into build/packaging/$target."
        templateDir.set(packagingRootDir.dir("packaging/$target"))
        version.set(appVersionName)
        pkgrel.set(providers.gradleProperty("pkgrel").orElse("1"))
        repositoryUrl.set(appRepositoryUrl)
        artifactNames.set(
            mapOf(
                "linuxX86_64" to "spotube-linux-$appVersionName-x86_64.tar.xz",
                "linuxAarch64" to "spotube-linux-$appVersionName-aarch64.tar.xz",
                "windowsX64" to "Spotube-windows-x86_64-setup.exe",
            )
        )
        artifactsDir.set(packagingArtifactsDir)
        changelogContext.set(gitCliffContext.flatMap { it.outputFile })
        outputDir.set(layout.buildDirectory.dir("packaging/$target"))
    }
}
val githubRepositoryPath = repositoryUrl.removePrefix("https://github.com/")
val latestReleaseApiUrl = "https://api.github.com/repos/$githubRepositoryPath/releases/latest"
val generatedAppInfoDir = layout.buildDirectory.dir("generated/appBuildInfo/commonMain/kotlin")
val generateAppBuildInfo by tasks.registering(Copy::class) {
    description = "Generate AppBuildInfo.kt"
    from(layout.projectDirectory.file("src/commonMain/templates/AppBuildInfo.kt.template"))
    into(layout.buildDirectory.dir("generated/appBuildInfo/commonMain/kotlin/org/spotube/spotube"))
    rename("AppBuildInfo.kt.template", "AppBuildInfo.kt")
    expand(
        mapOf(
            "appVersion" to appVersionName,
            "appBuildNumber" to appBuildNumber,
            "repositoryUrl" to appRepositoryUrl,
            "latestReleaseApiUrl" to latestReleaseApiUrl,
        )
    )
    inputs.properties(
        mapOf(
            "appVersion" to appVersionName,
            "appBuildNumber" to appBuildNumber,
            "repositoryUrl" to appRepositoryUrl,
            "latestReleaseApiUrl" to latestReleaseApiUrl,
        )
    )
}

tasks.configureEach {
    if (name.startsWith("compile") && name.contains("Kotlin")) {
        dependsOn(generateAppBuildInfo)
    }
}

// Windows/macOS always bundle VLC. On Linux only the self-contained AppImage (-PappImage=true) bundles
// VLC, GTK and WebKitGTK; deb/rpm use the distro's packages.
val isLinuxHost = System.getProperty("os.name").lowercase().contains("linux")
val bundleForAppImage = isLinuxHost && providers.gradleProperty("appImage").map { it.toBoolean() }.getOrElse(false)

vlcjBundler {
    packageName = "$bundleId.core.generated"   // choose a different package
    objectName = "VLCBundleLoaderGenerated"               // or rename the object
    bundleVlc = !isLinuxHost || bundleForAppImage
}

// GTK 3 + WebKitGTK for the AppImage (see DownloadLinuxGtkBundleTask in buildSrc)
val downloadLinuxGtkBundle = tasks.register<DownloadLinuxGtkBundleTask>("downloadLinuxGtkBundle") {
    outputDir.set(layout.buildDirectory.dir("linux-gtk-bundle"))
    ubuntuRelease.set("noble")
}
val linuxGtkBundleDir: Provider<Directory> = downloadLinuxGtkBundle.flatMap { it.outputDir }

// One resources root holding VLC and GTK/WebKit, laid out for the AppImage
val assembleAppImageResources = tasks.register<Sync>("assembleAppImageResources") {
    from(vlcjBundler.vlcNativesDirectory)
    from(linuxGtkBundleDir)
    exclude("**/.complete")
    into(layout.buildDirectory.dir("appimage-resources"))
}


kotlin {
    // Note: For Android application modules, androidTarget() is still required as of AGP 8.x.
    // The deprecation warning is expected. For libraries, use the androidKotlinMultiplatformLibrary plugin instead.
    // Full migration support for applications will be available in AGP 9.0.0+
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()

    listOf(iosArm64Target, iosSimulatorArm64Target).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()
    jvmToolchain(21)

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedAppInfoDir)
            dependencies {
                // Project dependencies
                implementation(project(":plugin_interfaces"))

                // Jetpack Compose dependencies
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.dns.sd.kt)

                // Millennium UI
                implementation(libs.millennium)

                // Navigation
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)

                // koin
                api(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.navigation3)

                // 3rd party libraries
                // ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization.kotlinx.json)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.websockets)
                // Zipline
                api(libs.zipline.core)
                implementation(libs.zipline.loader)
                // file-kit
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                // icons
                implementation(libs.feather.icons)

                // androidx-datastore
                implementation(libs.androidx.datastore)
                implementation(libs.androidx.datastore.preferences)
                // kmp-zip
                implementation(libs.kmp.zip)
                implementation(libs.kmp.zip.okio)
                implementation(libs.kmp.zip.kotlinx)
                implementation(libs.murmurhash)
                // logging
                implementation(libs.kermit)
                implementation(libs.kermit.koin)

                implementation(libs.semver)

                // crypto
                implementation(libs.cryptography.core)
                implementation(libs.cryptography.provider.optimal)

                // webview
                implementation(libs.compose.webview)

                // blur
                implementation(libs.haze)
                implementation(libs.haze.blur)
                implementation(libs.haze.materials)

                // caching
                implementation(libs.cache4k)

                // reorderable list
                implementation(libs.reorderable)

                // Shimmer effect
                implementation(libs.compose.shimmer)

                // DLNA
                implementation(libs.dns.sd.kt)

                // Markdown renderer
                implementation(libs.multiplatform.markdown.renderer)
                implementation(libs.multiplatform.markdown.renderer.m3)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        // Shared code between mobile targets (Android + iOS)
        val mobileMain by creating {
            dependsOn(commonMain)
            dependencies {
//                implementation(libs.kmedia)
            }
        }
        // Shared code between Java compatible targets (Android + Desktop)
        val androidJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.newpipeextractor)
                implementation(libs.ktor.client.okhttp)
            }
        }
        androidMain {
            dependsOn(mobileMain)
            dependsOn(androidJvmMain)
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.car.app)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.media3.common)
                implementation(libs.media3.exoplayer)
                implementation(libs.media3.session)
            }
        }
        val iosMain by creating {
            dependsOn(mobileMain)
            dependencies {
                // 3rd party libraries
                implementation(libs.ktor.client.darwin)
                implementation(libs.newpipe.extractor.kmp)
            }
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        jvmMain {
            dependsOn(androidJvmMain)
            dependencies {
                // Jetpack Compose dependencies
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)

                // vlcj dependencies
                implementation(libs.vlcj)
                implementation(libs.vlcj.natives)

                implementation(libs.appdirs)
                implementation(libs.jna)

                implementation(libs.nucleus.core.runtime)
                implementation(libs.nucleus.graalvm.runtime)
                implementation(libs.nucleus.nucleus.application)
                implementation(libs.nucleus.decorated.window.tao)
                implementation(libs.compose.native.tray)
                implementation(libs.nucleus.media.control)
                implementation(libs.nucleus.darkmode.detector)
            }
        }
    }
}

android {
    namespace = "org.spotube.spotube"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = bundleId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appBuildNumber
        versionName = appVersionName
    }
    // stable = production channel, nightly = beta channel (own icon, name and installable side by side)
    flavorDimensions += "channel"
    productFlavors {
        create("stable") {
            dimension = "channel"
        }
        create("nightly") {
            dimension = "channel"
            applicationIdSuffix = ".nightly"
            versionCode = nightlyBuildNumber
            versionNameSuffix = "-nightly.$nightlyBuildNumber"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val secretProps = Properties().apply {
        val propertiesFile = project.file("local.properties")
        if (propertiesFile.exists()) {
            load(FileInputStream(propertiesFile))
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = secretProps.getProperty("release.signing.storeFile")
            if (!storeFilePath.isNullOrEmpty()) {
                storeFile = file(storeFilePath)
                storePassword = secretProps.getProperty("release.signing.storePassword")
                keyAlias = secretProps.getProperty("release.signing.keyAlias")
                keyPassword = secretProps.getProperty("release.signing.keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            // Debug and release share the application id, so sign both with the release key
            // when it's configured; otherwise one can't be installed over the other.
            // Without local.properties signing (contributors) keep the default debug keystore.
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.compose.uiTooling)
}

nucleus.application {
    mainClass = "org.spotube.spotube.MainKt"

    jvmArgs += listOf(
        "--enable-native-access=ALL-UNNAMED", // need for JNA for compose-webview (wry) to work
        // --- ADD THESE FOR SPEED & LOW RAM ---
        "-Xms64m",                  // Start with a tiny heap (prevents grabbing 500MB upfront)
        "-Xmx384m",                 // Cap the max heap (VLC and WebView need a bit of room)
        "-XX:TieredStopAtLevel=1",  // Disables the heavy C2 compiler. Huge startup speedup!
        "-XX:+UseSerialGC"          // The Serial GC is the most efficient for heaps under 512MB
    )

    nativeDistributions {
        packageName = bundleId
        // User-facing name (.app bundle, DMG productName, Launchpad/Dock); packageName above is the reverse-DNS id.
        // The GraalVM DMG task reads only appName ?: packageName, not macOS.packageName
        appName = if (isNightly) "Spotube Nightly" else "Spotube"
        packageVersion = version
        licenseFile = project.file("../LICENSE")

        appResourcesRootDir.set(
            if (bundleForAppImage) assembleAppImageResources.map { layout.buildDirectory.dir("appimage-resources").get() } else vlcjBundler.vlcNativesDirectory
        )

        targetFormats(
            TargetFormat.Dmg,
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.AppImage,
            TargetFormat.Msi,
            TargetFormat.Exe,
        )

        modules("jdk.unsupported")

        homepage = homePageUrl

        linux {
            iconFile.set(project.file("$desktopIconDir/icon.png"))
            modules("jdk.security.auth")
            debMaintainer = "Team Spotube <team.spotube@proton.me>"
            rpmLicenseType = "AGPL-3.0-or-later"
            // deb/rpm use the distro's shared libraries (only the AppImage bundles VLC). VLC is not
            // bundled on Linux otherwise, the webview needs WebKitGTK 4.1, and the window/tray/MPRIS
            // natives link GTK 3, GLib/GIO and D-Bus. Setting these replaces electron-builder's
            // Electron-specific defaults (libnss3, libnotify, ...).
            debDepends = listOf(
                "libvlc5",
                "vlc-plugin-base",
                "libwebkit2gtk-4.1-0",
                "libgtk-3-0 | libgtk-3-0t64",
                "libglib2.0-0 | libglib2.0-0t64",
                "libdbus-1-3",
                "libasound2 | libasound2t64",
                "libgl1",
                "libfontconfig1",
                "libfreetype6",
                "libx11-6",
                "libxext6",
                "libxi6",
                "libxrender1",
                "libxtst6",
            )
            // vlc-libs comes from RPM Fusion (Fedora ships no VLC); the README tells users to enable it
            rpmRequires = listOf(
                "vlc-libs",
                "webkit2gtk4.1",
                "gtk3",
                "glib2",
                "dbus-libs",
                "alsa-lib",
                "mesa-libGL",
                "fontconfig",
                "freetype",
                "libX11",
                "libXext",
                "libXi",
                "libXrender",
                "libXtst",
            )
        }
        windows {
            iconFile.set(project.file("$desktopIconDir/icon.ico"))
            shortcut = true
            menu = true
            dirChooser = true
            perUserInstall = true
        }

        macOS {
            iconFile.set(project.file("$desktopIconDir/icon.icns"))
            bundleID = if (isNightly) "$bundleId.nightly" else bundleId
            dockName = appName
            notarization {

            }
        }
    }

    buildTypes.release.proguard {
        configurationFiles.from(project.file("proguard-rules.pro"))
    }

    // AOT-compiled native binary (no bundled JVM). Build with `packageGraalvmNative`, try it with
    // `runGraalvmNative`. Nucleus downloads GraalVM CE on first use (or uses $GRAALVM_HOME).
    // Reflection/JNI metadata the automatic passes miss (vlcj, Zipline, NewPipe/Rhino, uniffi)
    // lives in `graalvm/` and is (re)collected by exercising the app under `runWithNativeAgent`.
    graalvm {
        isEnabled = true
        imageName = "spotube"
        nativeImageConfigBaseDir.set(project.layout.projectDirectory.dir("graalvm"))
        // ProGuard is not used for native images; -Os is the size equivalent
        optimization = NativeImageOptimization.SIZE
        // Same ceiling as the JVM distribution's -Xmx
        maxHeapSize = "384m"
        // Skiko is loaded from lib/ next to the binary (Nucleus copies it there); don't embed
        // a second ~30 MB copy as a resource
        buildArgs.add("-H:+UnlockExperimentalVMOptions")
        buildArgs.add("-H:ExcludeResources=.*skiko-[^/]*\\.(so|dylib|dll)")
    }
}

// The uber JAR is what the GraalVM build compiles from, and native-image embeds every registered
// resource in the binary. Libraries ship natives for every OS/CPU, so keep only the host's
// (the uber JAR is per-OS/arch already; it isn't used by `run`).
tasks.withType<org.gradle.jvm.tasks.Jar>().matching { it.name.endsWith("UberJarForCurrentOS") }.configureEach {
    // Everything the exclude closure uses is local: the configuration cache can't serialize
    // references to the build script object.
    val hostOs = System.getProperty("os.name").lowercase()
    val foreignExtensions = when {
        hostOs.contains("win") -> setOf("so", "dylib", "jnilib", "a")
        hostOs.contains("mac") -> setOf("so", "dll", "a")
        else -> setOf("dll", "dylib", "jnilib", "a")
    }
    val nativeExtensions = setOf("so", "dll", "dylib", "jnilib", "a")
    // CPU names as libraries spell them in their native folders (linux-x86-64, jni/amd64, darwin-aarch64, …)
    val cpuDirToken = Regex(
        "(?:^|[-_])(x86[-_]64|amd64|x64|aarch64|arm64|armv6|armv7|armel|arm|x86|i[36]86|ppc64le|ppc64|ppc" +
            "|s390x|riscv64|loongarch64|mips64el|sparcv9|sparc)(?=$|[-_])"
    )
    val hostCpuTokens = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> setOf("aarch64", "arm64")
        else -> setOf("x86-64", "x86_64", "amd64", "x64")
    }
    // OS flavours we never ship desktop builds for (sqlite-jdbc and JNA bundle these)
    val foreignOsDirToken = Regex("android|musl|freebsd|openbsd|dragonflybsd|aix|sunos")
    exclude { element ->
        val segments = element.path.lowercase().split('/')
        val extension = segments.last().substringAfterLast('.', "")
        !element.isDirectory && extension in nativeExtensions && (
            extension in foreignExtensions ||
                segments.dropLast(1).any { dir ->
                    foreignOsDirToken.containsMatchIn(dir) ||
                        cpuDirToken.findAll(dir).any { it.groupValues[1] !in hostCpuTokens }
                }
            )
    }
    // The locally built compose-webview SNAPSHOT carries stale CEF binaries (~1.3 GB unstripped
    // libcef.so) that no code loads (Linux uses WebKitGTK).
    exclude(
        "nucleus/native/*/libcef.*",
        "nucleus/native/*/libcompose_cef_*",
        "nucleus/native/*/cef_subprocess*",
        "nucleus/native/*/*.pak",
        "nucleus/native/*/icudtl.dat",
        "nucleus/native/*/v8_context_snapshot.bin",
    )
}

// Linux: native-image emits 13 KB libjvm.so/libjava.so shims (the image itself provides the JVM
// symbols), but Nucleus copies the JDK's real ones next to the binary — libjvm.so alone is 28 MB.
// Ship native-image's own outputs instead: skip the libjvm copy, and in the AWT copy prefer
// nativeCompile's version of any library native-image produced over the JDK's.
tasks.matching { it.name == "copyGraalvmJvmSo" }.configureEach { enabled = false }
tasks.withType<Copy>().matching { it.name == "copyGraalvmAwtSoLibs" }.configureEach {
    val nativeCompileDir = layout.buildDirectory.dir("compose/tmp/main/graalvm/nativeCompile").get().asFile
    from(nativeCompileDir) { include("*.so") }
    eachFile {
        if (file.parentFile != nativeCompileDir && nativeCompileDir.resolve(name).exists()) exclude()
    }
}

// Issue with compose plugin
// https://github.com/JetBrains/compose-hot-reload/blob/master/docs/Known_limitations.md#property-composeapplicationresourcesdir-is-null-when-running-hot-reload-tasks
tasks.withType<ComposeHotRun>().configureEach {
    // Populates the resources dir below with the VLC natives (appResourcesRootDir); hot run doesn't depend on it by default
    dependsOn("prepareAppResources")
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED"
    )
    // Linux: the hot-reload agent starts AWT before main(), and JBR's X11 toolkit pins GTK to
    // XWayland, where the embedded WebKit WebView renders black. Tao owns the windows, so the
    // reloadable JVM stays headless (DevTools runs in its own JVM and is unaffected).
    if (System.getProperty("os.name").lowercase().contains("linux")) {
        jvmArgs("-Djava.awt.headless=true")
    }
    systemProperty(
        "compose.application.resources.dir",
        project.layout.buildDirectory.dir("compose/tmp/prepareAppResources").get()
    )
}

cargo {
    // Gobley defaults desktop to the debug profile, which ships an unstripped ~31 MB library
    if (isProductionBuild) jvmVariant = Variant.Release
    builds.jvm {
        embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
    }
}

// Gobley adds build/intermediates/rust/<triple>/<variant> as a jniLibs source dir, but AGP leaves source dirs
// that don't exist at configuration time out of the merge inputs. On a clean build (CI) libcompose_app.so would
// then silently miss the APK and crash at runtime with UnsatisfiedLinkError, so create the dirs up front.
RustAndroidTarget.entries.forEach { target ->
    Variant.entries.forEach { variant ->
        layout.buildDirectory.dir("intermediates/rust/${target.rustTriple}/$variant").get().asFile.mkdirs()
    }
}

// Google only ships x86_64 Linux NDK toolchains, so on a Linux arm64 host the Android Rust targets can never
// link (clang: "Exec format error"). Desktop packaging doesn't need them; Android is built on x86_64 runners.
run {
    val isLinuxArm64Host = System.getProperty("os.name").lowercase().contains("linux") &&
        System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")
    if (isLinuxArm64Host) {
        // Gobley generates the UniFFI bindings from the first Android build's library by default; use the host's.
        uniffi {
            generateFromLibrary {
                build = GobleyHost.current.rustTarget
            }
        }
        tasks.matching { it.name.startsWith("cargoBuildAndroid") || it.name.startsWith("rustUpTargetAddAndroid") }
            .configureEach { enabled = false }
    }
}

// Nucleus' resource copies (prepareAppResources, copyGraalvmAppResources) drop the execute bit, but WebKitGTK
// spawns its helper processes from the AppImage bundle (DownloadLinuxGtkBundleTask in buildSrc) and fails with
// "Permission denied" otherwise.
tasks.matching { it.name == "prepareAppResources" || it.name == "copyGraalvmAppResources" }.configureEach {
    (this as? org.gradle.api.tasks.AbstractCopyTask)?.filesMatching("**/webkit2gtk-4.1/WebKit*Process") {
        permissions { unix("rwxr-xr-x") }
    }
}
