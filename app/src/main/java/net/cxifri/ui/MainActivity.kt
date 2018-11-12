/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cxifri.ui

import android.app.AlertDialog
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import net.cxifri.R
import net.cxifri.crypto.*
import net.cxifri.keysdb.KeysDatabase
import net.cxifri.utils.*


class MainActivity : AppCompatActivity() {

    val buttonEncrypt by UIItem<Button>(R.id.buttonEncrypt)
    val buttonDecrypt by UIItem<Button>(R.id.buttonDecrypt)

    val messageText by UIItem<EditText>(R.id.message)
    val buttonKeySelect by UIItem<Button>(R.id.buttonKeySelect)
    val passwordText by UIItem<EditText>(R.id.password)
    val textViewError by UIItem<TextView>(R.id.textViewError)

    var isTextEncrypted = false

    var isKeyEverSelected = false

    var currentKey: KeyEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowHomeEnabled(true)

        buttonKeySelect.setOnClickListener(this::onButtonKeySelect)
        buttonEncrypt.setOnClickListener(this::onButtonEncrypt)
        buttonDecrypt.setOnClickListener(this::onButtonDecrypt)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_clear ->
                clearForm()

            R.id.menu_copy ->
                copyText()

            R.id.menu_paste ->
                pasteText()

            R.id.menu_share ->
                shareText()

            R.id.menu_keys ->
                startActivity(Intent(this, KeysActivity::class.java))

            R.id.menu_about ->
                startActivity(Intent(this, AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent) {
        val intentTextExtra = intent.getStringExtra(TextViewActivity.INTENT_EXTRA_TEXT)
        val intentKeyIdExtra = intent.getLongExtra(TextViewActivity.INTENT_EXTRA_KEY_ID, -1L)

        if (intentKeyIdExtra != -1L) {
            onKeySelected(intentKeyIdExtra)
            if (intentTextExtra != null) {
                messageText.setText(intentTextExtra)
                isTextEncrypted = false
            }
        }
        else if (intentTextExtra != null) {
            messageText.setText(intentTextExtra)
            isTextEncrypted = false
        }
        else when(intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    handleTextIntent(uri.toString())
                }
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (text != null) {
                    handleTextIntent(text)
                }
            }
            Intent.ACTION_PROCESS_TEXT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val text = intent.getCharSequenceExtra(android.content.Intent.EXTRA_PROCESS_TEXT)
                    if (text != null) {
                        handleTextIntent(text.toString())
                    }
                }
        }
    }

    private fun onIntentMessageDecrypted(message: MessageBase): Boolean {
        var success = false

        when (message) {
            is TextMessage -> {
                runOnUiThread {
                    val intent = Intent(this, TextViewActivity::class.java)
                    intent.putExtra(TextViewActivity.INTENT_EXTRA_TEXT, message.text)
                            .putExtra(TextViewActivity.INTENT_EXTRA_KEY_ID, message.key.id)
                            .putExtra(TextViewActivity.INTENT_EXTRA_KEY_NAME, message.key.name)
                    startActivity(intent)
                }
                currentKey = message.key
                success = true
            }

            is KeyReplacementMessage -> TODO("Not implemented")

            is KeyRevokeMessage -> TODO("Not implemented also")
        }

        return success
    }

    private fun onMessageDecrypted(message: MessageBase, updateKey: Boolean) {
        when (message) {
            is TextMessage ->
                runOnUiThread {
                    messageText.setText(message.text)
                    onKeySelected(message.key)
                }
            is KeyReplacementMessage -> TODO("Not implemented")

            is KeyRevokeMessage -> TODO("not impl allsss")
        }
    }

    private fun onMessageEncrypted(message: String) {
        runOnUiThread {
            messageText.setText(message)
        }
    }

    private fun onDecryptFailed() {
        runOnUiThread {
            Toast.makeText(this, R.string.failed_to_decrypt, Toast.LENGTH_LONG).show()
        }
    }

    private fun onEncryptFailed() {
        runOnUiThread {
            Toast.makeText(this, R.string.failed_to_encrypt, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleTextIntent(text: String) {

        // Text can be either encrypted (user wants to decrypt it) or plaintext (user wants to
        // encrypt it). Attempt to decrypt using known keys, if success - show the activity
        // with the decrypted text, otherwise -- just populate text field here
        background {
            val keys = KeysDatabase(context = this).use { it.keys }
            var success = false

            val messageHandler = CryptoFactory.createMessageHandler()

            val message = messageHandler.decrypt(text, keys)
            if (message != null) {
                success = onIntentMessageDecrypted(message)
            }

            if (!success) {
                isTextEncrypted = false // don't allow sharing
                runOnUiThread {
                    messageText.setText(text)
                    Toast.makeText(this, R.string.text_didnt_match_keys, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun doSelectKeys(continueWithEncrypt: Boolean) {
        val keys = KeysDatabase(context = this).use { it.keys }

        val builder = AlertDialog.Builder(this)
        builder.setIcon(android.R.drawable.ic_menu_directions)
        builder.setTitle(getString(R.string.select_a_key))

        val names = keys.map { it.name }.toList() + listOf<String>(getString(R.string.text_password))
        val values = keys.map { it.id }.toList() + listOf<Long>(-1L)

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, names)

        builder.setNegativeButton(android.R.string.cancel) {
            dialog, which -> dialog.dismiss()
        }

        builder.setAdapter(arrayAdapter) {
            dialog, which ->
            if (which >= 0 && which < values.size && which < names.size) {
                val keyId = values[which]
                onKeySelected(keyId)
                if (continueWithEncrypt) {
                    doEncrypt()
                }
            }
        }

        return builder.create().show()
    }

    private fun onButtonKeySelect(v: View) {
        doSelectKeys(false)
    }

    private fun onKeySelected(keyId: Long) {
        isKeyEverSelected = true
        if (keyId != -1L) {
            passwordText.visibility = View.GONE
            currentKey = KeysDatabase(context = this).use { it.getKey(keyId) }
            buttonKeySelect.text = getString(R.string.key_format).format(currentKey?.name)
        }
        else {
            passwordText.visibility = View.VISIBLE
            buttonKeySelect.text = getString(R.string.key_type_below)
            currentKey = null
        }
    }

    private fun onKeySelected(key: KeyEntry?) {
        isKeyEverSelected = true
        if (key != null) {
            passwordText.visibility = View.GONE
            currentKey = key
            buttonKeySelect.text = getString(R.string.key_format).format(currentKey?.name)
        }
        else {
            passwordText.visibility = View.VISIBLE
            buttonKeySelect.text = getString(R.string.key_type_below)
            currentKey = null
        }
    }


    private fun doEncrypt(key: KeyEntry, msg: String) {
        try {
            val encrypted = CryptoFactory.createMessageHandler().encrypt(TextMessage(key, msg), key)
            isTextEncrypted = true
            onMessageEncrypted(encrypted)
        } catch (ex: Exception) {
            onEncryptFailed()
        }
    }

    private fun doEncrypt() {
        val msg = messageText.text.toString()
        val password = passwordText.text.toString()

        textViewError.visibility = View.GONE

        background {
            var cKey = currentKey //  ?: CryptoFactory.deriveKeyFromPassword(key)
            if (cKey == null) {

                if (password.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, R.string.password_is_empty, Toast.LENGTH_LONG).show()
                    }
                    return@background
                }

                val complexity  = PasswordComplexityEstimator.getExhaustiveSearchComplexity(password)
                if (!complexity.isSecure) {

                    runOnUiThread {
                        val builder = AlertDialog.Builder(this)
                                .setIcon(R.drawable.ic_launcher_foreground)
                                .setTitle(getString(R.string.password_stength_notice))
                                .setMessage(getString(R.string.still_proceed_question).format(
                                        complexity.formatPc(this),
                                        complexity.formatGpu(this),
                                        complexity.formatCluster(this)
                                ))
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    background {
                                        doEncrypt(CryptoFactory.deriveKeyFromPassword(password, ""), msg)
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> }

                        builder.create().show()
                    }
                    return@background
                }

                cKey = CryptoFactory.deriveKeyFromPassword(password, "")
            }

            doEncrypt(cKey, msg)
        }
    }

    private fun onButtonEncrypt(v: View) {
        if (!isKeyEverSelected)
            doSelectKeys(continueWithEncrypt = true)
        else
            doEncrypt()
    }

    private fun decryptIterateAllKeys(text: String) {

        val keys = KeysDatabase(context = this).use { it.keys }

        val messageHandler = CryptoFactory.createMessageHandler()

        val message = messageHandler.decrypt(text, keys)
        if (message != null) {
            onMessageDecrypted(message, true)
        }
        else {
            onDecryptFailed()
        }
    }

    private fun onButtonDecrypt(v: View) {
        val msg = messageText.text.toString()
        val password = passwordText.text.toString()

        textViewError.visibility = View.GONE

        background {
            try {
                val cKey = currentKey ?: CryptoFactory.deriveKeyFromPassword(password, "")
                val decrypted = CryptoFactory.createMessageHandler().decrypt(msg, cKey)

                if (decrypted != null) {
                    isTextEncrypted = false
                    onMessageDecrypted(decrypted, false)
                }
                else {
                    decryptIterateAllKeys(msg)
                }
            } catch (ex: Exception) {
                decryptIterateAllKeys(msg)
            }
        }
    }

    private fun copyText() {
        val msg = messageText.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }

        Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    private fun pasteText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.getPrimaryClip()
        if (clip != null) {
            val item = clip.getItemAt(0)
            if (item != null)
                messageText.setText(item.text)
        }

        isTextEncrypted = false

        Toast.makeText(this, R.string.text_pasted, Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        messageText.setText("")
        passwordText.setText("")
        isTextEncrypted = false
        Toast.makeText(this, R.string.text_cleared, Toast.LENGTH_SHORT).show()
    }


    private fun doShareText() {
        val intent = Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, messageText.text.toString())
        startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
    }

    private fun shareText() {
        if (isTextEncrypted) {
            doShareText()
        }
        else {
            val builder = AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle(getString(R.string.share_non_encrypted_text_question_mark))
                    .setPositiveButton(android.R.string.ok) { _, _ -> doShareText() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
            return builder.create().show()
        }
    }
}

