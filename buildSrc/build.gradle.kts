// Build logic: the Linux AppImage bundle (DownloadLinuxGtkBundleTask) and the package manager manifests
// (GitCliffContextTask, RenderPackagingTask). Lives in buildSrc so the IDE resolves it; an
// `apply(from = ...)` script plugin gets no Kotlin classpath in Android Studio.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jmustache)
}
