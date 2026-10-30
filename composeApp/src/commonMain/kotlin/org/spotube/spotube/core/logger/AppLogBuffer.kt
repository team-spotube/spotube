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

package org.spotube.spotube.core.logger

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LogEntry(
    val id: Long,
    val timestamp: Instant = Clock.System.now(),
    val severity: Severity,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    val formattedTime: String
        get() {
            val dt = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            val s = dt.second.toString().padStart(2, '0')
            val ms = (dt.nanosecond / 1_000_000).toString().padStart(3, '0')
            return "$h:$m:$s.$ms"
        }
}

object AppLogBuffer {
    private const val MAX_LOGS = 1000
    private var counter = 0L

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    fun append(severity: Severity, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            id = ++counter,
            severity = severity,
            tag = tag,
            message = message,
            throwable = throwable
        )
        _logs.update { list ->
            if (list.size >= MAX_LOGS) {
                list.drop(list.size - MAX_LOGS + 1) + entry
            } else {
                list + entry
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun formatAllLogs(): String {
        return _logs.value.joinToString("\n") { entry ->
            val header = "[${entry.formattedTime}] [${entry.severity.name}] [${entry.tag}] ${entry.message}"
            val err = entry.throwable?.stackTraceToString()?.let { "\n$it" }.orEmpty()
            header + err
        }
    }
}

class AppLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        AppLogBuffer.append(
            severity = severity,
            tag = tag,
            message = message,
            throwable = throwable
        )
    }
}
