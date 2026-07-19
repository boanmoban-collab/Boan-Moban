package com.mexc.mariabot.util

import java.net.URLEncoder
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MexcSignatureHelper {

    fun generateSignature(params: Map<String, String>, secretKey: String): String {
        // Sort parameters alphabetically
        val sortedParams = TreeMap(params)
        val queryString = sortedParams.map { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")

        return hmacSha256(queryString, secretKey)
    }

    private fun hmacSha256(data: String, key: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256HMAC.init(secretKeySpec)
        val hash = sha256HMAC.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }
}
