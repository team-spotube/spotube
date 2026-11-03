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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSThread
import kotlin.coroutines.ContinuationInterceptor

// Same as JVM/Android. Zipline lets QuickJS recurse up to ~6 MiB of native stack, but
// iOS gives secondary threads (incl. newSingleThreadContext's) only 512 KiB, so loading
// a plugin overflowed into the guard page and crashed the app with SIGBUS.
private const val ZIPLINE_THREAD_STACK_SIZE: ULong = 8_388_608u // 8 MiB

actual fun createZiplineDispatcher(): ZiplineDispatcher {
    val dispatcher = CompletableDeferred<CoroutineDispatcher>()
    val stopped = CompletableDeferred<Unit>()
    // NSThread is the only way to choose a thread's stack size here; it hosts an event
    // loop (runBlocking) whose dispatcher accepts work from any thread.
    val thread = NSThread {
        runBlocking {
            dispatcher.complete(coroutineContext[ContinuationInterceptor] as CoroutineDispatcher)
            stopped.await()
        }
    }
    thread.name = "Zipline"
    thread.stackSize = ZIPLINE_THREAD_STACK_SIZE
    thread.start()
    return ZiplineDispatcher(runBlocking { dispatcher.await() }) {
        stopped.complete(Unit)
    }
}
