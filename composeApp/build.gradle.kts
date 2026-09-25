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
import gobley.gradle.cargo.dsl.jvm
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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

vlcjBundler {
    packageName = "dev.krtirtho.spotube.core.generated"   // choose a different package
    objectName = "VLCBundleLoaderGenerated"               // or rename the object
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Project dependencies
                implementation(project(":plugin_interfaces"))

                // Jetpack Compose dependencies
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
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
                implementation(libs.ktor.client.cio)
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

                // Material3 adaptive
                implementation(libs.jetbrains.material3.adaptive)
                implementation(libs.jetbrains.material3.adaptive.layout)
                implementation(libs.jetbrains.material3.adaptive.suite)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)
                implementation(libs.jetbrains.material3.window.size)
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

                implementation(libs.material.kolor)

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
                implementation(libs.compose.placeholder.material3)

                // DLNA
                implementation(libs.dns.sd.kt)

                // mqtt client for jam-session
                implementation(libs.mqtt.client)
                implementation(libs.mqtt.x.models)
                implementation(libs.mqtt.buffer)
                implementation(libs.mqtt.buffer.codec)
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
                implementation(libs.nucleus.nucleus.application)
                implementation(libs.nucleus.decorated.window.tao)
                implementation(libs.compose.native.tray)
                implementation(libs.nucleus.media.control)
            }
        }
    }
}

android {
    namespace = "dev.krtirtho.spotube"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.krtirtho.spotube"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = project.findProperty("versionName") as String? ?: "1.0"
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
    mainClass = "dev.krtirtho.spotube.MainKt"

    jvmArgs += listOf(
        "--enable-native-access=ALL-UNNAMED", // need for JNA for compose-webview (wry) to work
        // --- ADD THESE FOR SPEED & LOW RAM ---
        "-Xms64m",                  // Start with a tiny heap (prevents grabbing 500MB upfront)
        "-Xmx384m",                 // Cap the max heap (VLC and WebView need a bit of room)
        "-XX:TieredStopAtLevel=1",  // Disables the heavy C2 compiler. Huge startup speedup!
        "-XX:+UseSerialGC"          // The Serial GC is the most efficient for heaps under 512MB
    )

    nativeDistributions {
        packageName = "dev.krtirtho.spotube"
        packageVersion = project.findProperty("versionName") as String? ?: "6.0.0"
        licenseFile = project.file("../LICENSE")

        appResourcesRootDir.set(vlcjBundler.vlcNativesDirectory)

        targetFormats(
            TargetFormat.Dmg,
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.AppImage,
            TargetFormat.Msi,
            TargetFormat.Exe,
        )

        modules("jdk.unsupported")

        homepage = "https://spotube.cc"

        linux {
            modules("jdk.security.auth")
            debMaintainer = "Team Spotube <team.spotube@proton.me>"
            rpmLicenseType = "AGPL-3.0-or-later"
        }
        windows {
            shortcut = true
            menu = true
            dirChooser = true
            perUserInstall = true
        }

        macOS {
            notarization {

            }
        }
    }

    buildTypes.release.proguard {
        configurationFiles.from(project.file("proguard-rules.pro"))
    }
}

// Issue with compose plugin
// https://github.com/JetBrains/compose-hot-reload/blob/master/docs/Known_limitations.md#property-composeapplicationresourcesdir-is-null-when-running-hot-reload-tasks
tasks.withType<ComposeHotRun>().configureEach {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED"
    )
    systemProperty(
        "compose.application.resources.dir",
        project.layout.buildDirectory.dir("compose/tmp/prepareAppResources").get()
    )
}

cargo {
    builds.jvm {
        embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
    }
}