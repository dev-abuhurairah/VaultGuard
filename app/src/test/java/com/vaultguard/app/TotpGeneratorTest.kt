package com.vaultguard.app

import com.vaultguard.app.features.tools.TotpGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpGeneratorTest {

    @Test
    fun testBase32Decoding() {
        val secret = "JBSWY3DPEHPK3PXP" // "Hello!" in Base32
        val decoded = TotpGenerator.decodeBase32(secret)
        val decodedStr = String(decoded, Charsets.UTF_8)
        assertEquals("Hello!", decodedStr)
    }

    @Test
    fun testTotpGeneration() {
        val secret = "JBSWY3DPEHPK3PXP"
        val state = TotpGenerator.generateCurrentTotp(secret)
        assertNotNull(state)
        assertEquals(6, state!!.code.length)
        assertTrue(state.remainingSeconds in 0..30)
        assertTrue(state.progress in 0.0f..1.0f)
    }

    @Test
    fun testOtpUriParsing() {
        val uri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        val parsed = TotpGenerator.parseOtpUri(uri)
        assertNotNull(parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed!!.second)
        assertTrue(parsed.first.contains("GitHub"))
    }
}
