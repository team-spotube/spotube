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

package org.spotube.spotube.core.server

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals

class StreamFileRangeTest {
    // Several chunks plus a partial one.
    private val data = Random(42).nextBytes(64 * 1024 * 3 + 1234)

    private fun copy(start: Long, length: Long): ByteArray = runBlocking {
        val file = Files.createTempFile("spotube-cache", ".bin").also { it.toFile().deleteOnExit() }
        Files.write(file, data)
        val channel = ByteChannel()
        launch {
            streamFileRange(FileSystem.SYSTEM, file.toOkioPath(), start, length, channel)
            channel.flushAndClose()
        }
        channel.readRemaining().readByteArray()
    }

    @Test
    fun wholeFile() = assertContentEquals(data, copy(0, data.size.toLong()))

    @Test
    fun rangeAcrossChunks() {
        val start = 70_000L
        val length = 120_000L
        assertContentEquals(data.copyOfRange(start.toInt(), (start + length).toInt()), copy(start, length))
    }

    @Test
    fun lengthPastEndStopsAtEof() = assertContentEquals(data.copyOfRange(100, data.size), copy(100, data.size.toLong()))
}
