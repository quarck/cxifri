package com.github.quarck.kriptileto

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView

class TextViewActivity : Activity() {

    lateinit var textViewAuthStatus: TextView
    lateinit var textViewMessage: TextView
    lateinit var buttonReply: Button
    lateinit var buttonQuote: Button
    lateinit var buttonCopy: Button

    var currentKey: KeyEntry? = null
    var encryptedText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_view)

        textViewAuthStatus = findViewById(R.id.textViewAuthStatus)
        textViewMessage = findViewById(R.id.textViewMessage)
        buttonReply = findViewById(R.id.buttonReply)
        buttonQuote = findViewById(R.id.buttonQuote)
        buttonCopy = findViewById(R.id.buttonCopy)

        buttonCopy.setOnClickListener(this::onButtonCopy)
        buttonReply.setOnClickListener(this::onButtonReply)
        buttonQuote.setOnClickListener(this::onButtonQuote)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleEncryptedTextIntent(text: String) {

        val keys = KeysDatabase(context = this).use { it.keys }

        val unwrapped = UrlWrapper.unwrap(text) ?: text

        var success = false

        for (key in keys) {
            try {
                val decrypted = AESTextMessage.decrypt(unwrapped, key)
                if (decrypted != null) {
                    textViewMessage.setText(decrypted)
                    textViewAuthStatus.setText("Message authenticated with key ${key.name}:")
                    currentKey = key

                    success = true
                }
            }
            catch (ex: Exception) {
            }
        }

        if (!success) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.INTENT_EXTRA_TEXT, text)
            startActivity(intent)
            finish()
        }
    }

    fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                encryptedText = uri.toString()
                handleEncryptedTextIntent(encryptedText)
            }
        }
    }

    private fun onButtonCopy(v: View) {
        val msg = textViewMessage.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun onButtonReply(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.INTENT_EXTRA_KEY_ID, currentKey?.id ?: -1L)
        startActivity(intent)
        finish()
    }

    private fun onButtonQuote(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.INTENT_EXTRA_KEY_ID, currentKey?.id ?: -1L)
        val text =
                textViewMessage.text.toString()
                        .split("(\\n|\\r\\n)".toRegex())
                        .map { "> $it" }
                        .joinToString("\r\n")

        intent.putExtra(MainActivity.INTENT_EXTRA_TEXT, text)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1
    }
}

