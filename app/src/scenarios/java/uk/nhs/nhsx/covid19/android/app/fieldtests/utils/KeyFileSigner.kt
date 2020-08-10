/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.nhs.nhsx.covid19.android.app.fieldtests.utils

import android.security.keystore.KeyProperties
import uk.nhs.nhsx.covid19.android.app.fieldtests.proto.SignatureInfo
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.spec.ECGenParameterSpec

/**
 * Signs diagnosis key files.
 *
 *
 * Uses a randomly generated public/private keypair to sign files.
 */
class KeyFileSigner private constructor() {
    private var keyPair: KeyPair? = null
        private set

    private fun init() {
        keyPair = try {
            val keyGen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            keyGen.initialize(ECGenParameterSpec(EC_PARAM_SPEC_NAME))
            // Creates a random key each time.
            keyGen.generateKeyPair()
        } catch (e: InvalidAlgorithmParameterException) {
            // TODO: Better exception.
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun sign(message: ByteArray?): ByteArray {
        checkKeyStoreInit()
        return try {
            val sig = Signature.getInstance(SIG_ALGO)
            sig.initSign(keyPair!!.private)
            sig.update(message)
            sig.sign()
        } catch (e: NoSuchAlgorithmException) {
            // TODO: Better exception.
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: SignatureException) {
            throw RuntimeException(e)
        }
    }

    fun signatureInfo(): SignatureInfo {
        // KeyStore init is not strictly required here, but this sig info is useless without KeyStore.
        checkKeyStoreInit()
        return SignatureInfo.newBuilder()
            .setVerificationKeyId(SIGNATURE_ID)
            .setVerificationKeyVersion(SIGNATURE_VERSION)
            .setSignatureAlgorithm(SIG_ALGO_OID)
            .build()
    }

    private fun checkKeyStoreInit() {
        checkNotNull(keyPair) { "KeyPair was not initialised. That really shouldn't be possible." }
    }

    companion object {
        private const val TAG = "KeyFileSigner"
        private const val EC_PARAM_SPEC_NAME = "secp256r1"
        private const val SIG_ALGO = "SHA256withECDSA"

        // http://oid-info.com/get/1.2.840.10045.4.3.2
        private const val SIG_ALGO_OID = "1.2.840.10045.4.3.2"
        const val SIGNATURE_ID = "test-signature-id"
        const val SIGNATURE_VERSION = "test-signature-version"
        private var INSTANCE: KeyFileSigner? = null

        /**
         * Creator method used with private constructor, for singleton operation.
         */
        fun get(): KeyFileSigner? {
            if (INSTANCE == null) {
                INSTANCE = KeyFileSigner()
            }
            return INSTANCE
        }
    }
}
