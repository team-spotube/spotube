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

package org.spotube.spotube.core.zipline.host_apis

import org.spotube.plugin_interfaces.host_apis.AESEncodingFormats
import org.spotube.plugin_interfaces.host_apis.AESKeySize
import org.spotube.plugin_interfaces.host_apis.AESKeySize.B128
import org.spotube.plugin_interfaces.host_apis.AESKeySize.B192
import org.spotube.plugin_interfaces.host_apis.AESKeySize.B256
import org.spotube.plugin_interfaces.host_apis.CryptoAPI
import org.spotube.plugin_interfaces.host_apis.GenerateKeyPairAlgorithms
import org.spotube.plugin_interfaces.host_apis.ECCurves
import org.spotube.plugin_interfaces.host_apis.ECCurves.P256
import org.spotube.plugin_interfaces.host_apis.ECCurves.P384
import org.spotube.plugin_interfaces.host_apis.ECCurves.P521
import org.spotube.plugin_interfaces.host_apis.ECCurves.brainpoolP256r1
import org.spotube.plugin_interfaces.host_apis.ECCurves.brainpoolP384r1
import org.spotube.plugin_interfaces.host_apis.ECCurves.brainpoolP512r1
import org.spotube.plugin_interfaces.host_apis.ECCurves.secp256k1
import org.spotube.plugin_interfaces.host_apis.ECDSASignatureFormats
import org.spotube.plugin_interfaces.host_apis.ECEncodingFormats
import org.spotube.plugin_interfaces.host_apis.EdDSACurves
import org.spotube.plugin_interfaces.host_apis.EdDSACurves.Ed25519
import org.spotube.plugin_interfaces.host_apis.EdDSACurves.Ed448
import org.spotube.plugin_interfaces.host_apis.EdDSAEncodingFormats
import org.spotube.plugin_interfaces.host_apis.MACKeyGeneratorAlgorithms
import org.spotube.plugin_interfaces.host_apis.HMACEncodingFormats
import org.spotube.plugin_interfaces.host_apis.HashAlgorithms
import org.spotube.plugin_interfaces.host_apis.LegacyCipherAlgorithms
import org.spotube.plugin_interfaces.host_apis.RSAEncodingFormats
import org.spotube.plugin_interfaces.host_apis.RSAEncodingFormats.JWK
import org.spotube.plugin_interfaces.host_apis.RSAEncodingFormats.PEM
import org.spotube.plugin_interfaces.host_apis.SignAlgorithms
import org.spotube.plugin_interfaces.host_apis.MACSignatureAlgorithms
import org.spotube.spotube.core.zipline.host_apis.additionals.DES
import dev.whyoleg.cryptography.BinarySize
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyAlgorithmId
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.Digest
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.EdDSA
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.MD5
import dev.whyoleg.cryptography.algorithms.RIPEMD160
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA1
import dev.whyoleg.cryptography.algorithms.SHA224
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.algorithms.SHA384
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.bigint.toBigInt
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCryptographyApi::class)
fun HashAlgorithms.algorithm(): CryptographyAlgorithmId<Digest> {
    return when (this) {
        HashAlgorithms.SHA224 -> SHA224
        HashAlgorithms.SHA256 -> SHA256
        HashAlgorithms.SHA384 -> SHA384
        HashAlgorithms.SHA512 -> SHA512
        HashAlgorithms.SHA1 -> SHA1
        HashAlgorithms.MD5 -> MD5
        HashAlgorithms.RIPEMD160 -> RIPEMD160
    }
}

fun AESKeySize.size(): BinarySize {
    return when (this) {
        B128 -> AES.Key.Size.B128
        B192 -> AES.Key.Size.B192
        B256 -> AES.Key.Size.B256
    }
}

fun ECCurves.curve(): EC.Curve {
    return when (this) {
        P256 -> EC.Curve.P256
        P384 -> EC.Curve.P384
        P521 -> EC.Curve.P521
        secp256k1 -> EC.Curve.secp256k1
        brainpoolP256r1 -> EC.Curve.brainpoolP256r1
        brainpoolP384r1 -> EC.Curve.brainpoolP384r1
        brainpoolP512r1 -> EC.Curve.brainpoolP512r1
    }
}

fun EdDSACurves.curve(): EdDSA.Curve {
    return when (this) {
        Ed25519 -> EdDSA.Curve.Ed25519
        Ed448 -> EdDSA.Curve.Ed448
    }
}

fun ECEncodingFormats.privateKeyFormat(): EC.PrivateKey.Format {
    return when (this) {
        ECEncodingFormats.DER -> EC.PrivateKey.Format.DER
        ECEncodingFormats.RAW -> EC.PrivateKey.Format.RAW
        ECEncodingFormats.PEM -> EC.PrivateKey.Format.PEM
        ECEncodingFormats.JWK -> EC.PrivateKey.Format.JWK
    }
}

fun ECEncodingFormats.publicKeyFormat(): EC.PublicKey.Format {
    return when (this) {
        ECEncodingFormats.DER -> EC.PublicKey.Format.DER
        ECEncodingFormats.RAW -> EC.PublicKey.Format.RAW
        ECEncodingFormats.PEM -> EC.PublicKey.Format.PEM
        ECEncodingFormats.JWK -> EC.PublicKey.Format.JWK
    }
}

fun RSAEncodingFormats.privateKeyFormat(): RSA.PrivateKey.Format {
    return when (this) {
        RSAEncodingFormats.DER -> RSA.PrivateKey.Format.DER
        PEM -> RSA.PrivateKey.Format.PEM
        JWK -> RSA.PrivateKey.Format.JWK
    }
}

fun RSAEncodingFormats.publicKeyFormat(): RSA.PublicKey.Format {
    return when (this) {
        RSAEncodingFormats.DER -> RSA.PublicKey.Format.DER
        PEM -> RSA.PublicKey.Format.PEM
        JWK -> RSA.PublicKey.Format.JWK
    }
}

fun EdDSAEncodingFormats.privateKeyFormat(): EdDSA.PrivateKey.Format {
    return when (this) {
        EdDSAEncodingFormats.DER -> EdDSA.PrivateKey.Format.DER
        EdDSAEncodingFormats.PEM -> EdDSA.PrivateKey.Format.PEM
        EdDSAEncodingFormats.JWK -> EdDSA.PrivateKey.Format.JWK
        EdDSAEncodingFormats.RAW -> EdDSA.PrivateKey.Format.RAW
    }
}

fun EdDSAEncodingFormats.publicKeyFormat(): EdDSA.PublicKey.Format {
    return when (this) {
        EdDSAEncodingFormats.DER -> EdDSA.PublicKey.Format.DER
        EdDSAEncodingFormats.PEM -> EdDSA.PublicKey.Format.PEM
        EdDSAEncodingFormats.JWK -> EdDSA.PublicKey.Format.JWK
        EdDSAEncodingFormats.RAW -> EdDSA.PublicKey.Format.RAW
    }
}

fun ECDSASignatureFormats.signatureFormat(): ECDSA.SignatureFormat {
    return when (this) {
        ECDSASignatureFormats.DER -> ECDSA.SignatureFormat.DER
        ECDSASignatureFormats.RAW -> ECDSA.SignatureFormat.RAW
    }
}

fun HMACEncodingFormats.format(): HMAC.Key.Format {
    return when (this) {
        HMACEncodingFormats.RAW -> HMAC.Key.Format.RAW
        HMACEncodingFormats.JWK -> HMAC.Key.Format.JWK
    }
}

fun AESEncodingFormats.format(): AES.Key.Format {
    return when (this) {
        AESEncodingFormats.RAW -> AES.Key.Format.RAW
        AESEncodingFormats.JWK -> AES.Key.Format.JWK
    }
}

class RealCryptoAPI(private val scope: CoroutineContext) : CryptoAPI {
    private val provider = CryptographyProvider.Default

    override suspend fun generateRandomBytes(size: Int): ByteArray {
        return CryptographyRandom.nextBytes(size)
    }

    override suspend fun hash(algorithm: HashAlgorithms, data: ByteArray): ByteArray {
        val hasher = provider.get(algorithm.algorithm()).hasher()
        return hasher.hash(data)
    }

    override fun hashBlocking(algorithm: HashAlgorithms, data: ByteArray): ByteArray {
        return runBlocking(scope) {
            hash(algorithm, data)
        }
    }

    override suspend fun generateMACKey(algorithm: MACKeyGeneratorAlgorithms): ByteArray {
        return when (algorithm) {
            is MACKeyGeneratorAlgorithms.HMAC ->
                provider.get(HMAC).keyGenerator(algorithm.hashAlgorithm.algorithm()).generateKey()
                    .encodeToByteArray(algorithm.format.format())

            is MACKeyGeneratorAlgorithms.AES_CMAC -> provider.get(AES.CMAC)
                .keyGenerator(algorithm.keySize.size()).generateKey()
                .encodeToByteArray(algorithm.format.format())
        }
    }

    override fun generateMACKeyBlocking(algorithm: MACKeyGeneratorAlgorithms): ByteArray {
        return runBlocking(scope) {
            generateMACKey(algorithm)
        }
    }

    override suspend fun signWithMACKey(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        return when (algorithm) {
            is MACSignatureAlgorithms.HMAC ->
                provider.get(HMAC).keyDecoder(algorithm.hashAlgorithm.algorithm())
                    .decodeFromByteArray(algorithm.format.format(), key)
                    .signatureGenerator().generateSignature(data)

            is MACSignatureAlgorithms.AES_CMAC -> provider.get(AES.CMAC)
                .keyDecoder()
                .decodeFromByteArray(algorithm.format.format(), key)
                .signatureGenerator().generateSignature(data)
        }
    }

    override fun signWithMACKeyBlocking(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        return runBlocking(scope) {
            signWithMACKey(algorithm, key, data)
        }
    }

    override suspend fun verifyMACSignatureWithKey(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        signature: ByteArray,
        data: ByteArray
    ): Boolean {
        try {
            when (algorithm) {
                is MACSignatureAlgorithms.HMAC ->
                    provider.get(HMAC).keyDecoder(algorithm.hashAlgorithm.algorithm())
                        .decodeFromByteArray(
                            algorithm.format.format(),
                            key
                        )
                        .signatureVerifier().verifySignature(
                            data,
                            signature
                        )

                is MACSignatureAlgorithms.AES_CMAC -> provider.get(AES.CMAC)
                    .keyDecoder()
                    .decodeFromByteArray(algorithm.format.format(), key)
                    .signatureVerifier().verifySignature(
                        data,
                        signature
                    )
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun verifyMACSignatureWithKeyBlocking(
        algorithm: MACSignatureAlgorithms,
        key: ByteArray,
        signature: ByteArray,
        data: ByteArray
    ): Boolean {
        return runBlocking(scope) {
            verifyMACSignatureWithKey(algorithm, key, signature, data)
        }
    }

    override suspend fun generateKeyPair(algorithm: GenerateKeyPairAlgorithms): Pair<ByteArray, ByteArray> {
        return when (algorithm) {
            is GenerateKeyPairAlgorithms.RSA_PSS -> {
                val keyPair = provider.get(RSA.PSS).keyPairGenerator(
                    algorithm.keySizeBits.bits,
                    algorithm.hashAlgorithm.algorithm(),
                    algorithm.publicExponent.toBigInt()).generateKey()

                Pair(
                    keyPair.publicKey.encodeToByteArray(algorithm.format.publicKeyFormat()),
                    keyPair.privateKey.encodeToByteArray(algorithm.format.privateKeyFormat())
                )
            }

            is GenerateKeyPairAlgorithms.ECDSA -> {
                val keyPair = provider.get(ECDSA)
                    .keyPairGenerator(algorithm.curve.curve())
                    .generateKey()
                Pair(
                    keyPair.publicKey.encodeToByteArray(algorithm.format.publicKeyFormat()),
                    keyPair.privateKey.encodeToByteArray(algorithm.format.privateKeyFormat())
                )
            }

            is GenerateKeyPairAlgorithms.EdDSA -> {
                val keyPair = provider.get(EdDSA)
                    .keyPairGenerator(algorithm.curve.curve())
                    .generateKey()
                Pair(
                    keyPair.publicKey.encodeToByteArray(algorithm.format.publicKeyFormat()),
                    keyPair.privateKey.encodeToByteArray(algorithm.format.privateKeyFormat())
                )
            }

            is GenerateKeyPairAlgorithms.RSA_PKCS1 -> {
                val keyPair = provider.get(RSA.PKCS1)
                    .keyPairGenerator(
                        algorithm.keySizeBits.bits,
                        algorithm.hashAlgorithm.algorithm()
                    )
                    .generateKey()
                Pair(
                    keyPair.publicKey.encodeToByteArray(algorithm.format.publicKeyFormat()),
                    keyPair.privateKey.encodeToByteArray(algorithm.format.privateKeyFormat())
                )
            }
        }
    }

    override fun generateKeyPairBlocking(algorithm: GenerateKeyPairAlgorithms): Pair<ByteArray, ByteArray> {
        return runBlocking(scope) {
            generateKeyPair(algorithm)
        }
    }

    override suspend fun signWithPrivateKey(
        algorithm: SignAlgorithms,
        data: ByteArray,
        privateKey: ByteArray
    ): ByteArray {
        return when (algorithm) {
            is SignAlgorithms.RSA_PSS ->
                provider.get(RSA.PSS).privateKeyDecoder(algorithm.hashAlgorithm.algorithm())
                    .decodeFromByteArray(
                        algorithm.format.privateKeyFormat(),
                        privateKey
                    ).signatureGenerator().generateSignature(data)

            is SignAlgorithms.ECDSA ->
                provider.get(ECDSA).privateKeyDecoder(
                    algorithm.curve.curve()).decodeFromByteArray(
                    algorithm.format.privateKeyFormat(),
                    privateKey
                ).signatureGenerator(
                    algorithm.hashAlgorithm.algorithm(),
                    algorithm.signatureFormat.signatureFormat()
                ).generateSignature(data)

            is SignAlgorithms.EdDSA ->
                provider.get(EdDSA).privateKeyDecoder(
                    algorithm.curve.curve()).decodeFromByteArray(
                    algorithm.format.privateKeyFormat(),
                    privateKey
                ).signatureGenerator().generateSignature(data)

            is SignAlgorithms.RSA_PKCS1 ->
                provider.get(RSA.PKCS1).privateKeyDecoder(algorithm.hashAlgorithm.algorithm())
                    .decodeFromByteArray(
                        algorithm.format.privateKeyFormat(),
                        privateKey
                    ).signatureGenerator().generateSignature(data)
        }
    }

    override fun signWithPrivateKeyBlocking(
        algorithm: SignAlgorithms,
        data: ByteArray,
        privateKey: ByteArray
    ): ByteArray {
        return runBlocking(scope) {
            signWithPrivateKey(algorithm, data, privateKey)
        }
    }

    override suspend fun verifySignatureWithPublicKey(
        algorithm: SignAlgorithms,
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        return try {
            when (algorithm) {
                is SignAlgorithms.RSA_PSS ->
                    provider.get(RSA.PSS).publicKeyDecoder(algorithm.hashAlgorithm.algorithm())
                        .decodeFromByteArray(
                            algorithm.format.publicKeyFormat(),
                            publicKey
                        ).signatureVerifier().verifySignature(
                            data,
                            signature
                        )

                is SignAlgorithms.ECDSA ->
                    provider.get(ECDSA).publicKeyDecoder(
                        algorithm.curve.curve()).decodeFromByteArray(
                        algorithm.format.publicKeyFormat(),
                        publicKey
                    ).signatureVerifier(
                        algorithm.hashAlgorithm.algorithm(),
                        algorithm.signatureFormat.signatureFormat()
                    ).verifySignature(
                        data,
                        signature
                    )

                is SignAlgorithms.EdDSA ->
                    provider.get(EdDSA).publicKeyDecoder(
                        algorithm.curve.curve()).decodeFromByteArray(
                        algorithm.format.publicKeyFormat(),
                        publicKey
                    ).signatureVerifier().verifySignature(
                        data,
                        signature
                    )

                is SignAlgorithms.RSA_PKCS1 ->
                    provider.get(RSA.PKCS1).publicKeyDecoder(algorithm.hashAlgorithm.algorithm())
                        .decodeFromByteArray(
                            algorithm.format.publicKeyFormat(),
                            publicKey
                        ).signatureVerifier().verifySignature(
                            data,
                            signature
                        )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun verifySignatureWithPublicKeyBlocking(
        algorithm: SignAlgorithms,
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        return runBlocking(scope) {
            verifySignatureWithPublicKey(algorithm, data, signature, publicKey)
        }
    }

    override suspend fun encryptLegacy(
        algorithm: LegacyCipherAlgorithms,
        key: ByteArray,
        data: ByteArray,
        iv: ByteArray?
    ): ByteArray {
        return when (algorithm) {
            is LegacyCipherAlgorithms.DES -> DES.encrypt(
                data,
                key,
                mode = algorithm.mode,
                padding = algorithm.padding
            )

//            else -> throw IllegalArgumentException("Unsupported legacy cipher algorithm: $algorithm")
        }
    }

    override suspend fun decryptLegacy(
        algorithm: LegacyCipherAlgorithms,
        key: ByteArray,
        cipherText: ByteArray,
        iv: ByteArray?
    ): ByteArray {
        return when (algorithm) {
            is LegacyCipherAlgorithms.DES -> DES.decrypt(
                cipherText,
                key,
                mode = algorithm.mode,
                padding = algorithm.padding
            )
//            else -> throw IllegalArgumentException("Unsupported legacy cipher algorithm: $algorithm")
        }
    }

}