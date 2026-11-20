/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local

/** Display-ready local track used by all three collection detail screens. */
interface LocalCollectionTrack {
    val path: String
    val title: String
    val subtitle: String?
    val durationLabel: String?
    val artwork: ByteArray?
}
