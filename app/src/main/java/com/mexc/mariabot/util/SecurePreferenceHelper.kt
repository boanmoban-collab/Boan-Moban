package com.mexc.mariabot.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurePreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mariabot_secure_settings", Context.MODE_PRIVATE)
    
    // Fallback lightweight secure encryption key to safeguard configurations
    private val keyBytes = byteArrayOf(
        0x4d, 0x61, 0x72, 0x69, 0x61, 0x42, 0x6f, 0x74, 
        0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x4b, 0x65, 
        0x79, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37
    ).copyOf(16) // Exactly 128-bit key for AES-128
    
    private val ivBytes = byteArrayOf(
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
    )

    private fun encrypt(value: String): String {
        return try {
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            value
        }
    }

    private fun decrypt(encryptedValue: String): String {
        return try {
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decoded = Base64.decode(encryptedValue, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedValue
        }
    }

    fun saveString(key: String, value: String) {
        val encrypted = encrypt(value)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun getString(key: String, defaultValue: String): String {
        val encrypted = prefs.getString(key, null) ?: return defaultValue
        return decrypt(encrypted)
    }

    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
