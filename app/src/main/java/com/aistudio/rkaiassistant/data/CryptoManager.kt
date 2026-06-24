package com.aistudio.rkaiassistant.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    } catch (e: Exception) {
        android.util.Log.e("RKAI", "KeyStore initialization failed", e)
        null
    }

    private fun getEncryptCipher(): Cipher? {
        return try {
            Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.ENCRYPT_MODE, getKey())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDecryptCipherForIv(iv: ByteArray): Cipher? {
        return try {
            Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getKey(): SecretKey? {
        if (keyStore == null) return null
        return try {
            val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            existingKey?.secretKey ?: createKey()
        } catch (e: Exception) {
            createKey()
        }
    }

    private fun createKey(): SecretKey? {
        return try {
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
            keyGenerator.generateKey()
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = getEncryptCipher() ?: return bytes
        return try {
            val encryptedBytes = cipher.doFinal(bytes)
            cipher.iv + encryptedBytes
        } catch (e: Exception) {
            bytes
        }
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        if (bytes.size < 12) return bytes // Too short to have an IV
        val iv = bytes.decodeIv()
        val encryptedPart = bytes.decodeEncryptedPart()
        val cipher = getDecryptCipherForIv(iv) ?: return bytes
        return try {
            cipher.doFinal(encryptedPart)
        } catch (e: Exception) {
            bytes
        }
    }

    fun encryptString(text: String): String {
        return try {
            android.util.Base64.encodeToString(encrypt(text.toByteArray()), android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            text // Fallback to plain text if encryption fails
        }
    }

    fun decryptString(encryptedBase64: String): String {
        return try {
            val bytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
            String(decrypt(bytes))
        } catch (e: Exception) {
            encryptedBase64 // Return as is
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
