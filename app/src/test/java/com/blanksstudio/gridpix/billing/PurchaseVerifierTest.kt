package com.blanksstudio.gridpix.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PurchaseVerifierTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
    private val purchaseJson = """{"orderId":"GPA.1234","productId":"pack_animals","purchaseState":0}"""

    private fun sign(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    @Test
    fun `valid signature is accepted`() {
        assertTrue(PurchaseVerifier.verify(publicKeyBase64, purchaseJson, sign(purchaseJson)))
    }

    @Test
    fun `tampered payload is rejected`() {
        val signature = sign(purchaseJson)
        assertFalse(PurchaseVerifier.verify(publicKeyBase64, purchaseJson.replace("pack_animals", "everything"), signature))
    }

    @Test
    fun `wrong key, blank key and garbage are rejected`() {
        val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val otherKeyBase64 = Base64.getEncoder().encodeToString(otherKey.public.encoded)
        assertFalse(PurchaseVerifier.verify(otherKeyBase64, purchaseJson, sign(purchaseJson)))
        assertFalse(PurchaseVerifier.verify("", purchaseJson, sign(purchaseJson)))
        assertFalse(PurchaseVerifier.verify(publicKeyBase64, purchaseJson, "not-base64!!"))
        assertFalse(PurchaseVerifier.verify("bm90IGEga2V5", purchaseJson, sign(purchaseJson)))
    }
}
