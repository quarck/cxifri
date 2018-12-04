package net.cxifri.ui

import android.content.Context
import android.widget.Toast
import net.cxifri.R.id.message
import net.cxifri.R.id.password
import net.cxifri.crypto.*
import net.cxifri.keysdb.KeysDatabase
import net.cxifri.utils.ExhaustiveSearchComplexity
import net.cxifri.utils.PasswordComplexityEstimator
import net.cxifri.utils.background

interface MainView  {
    fun onControllerKeySelected(key: KeyEntry?)
    fun onControllerKeyRevoked(key: KeyEntry)

    fun onKeyDerivationStarted()
    fun onKeyDerivationFinished()

    fun onEncryptPasswordIsEmpty()
    fun onEncryptPasswordIsInsecure(complexity: ExhaustiveSearchComplexity)

    fun onMessageEncryptResult(encrypted: String?)
    fun onIntentMessageDecryptResult(text: String, message: MessageBase?)
    fun onMessageDecryptResult(text: String, msg: MessageBase?)
}

class MainActivityController(val context: Context, val view: MainView) {

    private var _currentKey: KeyEntry? = null
    private var _isKeyEverSelected = false

    val isKeyEverSelected get() = _isKeyEverSelected

    private val messageHandler: MessageHandlerInterface by lazy { CryptoFactory.createMessageHandler() }

    var currentKey: KeyEntry?
        get() {
            return _currentKey
        }
        set(value) {
            _isKeyEverSelected = true
            _currentKey = value
            view.onControllerKeySelected(_currentKey)
        }

    var currentKeyId: Long
        get() {
            return _currentKey?.id ?: -1L
        }
        set(value) {
            _isKeyEverSelected = true
            _currentKey = KeysDatabase(context).use { it.getKey(value) }
            view.onControllerKeySelected(_currentKey)
        }

    fun encrypt(msg: String, passwordForEmptyKey: String?, allowInsecurePassword: Boolean = false) {

        background {
            var key = _currentKey

            if (key == null) {
                if (passwordForEmptyKey == null || passwordForEmptyKey.isEmpty()) {
                    view.onEncryptPasswordIsEmpty()
                    return@background
                }

                if (!allowInsecurePassword) {
                    val complexity = PasswordComplexityEstimator.getExhaustiveSearchComplexity(passwordForEmptyKey)
                    if (!complexity.isSecure) {
                        view.onEncryptPasswordIsInsecure(complexity)
                        return@background
                    }
                }

                view.onKeyDerivationStarted()

                key = CryptoFactory.deriveKeyFromPassword(
                        password = passwordForEmptyKey,
                        name = "")

                view.onKeyDerivationFinished()
            }

            val encrypted =
                    try {
                        messageHandler.encrypt(TextMessage(key, msg), key)
                    } catch (ex: Exception) {
                        null
                    }
            view.onMessageEncryptResult(encrypted)
        }
    }

    private fun decryptIterateAll(text: String, onResult: (MessageBase?) -> Unit) {

        val keys = KeysDatabase(context).use { it.keys }

        val message = messageHandler.decrypt(text, keys)
        if (message != null && message.key.id != currentKeyId) {
            currentKey = message.key
        }

        onResult(message)
    }

    fun decryptIntentText(text: String) {
        background {
            decryptIterateAll(text) { msg -> view.onIntentMessageDecryptResult(text, msg) }
        }
    }

    fun decrypt(text: String, passwordForEmptyKey: String) {

        background {
            var key = _currentKey

            if (key == null) {
                view.onKeyDerivationStarted()
                key = CryptoFactory.deriveKeyFromPassword(
                        password = passwordForEmptyKey,
                        name = "")
                view.onKeyDerivationFinished()
            }

            try {
                val decrypted = messageHandler.decrypt(text, key)
                if (decrypted != null) {
                    view.onMessageDecryptResult(text, decrypted)
                } else {
                    decryptIterateAll(text) { msg -> view.onMessageDecryptResult(text, msg) }
                }
            } catch (ex: Exception) {
                decryptIterateAll(text) { msg -> view.onMessageDecryptResult(text, msg) }
            }
        }
    }

    fun getKeyIdsWithNames(): Pair<List<Long>, List<String>> {
        val keys = KeysDatabase(context).use { it.keys }

        val names = keys.map { it.name }.toList()
        val values = keys.map { it.id }.toList()

        return Pair(values, names)
    }

    fun revokeKey(key: KeyEntry) {
        KeysDatabase(context).use {
            it.update(key.copy(revoked = true))
        }
        view.onControllerKeyRevoked(key)
    }
}