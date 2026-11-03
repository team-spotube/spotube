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

plugins {
    alias(libs.plugins.vanniktechMavenPublish)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.zipline.gradle.plugin)
}

group = "org.spotube.spotube"
version = "0.1.0"

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "org.spotube.spotube.plugin_interfaces"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm()

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "plugin_interfacesKit"

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = xcfName
        }
    }

    js {
        browser()
    }


    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
                api(libs.zipline.core)
                implementation(libs.semver)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jsMain.dependencies {}
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        group.toString(),
        "plugin_interfaces",
        version.toString()
    )
    pom {
        name = "plugin_interfaces"
        description = "Plugin interfaces for Spotube"
        inceptionYear = "2026"
        url = "https://github.com/team-spotube/spotube"
        licenses {
            license {
                name = "AGPL-3.0-or-later"
                url = "https://spdx.org/licenses/AGPL-3.0-or-later.html"
                distribution = "https://spdx.org/licenses/AGPL-3.0-or-later.html"
            }
        }
        developers {
            developer {
                id = "team-spotube"
                name = "Team Spotube"
                url = "https://github.com/team-spotube/"
                email = "team.spotube@proton.me"
            }
        }
        scm {
            url = "https://github.com/team-spotube/spotube"
            connection = "scm:git:git://github.com/team-spotube/spotube.git"
            developerConnection = "scm:git:ssh://git@github.com/team-spotube/spotube.git"
        }
    }
}