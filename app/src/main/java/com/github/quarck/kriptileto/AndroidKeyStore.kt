// Android key store is only used to encrypt keypairs stored in the DB
// Main app encryption is performed by the Bouncy Castle implementation, not by this
package com.github.quarck.kriptileto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AndroidKeyStore() {

    private val KEY_PREFIX = "key_"

    private fun keyNameForId(id: Long) = "${KEY_PREFIX}_$id"

    fun encrypt(keyId: Long, input: ByteArray): ByteArray? = encryptDecryptData(keyId, input, true)

    fun decrypt(keyId: Long, input: ByteArray): ByteArray? = encryptDecryptData(keyId, input, false)

    private fun encryptDecryptData(keyId: Long, input: ByteArray, encrypt: Boolean): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(keyNameForId(keyId), null) as SecretKey?
            if (secretKey != null) {
                // YES, ECB! We are encrypting two-block length binary random key, so no need for IV and
                // other overhead
                val transformation = KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7

                val cipher = Cipher.getInstance(transformation)

                if (encrypt) {
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    val iv = cipher.iv
                    val encoded = cipher.doFinal(input)
                    return iv + encoded
                }
                else  {
                    val ivLen = 16
                    val iv = ByteArray(ivLen)
                    val msg = ByteArray(input.size - ivLen)
                    System.arraycopy(input, 0, iv, 0, ivLen)
                    System.arraycopy(input, ivLen, msg, 0, input.size - ivLen)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey,  IvParameterSpec(iv))
                    return cipher.doFinal(msg)
                }
            }
            return null
        } catch (e: UserNotAuthenticatedException) {
            return null
        } catch (e: KeyPermanentlyInvalidatedException) {
            return null
        } catch (e: Exception) {
            return null
        }
    }

    fun createKey(id: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        try {
            val keyName = keyNameForId(id)

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (keyStore.containsAlias(keyName))
                return // already there

            val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            val purpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT

            keyGenerator.init(
                    KeyGenParameterSpec.Builder(keyName, purpose)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    //.setUserAuthenticationRequired(true)
                    //.setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()

        } catch (ex: Exception) {
            throw ex
        }
    }

    fun dropKey(id: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        try {
            val keyName = keyNameForId(id)

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(keyName)
        } catch (ex: Exception) {
            throw ex
        }
    }

    companion object {
        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}