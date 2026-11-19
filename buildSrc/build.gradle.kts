// Build logic: the Linux AppImage bundle (DownloadLinuxGtkBundleTask), the package manager manifests
// (GitCliffContextTask, RenderPackagingTask) and the Millennium interaction patch
// (MillenniumInteractionPatch). Lives in buildSrc so the IDE resolves it; an
// `apply(from = ...)` script plugin gets no Kotlin classpath in Android Studio.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jmustache)
    // Shared with AGP (Gradle class loaders are parent-first), so both use one copy of ASM.
    implementation(libs.asm)
}
