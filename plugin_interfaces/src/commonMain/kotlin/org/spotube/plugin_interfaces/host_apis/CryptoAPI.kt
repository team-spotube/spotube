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

package org.spotube.plugin_interfaces.host_apis

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

const val CryptoAPI_SERVICE_NAME = "Crypto"

@Serializable
enum class HashAlgorithms {
    SHA224, SHA256, SHA384, SHA512, SHA1, MD5, RIPEMD160;
}

@Serializable
enum class AESKeySize {
    B128, B192, B256
}

@Serializable
enum class ECCurves {
    P256, P384, P521, secp256k1, brainpoolP256r1, brainpoolP384r1, brainpoolP512r1
}

@Serializable
enum class EdDSACurves {
    Ed25519, Ed448
}

@Serializable
enum class ECDSASignatureFormats {
    DER, RAW
}

@Serializable
enum class ECEncodingFormats {
    DER, RAW, PEM, JWK
}

@Serializable
enum class RSAEncodingFormats {
    DER, PEM, JWK
}

@Serializable
enum class EdDSAEncodingFormats {
    DER, PEM, JWK, RAW
}

@Serializable
sealed class GenerateKeyPairAlgorithms {
    @Serializable
    data class RSA_PSS(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val keySizeBits: Int = 4096,
        val publicExponent: Long = 65537L,
        val format: RSAEncodingFormats = RSAEncodingFormats.DER
    ) : GenerateKeyPairAlgorithms()

    @Serializable
    data class RSA_PKCS1(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val keySizeBits: Int = 4096,
        val publicExponent: Long = 65537L,
        val format: RSAEncodingFormats = RSAEncodingFormats.DER
    ) : GenerateKeyPairAlgorithms()

    @Serializable
    data class ECDSA(
        val curve: ECCurves = ECCurves.P256,
        val format: ECEncodingFormats = ECEncodingFormats.DER,
    ) : GenerateKeyPairAlgorithms()

    @Serializable
    data class EdDSA(
        val curve: EdDSACurves = EdDSACurves.Ed25519,
        val format: EdDSAEncodingFormats = EdDSAEncodingFormats.DER
    ) : GenerateKeyPairAlgorithms()
}

@Serializable
sealed class SignAlgorithms {
    @Serializable
    data class RSA_PSS(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val format: RSAEncodingFormats = RSAEncodingFormats.DER
    ) : SignAlgorithms()

    @Serializable
    data class RSA_PKCS1(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val format: RSAEncodingFormats = RSAEncodingFormats.DER
    ) : SignAlgorithms()

    @Serializable
    data class ECDSA(
        val curve: ECCurves = ECCurves.P256,
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val format: ECEncodingFormats = ECEncodingFormats.DER,
        val signatureFormat: ECDSASignatureFormats = ECDSASignatureFormats.DER
    ) : SignAlgorithms()

    @Serializable
    data class EdDSA(
        val curve: EdDSACurves = EdDSACurves.Ed25519,
        val format: EdDSAEncodingFormats = EdDSAEncodingFormats.DER
    ) : SignAlgorithms()
}

@Serializable
enum class HMACEncodingFormats {
    RAW, JWK
}

@Serializable
enum class AESEncodingFormats {
    RAW, JWK
}

@Serializable
enum class SymmetricModes {
    ECB
}

@Serializable
enum class PaddingTypes {
    NONE, PKCS7
}

@Serializable
sealed class LegacyCipherAlgorithms {
    @Serializable
    data class DES(
        val mode: SymmetricModes = SymmetricModes.ECB,
        val padding: PaddingTypes = PaddingTypes.PKCS7
    ) : LegacyCipherAlgorithms()
}


@Serializable
sealed class MACKeyGeneratorAlgorithms {
    @Serializable
    data class HMAC(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val format: HMACEncodingFormats = HMACEncodingFormats.JWK
    ) : MACKeyGeneratorAlgorithms()

    @Serializable
    data class AES_CMAC(
        val keySize: AESKeySize = AESKeySize.B256,
        val format: AESEncodingFormats = AESEncodingFormats.JWK
    ) : MACKeyGeneratorAlgorithms()
}

@Serializable
sealed class MACSignatureAlgorithms {
    @Serializable
    data class HMAC(
        val hashAlgorithm: HashAlgorithms = HashAlgorithms.SHA256,
        val format: HMACEncodingFormats = HMACEncodingFormats.JWK
    ) : MACSignatureAlgorithms()

    @Serializable
    data class AES_CMAC(
        val format: AESEncodingFormats = AESEncodingFormats.JWK
    ) : MACSignatureAlgorithms()
}

interface CryptoAPI : ZiplineService {
    suspend fun generateRandomBytes(size: Int): ByteArray
    suspend fun hash(algorithm: HashAlgorithms, data: ByteArray): ByteArray
    fun hashBlocking(algorithm: HashAlgorithms, data: ByteArray): ByteArray
    suspend fun generateMACKey(algorithm: MACKeyGeneratorAlgorithms): ByteArray
    fun generateMACKeyBlocking(algorithm: MACKeyGeneratorAlgorithms): ByteArray

    suspend fun signWithMACKey(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        data: ByteArray
    ): ByteArray

    fun signWithMACKeyBlocking(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        data: ByteArray
    ): ByteArray

    suspend fun verifyMACSignatureWithKey(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        signature: ByteArray,
        data: ByteArray
    ): Boolean

    fun verifyMACSignatureWithKeyBlocking(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        signature: ByteArray,
        data: ByteArray
    ): Boolean

    suspend fun generateKeyPair(algorithm: GenerateKeyPairAlgorithms): Pair<ByteArray, ByteArray>

    fun generateKeyPairBlocking(algorithm: GenerateKeyPairAlgorithms): Pair<ByteArray, ByteArray>

    suspend fun signWithPrivateKey(
        algorithm: SignAlgorithms,
        data: ByteArray,
        privateKey: ByteArray
    ): ByteArray

    fun signWithPrivateKeyBlocking(
        algorithm: SignAlgorithms,
        data: ByteArray,
        privateKey: ByteArray
    ): ByteArray

    suspend fun verifySignatureWithPublicKey(
        algorithm: SignAlgorithms,
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean

    fun verifySignatureWithPublicKeyBlocking(
        algorithm: SignAlgorithms,
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean

    suspend fun encryptLegacy(
        algorithm: LegacyCipherAlgorithms,
        key: ByteArray,
        data: ByteArray,
        iv: ByteArray? = null // Ciphers like CBC/GCM will need an Initialization Vector
    ): ByteArray

    suspend fun decryptLegacy(
        algorithm: LegacyCipherAlgorithms,
        key: ByteArray,
        cipherText: ByteArray,
        iv: ByteArray? = null
    ): ByteArray
}