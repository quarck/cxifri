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
import android.widget.Button
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import net.cxifri.crypto.CxifriMessage
import net.cxifri.R
import net.cxifri.keysdb.KeyEntry
import net.cxifri.keysdb.KeysDatabase
import net.cxifri.utils.background


class MainActivity : AppCompatActivity() {

    lateinit var buttonEncrypt: Button
    lateinit var buttonDecrypt: Button

    lateinit var message: EditText
    lateinit var buttonKeySelect: Button
    lateinit var password: EditText
    lateinit var textViewError: TextView

    var isTextEncrypted = false

    var currentKey: KeyEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowHomeEnabled(true)


        buttonEncrypt = findViewById(R.id.buttonEncrypt)
        buttonDecrypt = findViewById(R.id.buttonDecrypt)
        buttonKeySelect = findViewById(R.id.buttonKeySelect)
        textViewError = findViewById(R.id.textViewError)

        message = findViewById(R.id.message)
        password = findViewById(R.id.password)

        buttonKeySelect.setOnClickListener(this::onButtonKeySelect)
        buttonEncrypt.setOnClickListener(this::onButtonEncrypt)
        buttonDecrypt.setOnClickListener(this::onButtonDecrypt)

        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

//        val menuShare = menu.findItem(R.id.menu_share)
//        if (menuShare != null) {
//            menuShare.isVisible = true
//            menuShare.isEnabled = isTextEncrypted
//        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_clear ->
                clearForm()

            R.id.menu_copy ->
                copyText()

            R.id.menu_share ->
                shareText()

            R.id.menu_keys ->
                startActivity(Intent(this, KeysActivity::class.java))

            R.id.menu_about ->
                Unit
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
                message.setText(intentTextExtra)
                isTextEncrypted = false
            }
        }
        else if (intentTextExtra != null) {
            message.setText(intentTextExtra)
            isTextEncrypted = false
        }
//        else if (intent.action == Intent.ACTION_SEND) {
//            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
//            if (text != null) {
//                message.setText(text)
//                isTextEncrypted = false
//                buttonShare.isEnabled = false
//            }
//        }
        else if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                handleTextIntent(uri.toString())
            }
        }
        else if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (text != null) {
                handleTextIntent(text)
            }
        }
        else if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val text = intent.getCharSequenceExtra(android.content.Intent.EXTRA_PROCESS_TEXT)
                if (text != null) {
                    handleTextIntent(text.toString())
                }
            }
        }
    }

    fun handleTextIntent(text: String) {

        // Text can be either encrypted (user wants to decrypt it) or plaintext (user wants to
        // encrypt it). Attempt to decrypt using known keys, if success - show the activity
        // with the decrypted text, otherwise -- just populate text field here
        background {
            val keys = KeysDatabase(context = this).use { it.keys }
            var success = false

            val cryptoMessage = CxifriMessage()

            // TODO: move this to a "worker" file
            for (key in keys) {
                try {
                    val decrypted = cryptoMessage.decrypt(text, key)
                    if (decrypted != null) {
                        runOnUiThread {
                            val intent = Intent(this, TextViewActivity::class.java)
                            intent.putExtra(TextViewActivity.INTENT_EXTRA_TEXT, decrypted)
                                    .putExtra(TextViewActivity.INTENT_EXTRA_KEY_ID, key.id)
                                    .putExtra(TextViewActivity.INTENT_EXTRA_KEY_NAME, key.name)
                            startActivity(intent)
//                            textViewMessage.setText(decrypted)
//                            textViewAuthStatus.setText("Decrypted and valid, key: ${key.name}")
                        }
                        currentKey = key
                        success = true
                        break
                    }
                } catch (ex: Exception) {
                }
            }

            if (!success) {
                isTextEncrypted = false // don't allow sharing
                runOnUiThread {
                    message.setText(text)
                }
            }
        }
    }

    private fun onButtonKeySelect(v: View) {
        val keys = KeysDatabase(context = this).use { it.keys }

        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.drawable.ic_launcher_foreground)
        builder.setTitle("Pick a key")

        val names = keys.map { it.name }.toList() + listOf<String>("Text password")
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
            }
        }

        return builder.create().show()

    }

    private fun onKeySelected(keyId: Long) {
        if (keyId != -1L) {
            password.visibility = View.GONE
            currentKey = KeysDatabase(context = this).use { it.getKey(keyId) }
            buttonKeySelect.text = "key: ${currentKey?.name}"
        }
        else {
            password.visibility = View.VISIBLE
            buttonKeySelect.text = "key: type below"
            currentKey = null
        }
    }



    private fun onButtonEncrypt(v: View) {
        val msg = message.text.toString()
        val key = password.text.toString()

        textViewError.visibility = View.GONE

        background {
            var encrypted: String? = null
            val cKey = currentKey

            if (cKey != null) {
                try {
                    encrypted = CxifriMessage().encrypt(msg, cKey)
                } catch (ex: Exception) {
                    encrypted = null
                }
            } else {
                if (key.isEmpty()) {
                    runOnUiThread {
                        textViewError.setText("Key is empty")
                        textViewError.visibility = View.VISIBLE
                    }
                } else if (key.length < 8) {
                    runOnUiThread {
                        textViewError.setText("Key is too short (min 8 chars)")
                        textViewError.visibility = View.VISIBLE
                    }
                } else {
                    encrypted = CxifriMessage().encrypt(msg, key)
                }
            }

            if (encrypted != null) {
                isTextEncrypted = true
                runOnUiThread {
                    message.setText(encrypted)
                }
            }
        }
    }

    private fun onButtonDecrypt(v: View) {
        val msg = message.text.toString()
        val key = password.text.toString()

        textViewError.visibility = View.GONE

        background {
            try {
                val cKey = currentKey
                val decrypted =
                        if (cKey != null)
                            CxifriMessage().decrypt(msg, cKey)
                        else
                            CxifriMessage().decrypt(msg, key)

                if (decrypted != null) {
                    isTextEncrypted = false
                    runOnUiThread {
                        message.setText(decrypted)
                    }
                }
                else
                    runOnUiThread {
                        textViewError.visibility = View.VISIBLE
                        textViewError.setText("Failed to decrypt (wrong key?)")
                    }
            } catch (ex: Exception) {
                runOnUiThread {
                    textViewError.visibility = View.VISIBLE
                    textViewError.setText("Failed to decrypt (wrong key?)")
                }
            }
        }
    }

    private fun copyText() {
        val msg = message.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }
    }

//    private fun onButtonPaste(v: View) {
//        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        val clip = clipboard.getPrimaryClip()
//        if (clip != null) {
//            val item = clip.getItemAt(0)
//            if (item != null)
//                message.setText(item.text)
//        }
//
//        isTextEncrypted = false
//        buttonShare.isEnabled = isTextEncrypted
//    }

    private fun clearForm() {
        message.setText("")
        password.setText("")
        isTextEncrypted = false
    }


    private fun shareText() {
        if (isTextEncrypted) {
            val intent = Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, message.text.toString())
            startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
        }
        else {
            val builder = AlertDialog.Builder(this)
            builder.setIcon(R.drawable.ic_launcher_foreground)
            builder.setTitle("Share un-encrypted text?")

            builder.setPositiveButton(android.R.string.ok) {
                dialog, which ->
                val intent = Intent(android.content.Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(android.content.Intent.EXTRA_TEXT, message.text.toString())
                startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
            }

            return builder.create().show()
        }
    }
}

