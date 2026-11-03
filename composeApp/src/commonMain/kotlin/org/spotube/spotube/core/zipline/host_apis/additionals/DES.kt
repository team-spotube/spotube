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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DES {

    // --- 1. Tables & Matrices (Keep these identical to the previous implementation) ---
    private val IP = intArrayOf(58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6, 64, 56, 48, 40, 32, 24, 16, 8, 57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7)
    private val FP = intArrayOf(40, 8, 48, 16, 56, 24, 64, 32, 39, 7, 47, 15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22, 62, 30, 37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20, 60, 28, 35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26, 33, 1, 41, 9, 49, 17, 57, 25)
    private val PC1 = intArrayOf(57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36, 63, 55, 47, 39, 31, 23, 15, 7, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4)
    private val PC2 = intArrayOf(14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4, 26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32)
    private val SHIFTS = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
    private val E = intArrayOf(32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 8, 9, 10, 11, 12, 13, 12, 13, 14, 15, 16, 17, 16, 17, 18, 19, 20, 21, 20, 21, 22, 23, 24, 25, 24, 25, 26, 27, 28, 29, 28, 29, 30, 31, 32, 1)
    private val P = intArrayOf(16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31, 10, 2, 8, 24, 14, 32, 27, 3, 9, 19, 13, 30, 6, 22, 11, 4, 25)
    private val S_BOXES = arrayOf(
        intArrayOf(14,4,13,1,2,15,11,8,3,10,6,12,5,9,0,7, 0,15,7,4,14,2,13,1,10,6,12,11,9,5,3,8, 4,1,14,8,13,6,2,11,15,12,9,7,3,10,5,0, 15,12,8,2,4,9,1,7,5,11,3,14,10,0,6,13),
        intArrayOf(15,1,8,14,6,11,3,4,9,7,2,13,12,0,5,10, 3,13,4,7,15,2,8,14,12,0,1,10,6,9,11,5, 0,14,7,11,10,4,13,1,5,8,12,6,9,3,2,15, 13,8,10,1,3,15,4,2,11,6,7,12,0,5,14,9),
        intArrayOf(10,0,9,14,6,3,15,5,1,13,12,7,11,4,2,8, 13,7,0,9,3,4,6,10,2,8,5,14,12,11,15,1, 13,6,4,9,8,15,3,0,11,1,2,12,5,10,14,7, 1,10,13,0,6,9,8,7,4,15,14,3,11,5,2,12),
        intArrayOf(7,13,14,3,0,6,9,10,1,2,8,5,11,12,4,15, 13,8,11,5,6,15,0,3,4,7,2,12,1,10,14,9, 10,6,9,0,12,11,7,13,15,1,3,14,5,2,8,4, 3,15,0,6,10,1,13,8,9,4,5,11,12,7,2,14),
        intArrayOf(2,12,4,1,7,10,11,6,8,5,3,15,13,0,14,9, 14,11,2,12,4,7,13,1,5,0,15,10,3,9,8,6, 4,2,1,11,10,13,7,8,15,9,12,5,6,3,0,14, 11,8,12,7,1,14,2,13,6,15,0,9,10,4,5,3),
        intArrayOf(12,1,10,15,9,2,6,8,0,13,3,4,14,7,5,11, 10,15,4,2,7,12,9,5,6,1,13,14,0,11,3,8, 9,14,15,5,2,8,12,3,7,0,4,10,1,13,11,6, 4,3,2,12,9,5,15,10,11,14,1,7,6,0,8,13),
        intArrayOf(4,11,2,14,15,0,8,13,3,12,9,7,5,10,6,1, 13,0,11,7,4,9,1,10,14,3,5,12,2,15,8,6, 1,4,11,13,12,3,7,14,10,15,6,8,0,5,9,2, 6,11,13,8,1,4,10,7,9,5,0,15,14,2,3,12),
        intArrayOf(13,2,8,4,6,15,11,1,10,9,3,14,5,0,12,7, 1,15,13,8,10,3,7,4,12,5,6,11,0,14,9,2, 7,11,4,1,9,12,14,2,0,6,10,13,15,3,5,8, 2,1,14,7,4,10,8,13,15,12,9,0,3,5,6,11)
    )

    // --- 2. Bitwise Mechanics Engine ---
    private fun permute(input: Long, table: IntArray, inputLen: Int): Long {
        var output = 0L
        for (i in table.indices) {
            val bitPos = inputLen - table[i]
            val bit = (input shr bitPos) and 1L
            output = (output shl 1) or bit
        }
        return output
    }

    private fun generateSubkeys(key64: Long): LongArray {
        val subkeys = LongArray(16)
        val permutedKey = permute(key64, PC1, 64)
        var c = (permutedKey shr 28) and 0x0FFFFFFFUL.toLong()
        var d = permutedKey and 0x0FFFFFFFUL.toLong()

        for (i in 0 until 16) {
            val shift = SHIFTS[i]
            c = ((c shl shift) or (c shr (28 - shift))) and 0x0FFFFFFFUL.toLong()
            d = ((d shl shift) or (d shr (28 - shift))) and 0x0FFFFFFFUL.toLong()
            val combined = (c shl 28) or d
            subkeys[i] = permute(combined, PC2, 56)
        }
        return subkeys
    }

    private fun feistel(right32: Long, subkey48: Long): Long {
        val expanded = permute(right32, E, 32)
        val xored = expanded xor subkey48
        var sBoxOutput = 0L

        for (i in 0 until 8) {
            val chunk = (xored shr (42 - i * 6)) and 0x3F
            val row = (((chunk shr 5) and 1) shl 1) or (chunk and 1)
            val col = (chunk shr 1) and 0x0F
            val sValue = S_BOXES[i][(row.toInt() shl 4) or col.toInt()]
            sBoxOutput = (sBoxOutput shl 4) or sValue.toLong()
        }
        return permute(sBoxOutput, P, 32)
    }

    private fun processBlock(block64: Long, subkeys: LongArray, encrypt: Boolean): Long {
        val permutedBlock = permute(block64, IP, 64)
        var left = (permutedBlock shr 32) and 0xFFFFFFFFUL.toLong()
        var right = permutedBlock and 0xFFFFFFFFUL.toLong()

        for (i in 0 until 16) {
            val roundKey = if (encrypt) subkeys[i] else subkeys[15 - i]
            val nextLeft = right
            val nextRight = left xor feistel(right, roundKey)
            left = nextLeft
            right = nextRight
        }

        val preOutput = (right shl 32) or left
        return permute(preOutput, FP, 64)
    }

    private fun bytesToLong(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFFL)
        }
        return value
    }

    private fun longToBytes(value: Long, out: ByteArray, offset: Int) {
        for (i in 7 downTo 0) {
            out[offset + i] = (value shr (8 * (7 - i))).toByte()
        }
    }

    // --- 3. Public Configurable Interface APIs ---

    /**
     * Configuration parameters:
     * mode: Currently handles DESMode.ECB
     * padding: Can be PaddingTypes.NONE or PaddingTypes.PKCS7
     */
    suspend fun encrypt(
        data: ByteArray,
        key: ByteArray,
        mode: SymmetricModes = SymmetricModes.ECB,
        padding: PaddingTypes = PaddingTypes.PKCS7
    ): ByteArray = withContext(Dispatchers.Default) {
        require(key.size == 8) { "DES key must be exactly 8 bytes (64 bits)" }
        require(mode == SymmetricModes.ECB) { "Only ECB mode is currently supported natively" }

        // 1. Process Padding Types Strategy
        val workingBuffer = when (padding) {
            PaddingTypes.NONE -> {
                require(data.size % 8 == 0) { "Data size must be a multiple of 8 when using PaddingTypes.NONE" }
                data
            }
            PaddingTypes.PKCS7 -> {
                val paddingLen = 8 - (data.size % 8)
                val padded = ByteArray(data.size + paddingLen)
                data.copyInto(padded, destinationOffset = 0, startIndex = 0, endIndex = data.size)
                for (i in data.size until padded.size) {
                    padded[i] = paddingLen.toByte()
                }
                padded
            }
        }

        val key64 = bytesToLong(key, 0)
        val subkeys = generateSubkeys(key64)
        val output = ByteArray(workingBuffer.size)

        // 2. Loop blocks independently (ECB Specification mechanics)
        for (i in workingBuffer.indices step 8) {
            val block = bytesToLong(workingBuffer, i)
            val cipherBlock = processBlock(block, subkeys, encrypt = true)
            longToBytes(cipherBlock, output, i)
        }
        output
    }

    suspend fun decrypt(
        cipherText: ByteArray,
        key: ByteArray,
        mode: SymmetricModes = SymmetricModes.ECB,
        padding: PaddingTypes = PaddingTypes.PKCS7
    ): ByteArray = withContext(Dispatchers.Default) {
        require(key.size == 8) { "DES key must be exactly 8 bytes" }
        require(cipherText.size % 8 == 0) { "Cipher text block array size must be a multiple of 8" }
        require(mode == SymmetricModes.ECB) { "Only ECB mode is supported" }

        val key64 = bytesToLong(key, 0)
        val subkeys = generateSubkeys(key64)
        val decryptedBuffer = ByteArray(cipherText.size)

        for (i in cipherText.indices step 8) {
            val block = bytesToLong(cipherText, i)
            val plainBlock = processBlock(block, subkeys, encrypt = false)
            longToBytes(plainBlock, decryptedBuffer, i)
        }

        // 3. Process Padding Removal Strategy
        when (padding) {
            PaddingTypes.NONE -> decryptedBuffer
            PaddingTypes.PKCS7 -> {
                val paddingLen = decryptedBuffer.last().toInt() and 0xFF
                require(paddingLen in 1..8) { "Invalid PKCS7 padding formatting encountered" }

                // Ensure padding values are mathematically consistent
                for (i in (decryptedBuffer.size - paddingLen) until decryptedBuffer.size) {
                    require(decryptedBuffer[i].toInt() == paddingLen) { "Corrupted PKCS7 padding byte structural layout" }
                }

                val outputLen = decryptedBuffer.size - paddingLen
                val output = ByteArray(outputLen)
                decryptedBuffer.copyInto(output, destinationOffset = 0, startIndex = 0, endIndex = outputLen)
                output
            }
        }
    }
}