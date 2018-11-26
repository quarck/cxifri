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
import net.cxifri.utils.*


class MainActivity : AppCompatActivity(), MainView {

    private val buttonEncrypt by UIItem<Button>(R.id.buttonEncrypt)
    private val buttonDecrypt by UIItem<Button>(R.id.buttonDecrypt)

    private val messageText by UIItem<EditText>(R.id.message)
    private val buttonKeySelect by UIItem<Button>(R.id.buttonKeySelect)
    private val passwordText by UIItem<EditText>(R.id.password)

    private val pleaseWaitText by UIItem<TextView>(R.id.textDerivingKeysStatus)

    private var isTextEncrypted = false

    private lateinit var controller: MainActivityController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowHomeEnabled(true)

        controller = MainActivityController(context = this, view = this)

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

    private fun handleIntent(intent: Intent) {
        val intentTextExtra = intent.getStringExtra(TextViewActivity.INTENT_EXTRA_TEXT)
        val intentKeyIdExtra = intent.getLongExtra(TextViewActivity.INTENT_EXTRA_KEY_ID, -1L)

        if (intentKeyIdExtra != -1L) {
            controller.currentKeyId = intentKeyIdExtra
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
                    controller.decryptIntentText(uri.toString())
                }
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (text != null) {
                    controller.decryptIntentText(text)
                }
            }
            Intent.ACTION_PROCESS_TEXT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val text = intent.getCharSequenceExtra(android.content.Intent.EXTRA_PROCESS_TEXT)
                    if (text != null) {
                        controller.decryptIntentText(text.toString())
                    }
                }
        }
    }

    override fun onIntentMessageDecryptResult(text: String, message: MessageBase?) {

        isTextEncrypted = (message == null) || (message !is TextMessage)

        runOnUiThread {
            when (message) {
                null -> {
                    // Decrypt failed
                    messageText.setText(text)
                    Toast.makeText(this, R.string.text_didnt_match_keys, Toast.LENGTH_LONG).show()
                }

                is TextMessage -> {
                    val intent = Intent(this, TextViewActivity::class.java)
                    intent.putExtra(TextViewActivity.INTENT_EXTRA_TEXT, message.text)
                            .putExtra(TextViewActivity.INTENT_EXTRA_KEY_ID, message.key.id)
                            .putExtra(TextViewActivity.INTENT_EXTRA_KEY_NAME, message.key.name)
                    startActivity(intent)
                }

                is KeyReplacementMessage ->
                    onKeyReplacementMessage(message)

                is KeyRevokeMessage ->
                    onKeyRevokeMessage(message)
            }
        }
    }

    override fun onMessageDecryptResult(text: String, message: MessageBase?) {

        isTextEncrypted = (message == null) || (message !is TextMessage)

        runOnUiThread {
            when (message) {
                null ->
                    Toast.makeText(this, R.string.failed_to_decrypt, Toast.LENGTH_LONG)
                            .show()

                is TextMessage ->
                    messageText.setText(message.text)

                is KeyReplacementMessage ->
                    onKeyReplacementMessage(message)

                is KeyRevokeMessage ->
                    onKeyRevokeMessage(message)
            }
        }
    }

    private fun onKeyRevokeMessage(message: KeyRevokeMessage) {
        controller.revokeKey(message.key)
    }

    override fun onControllerKeyRevoked(key: KeyEntry) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle(getString(R.string.key_revoked_title_fmt).format(key.name))
                    .setMessage(getString(R.string.key_revoked_desc))
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
            builder.create().show()
        }
    }

    private fun onKeyReplacementMessage(message: KeyReplacementMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onMessageEncryptResult(message: String?) {

        isTextEncrypted = message != null

        runOnUiThread{
            if (message != null) {
                messageText.setText(message)
            } else {
                Toast.makeText(this, R.string.failed_to_encrypt, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doSelectKeys(continueWithEncrypt: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(android.R.drawable.ic_menu_directions)
        builder.setTitle(getString(R.string.select_a_key))

        var (values, names) = controller.getKeyIdsWithNames()

        names += listOf<String>(getString(R.string.text_password))
        values += listOf(-1L)

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, names)

        builder.setNegativeButton(android.R.string.cancel) {
            dialog, which -> dialog.dismiss()
        }

        builder.setAdapter(arrayAdapter) {
            dialog, which ->
            if (which >= 0 && which < values.size && which < names.size) {
                val keyId = values[which]
                controller.currentKeyId = keyId
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

    override fun onControllerKeySelected(key: KeyEntry?) {
        runOnUiThread {
            if (key != null) {
                passwordText.visibility = View.GONE
                buttonKeySelect.text = getString(R.string.key_format).format(key.name)
            } else {
                passwordText.visibility = View.VISIBLE
                buttonKeySelect.text = getString(R.string.key_type_below)
            }
        }
    }

    override fun onEncryptPasswordIsEmpty() {
        runOnUiThread {
            Toast.makeText(this, R.string.password_is_empty, Toast.LENGTH_LONG).show()
        }
    }

    override fun onEncryptPasswordIsInsecure(complexity: ExhaustiveSearchComplexity) {

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
                        controller.encrypt(
                                messageText.text.toString(),
                                passwordText.text.toString(),
                                allowInsecurePassword = true
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            builder.create().show()
        }
    }

    private fun doEncrypt() {
        controller.encrypt(
                messageText.text.toString(),
                passwordText.text.toString()
        )
    }

    private fun onButtonEncrypt(v: View) {
        if (!controller.isKeyEverSelected)
            doSelectKeys(continueWithEncrypt = true)
        else
            doEncrypt()
    }

    override fun onKeyDerivationStarted() {
        runOnUiThread { pleaseWaitText.visibility = View.VISIBLE }
    }

    override fun onKeyDerivationFinished() {
        runOnUiThread { pleaseWaitText.visibility = View.GONE }
    }



    private fun onButtonDecrypt(v: View) {
        controller.decrypt(
                messageText.text.toString(),
                passwordText.text.toString()
        )
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

