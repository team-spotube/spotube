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

package org.spotube.spotube.core.appimage

import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import java.io.File

/**
 * Makes the GTK 3 and WebKitGTK bundled into the AppImage usable. The AppImage's AppRun already puts
 * `$APPDIR/usr/lib` on `LD_LIBRARY_PATH`; the rest has to be done in-process, before any GTK/WebKit code
 * loads, because the AppRun is generated and can't be customised:
 *
 *  - WebKitGTK has its helper-process path compiled in. The build patches it to a path relative to the
 *    working directory (see `DownloadLinuxGtkBundleTask` in buildSrc), so the working directory is moved to
 *    `$APPDIR/usr`.
 *  - TLS in WebKit/libsoup needs glib-networking's GIO module, which lives in the bundle.
 *  - The bundled WebKitGTK 2.44 uses its dmabuf renderer by default. Against a newer host Mesa/compositor
 *    that path never paints the first frame (the webview stays see-through or black until the window is
 *    resized), and the bundled GTK/epoxy can even abort while creating the GL context. Disabling it makes
 *    the webview paint on native Wayland. Left alone if the user already set it.
 *
 * Does nothing outside the AppImage (deb/rpm/dev runs have no bundled `usr/lib/libgtk-3.so.0`).
 */
object AppImageEnvironment {
    fun apply() {
        if (!System.getProperty("os.name").lowercase().contains("linux")) return
        val resourcesDir = System.getProperty("compose.application.resources.dir") ?: return
        val usrDir = File(resourcesDir, "usr")
        if (!File(usrDir, "lib/libgtk-3.so.0").isFile) return

        try {
            val libc = NativeLibrary.getInstance("c")
            val setenv = libc.getFunction("setenv")
            val triplet = if (System.getProperty("os.arch").lowercase().let { "aarch64" in it || "arm64" in it }) {
                "aarch64-linux-gnu"
            } else {
                "x86_64-linux-gnu"
            }
            setenv.set("GIO_EXTRA_MODULES", File(usrDir, "lib/$triplet/gio/modules").absolutePath)
            if (System.getenv("WEBKIT_DISABLE_DMABUF_RENDERER").isNullOrBlank()) {
                setenv.set("WEBKIT_DISABLE_DMABUF_RENDERER", "1")
            }
            libc.getFunction("chdir").invokeInt(arrayOf<Any>(usrDir.absolutePath))
        } catch (_: Throwable) {
            // Falls back to the host's libraries, which only works where they are installed
        }
    }

    private fun Function.set(name: String, value: String) {
        invokeInt(arrayOf<Any>(name, value, 1))
    }
}
