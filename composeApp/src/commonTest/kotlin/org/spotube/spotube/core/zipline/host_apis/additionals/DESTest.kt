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

package org.spotube.spotube.core.zipline.host_apis.additionals

import org.spotube.plugin_interfaces.host_apis.PaddingTypes
import org.spotube.plugin_interfaces.host_apis.SymmetricModes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DESTest {

    @Test
    fun `encrypt and decrypt roundtrip with 8-byte key`() = runTest {
        val key = "12345678".encodeToByteArray()
        val plaintext = "Hello!!!".encodeToByteArray()

        val encrypted = DES.encrypt(plaintext, key, SymmetricModes.ECB, PaddingTypes.PKCS7)
        val decrypted = DES.decrypt(encrypted, key, SymmetricModes.ECB, PaddingTypes.PKCS7)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt roundtrip with longer plaintext`() = runTest {
        val key = "38346591".encodeToByteArray()
        val plaintext = "This is a longer message that spans multiple blocks!".encodeToByteArray()

        val encrypted = DES.encrypt(plaintext, key, SymmetricModes.ECB, PaddingTypes.PKCS7)
        val decrypted = DES.decrypt(encrypted, key, SymmetricModes.ECB, PaddingTypes.PKCS7)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt roundtrip with exact 8-byte plaintext`() = runTest {
        val key = "testkey1".encodeToByteArray()
        val plaintext = "12345678".encodeToByteArray()

        val encrypted = DES.encrypt(plaintext, key, SymmetricModes.ECB, PaddingTypes.PKCS7)
        assertEquals(16, encrypted.size)
        val decrypted = DES.decrypt(encrypted, key, SymmetricModes.ECB, PaddingTypes.PKCS7)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt with no padding`() = runTest {
        val key = "12345678".encodeToByteArray()
        val plaintext = "12345678".encodeToByteArray()

        val encrypted = DES.encrypt(plaintext, key, SymmetricModes.ECB, PaddingTypes.NONE)
        val decrypted = DES.decrypt(encrypted, key, SymmetricModes.ECB, PaddingTypes.NONE)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `decrypt with string key 38346591`() = runTest {
        val key = "38346591".encodeToByteArray()
        assertEquals(8, key.size)

        val plaintext = "SecretData".encodeToByteArray()
        val encrypted = DES.encrypt(plaintext, key, SymmetricModes.ECB, PaddingTypes.PKCS7)
        val decrypted = DES.decrypt(encrypted, key, SymmetricModes.ECB, PaddingTypes.PKCS7)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `key must be exactly 8 bytes`() = runTest {
        val shortKey = "1234567".encodeToByteArray()
        val data = "testdata".encodeToByteArray()

        try {
            DES.encrypt(data, shortKey)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("DES key must be exactly 8 bytes (64 bits)", e.message)
        }
    }

    @Test
    fun `ciphertext must be multiple of 8 bytes`() = runTest {
        val key = "12345678".encodeToByteArray()
        val badCiphertext = byteArrayOf(1, 2, 3)

        try {
            DES.decrypt(badCiphertext, key)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("multiple of 8"))
        }
    }
}
