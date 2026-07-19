package com.mexc.mariabot.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurePreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mariabot_secure_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "MariaBotCryptoKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val DELIMITER = "]"
    }

    // Fallback key (AES-128) if KeyStore is not available
    private val fallbackKeyBytes = byteArrayOf(
        0x4d, 0x61, 0x72, 0x69, 0x61, 0x42, 0x6f, 0x74, 
        0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x4b, 0x65, 
        0x79, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37
    ).copyOf(16)

    private val fallbackIvBytes = byteArrayOf(
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f
    )

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: generateNewKey()
            } else {
                generateNewKey()
            }
        } catch (e: Exception) {
            SecretKeySpec(fallbackKeyBytes, "AES")
        }
    }

    private fun generateNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val ivStr = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val encryptedStr = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivStr$DELIMITER$encryptedStr"
        } catch (e: Exception) {
            // Fallback lightweight CBC encryption if KeyStore GCM fails
            try {
                val keySpec = SecretKeySpec(fallbackKeyBytes, "AES")
                val ivSpec = IvParameterSpec(fallbackIvBytes)
                val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
                val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
                Base64.encodeToString(encrypted, Base64.DEFAULT)
            } catch (ex: Exception) {
                value
            }
        }
    }

    private fun decrypt(encryptedValue: String): String {
        if (encryptedValue.isEmpty()) return ""
        return try {
            if (encryptedValue.contains(DELIMITER)) {
                val parts = encryptedValue.split(DELIMITER)
                val ivBytes = Base64.decode(parts[0], Base64.NO_WRAP)
                val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
                
                val key = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                val spec = GCMParameterSpec(128, ivBytes)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                String(decryptedBytes, StandardCharsets.UTF_8)
            } else {
                // Decrypt fallback AES-CBC legacy strings
                val keySpec = SecretKeySpec(fallbackKeyBytes, "AES")
                val ivSpec = IvParameterSpec(fallbackIvBytes)
                val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                val decoded = Base64.decode(encryptedValue, Base64.DEFAULT)
                val decrypted = cipher.doFinal(decoded)
                String(decrypted, StandardCharsets.UTF_8)
            }
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
