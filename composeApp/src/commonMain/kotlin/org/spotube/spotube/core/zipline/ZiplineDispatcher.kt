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

package org.spotube.spotube.core.zipline

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A coroutine dispatcher that can be closed/shut down.
 */
class ZiplineDispatcher(
    val dispatcher: CoroutineDispatcher,
    private val onClose: () -> Unit = {}
) {
    fun close() {
        onClose()
    }
}

/**
 * Creates a single-threaded coroutine dispatcher suitable for running Zipline/QuickJS.
 *
 * QuickJS's compile() uses deep C-level recursion on the native thread stack.
 * Zipline.create() sets maxStackSize to 6 MiB and expects the calling thread
 * to have at least 8 MiB of stack. On JVM/Android, the default thread stack
 * size is ~1 MiB, which causes native stack overflow when compiling large
 * JS modules like kotlin-stdlib (~491 KB).
 *
 * iOS secondary threads get only 512 KiB, so every platform must create its own
 * large-stack thread. This expect/actual ensures the backing thread has one.
 */
expect fun createZiplineDispatcher(): ZiplineDispatcher

