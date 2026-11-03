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

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual fun createZiplineDispatcher(): ZiplineDispatcher {
    val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(null, runnable, "Zipline", 8L * 1024 * 1024) // 8 MiB stack for QuickJS compile()
    }
    val dispatcher = executor.asCoroutineDispatcher()
    return ZiplineDispatcher(dispatcher) {
        dispatcher.close()
        executor.shutdown()
    }
}

