package com.aistudio.rkaiassistant.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getEncryptCipher(): Cipher {
        return Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }
    }

    private fun getDecryptCipherForIv(iv: ByteArray): Cipher {
        return Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        }
    }

    private fun getKey(): SecretKey {
        return try {
            val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            existingKey?.secretKey ?: createKey()
        } catch (e: Exception) {
            createKey()
        }
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM_KEY, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = getEncryptCipher()
        val encryptedBytes = cipher.doFinal(bytes)
        return cipher.iv + encryptedBytes
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        if (bytes.size < 12) return bytes // Too short to have an IV
        val iv = bytes.decodeIv()
        val encryptedPart = bytes.decodeEncryptedPart()
        return getDecryptCipherForIv(iv).doFinal(encryptedPart)
    }

    fun encryptString(text: String): String {
        return try {
            android.util.Base64.encodeToString(encrypt(text.toByteArray()), android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    fun decryptString(encryptedBase64: String): String {
        return try {
            val bytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
            String(decrypt(bytes))
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    private fun ByteArray.decodeIv(): ByteArray = this.copyOfRange(0, 12)
    private fun ByteArray.decodeEncryptedPart(): ByteArray = this.copyOfRange(12, this.size)

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val ALGORITHM_KEY = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val KEY_ALIAS = "rk_assistant_private_space_key_v2"
    }
}
