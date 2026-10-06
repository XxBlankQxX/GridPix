package com.blanksstudio.gridpix.billing

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Verifies the Play-signed purchase JSON against the app's Play licence public key
 * (Play Console > Monetisation setup). Local-only app, no server, so this is the only check we have.
 * Pure JVM code: unit-tested with a generated key pair.
 */
object PurchaseVerifier {

    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /** True when [signatureBase64] is a valid signature of [signedData] under [base64PublicKey]. */
    fun verify(base64PublicKey: String, signedData: String, signatureBase64: String): Boolean {
        if (base64PublicKey.isBlank() || signedData.isBlank() || signatureBase64.isBlank()) return false
        return try {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey(base64PublicKey))
            signature.update(signedData.toByteArray(Charsets.UTF_8))
            signature.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (e: Exception) {
            false
        }
    }

    private fun publicKey(base64: String): PublicKey {
        val bytes = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance(KEY_FACTORY_ALGORITHM).generatePublic(X509EncodedKeySpec(bytes))
    }
}
