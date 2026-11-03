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

// Bundles GTK 3 and WebKitGTK 4.1 into the Linux AppImage, the way Tauri apps do, so it runs on a
// system that has neither. Used only with -PappImage=true; deb/rpm depend on the distro's packages.
//
// The libraries come from the Ubuntu archive. Like the AppImage exclude list prescribes, nothing that
// has to match the host is bundled: glibc, libstdc++, libz, GL/EGL/DRM, X11/xcb, ALSA, fontconfig,
// freetype, harfbuzz, expat, gmp and the wayland libraries. What is left is pruned to the libraries
// reachable from GTK/WebKit through their ELF NEEDED entries.
//
// Layout (copied into the AppDir root by Nucleus; AppRun puts $APPDIR/usr/lib on LD_LIBRARY_PATH):
//   usr/lib/*.so.N                                  shared libraries, RUNPATH=$ORIGIN
//   usr/lib/<triplet>/webkit2gtk-4.1/WebKit*Process helper processes
//   usr/lib/<triplet>/gio/modules/libgiognutls.so   TLS backend for WebKit/libsoup
// libwebkit2gtk has the helper path compiled in as /usr/lib/<triplet>/webkit2gtk-4.1; "/usr" is
// patched to "././" (same length), so the helpers resolve relative to the working directory, which the
// app sets to $APPDIR/usr at startup (AppImageEnvironment). Needs `ar`, `tar` (+ `zstd`), `readelf`
// and `patchelf` on the build machine.

package org.spotube.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.util.zip.GZIPInputStream

@DisableCachingByDefault(because = "Downloads from the network")
abstract class DownloadLinuxGtkBundleTask : DefaultTask() {
    /** Root passed on to Nucleus' `appResourcesRootDir`; the files land in `<root>/linux-<arch>/usr`. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** Ubuntu release the binaries come from. Its glibc is the minimum the bundle runs on. */
    @get:Input
    abstract val ubuntuRelease: Property<String>

    init {
        group = "dependencies"
        description = "Downloads and relocates GTK 3 and WebKitGTK 4.1 for the Linux AppImage."
    }

    private val packages = listOf(
        "libgtk-3-0t64", "libwebkit2gtk-4.1-0", "libjavascriptcoregtk-4.1-0", "glib-networking",
        "libatk-bridge2.0-0t64", "libatk1.0-0t64", "libatomic1", "libatspi2.0-0t64", "libbrotli1",
        "libbz2-1.0", "libcairo-gobject2", "libcairo2", "libcap2", "libdatrie1", "libdbus-1-3",
        "libdw1t64", "libelf1t64", "libenchant-2-2", "libepoxy0", "libevdev2", "libffi8", "libgcrypt20",
        "libgdk-pixbuf-2.0-0", "libglib2.0-0t64", "libgnutls30t64", "libgssapi-krb5-2",
        "libgstreamer-gl1.0-0", "libgstreamer-plugins-base1.0-0", "libgstreamer1.0-0", "libgudev-1.0-0",
        "libharfbuzz-icu0", "libhogweed6t64", "libhyphen0", "libicu74", "libidn2-0", "libjpeg-turbo8",
        "libjpeg8", "libk5crypto3", "libkeyutils1", "libkrb5-3", "libkrb5support0", "liblcms2-2",
        "liblz4-1", "liblzma5", "libmanette-0.2-0", "libnettle8t64", "libnghttp2-14", "liborc-0.4-0t64",
        "libp11-kit0", "libpango-1.0-0", "libpangocairo-1.0-0", "libpangoft2-1.0-0", "libpixman-1-0",
        "libpng16-16t64", "libpsl5t64", "libseccomp2", "libsecret-1-0", "libsharpyuv0", "libsoup-3.0-0",
        "libsqlite3-0", "libsystemd0", "libtasn1-6", "libthai0", "libudev1", "libunistring5", "libunwind8",
        "libwebp7", "libwebpdemux2", "libwebpmux3", "libxcb-render0", "libxcb-shm0", "libxcomposite1",
        "libxcursor1", "libxdamage1", "libxext6", "libxfixes3", "libxi6", "libxinerama1", "libxkbcommon0",
        "libxml2", "libxrandr2", "libxrender1", "libxslt1.1", "libzstd1",
    )

    // Provided by the host, per the AppImage exclude list (which includes the wayland libraries). Never
    // copied even if a bundled package carries them.
    private val hostLibraries = setOf(
        "libc.so.6", "libm.so.6", "libdl.so.2", "libpthread.so.0", "librt.so.1", "libresolv.so.2",
        "libutil.so.1", "libnsl.so.1", "libstdc++.so.6", "libgcc_s.so.1", "libz.so.1", "libGL.so.1",
        "libEGL.so.1", "libGLX.so.0", "libGLdispatch.so.0", "libOpenGL.so.0", "libdrm.so.2",
        "libgbm.so.1", "libX11.so.6", "libX11-xcb.so.1", "libxcb.so.1", "libasound.so.2",
        "libfontconfig.so.1", "libfreetype.so.6", "libharfbuzz.so.0", "libexpat.so.1", "libgmp.so.10",
        "libwayland-client.so.0", "libwayland-server.so.0", "libwayland-cursor.so.0",
        "libwayland-egl.so.1", "libuuid.so.1", "libgpg-error.so.0", "libfribidi.so.0", "libXau.so.6",
        "libXdmcp.so.6", "libpcre2-8.so.0", "libselinux.so.1", "libmount.so.1", "libblkid.so.1",
    )

    private val rootLibraries = listOf(
        "libgtk-3.so.0", "libgdk-3.so.0", "libwebkit2gtk-4.1.so.0", "libjavascriptcoregtk-4.1.so.0",
    )

    @TaskAction
    fun run() {
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm64")
        val resourceDir = if (isArm) "linux-arm64" else "linux-x64"
        val debArch = if (isArm) "arm64" else "amd64"
        val triplet = if (isArm) "aarch64-linux-gnu" else "x86_64-linux-gnu"
        val mirror = if (isArm) "http://ports.ubuntu.com/ubuntu-ports" else "http://archive.ubuntu.com/ubuntu"

        val target = outputDir.dir(resourceDir).get().asFile
        val marker = File(target, ".complete")
        if (marker.exists()) {
            println("Linux GTK/WebKit bundle already present. Skipping download.")
            return
        }
        for (tool in listOf("ar", "tar", "readelf", "patchelf")) {
            val found = try {
                ProcessBuilder(tool, "--version").redirectErrorStream(true).start()
                    .also { it.inputStream.readBytes() }.waitFor() == 0
            } catch (_: Exception) {
                false
            }
            if (!found) throw GradleException("`$tool` is required to bundle GTK/WebKit on Linux but was not found on PATH.")
        }

        target.deleteRecursively()
        val work = File(temporaryDir, "work").apply { deleteRecursively(); mkdirs() }
        val root = File(work, "root").apply { mkdirs() }

        val index = fetchPackageIndex(mirror, debArch)
        for (pkg in packages) {
            val filename = index[pkg] ?: throw GradleException("Package $pkg not found in the ${ubuntuRelease.get()} index")
            val deb = File(work, "$pkg.deb")
            println("Downloading $pkg")
            URI("$mirror/$filename").toURL().openStream().use { input -> deb.outputStream().use { input.copyTo(it) } }
            extractDeb(deb, root, File(work, "ar-$pkg"))
        }

        val srcLib = File(root, "usr/lib/$triplet")
        val libOut = File(target, "usr/lib").apply { mkdirs() }

        // Libraries by SONAME (libbz2.so.1.0 and friends don't end in a single number)
        val available = HashMap<String, File>()
        srcLib.listFiles { f -> f.isFile && ".so" in f.name }.orEmpty().forEach { file ->
            soname(file)?.let { available.putIfAbsent(it, file) }
        }

        val helperSrc = File(srcLib, "webkit2gtk-4.1")
        val helperOut = File(libOut, "$triplet/webkit2gtk-4.1")
        val helpers = listOf("WebKitWebProcess", "WebKitNetworkProcess", "WebKitGPUProcess")
        for (name in helpers) {
            // copyTo drops the mode bits; WebKit spawns these, so they must stay executable
            File(helperSrc, name).copyTo(File(helperOut, name), overwrite = true).setExecutable(true, false)
        }
        val injected = File(helperSrc, "injected-bundle/libwebkit2gtkinjectedbundle.so")
        injected.copyTo(File(helperOut, "injected-bundle/${injected.name}"), overwrite = true)
        val gioTls = File(srcLib, "gio/modules/libgiognutls.so")
        gioTls.copyTo(File(libOut, "$triplet/gio/modules/${gioTls.name}"), overwrite = true)

        // Keep only what GTK, WebKit, their helpers and the TLS module pull in via NEEDED
        val pending = ArrayDeque(rootLibraries)
        listOf(*helpers.map { File(helperOut, it) }.toTypedArray(), File(helperOut, "injected-bundle/${injected.name}"),
            File(libOut, "$triplet/gio/modules/${gioTls.name}")).forEach { pending += needed(it) }
        val keep = LinkedHashSet<String>()
        while (pending.isNotEmpty()) {
            val name = pending.removeFirst()
            if (name in hostLibraries || name in keep) continue
            val file = available[name] ?: continue
            keep += name
            pending += needed(file)
        }
        for (name in keep) available.getValue(name).copyTo(File(libOut, name), overwrite = true)

        patchWebKitPaths(File(libOut, "libwebkit2gtk-4.1.so.0"), triplet)

        // Libraries find each other next to themselves; the helpers and modules sit two/three levels down
        libOut.listFiles { f -> f.isFile }.orEmpty().forEach { patchRpath(it, "\$ORIGIN") }
        helpers.forEach { patchRpath(File(helperOut, it), "\$ORIGIN/../..") }
        patchRpath(File(helperOut, "injected-bundle/${injected.name}"), "\$ORIGIN/../../..")
        patchRpath(File(libOut, "$triplet/gio/modules/${gioTls.name}"), "\$ORIGIN/../../..")

        marker.writeText("")
        work.deleteRecursively()
        println("GTK/WebKit bundled into $target (${keep.size} libraries)")
    }

    /** "/usr/lib/<triplet>/webkit2gtk-4.1[/injected-bundle]" -> "././lib/<triplet>/webkit2gtk-4.1[/...]". */
    private fun patchWebKitPaths(lib: File, triplet: String) {
        val bytes = lib.readBytes()
        val needle = "/usr/lib/$triplet/webkit2gtk-4.1".toByteArray()
        val replacement = "././".toByteArray()
        var count = 0
        var i = 0
        while (i <= bytes.size - needle.size) {
            var match = true
            for (j in needle.indices) {
                if (bytes[i + j] != needle[j]) { match = false; break }
            }
            if (match) {
                replacement.copyInto(bytes, i)
                count++
                i += needle.size
            } else {
                i++
            }
        }
        if (count == 0) throw GradleException("WebKit helper path not found in ${lib.name}; the bundling layout needs updating")
        lib.writeBytes(bytes)
    }

    private fun soname(file: File): String? =
        exec(file.parentFile, "readelf", "-d", file.absolutePath).lineSequence()
            .firstOrNull { "(SONAME)" in it }?.substringAfter('[')?.substringBefore(']')

    private fun needed(file: File): List<String> =
        exec(file.parentFile, "readelf", "-d", file.absolutePath).lineSequence()
            .filter { "(NEEDED)" in it }.map { it.substringAfter('[').substringBefore(']') }.toList()

    /** name -> pool filename; `-updates` is read first so security/bugfix builds win. */
    private fun fetchPackageIndex(mirror: String, debArch: String): Map<String, String> {
        val release = ubuntuRelease.get()
        val index = HashMap<String, String>()
        for (suite in listOf("$release-updates", release)) {
            for (component in listOf("main", "universe")) {
                val url = URI("$mirror/dists/$suite/$component/binary-$debArch/Packages.gz").toURL()
                GZIPInputStream(url.openStream()).bufferedReader().useLines { lines ->
                    var name: String? = null
                    for (line in lines) {
                        when {
                            line.startsWith("Package: ") -> name = line.removePrefix("Package: ")
                            line.startsWith("Filename: ") -> name?.let { index.putIfAbsent(it, line.removePrefix("Filename: ")) }
                            line.isEmpty() -> name = null
                        }
                    }
                }
            }
        }
        return index
    }

    private fun extractDeb(deb: File, into: File, scratch: File) {
        scratch.mkdirs()
        exec(scratch, "ar", "x", deb.absolutePath)
        val data = scratch.listFiles { f -> f.name.startsWith("data.tar") }?.singleOrNull()
            ?: throw GradleException("No data archive in ${deb.name}")
        // GNU tar picks the decompressor from the content (zstd needs the zstd binary installed)
        exec(scratch, "tar", "-xf", data.absolutePath, "-C", into.absolutePath)
    }

    private fun patchRpath(file: File, rpath: String) {
        exec(file.parentFile, "patchelf", "--set-rpath", rpath, file.absolutePath)
    }

    private fun exec(dir: File, vararg command: String): String {
        val process = ProcessBuilder(*command).directory(dir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw GradleException("`${command.joinToString(" ")}` failed:\n$output")
        }
        return output
    }
}
