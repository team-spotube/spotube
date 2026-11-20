/* Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.spotube.spotube.modules.library.local_tracks.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

private const val TRACK_COVER_MAX_EDGE = 480.0
private const val TRACK_COVER_QUALITY = 0.85

/**
 * Images written into audio files must be PNG or JPEG (Lofty rejects anything else), so iOS
 * re-encodes every picked cover to a small JPEG instead of passing the original bytes through.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun encodeTrackCover(bytes: ByteArray): ByteArray? = runCatching {
    if (bytes.isEmpty()) return null
    val data = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }
    val image = UIImage(data = data) ?: return null
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = (TRACK_COVER_MAX_EDGE / minOf(width, height)).coerceAtMost(1.0)
    val rendered = if (scale < 1.0) {
        val targetWidth = width * scale
        val targetHeight = height * scale
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        resized ?: image
    } else {
        image
    }
    UIImageJPEGRepresentation(rendered, TRACK_COVER_QUALITY)?.toByteArray()
}.getOrNull()

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
