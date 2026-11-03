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

package org.spotube.plugin_interfaces.extras.logger

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class Logger actual constructor(tag: String) {
    internal val tag = tag

    actual fun d(message: () -> String) {
        println("DEBUG [${this.tag}] ${message.invoke()}")
    }

    actual fun i(message: () -> String) {
        println("INFO [${this.tag}] ${message.invoke()}")
    }

    actual fun w(message: () -> String) {
        println("WARN [${this.tag}] ${message.invoke()}")
    }

    actual fun e(message: () -> String, throwable: Throwable?) {
        println("ERROR [${this.tag}] ${message.invoke()} ${throwable?.stackTraceToString() ?: ""}")
    }
}