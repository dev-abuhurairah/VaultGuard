package com.vaultguard.app.features.tools

import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

/**
 * RFC 6238 Standard Time-Based One-Time Password (TOTP) Authenticator.
 * Compatible with Google Authenticator, Bitwarden, and Microsoft Authenticator.
 */
object TotpGenerator {

    private const val DEFAULT_TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6
    private const val DIGITS_MODULO = 1_000_000

    /**
     * Represents the live state of a 2FA TOTP token.
     */
    data class TotpState(
        val code: String,
        val remainingSeconds: Int,
        val progress: Float // 1.0f to 0.0f
    )

    /**
     * Generates the current 6-digit TOTP code and countdown progress.
     */
    fun generateCurrentTotp(secretBase32: String, timeStep: Long = DEFAULT_TIME_STEP_SECONDS): TotpState? {
        val cleanSecret = secretBase32.trim().replace(" ", "").uppercase()
        if (cleanSecret.isBlank()) return null

        return try {
            val keyBytes = decodeBase32(cleanSecret)
            val currentTimeSec = System.currentTimeMillis() / 1000L
            val currentInterval = floor(currentTimeSec.toDouble() / timeStep).toLong()
            val remaining = (timeStep - (currentTimeSec % timeStep)).toInt()
            val progress = remaining.toFloat() / timeStep.toFloat()

            val code = calculateTotp(keyBytes, currentInterval)
            TotpState(code, remaining, progress)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the OTP code for a specific interval counter using HMAC-SHA1.
     */
    private fun calculateTotp(keyBytes: ByteArray, counter: Long): String {
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(keyBytes, "RAW")
        mac.init(keySpec)
        val hash = mac.doFinal(counterBytes)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % DIGITS_MODULO
        return String.format("%06d", otp)
    }

    /**
     * Base32 decoder without third-party dependencies (RFC 4648).
     */
    fun decodeBase32(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trim().uppercase().replace("=", "")
        val bytes = mutableListOf<Byte>()

        var buffer = 0
        var bitsLeft = 0

        for (c in clean) {
            val charValue = base32Chars.indexOf(c)
            if (charValue < 0) continue

            buffer = (buffer shl 5) or charValue
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return bytes.toByteArray()
    }

    /**
     * Parses standard 'otpauth://totp/...' URIs from QR codes.
     */
    fun parseOtpUri(uriString: String): Pair<String, String>? {
        return try {
            val uri = URI(uriString)
            if (uri.scheme != "otpauth" || uri.host != "totp") return null

            var label = uri.path.trimStart('/')
            label = URLDecoder.decode(label, "UTF-8")

            val queryParams = uri.query?.split("&")?.associate {
                val parts = it.split("=")
                parts[0] to (if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else "")
            } ?: emptyMap()

            val secret = queryParams["secret"] ?: return null
            val issuer = queryParams["issuer"] ?: ""
            val title = if (issuer.isNotBlank() && !label.contains(issuer)) "$issuer ($label)" else label

            Pair(title, secret)
        } catch (e: Exception) {
            null
        }
    }
}
