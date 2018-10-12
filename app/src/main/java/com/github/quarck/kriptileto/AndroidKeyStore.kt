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

class AndroidKeyStore() {

    private val MASTER_KEY_NAME = "master_key"

    init {
        if (!wasKeyCreated())
            createKey()
    }

    fun encrypt(input: ByteArray): ByteArray? = encryptDecryptData(input, true)

    fun decrypt(input: ByteArray): ByteArray? = encryptDecryptData(input, false)

    private fun wasKeyCreated(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false

        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(MASTER_KEY_NAME, null) as SecretKey?
            return secretKey != null
        } catch (e: UserNotAuthenticatedException) {
            // User is not authenticated, let's authenticate with device credentials.
            return false
        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: Exception) {
            throw e
        }
    }

    private fun encryptDecryptData(input: ByteArray, encrypt: Boolean): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(MASTER_KEY_NAME, null) as SecretKey?
            if (secretKey != null) {
                val transformation = KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7

                val cipher = Cipher.getInstance(transformation)
                cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, secretKey)

                return cipher.doFinal(input)
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

    private fun createKey() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            val purpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT

            keyGenerator.init(
                    KeyGenParameterSpec.Builder(MASTER_KEY_NAME, purpose)
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

    companion object {
        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}